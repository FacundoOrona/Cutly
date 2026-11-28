import { useRef, useState } from 'react'
import { Ban, CircleAlert, LoaderCircle, RefreshCw, X } from 'lucide-react'
import { getAnularOwnerLiquidacionErrorMessage, getOwnerLiquidacionesErrorMessage } from '../api/ownerRemuneracionesApi.js'
import { useAnularOwnerLiquidacion, useOwnerLiquidacion } from '../hooks/useOwnerRemuneraciones.js'

const methodLabels = {
  EFECTIVO: 'Efectivo', TRANSFERENCIA_QR: 'Transferencia / QR', DEBITO: 'Débito', CREDITO: 'Crédito', OTRO: 'Otro',
  CASH: 'Efectivo (histórico)', DEBIT_CARD: 'Débito (histórico)', CREDIT_CARD: 'Crédito (histórico)',
  TRANSFER: 'Transferencia (histórico)', MERCADO_PAGO: 'Mercado Pago (histórico)',
}
const statusLabels = { VIGENTE: 'Vigente', ANULADA: 'Anulada' }

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

function dateOnly(value) {
  if (!value) return '—'
  const parsed = new Date(`${value}T12:00:00`)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium' }).format(parsed)
}

export default function OwnerLiquidacionDetailDrawer({ negocioId, liquidacionId, onClose }) {
  const query = useOwnerLiquidacion({ negocioId, liquidacionId })
  const mutation = useAnularOwnerLiquidacion()
  const [motivo, setMotivo] = useState('')
  const [mutationError, setMutationError] = useState('')
  const [success, setSuccess] = useState('')
  const operationRef = useRef(false)
  const closeButtonRef = useRef(null)
  const liquidacion = query.data
  const canAnnul = liquidacion?.estado === 'VIGENTE'

  async function handleAnnul(event) {
    event.preventDefault()
    const trimmedReason = motivo.trim()
    if (!canAnnul || mutation.isPending || operationRef.current || !trimmedReason || trimmedReason.length > 500) return
    const confirmed = window.confirm('La anulación es administrativa y no implica que el dinero haya sido devuelto. ¿Confirmás anular esta liquidación?')
    if (!confirmed) return
    operationRef.current = true
    setMutationError('')
    setSuccess('')
    try {
      await mutation.mutateAsync({ negocioId, liquidacionId, motivo: trimmedReason })
      setMotivo('')
      setSuccess('La liquidación fue anulada administrativamente. Esto no confirma la devolución del dinero.')
    } catch (error) {
      setMutationError(getAnularOwnerLiquidacionErrorMessage(error))
    } finally {
      operationRef.current = false
    }
  }

  function close() {
    if (!mutation.isPending) onClose()
  }

  return <div className="owner-remuneracion-overlay" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) close() }}>
    <section className="owner-remuneracion-drawer owner-liquidacion-detail-drawer" role="dialog" aria-modal="true" aria-labelledby="owner-liquidacion-detail-title">
      <header className="owner-remuneracion-drawer-header"><div><span className="owner-remuneracion-overline">DETALLE DE LIQUIDACIÓN</span><h2 id="owner-liquidacion-detail-title">Liquidación #{liquidacionId}</h2></div><button ref={closeButtonRef} type="button" className="icon-button" onClick={close} aria-label="Cerrar detalle" disabled={mutation.isPending}><X size={19} /></button></header>
      <div className="owner-remuneracion-drawer-content">
        {query.isPending ? <div className="owner-remuneraciones-state" role="status"><LoaderCircle size={18} className="spin" /> Cargando detalle…</div>
          : query.isError ? <div className="owner-remuneraciones-state is-error" role="alert"><CircleAlert size={17} /><span>{getOwnerLiquidacionesErrorMessage(query.error, 'cargar')}</span><button type="button" className="owners-secondary-button" onClick={() => void query.refetch()} disabled={query.isFetching}><RefreshCw size={14} /> Reintentar</button></div>
            : liquidacion && <>
              <dl className="owner-liquidacion-detail-grid">
                <div><dt>Empleado</dt><dd>{liquidacion.empleadoNombre || `#${liquidacion.empleadoId}`}</dd></div>
                <div><dt>Empleado ID</dt><dd>{liquidacion.empleadoId ?? '—'}</dd></div>
                <div><dt>Fecha efectiva</dt><dd>{dateOnly(liquidacion.fechaEfectiva)}</dd></div>
                <div><dt>Fecha de registro</dt><dd>{dateTime(liquidacion.fechaRegistro)}</dd></div>
                <div><dt>Medio de pago</dt><dd>{methodLabels[liquidacion.metodoPago] || liquidacion.metodoPago || '—'}</dd></div>
                {liquidacion.descripcionMetodo && <div><dt>Descripción del medio</dt><dd>{liquidacion.descripcionMetodo}</dd></div>}
                <div><dt>Importe total</dt><dd>{money(liquidacion.importeTotal)}</dd></div>
                <div><dt>Estado</dt><dd><span className={`owner-remuneraciones-status ${liquidacion.estado === 'ANULADA' ? 'liquidacion-anulada' : 'liquidacion-vigente'}`}>{statusLabels[liquidacion.estado] || liquidacion.estado || '—'}</span></dd></div>
                {liquidacion.ownerRegistrante && <div><dt>Registrado por</dt><dd>{liquidacion.ownerRegistrante}</dd></div>}
                {liquidacion.observaciones && <div className="is-wide"><dt>Observaciones</dt><dd>{liquidacion.observaciones}</dd></div>}
                {liquidacion.estado === 'ANULADA' && <>
                  <div className="is-wide annulled-detail"><dt>Motivo de anulación</dt><dd>{liquidacion.motivoAnulacion || '—'}</dd></div>
                  <div><dt>Fecha de anulación</dt><dd>{dateTime(liquidacion.fechaAnulacion)}</dd></div>
                  {liquidacion.ownerAnulador && <div><dt>Anulada por</dt><dd>{liquidacion.ownerAnulador}</dd></div>}
                </>}
              </dl>
              {liquidacion.estado === 'ANULADA' && <p className="owner-liquidacion-annulled-notice" role="status">Esta anulación es administrativa y no significa que el dinero haya sido devuelto.</p>}
              <section className="owner-remuneracion-applications owner-liquidacion-applications"><div><h3>Aplicaciones</h3><p>Remuneraciones afectadas por esta liquidación.</p></div>
                {!liquidacion.aplicaciones?.length ? <p className="owner-remuneracion-no-applications">No hay aplicaciones informadas.</p> : <ol>{liquidacion.aplicaciones.map((application) => <li key={application.remuneracionId}>
                  <div className="owner-remuneracion-application-heading"><strong>Remuneración #{application.remuneracionId}</strong><span>Turno #{application.turnoId ?? '—'}</span></div>
                  <dl><div><dt>Importe aplicado</dt><dd>{money(application.importe)}</dd></div><div><dt>Total pagado vigente</dt><dd>{money(application.totalPagado)}</dd></div><div><dt>Saldo pendiente</dt><dd>{money(application.saldoPendiente)}</dd></div><div><dt>Fecha efectiva</dt><dd>{dateOnly(application.fechaEfectiva)}</dd></div><div><dt>Medio</dt><dd>{methodLabels[application.metodoPago] || application.metodoPago || '—'}</dd></div><div><dt>Estado de liquidación</dt><dd>{statusLabels[application.estadoLiquidacion] || application.estadoLiquidacion || '—'}</dd></div></dl>
                </li>)}</ol>}
              </section>
              {canAnnul && <form className="owner-liquidacion-annul-form" onSubmit={handleAnnul}>
                <h3>Anular liquidación</h3><p className="owner-liquidacion-annul-warning">La anulación es administrativa y no implica que el dinero haya sido devuelto.</p>
                <label className="owner-form-field"><span>Motivo obligatorio <small>{motivo.length}/500</small></span><textarea value={motivo} maxLength={500} rows="3" onChange={(event) => { setMotivo(event.target.value); setMutationError('') }} required /></label>
                {mutationError && <p className="owner-liquidacion-error" role="alert"><CircleAlert size={16} />{mutationError}</p>}
                {success && <p className="owner-liquidacion-annul-success" role="status">{success}</p>}
                <button type="submit" className="owner-liquidacion-annul-button" disabled={!motivo.trim() || mutation.isPending || mutation.isSuccess}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Anulando…</> : <><Ban size={15} /> Anular liquidación</>}</button>
              </form>}
              {success && !canAnnul && <p className="owner-liquidacion-annul-success" role="status">{success}</p>}
            </>}
      </div>
      <footer className="owner-remuneracion-drawer-footer"><button type="button" className="owners-secondary-button" onClick={close} disabled={mutation.isPending}>Cerrar detalle</button></footer>
    </section>
  </div>
}
