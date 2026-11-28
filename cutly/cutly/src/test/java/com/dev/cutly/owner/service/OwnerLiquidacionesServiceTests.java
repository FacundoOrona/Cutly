package com.dev.cutly.owner.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.liquidacion.entity.LiquidacionEmpleado;
import com.dev.cutly.liquidacion.entity.AplicacionLiquidacion;
import com.dev.cutly.liquidacion.repository.AplicacionLiquidacionRepository;
import com.dev.cutly.liquidacion.repository.LiquidacionEmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.owner.dto.liquidacion.CrearLiquidacionEmpleadoRequestDto;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.remuneracion.entity.RemuneracionEmpleado;
import com.dev.cutly.remuneracion.enums.EstadoRemuneracion;
import com.dev.cutly.remuneracion.repository.RemuneracionEmpleadoRepository;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyCollection;

@ExtendWith(MockitoExtension.class)
class OwnerLiquidacionesServiceTests {
    @Mock private OwnerNegocioService ownerNegocio;
    @Mock private NegocioRepository negocioRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RemuneracionEmpleadoRepository remuneracionRepository;
    @Mock private LiquidacionEmpleadoRepository liquidacionRepository;
    @Mock private AplicacionLiquidacionRepository aplicacionRepository;
    private OwnerLiquidacionesService service;
    private Negocio negocio;
    private Usuario owner;

    @BeforeEach void setup() {
        service=new OwnerLiquidacionesService(ownerNegocio,negocioRepository,empleadoRepository,usuarioRepository,
                remuneracionRepository,liquidacionRepository,aplicacionRepository);
        owner=new Usuario(); owner.setUsuarioId(7L); owner.setNombre("Ana"); owner.setApellido("Owner");
        owner.setEmail("owner@test.local"); owner.setRol(Rol.OWNER);
        negocio=new Negocio(); negocio.setNegocioId(5L); negocio.setOwner(owner);
        lenient().when(ownerNegocio.obtenerEntidadNegocioPropio(5L,owner.getEmail())).thenReturn(negocio);
        lenient().when(negocioRepository.findWithLockingByNegocioIdAndOwner_UsuarioId(5L,7L)).thenReturn(Optional.of(negocio));
        lenient().when(usuarioRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
    }

    @Test void mismaClaveYContenidoDevuelveLaLiquidacionPersistidaSinCrearOtra() throws Exception {
        var request=request();
        RemuneracionEmpleado remuneracion = remuneracion(new BigDecimal("100.00"));
        LiquidacionEmpleado previa=new LiquidacionEmpleado(negocio,remuneracion.getEmpleado(),owner,request.fechaEfectiva(),
                request.metodoPago(),null,null,new BigDecimal("25.00"),"key-1","legacy-hash");
        AplicacionLiquidacion aplicacion = new AplicacionLiquidacion(previa,remuneracion,new BigDecimal("25.00"));
        previa.agregar(aplicacion);
        field(previa,"liquidacionId",22L);
        when(liquidacionRepository.findByNegocio_NegocioIdAndClaveIdempotencia(5L,"key-1")).thenReturn(Optional.of(previa));
        when(aplicacionRepository.findByLiquidacion_LiquidacionIdOrderByAplicacionLiquidacionId(22L)).thenReturn(java.util.List.of(aplicacion));
        when(aplicacionRepository.totalesVigentes(anyCollection())).thenReturn(java.util.List.<Object[]>of(new Object[]{10L,new BigDecimal("25.00")}));

        var result=service.crear(5L,request,"key-1",owner.getEmail());

        assertEquals(new BigDecimal("25.00"),result.importeTotal());
        verify(liquidacionRepository,never()).saveAndFlush(any());
        verifyNoInteractions(remuneracionRepository);
    }

    @Test void rechazaReutilizarClaveConContenidoDiferente() {
        when(liquidacionRepository.findByNegocio_NegocioIdAndClaveIdempotencia(5L,"key-1"))
                .thenReturn(Optional.of(new LiquidacionEmpleado(negocio,empleado(),owner,LocalDate.now(),
                        MetodoPago.EFECTIVO,null,null,new BigDecimal("25.00"),"key-1","otro-hash")));

        var error=assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.crear(5L,request(),"key-1",owner.getEmail()));

        assertEquals(409,error.getStatusCode().value());
        verify(remuneracionRepository,never()).bloquearEnOrden(any());
    }

