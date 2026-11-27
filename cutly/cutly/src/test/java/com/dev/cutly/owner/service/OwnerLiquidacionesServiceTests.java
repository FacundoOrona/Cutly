package com.dev.cutly.owner.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.liquidacion.entity.LiquidacionEmpleado;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
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
        when(ownerNegocio.obtenerEntidadNegocioPropio(5L,owner.getEmail())).thenReturn(negocio);
        when(negocioRepository.findWithLockingByNegocioIdAndOwner_UsuarioId(5L,7L)).thenReturn(Optional.of(negocio));
        when(usuarioRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
    }

    @Test void mismaClaveYContenidoDevuelveLaLiquidacionPersistidaSinCrearOtra() throws Exception {
        var request=request();
        LiquidacionEmpleado previa=new LiquidacionEmpleado(negocio,empleado(),owner,request.fechaEfectiva(),
                request.metodoPago(),null,null,new BigDecimal("25.00"),"key-1",hash(request));
        when(liquidacionRepository.findByNegocio_NegocioIdAndClaveIdempotencia(5L,"key-1")).thenReturn(Optional.of(previa));
        when(aplicacionRepository.findByLiquidacion_LiquidacionIdOrderByAplicacionLiquidacionId(null)).thenReturn(java.util.List.of());

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
        var field=RemuneracionEmpleado.class.getDeclaredField(name); field.setAccessible(true); field.set(target,value);
    }
    private String hash(CrearLiquidacionEmpleadoRequestDto request) throws Exception {
        String body=request.fechaEfectiva()+"|"+request.metodoPago()+"|"+request.descripcionMetodo()+"|"+request.observaciones()+"|"+new java.util.TreeMap<>(request.aplicaciones());
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
    }
}
