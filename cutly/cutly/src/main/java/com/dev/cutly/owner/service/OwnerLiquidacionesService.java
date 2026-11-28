package com.dev.cutly.owner.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.liquidacion.entity.AplicacionLiquidacion;
import com.dev.cutly.liquidacion.entity.LiquidacionEmpleado;
import com.dev.cutly.liquidacion.enums.EstadoLiquidacion;
import com.dev.cutly.liquidacion.repository.AplicacionLiquidacionRepository;
import com.dev.cutly.liquidacion.repository.LiquidacionEmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.owner.dto.liquidacion.*;
import com.dev.cutly.owner.service.OwnerNegocioService;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.remuneracion.entity.RemuneracionEmpleado;
import com.dev.cutly.remuneracion.enums.EstadoRemuneracion;
import com.dev.cutly.remuneracion.repository.RemuneracionEmpleadoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OwnerLiquidacionesService {
    private final OwnerNegocioService ownerNegocio;
    private final NegocioRepository negocioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RemuneracionEmpleadoRepository remuneracionRepository;
    private final LiquidacionEmpleadoRepository liquidacionRepository;
    private final AplicacionLiquidacionRepository aplicacionRepository;

    public OwnerLiquidacionesService(OwnerNegocioService ownerNegocio, NegocioRepository negocioRepository,
            EmpleadoRepository empleadoRepository, UsuarioRepository usuarioRepository,
            RemuneracionEmpleadoRepository remuneracionRepository, LiquidacionEmpleadoRepository liquidacionRepository,
            AplicacionLiquidacionRepository aplicacionRepository) {
        this.ownerNegocio = ownerNegocio; this.negocioRepository = negocioRepository;
        this.empleadoRepository = empleadoRepository; this.usuarioRepository = usuarioRepository;
        this.remuneracionRepository = remuneracionRepository; this.liquidacionRepository = liquidacionRepository;
        this.aplicacionRepository = aplicacionRepository;
    }

    @Transactional
    public LiquidacionEmpleadoDto crear(Long negocioId, CrearLiquidacionEmpleadoRequestDto request,
                                         String clave, String email) {
        if (clave == null || clave.isBlank() || clave.length() > 100)
            throw error(HttpStatus.BAD_REQUEST, "La clave de idempotencia es obligatoria y debe tener hasta 100 caracteres");
        Negocio autorizado = ownerNegocio.obtenerEntidadNegocioPropio(negocioId, email);
        Negocio negocio = negocioRepository.findWithLockingByNegocioIdAndOwner_UsuarioId(negocioId,
                autorizado.getOwner().getUsuarioId()).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No se encontró el negocio solicitado"));
        Usuario owner = owner(negocio, email);
        if (request.aplicaciones() == null || request.aplicaciones().isEmpty()) throw error(HttpStatus.BAD_REQUEST, "La liquidación debe incluir remuneraciones");
        String descripcion = normalizar(request.descripcionMetodo());
        CrearLiquidacionEmpleadoRequestDto normalizada = new CrearLiquidacionEmpleadoRequestDto(
                request.fechaEfectiva(), request.metodoPago(), descripcion, request.observaciones(), request.aplicaciones());
        String hash = hash(normalizada);
        LiquidacionEmpleado previa = liquidacionRepository.findByNegocio_NegocioIdAndClaveIdempotencia(negocioId, clave).orElse(null);
        if (previa != null) {
            if (!hashPersistido(previa).equals(hash)) throw error(HttpStatus.CONFLICT, "La clave de idempotencia ya se usó con otra solicitud");
            return dto(previa, false);
        }
        validarTexto(request);
        List<Long> ids = request.aplicaciones().keySet().stream().sorted().toList();
        List<RemuneracionEmpleado> remuneraciones = remuneracionRepository.bloquearEnOrden(ids);
        if (remuneraciones.size() != ids.size()) throw error(HttpStatus.NOT_FOUND, "No se encontraron todas las remuneraciones solicitadas");
        Long empleadoId = remuneraciones.get(0).getEmpleado().getEmpleadoId();
        BigDecimal total = BigDecimal.ZERO;
        Map<Long,BigDecimal> pagosPrevios = new HashMap<>();
        for (Object[] fila : aplicacionRepository.totalesVigentes(ids)) pagosPrevios.put((Long)fila[0],(BigDecimal)fila[1]);
        for (RemuneracionEmpleado r : remuneraciones) {
            if (!r.getNegocio().getNegocioId().equals(negocioId) || !r.getEmpleado().getEmpleadoId().equals(empleadoId))
                throw error(HttpStatus.BAD_REQUEST, "Todas las remuneraciones deben pertenecer al mismo empleado y negocio");
            if (r.getEstado() != EstadoRemuneracion.CONFIRMADA || r.getMontoCalculado() == null || r.getMontoCalculado().signum() <= 0)
                throw error(HttpStatus.CONFLICT, "Solo se pueden liquidar remuneraciones confirmadas con importe positivo");
            BigDecimal pagado = pagosPrevios.getOrDefault(r.getRemuneracionId(),BigDecimal.ZERO);
            BigDecimal saldo = r.getMontoCalculado().subtract(pagado);
            BigDecimal importe = request.aplicaciones().get(r.getRemuneracionId());
            if (importe == null || importe.signum() <= 0 || importe.scale() > 2 || importe.compareTo(saldo) > 0)
                throw error(HttpStatus.CONFLICT, "El importe aplicado debe ser positivo y no superar el saldo pendiente");
            total = total.add(importe);
        }
        Empleado empleado = empleadoRepository.findByEmpleadoIdAndNegocio_NegocioId(empleadoId, negocioId)
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "La relación entre empleado y negocio no es válida"));
        LiquidacionEmpleado liquidacion = new LiquidacionEmpleado(negocio, empleado, owner,
                request.fechaEfectiva(), request.metodoPago(), descripcion,
                request.observaciones(), total, clave, hash);
        for (RemuneracionEmpleado r : remuneraciones) liquidacion.agregar(new AplicacionLiquidacion(
                liquidacion, r, request.aplicaciones().get(r.getRemuneracionId())));
        BigDecimal sumaAplicaciones = liquidacion.getAplicaciones().stream().map(AplicacionLiquidacion::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(sumaAplicaciones) != 0) throw new IllegalStateException("El total debe coincidir con las aplicaciones validadas");
        liquidacionRepository.saveAndFlush(liquidacion);
        return dto(liquidacion, false);
    }

    @Transactional
    public LiquidacionEmpleadoDto anular(Long negocioId, Long id, String motivo, String email) {
        Negocio n = ownerNegocio.obtenerEntidadNegocioPropio(negocioId, email);
        negocioRepository.findWithLockingByNegocioIdAndOwner_UsuarioId(negocioId, n.getOwner().getUsuarioId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No se encontró el negocio solicitado"));
        LiquidacionEmpleado l = liquidacionRepository.findByLiquidacionIdAndNegocio_NegocioId(id, negocioId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No se encontró la liquidación solicitada"));
        if (motivo == null || motivo.isBlank()) throw error(HttpStatus.BAD_REQUEST, "El motivo de anulación es obligatorio");
        Usuario responsable = owner(n, email);
        if (l.getEstado() == EstadoLiquidacion.ANULADA) throw error(HttpStatus.CONFLICT, "La liquidación ya fue anulada");
        l.anular(motivo.trim(), responsable);
        liquidacionRepository.flush();
        return dto(l, false);
    }

    @Transactional(readOnly=true)
    public Page<LiquidacionEmpleadoDto> historial(Long negocioId, Long empleadoId, int page, int size, String email) {
        Negocio n = ownerNegocio.obtenerEntidadNegocioPropio(negocioId, email);
        validarPagina(page,size);
        Pageable p = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "fechaRegistro").and(Sort.by(Sort.Direction.DESC, "liquidacionId")));
        Page<LiquidacionEmpleado> data = empleadoId == null ? liquidacionRepository.findByNegocio_NegocioId(negocioId, p)
                : liquidacionRepository.findByNegocio_NegocioIdAndEmpleado_EmpleadoId(negocioId, empleadoId, p);
        List<Long> liquidacionIds = data.getContent().stream().map(LiquidacionEmpleado::getLiquidacionId).toList();
        List<AplicacionLiquidacion> aplicaciones = liquidacionIds.isEmpty() ? List.of()
                : aplicacionRepository.findByLiquidacion_LiquidacionIdInOrderByAplicacionLiquidacionId(liquidacionIds);
        Map<Long,List<AplicacionLiquidacion>> porLiquidacion = aplicaciones.stream().collect(Collectors.groupingBy(
                a -> a.getLiquidacion().getLiquidacionId()));
        List<Long> remuneracionIds = aplicaciones.stream().map(a -> a.getRemuneracion().getRemuneracionId()).distinct().toList();
        Map<Long,BigDecimal> pagos = totalesPagados(remuneracionIds);
        return data.map(l -> dto(l, porLiquidacion.getOrDefault(l.getLiquidacionId(), List.of()), pagos));
    }

    @Transactional(readOnly=true)
    public LiquidacionEmpleadoDto detalleLiquidacion(Long negocioId, Long id, String email) {
        ownerNegocio.obtenerEntidadNegocioPropio(negocioId, email);
        LiquidacionEmpleado liquidacion=liquidacionRepository.findByLiquidacionIdAndNegocio_NegocioId(id,negocioId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND,"No se encontró la liquidación solicitada"));
        return dto(liquidacion,false);
    }

    @Transactional(readOnly=true)
    public Page<RemuneracionPagoDto> remuneraciones(Long negocioId, Long empleadoId, EstadoRemuneracion estado,
            LocalDate desde, LocalDate hasta, int page, int size, String email) {
        ownerNegocio.obtenerEntidadNegocioPropio(negocioId, email);
        validarPagina(page,size);
        if (desde != null && hasta != null && hasta.isBefore(desde)) throw error(HttpStatus.BAD_REQUEST, "El período es inválido");
        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay();
        Page<RemuneracionEmpleado> rs = remuneracionRepository.listar(negocioId, empleadoId, estado, inicio, fin,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "turno.fechaHoraInicio").and(Sort.by("remuneracionId"))));
        List<Long> ids=rs.getContent().stream().map(RemuneracionEmpleado::getRemuneracionId).toList();
        Map<Long,BigDecimal> pagos=new HashMap<>();
        if (!ids.isEmpty()) for (Object[] fila:aplicacionRepository.totalesVigentes(ids)) pagos.put((Long)fila[0],(BigDecimal)fila[1]);
        return rs.map(r -> remuneracionDto(r, pagos.getOrDefault(r.getRemuneracionId(),BigDecimal.ZERO),false));
    }

    @Transactional(readOnly=true)
    public RemuneracionPagoDto detalleRemuneracion(Long negocioId, Long id, String email) {
        ownerNegocio.obtenerEntidadNegocioPropio(negocioId, email);
        RemuneracionEmpleado r = remuneracionRepository.findByRemuneracionIdAndNegocio_NegocioId(id, negocioId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No se encontró la remuneración solicitada"));
        return remuneracionDto(r, aplicacionRepository.totalVigente(id),true);
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> resumen(Long negocioId, String email) {
        ownerNegocio.obtenerEntidadNegocioPropio(negocioId, email);
        Map<Long,List<Object[]>> porEmpleado = remuneracionRepository.resumenPorEmpleado(negocioId).stream()
                .collect(Collectors.groupingBy(f -> (Long) f[0]));
        return empleadoRepository.findByNegocio_NegocioIdOrderByEmpleadoIdAsc(negocioId).stream().map(e -> {
            List<Object[]> remuneracionesEmpleado=porEmpleado.getOrDefault(e.getEmpleadoId(),List.of());
            BigDecimal confirmado=BigDecimal.ZERO, saldo=BigDecimal.ZERO;
            int pendientes=0, parciales=0, completos=0, revision=0, definicion=0;
            BigDecimal pago = BigDecimal.ZERO;
            for (Object[] fila : remuneracionesEmpleado) {
                EstadoRemuneracion estado = (EstadoRemuneracion) fila[1];
                BigDecimal monto = (BigDecimal) fila[2];
                BigDecimal pagado = (BigDecimal) fila[3];
                pago = pago.add(pagado);
                if (estado==EstadoRemuneracion.PENDIENTE_REVISION_MANUAL) revision++;
                if (estado==EstadoRemuneracion.PENDIENTE_DEFINICION_FINANCIERA) definicion++;
                if (estado!=EstadoRemuneracion.CONFIRMADA || monto==null || monto.signum()<=0) continue;
                BigDecimal pendiente=monto.subtract(pagado).max(BigDecimal.ZERO);
                confirmado=confirmado.add(monto); saldo=saldo.add(pendiente);
                if (pagado.signum()==0) pendientes++; else if (pendiente.signum()>0) parciales++; else completos++;
            }
            return Map.<String,Object>of("empleadoId",e.getEmpleadoId(),"nombre",e.getUsuario().getNombre()+" "+e.getUsuario().getApellido(),
                    "totalConfirmado",confirmado,"totalPagado",pago,"totalPendiente",saldo,"pendientes",pendientes,
                    "parcialmentePagadas",parciales,"totalmentePagadas",completos,"pendientesRevisionManual",revision,"pendientesDefinicionFinanciera",definicion);
        }).toList();
    }

    private RemuneracionPagoDto remuneracionDto(RemuneracionEmpleado r, BigDecimal pagado, boolean incluirHistorial) {
        BigDecimal monto = r.getEstado()==EstadoRemuneracion.CONFIRMADA ? r.getMontoCalculado() : null;
        BigDecimal saldo = monto == null ? null : monto.subtract(pagado);
        String estado = monto == null || monto.signum()==0 ? "NO_PAGABLE" : pagado.signum()==0 ? "PENDIENTE" : saldo.signum()<=0 ? "PAGADA" : "PARCIAL";
        var turno=r.getTurno();
        String cliente=turno.getCliente()!=null ? turno.getCliente().getNombre()+" "+turno.getCliente().getApellido()
                : turno.getClienteLocal()!=null ? turno.getClienteLocal().getNombre()+" "+turno.getClienteLocal().getApellido() : null;
        List<AplicacionLiquidacionDto> historial=incluirHistorial ? aplicacionRepository.findByRemuneracion_RemuneracionIdOrderByAplicacionLiquidacionId(
                r.getRemuneracionId()).stream().map(a -> new AplicacionLiquidacionDto(r.getRemuneracionId(),turno.getTurnoId(),a.getImporte(),
                pagado, saldo,a.getLiquidacion().getLiquidacionId(),
                a.getLiquidacion().getFechaEfectiva(),a.getLiquidacion().getMetodoPago(),a.getLiquidacion().getEstado())).toList() : List.of();
        return new RemuneracionPagoDto(r.getRemuneracionId(),r.getEmpleado().getEmpleadoId(),turno.getTurnoId(),
                turno.getServicio().getServicioId(),cliente,turno.getServicio().getNombre(),turno.getFechaHoraInicio(),r.getEstado(),r.getPrecioAplicado(),
                r.getPorcentajeAplicado(),monto,pagado,saldo,estado,historial);
    }

    private LiquidacionEmpleadoDto dto(LiquidacionEmpleado l, boolean ignored) {
        List<com.dev.cutly.liquidacion.entity.AplicacionLiquidacion> filas=aplicacionRepository.findByLiquidacion_LiquidacionIdOrderByAplicacionLiquidacionId(l.getLiquidacionId());
        List<Long> ids=filas.stream().map(a -> a.getRemuneracion().getRemuneracionId()).toList();
        return dto(l, filas, totalesPagados(ids));
    }

    private LiquidacionEmpleadoDto dto(LiquidacionEmpleado l, List<AplicacionLiquidacion> filas, Map<Long,BigDecimal> pagos) {
        List<AplicacionLiquidacionDto> aplicaciones = filas.stream().map(a -> {
            BigDecimal pagado=pagos.getOrDefault(a.getRemuneracion().getRemuneracionId(),BigDecimal.ZERO);
            BigDecimal saldo=a.getRemuneracion().getMontoCalculado().subtract(pagado);
            return new AplicacionLiquidacionDto(a.getRemuneracion().getRemuneracionId(),a.getRemuneracion().getTurno().getTurnoId(),a.getImporte(),pagado,saldo,
                    l.getLiquidacionId(),l.getFechaEfectiva(),l.getMetodoPago(),l.getEstado());
        }).toList();
        return new LiquidacionEmpleadoDto(l.getLiquidacionId(),l.getEmpleado().getEmpleadoId(),l.getEmpleado().getUsuario().getNombre()+" "+l.getEmpleado().getUsuario().getApellido(),
                l.getFechaEfectiva(),l.getFechaRegistro(),l.getMetodoPago(),l.getDescripcionMetodo(),l.getObservaciones(),l.getImporteTotal(),l.getEstado(),l.getMotivoAnulacion(),l.getFechaAnulacion(),
                l.getOwnerRegistrante().getEmail(),l.getOwnerAnulador()==null?null:l.getOwnerAnulador().getEmail(),aplicaciones);
    }

    private Map<Long,BigDecimal> totalesPagados(List<Long> ids) {
        Map<Long,BigDecimal> pagos = new HashMap<>();
        if (!ids.isEmpty()) for (Object[] fila : aplicacionRepository.totalesVigentes(ids)) pagos.put((Long) fila[0], (BigDecimal) fila[1]);
        return pagos;
    }

    private void validarTexto(CrearLiquidacionEmpleadoRequestDto request) {
        String descripcion = request.descripcionMetodo();
        String observaciones = request.observaciones();
        if (descripcion != null && descripcion.length() > 100) throw error(HttpStatus.BAD_REQUEST, "La descripción del medio de pago admite hasta 100 caracteres");
        if (observaciones != null && observaciones.length() > 500) throw error(HttpStatus.BAD_REQUEST, "Las observaciones admiten hasta 500 caracteres");
        if (request.metodoPago() == null) throw error(HttpStatus.BAD_REQUEST, "El medio de pago es obligatorio");
        if (request.metodoPago() == MetodoPago.OTRO && normalizar(descripcion) == null)
            throw error(HttpStatus.BAD_REQUEST, "Debe describir el medio de pago OTRO");
        if (request.metodoPago() != MetodoPago.OTRO && normalizar(descripcion) != null)
            throw error(HttpStatus.BAD_REQUEST, "La descripción solo corresponde al medio de pago OTRO");
    }

    private Usuario owner(Negocio n, String email) {
        Usuario u=usuarioRepository.findByEmail(email).filter(x -> x.getRol()==Rol.OWNER && x.getUsuarioId().equals(n.getOwner().getUsuarioId()))
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN,"La cuenta autenticada no es OWNER de este negocio"));
        return u;
    }
    String hash(CrearLiquidacionEmpleadoRequestDto r) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            writeString(out, r.fechaEfectiva().toString());
            writeString(out, r.metodoPago().name());
            writeString(out, normalizar(r.descripcionMetodo()));
            writeString(out, r.observaciones());
            TreeMap<Long,BigDecimal> ordenadas = new TreeMap<>(r.aplicaciones());
            out.writeInt(ordenadas.size());
            for (var entry : ordenadas.entrySet()) {
                out.writeLong(entry.getKey());
                writeString(out, entry.getValue().stripTrailingZeros().toPlainString());
            }
            out.flush();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private String hashPersistido(LiquidacionEmpleado l) {
        Map<Long,BigDecimal> aplicaciones = aplicacionRepository.findByLiquidacion_LiquidacionIdOrderByAplicacionLiquidacionId(l.getLiquidacionId())
                .stream().collect(Collectors.toMap(a -> a.getRemuneracion().getRemuneracionId(), AplicacionLiquidacion::getImporte));
        CrearLiquidacionEmpleadoRequestDto persistida = new CrearLiquidacionEmpleadoRequestDto(
                l.getFechaEfectiva(), l.getMetodoPago(), l.getDescripcionMetodo(), l.getObservaciones(), aplicaciones);
        return hash(persistida);
    }

    private void writeString(DataOutputStream out, String value) throws IOException {
        if (value == null) { out.writeInt(-1); return; }
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(encoded.length);
        out.write(encoded);
    }
    private String normalizar(String s) { return s==null||s.isBlank()?null:s.trim(); }
    private void validarPagina(int page,int size) {
        if (page<0 || size<1) throw error(HttpStatus.BAD_REQUEST,"La página debe ser no negativa y el tamaño debe ser positivo");
    }
    private ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
