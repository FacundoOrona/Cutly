import { useEffect, useRef } from 'react'
import { CircleAlert, LoaderCircle, RefreshCw, X } from 'lucide-react'
import { getOwnerRemuneracionesErrorMessage } from '../api/ownerRemuneracionesApi.js'
import { useOwnerRemuneracion } from '../hooks/useOwnerRemuneraciones.js'

const remunerationLabels = {
  PENDIENTE_DEFINICION_FINANCIERA: 'Pendiente de definición financiera',
  PENDIENTE_REVISION_MANUAL: 'Pendiente de revisión manual',
  CONFIRMADA: 'Confirmada',
  SIN_REMUNERACION: 'Sin remuneración',
}
const paymentLabels = { PENDIENTE: 'Pendiente de pago', PARCIAL: 'Parcialmente pagada', PAGADA: 'Totalmente pagada', NO_PAGABLE: 'No pagable' }
const methodLabels = { EFECTIVO: 'Efectivo', TRANSFERENCIA_QR: 'Transferencia / QR', DEBITO: 'Débito', CREDITO: 'Crédito', OTRO: 'Otro' }
const liquidationLabels = { VIGENTE: 'Vigente', ANULADA: 'Anulada' }

function money(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value))
}

function dateTime(value) {
  if (!value) return '—'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium', timeStyle: 'short' }).format(parsed)
}

export default function OwnerRemuneracionDetailDrawer({ negocioId, remuneracionId, employeeName, onClose }) {
  const query = useOwnerRemuneracion({ negocioId, remuneracionId })
  const closeButtonRef = useRef(null)
  const item = query.data
  useEffect(() => {
    closeButtonRef.current?.focus()
    function handleKeyDown(event) {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])
  return <div className="owner-remuneracion-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className="owner-remuneracion-drawer" role="dialog" aria-modal="true" aria-labelledby="owner-remuneracion-detail-title">
      <header className="owner-remuneracion-drawer-header"><div><span className="owner-remuneracion-overline">DETALLE DE REMUNERACIÓN</span><h2 id="owner-remuneracion-detail-title">{remuneracionId ? `Remuneración #${remuneracionId}` : 'Detalle'}</h2></div><button ref={closeButtonRef} type="button" className="icon-button" onClick={onClose} aria-label="Cerrar detalle"><X size={19} /></button></header>
      <div className="owner-remuneracion-drawer-content">
        {query.isPending ? <div className="owner-remuneraciones-state" role="status"><LoaderCircle size={18} className="spin" /> Cargando detalle…</div>
          : query.isError ? <div className="owner-remuneraciones-state is-error" role="alert"><CircleAlert size={17} /><span>{getOwnerRemuneracionesErrorMessage(query.error)}</span><button type="button" className="owners-secondary-button" onClick={() => void query.refetch()} disabled={query.isFetching}><RefreshCw size={14} /> Reintentar</button></div>
            : item && <>
              <dl className="owner-remuneracion-detail-grid">
                <div><dt>Empleado</dt><dd>{employeeName || `#${item.empleadoId}`}</dd></div>
                <div><dt>Turno</dt><dd>#{item.turnoId ?? '—'}</dd></div>
                <div><dt>Cliente</dt><dd>{item.clienteNombre || '—'}</dd></div>
                <div><dt>Servicio</dt><dd>{item.servicioNombre || `#${item.servicioId}`}</dd></div>
                <div><dt>Fecha y hora del turno</dt><dd>{dateTime(item.fechaHoraInicio)}</dd></div>
                <div><dt>Precio histórico</dt><dd>{money(item.precioAplicado)}</dd></div>
                <div><dt>Porcentaje aplicado</dt><dd>{item.porcentajeAplicado == null ? '—' : `${item.porcentajeAplicado}%`}</dd></div>
                <div><dt>Importe de remuneración</dt><dd>{money(item.importeConfirmado)}</dd></div>
                <div><dt>Importe pagado</dt><dd>{money(item.totalPagado)}</dd></div>
                <div><dt>Saldo pendiente</dt><dd>{money(item.saldoPendiente)}</dd></div>
                <div><dt>Estado de remuneración</dt><dd><span className={`owner-remuneraciones-status state-${String(item.estado || '').toLowerCase()}`}>{remunerationLabels[item.estado] || item.estado || '—'}</span></dd></div>
                <div><dt>Estado de pago</dt><dd><span className={`owner-remuneraciones-status payment-${String(item.estadoPago || '').toLowerCase()}`}>{paymentLabels[item.estadoPago] || item.estadoPago || '—'}</span></dd></div>
              </dl>
              <section className="owner-remuneracion-applications"><div><h3>Aplicaciones de liquidaciones</h3><p>Historial asociado a esta remuneración.</p></div>
                {!item.aplicaciones?.length ? <p className="owner-remuneracion-no-applications">No hay aplicaciones registradas.</p> : <ol>{item.aplicaciones.map((application, index) => <li key={`${application.liquidacionId}-${index}`}>
                  <div className="owner-remuneracion-application-heading"><strong>Liquidación #{application.liquidacionId}</strong><span className={`owner-remuneraciones-status ${application.estadoLiquidacion === 'ANULADA' ? 'state-pendiente_revision_manual' : 'state-confirmada'}`}>{liquidationLabels[application.estadoLiquidacion] || application.estadoLiquidacion || '—'}</span></div>
                  <dl><div><dt>Fecha efectiva</dt><dd>{application.fechaEfectiva || '—'}</dd></div><div><dt>Medio</dt><dd>{methodLabels[application.metodoPago] || application.metodoPago || '—'}</dd></div><div><dt>Importe aplicado</dt><dd>{money(application.importe)}</dd></div></dl>
                </li>)}</ol>}
              </section>
            </>}
      </div>
      <footer className="owner-remuneracion-drawer-footer"><button type="button" className="owners-secondary-button" onClick={onClose}>Cerrar</button></footer>
    </section>
  </div>
}