    @Test void registraPagoParcialConTotalIgualALaAplicacion() throws Exception {
        var request=request();
        RemuneracionEmpleado remuneracion=remuneracion(new BigDecimal("100.00"));
        when(liquidacionRepository.findByNegocio_NegocioIdAndClaveIdempotencia(5L,"key-1")).thenReturn(Optional.empty());
        when(remuneracionRepository.bloquearEnOrden(java.util.List.of(10L))).thenReturn(java.util.List.of(remuneracion));
        when(aplicacionRepository.totalesVigentes(anyCollection())).thenReturn(java.util.List.of());
        when(empleadoRepository.findByEmpleadoIdAndNegocio_NegocioId(4L,5L)).thenReturn(Optional.of(remuneracion.getEmpleado()));
        when(aplicacionRepository.findByLiquidacion_LiquidacionIdOrderByAplicacionLiquidacionId(null)).thenReturn(java.util.List.of());

        var result=service.crear(5L,request,"key-1",owner.getEmail());

        assertEquals(new BigDecimal("25.00"),result.importeTotal());
        assertEquals(com.dev.cutly.liquidacion.enums.EstadoLiquidacion.VIGENTE,result.estado());
        var captor=org.mockito.ArgumentCaptor.forClass(LiquidacionEmpleado.class);
        verify(liquidacionRepository).saveAndFlush(captor.capture());
        assertEquals(1,captor.getValue().getAplicaciones().size());
        assertEquals(new BigDecimal("25.00"),captor.getValue().getAplicaciones().get(0).getImporte());
    }

    @Test void noPermiteAplicarMasQueElSaldo() throws Exception {
        RemuneracionEmpleado remuneracion=remuneracion(new BigDecimal("100.00"));
        when(liquidacionRepository.findByNegocio_NegocioIdAndClaveIdempotencia(5L,"key-1")).thenReturn(Optional.empty());
        when(remuneracionRepository.bloquearEnOrden(java.util.List.of(10L))).thenReturn(java.util.List.of(remuneracion));
        when(aplicacionRepository.totalesVigentes(anyCollection())).thenReturn(java.util.List.<Object[]>of(new Object[]{10L,new BigDecimal("80.00")}));

        var error=assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.crear(5L,request(),"key-1",owner.getEmail()));

