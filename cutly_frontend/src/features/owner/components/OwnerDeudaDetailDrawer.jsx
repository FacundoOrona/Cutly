import { useEffect, useRef } from 'react'
import { Banknote, CalendarDays, CircleAlert, Clock3, LoaderCircle, RefreshCw, X } from 'lucide-react'
import { useOwnerDeuda } from '../hooks/useOwnerDeudas.js'
import OwnerAbonoForm from './OwnerAbonoForm.jsx'

const statusLabels = { PENDIENTE: 'Pendiente', PARCIAL: 'Parcial', PAGADA: 'Pagada' }
const paymentMethodLabels = {
  EFECTIVO: 'Efectivo', TRANSFERENCIA_QR: 'Transferencia / QR', DEBITO: 'Débito', CREDITO: 'Crédito', OTRO: 'Otro',
}

function money(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value))
}

function dateTime(value) {
  if (!value) return '—'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'full', timeStyle: 'short' }).format(parsed)
}

function getErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 404) return 'No se encontró esta deuda. Puede que ya no esté disponible.'
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a esta deuda.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || 'No pudimos cargar los detalles de la deuda.'
}

export default function OwnerDeudaDetailDrawer({ negocioId, deudaId, abonoMutation, onClose, onAbonar }) {
  const query = useOwnerDeuda({ negocioId, deudaId })
  const closeButtonRef = useRef(null)
  const headingId = `owner-deuda-detail-title-${negocioId}-${deudaId}`

  useEffect(() => {
    closeButtonRef.current?.focus()
    function handleKeyDown(event) {
      if (event.key === 'Escape' && !abonoMutation.isPending) onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose, abonoMutation.isPending])

  const deuda = query.data
  const clientName = [deuda?.nombreCliente, deuda?.apellidoCliente].filter(Boolean).join(' ') || 'Cliente'
  const abonos = Array.isArray(deuda?.abonos) ? deuda.abonos : []
  const saldo = Number(deuda?.saldo ?? 0)
  const isPaid = deuda?.estado === 'PAGADA' || saldo <= 0

  return <div className="owner-turno-drawer-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !abonoMutation.isPending) onClose() }}>
    <aside className="owner-turno-drawer owner-deuda-drawer" role="dialog" aria-modal="true" aria-labelledby={headingId}>
      <header className="owner-turno-drawer-header">
        <div><span className="eyebrow"><span className="eyebrow-dot" /> CUENTA CORRIENTE</span><h2 id={headingId}>Detalle de deuda</h2></div>
        <button ref={closeButtonRef} type="button" className="icon-button" onClick={onClose} disabled={abonoMutation.isPending} aria-label="Cerrar detalle de la deuda"><X size={19} /></button>
      </header>
      {query.isPending ? <div className="owner-turno-drawer-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando detalle de la deuda…</div>
        : query.isError ? <div className="owner-turno-drawer-state owner-turno-drawer-error" role="alert"><CircleAlert size={19} /><p>{getErrorMessage(query.error)}</p>{![400, 401, 403, 404].includes(query.error.response?.status) && <button type="button" className="owners-secondary-button" onClick={() => query.refetch()}><RefreshCw size={14} /> Reintentar</button>}</div>
          : !deuda ? <div className="owner-turno-drawer-state" role="status">No hay datos disponibles para esta deuda.</div>
            : <div className="owner-turno-drawer-content owner-deuda-drawer-content">
              <section className="owner-turno-detail-section owner-deuda-client-section"><h3>Cliente</h3><p>{clientName}</p><small>Turno #{deuda.turnoId ?? '—'}</small><div className="owner-deuda-origin"><CalendarDays size={15} /><span>Origen: {dateTime(deuda.fechaOrigen)}</span></div></section>

              <section className="owner-turno-detail-section owner-deuda-summary"><h3>Resumen financiero</h3><dl>
                <div><dt>Importe original/acordado</dt><dd>{money(deuda.montoAcordado ?? deuda.montoOriginal)}</dd></div>
                <div><dt>Total abonado</dt><dd>{money(deuda.totalAbonado)}</dd></div>
                <div className="is-balance"><dt>Saldo pendiente</dt><dd>{money(deuda.saldo)}</dd></div>
                <div className="owner-deuda-summary-status"><dt>Estado</dt><dd><span className={`owner-status owner-deuda-status owner-deuda-status-${String(deuda.estado || '').toLowerCase()}`}>{statusLabels[deuda.estado] || deuda.estado || '—'}</span></dd></div>
              </dl></section>

              <section className="owner-turno-detail-section owner-deuda-payments"><div className="owner-deuda-section-heading"><h3>Historial de abonos</h3><span>{abonos.length}</span></div>
                {abonos.length === 0 ? <div className="owner-deuda-empty-payments"><Banknote size={18} /><p>Todavía no se registraron abonos.</p></div>
                  : <ol className="owner-deuda-payment-list">{abonos.map((abono) => <li key={abono.pagoId} className="owner-deuda-payment"><span className="owner-deuda-payment-icon"><Banknote size={15} /></span><div className="owner-deuda-payment-main"><strong>{money(abono.monto)}</strong><span>{paymentMethodLabels[abono.metodoPago] || abono.metodoPago || 'Método no disponible'}{abono.metodoPago === 'OTRO' && abono.descripcionOtro ? ` · ${abono.descripcionOtro}` : ''}</span><small><Clock3 size={12} /> {dateTime(abono.fechaPago)}</small></div></li>)}</ol>}
              </section>

              {!isPaid && <section className="owner-turno-detail-section owner-deuda-abono-section"><div className="owner-deuda-section-heading"><h3>Registrar un abono</h3></div><OwnerAbonoForm saldo={saldo} isPending={abonoMutation.isPending} error={abonoMutation.error ? getErrorMessage(abonoMutation.error) : ''} onSubmit={onAbonar} /></section>}
              {isPaid && <div className="owner-deuda-paid-notice" role="status">La deuda está saldada. No se pueden registrar más abonos.</div>}
            </div>}
      <footer className="owner-turno-drawer-footer"><button type="button" className="owners-secondary-button" onClick={onClose} disabled={abonoMutation.isPending}>Cerrar detalle</button></footer>
    </aside>
  </div>
}