        assertEquals(409,error.getStatusCode().value());
        verify(liquidacionRepository,never()).saveAndFlush(any());
    }

    @Test void hashEsIndependienteDelOrdenYEscalaDecimalDeAplicaciones() {
        var first = new CrearLiquidacionEmpleadoRequestDto(LocalDate.of(2026, 1, 10), MetodoPago.EFECTIVO,
                null, "nota", new LinkedHashMap<>(Map.of(10L, new BigDecimal("25.0"), 11L, new BigDecimal("5.00"))));
        var reversed = new LinkedHashMap<Long,BigDecimal>();
        reversed.put(11L, new BigDecimal("5")); reversed.put(10L, new BigDecimal("25.00"));
        var second = new CrearLiquidacionEmpleadoRequestDto(first.fechaEfectiva(), first.metodoPago(), "   ", "nota", reversed);
        assertEquals(service.hash(first), service.hash(second));
    }

    @Test void hashDistingueSeparadoresDentroDeCampos() {
        var first = new CrearLiquidacionEmpleadoRequestDto(LocalDate.of(2026, 1, 10), MetodoPago.OTRO,
                "efectivo|manual", "transferencia", Map.of(10L, new BigDecimal("25.00")));
        var second = new CrearLiquidacionEmpleadoRequestDto(first.fechaEfectiva(), first.metodoPago(),
                "efectivo", "manual|transferencia", first.aplicaciones());
        assertNotEquals(service.hash(first), service.hash(second));
    }

    @Test void hashDistingueSolicitudesQueColisionabanConLaConcatenacionAnterior() {
        var first = new CrearLiquidacionEmpleadoRequestDto(LocalDate.of(2026, 1, 10), MetodoPago.OTRO,
                "x", "y|z", Map.of(10L, new BigDecimal("25.00")));
        var second = new CrearLiquidacionEmpleadoRequestDto(first.fechaEfectiva(), first.metodoPago(),
                "x|y", "z", first.aplicaciones());
        assertNotEquals(service.hash(first), service.hash(second));
    }

    @Test void normalizaDescripcionEquivalenteParaMetodoOtro() {
        var first = new CrearLiquidacionEmpleadoRequestDto(LocalDate.of(2026, 1, 10), MetodoPago.OTRO,
                " Transferencia ", "nota", Map.of(10L, new BigDecimal("25.00")));
        var second = new CrearLiquidacionEmpleadoRequestDto(first.fechaEfectiva(), first.metodoPago(),
                "Transferencia", "nota", first.aplicaciones());
        assertEquals(service.hash(first), service.hash(second));
    }

    @Test void resumenCalculaIndicadoresPorRemuneracionSinDuplicarAgregaciones() {
        Empleado empleado = empleado();
        empleado.setActivo(false);
        when(remuneracionRepository.resumenPorEmpleado(5L)).thenReturn(List.<Object[]>of(
                resumenRow(EstadoRemuneracion.CONFIRMADA, "100.00", "0.00"),
                resumenRow(EstadoRemuneracion.CONFIRMADA, "80.00", "30.00"),
                resumenRow(EstadoRemuneracion.CONFIRMADA, "120.00", "50.00"),
                resumenRow(EstadoRemuneracion.CONFIRMADA, "40.00", "40.00"),
                resumenRow(EstadoRemuneracion.CONFIRMADA, "10.00", "15.00"),
                resumenRow(EstadoRemuneracion.PENDIENTE_REVISION_MANUAL, null, "0.00"),
                resumenRow(EstadoRemuneracion.PENDIENTE_DEFINICION_FINANCIERA, null, "0.00"),
                resumenRow(EstadoRemuneracion.SIN_REMUNERACION, null, "0.00")));
        when(empleadoRepository.findByNegocio_NegocioIdOrderByEmpleadoIdAsc(5L)).thenReturn(List.of(empleado));

        @SuppressWarnings("unchecked") Map<String, Object> row = (Map<String, Object>) service.resumen(5L, owner.getEmail()).get(0);

        assertEquals(new BigDecimal("350.00"), row.get("totalConfirmado"));
        assertEquals(new BigDecimal("135.00"), row.get("totalPagado"));
        assertEquals(new BigDecimal("220.00"), row.get("totalPendiente"));
        assertEquals(1, row.get("pendientes"));
        assertEquals(2, row.get("parcialmentePagadas"));
        assertEquals(2, row.get("totalmentePagadas"));
        assertEquals(1, row.get("pendientesRevisionManual"));
        assertEquals(1, row.get("pendientesDefinicionFinanciera"));
        assertEquals(5, (int) row.get("pendientes") + (int) row.get("parcialmentePagadas") + (int) row.get("totalmentePagadas"));
        assertFalse(empleado.isActivo());
        verify(remuneracionRepository).resumenPorEmpleado(5L);
    }

    @Test void resumenNoPermiteSaldoPendienteNegativoPorSobreaplicacion() {
        Empleado empleado = empleado();
        when(remuneracionRepository.resumenPorEmpleado(5L)).thenReturn(List.<Object[]>of(
                resumenRow(EstadoRemuneracion.CONFIRMADA, "100.00", "125.00")));
        when(empleadoRepository.findByNegocio_NegocioIdOrderByEmpleadoIdAsc(5L)).thenReturn(List.of(empleado));

        @SuppressWarnings("unchecked") Map<String, Object> row = (Map<String, Object>) service.resumen(5L, owner.getEmail()).get(0);

        assertEquals(BigDecimal.ZERO, row.get("totalPendiente"));
        assertEquals(1, row.get("totalmentePagadas"));
        assertEquals(new BigDecimal("125.00"), row.get("totalPagado"));
    }

    private Object[] resumenRow(EstadoRemuneracion estado, String monto, String pagado) {
        return new Object[]{4L, estado, monto == null ? null : new BigDecimal(monto), new BigDecimal(pagado)};
    }

    @Test void validaLongitudesDeTextoEnLosLimitesExactos() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var exacto = new CrearLiquidacionEmpleadoRequestDto(LocalDate.now(), MetodoPago.OTRO,
                "d".repeat(100), "o".repeat(500), Map.of(1L, BigDecimal.ONE));
        var excedido = new CrearLiquidacionEmpleadoRequestDto(LocalDate.now(), MetodoPago.OTRO,
                "d".repeat(101), "o".repeat(501), Map.of(1L, BigDecimal.ONE));
        assertTrue(validator.validate(exacto).isEmpty());
        assertEquals(2, validator.validate(excedido).size());
    }

    @Test void descripcionVaciaSoloSeAceptaCuandoMetodoNoEsOtro() {
        when(liquidacionRepository.findByNegocio_NegocioIdAndClaveIdempotencia(5L,"key-1")).thenReturn(Optional.empty());
        var otroVacio = new CrearLiquidacionEmpleadoRequestDto(LocalDate.now(), MetodoPago.OTRO,
                "   ", null, Map.of(10L, BigDecimal.ONE));
        var ordinarioDescrito = new CrearLiquidacionEmpleadoRequestDto(LocalDate.now(), MetodoPago.EFECTIVO,
                "caja", null, Map.of(10L, BigDecimal.ONE));
        assertEquals(400, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.crear(5L, otroVacio, "key-1", owner.getEmail())).getStatusCode().value());
        assertEquals(400, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.crear(5L, ordinarioDescrito, "key-1", owner.getEmail())).getStatusCode().value());
        verifyNoInteractions(remuneracionRepository);
    }

    private CrearLiquidacionEmpleadoRequestDto request() {
        return new CrearLiquidacionEmpleadoRequestDto(LocalDate.of(2026,1,10),MetodoPago.EFECTIVO,null,null,
                Map.of(10L,new BigDecimal("25.00")));
    }
    private Empleado empleado() {
        Usuario usuario=new Usuario(); usuario.setNombre("Eva"); usuario.setApellido("Employee");
        Empleado empleado=new Empleado(); empleado.setEmpleadoId(4L); empleado.setUsuario(usuario); empleado.setNegocio(negocio);
        return empleado;
    }
    private RemuneracionEmpleado remuneracion(BigDecimal importe) throws Exception {
        Empleado empleado=empleado();
        Servicio servicio=new Servicio(); servicio.setServicioId(2L); servicio.setNombre("Corte");
        Turno turno=new Turno(); turno.setTurnoId(3L); turno.setServicio(servicio); turno.setFechaHoraInicio(LocalDate.of(2026,1,9).atTime(10,0));
        RemuneracionEmpleado remuneracion=new RemuneracionEmpleado();
        field(remuneracion,"remuneracionId",10L); field(remuneracion,"negocio",negocio); field(remuneracion,"empleado",empleado);
        field(remuneracion,"turno",turno); field(remuneracion,"estado",EstadoRemuneracion.CONFIRMADA); field(remuneracion,"montoCalculado",importe);
        return remuneracion;
    }
    private void field(Object target,String name,Object value) throws Exception {
        var field=target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target,value);
    }
}
