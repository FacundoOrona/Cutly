import { useMemo, useState } from 'react'
import { CheckCircle2, Clock3, LoaderCircle, RefreshCw, X } from 'lucide-react'
import { getSubscriptionErrorMessage } from '../api/subscriptionsApi.js'
import {
  useAssignSubscription,
  useChangeSubscriptionStatus,
  useRegularizeSubscription,
  useSubscription,
  useSubscriptionHistory,
  useSubscriptionPlans,
} from '../hooks/useSubscriptions.js'
import './businessSubscriptionManagement.css'

const statusLabels = { ACTIVE: 'Activa', TRIAL: 'Prueba', EXPIRED: 'Vencida', SUSPENDED: 'Suspendida', CANCELLED: 'Cancelada' }
const eventLabels = {
  SUBSCRIPTION_ASSIGNED: 'Suscripción asignada',
  ADMINISTRATIVE_CHANGE: 'Cambio administrativo',
  SUSPENDED: 'Suscripción suspendida',
  REACTIVATED: 'Suscripción reactivada',
  CANCELLED: 'Suscripción cancelada',
}
const money = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

function todayString() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function addDays(date, days) {
  const [year, month, day] = date.split('-').map(Number)
  const result = new Date(year, month - 1, day + days)
  return `${result.getFullYear()}-${String(result.getMonth() + 1).padStart(2, '0')}-${String(result.getDate()).padStart(2, '0')}`
}

function formatDate(value) {
  if (!value) return '—'
  const [year, month, day] = value.split('-')
  return `${day}/${month}/${year}`
}

function eventDate(value) {
  if (!value) return 'Fecha desconocida'
  return new Date(value).toLocaleString('es-AR')
}

function Field({ label, children }) {
  return <div className="business-subscription-field"><dt>{label}</dt><dd>{children || '—'}</dd></div>
}

export default function BusinessSubscriptionManagement({ business }) {
  const hasSubscription = business.suscripcionId !== null && business.suscripcionId !== undefined
  const id = business.suscripcionId
  const subscriptionQuery = useSubscription(id, hasSubscription)
  const historyQuery = useSubscriptionHistory(id, hasSubscription)
  const plansQuery = useSubscriptionPlans()
  const assignMutation = useAssignSubscription()
  const statusMutation = useChangeSubscriptionStatus()
  const regularizeMutation = useRegularizeSubscription()
  const pending = assignMutation.isPending || statusMutation.isPending || regularizeMutation.isPending
  const subscription = subscriptionQuery.data
  const [dialog, setDialog] = useState(null)
  const [notice, setNotice] = useState(null)
  const [observation, setObservation] = useState('')
  const [planId, setPlanId] = useState('')
  const [startDate, setStartDate] = useState(todayString)
  const [dueDate, setDueDate] = useState(() => addDays(todayString(), 29))
  const [initialStatus, setInitialStatus] = useState('ACTIVE')
  const [regularize, setRegularize] = useState({ planId: '', fechaInicio: '', fechaVencimiento: '', precioContratado: '' })

  const availablePlans = useMemo(() => (Array.isArray(plansQuery.data) ? plansQuery.data : [])
    .filter((plan) => plan.tipo === 'COMERCIAL' && plan.activo), [plansQuery.data])

  function openStatusAction(action) {
    setNotice(null)
    setObservation('')
    setDialog({ type: 'status', action })
  }

  function openAssign() {
    setNotice(null)
    setPlanId(availablePlans[0]?.planId ? String(availablePlans[0].planId) : '')
    setStartDate(todayString())
    setDueDate(addDays(todayString(), 29))
    setInitialStatus('ACTIVE')
    setObservation('')
    setDialog({ type: 'assign' })
  }

  function openRegularize() {
    setNotice(null)
    setObservation('')
    setRegularize({ planId: '', fechaInicio: '', fechaVencimiento: '', precioContratado: '' })
    setDialog({ type: 'regularize' })
  }

  async function submit(event) {
    event.preventDefault()
    if (!dialog || pending) return
    setNotice(null)
    try {
      if (dialog.type === 'status') {
        const labels = { activate: 'activada', suspend: 'suspendida', cancel: 'cancelada' }
        await statusMutation.mutateAsync({ businessId: business.negocioId, subscriptionId: id, action: dialog.action, observacion: observation.trim() || null })
        setNotice({ type: 'success', text: `La suscripción fue ${labels[dialog.action]} correctamente.` })
      } else if (dialog.type === 'assign') {
        await assignMutation.mutateAsync({
          businessId: business.negocioId,
          planId: Number(planId),
          fechaInicio: startDate,
          fechaVencimiento: dueDate,
          status: initialStatus,
          observacion: observation.trim() || null,
        })
        setNotice({ type: 'success', text: 'La suscripción fue asignada correctamente.' })
      } else {
        const body = {
          ...Object.fromEntries(Object.entries(regularize).filter(([, value]) => value !== '').map(([key, value]) => [key, key === 'planId' ? Number(value) : value])),
          versionEsperada: subscription.version,
          observacion: observation.trim() || null,
        }
        if (!Object.keys(body).some((key) => ['planId', 'fechaInicio', 'fechaVencimiento', 'precioContratado'].includes(key))) {
          setNotice({ type: 'error', text: 'Indicá al menos un dato para regularizar.' })
          return
        }
        await regularizeMutation.mutateAsync({ businessId: business.negocioId, subscriptionId: id, ...body })
        setNotice({ type: 'success', text: 'La suscripción fue regularizada correctamente.' })
      }
      setDialog(null)
    } catch (error) {
      setNotice({ type: 'error', text: getSubscriptionErrorMessage(error) })
    }
  }

  const today = todayString()
  const canActivate = subscription && (subscription.status === 'TRIAL'
    ? subscription.fechaVencimiento >= today
    : subscription.status === 'SUSPENDED' && subscription.fechaInicio <= today && subscription.fechaVencimiento >= today)
  const statusActions = subscription ? [
    ...(canActivate ? [['activate', subscription.status === 'SUSPENDED' ? 'Reactivar suscripción' : 'Activar suscripción']] : []),
    ...(['ACTIVE', 'TRIAL'].includes(subscription.status) ? [['suspend', 'Suspender suscripción']] : []),
    ...(['TRIAL', 'ACTIVE', 'SUSPENDED', 'EXPIRED'].includes(subscription.status) ? [['cancel', 'Cancelar suscripción']] : []),
  ] : []

  return (
    <section className="user-detail-card business-subscription-card" aria-labelledby="business-subscription-title">
      <div className="business-subscription-heading">
        <div><h2 id="business-subscription-title">Suscripción</h2><p>Contrato y acceso por suscripción. El estado del negocio se gestiona por separado.</p></div>
        {hasSubscription && subscription && <span className={`business-subscription-badge status-${subscription.status.toLowerCase()}`}>{statusLabels[subscription.status] || subscription.status}</span>}
      </div>

      {notice && <div className={`owner-created-notice${notice.type === 'error' ? ' owner-status-error' : ''}`} role={notice.type === 'error' ? 'alert' : 'status'}>
        {notice.type === 'success' && <CheckCircle2 size={17} />}{notice.text}<button type="button" className="icon-button" onClick={() => setNotice(null)} aria-label="Cerrar mensaje"><X size={16} /></button>
      </div>}

      {!hasSubscription ? <div className="business-subscription-empty">
        <p>Este negocio todavía no tiene una suscripción asignada.</p>
        {plansQuery.isPending ? <span role="status"><LoaderCircle size={15} className="spin" /> Cargando planes…</span>
          : plansQuery.isError ? <div className="user-detail-error" role="alert">{getSubscriptionErrorMessage(plansQuery.error)} <button type="button" className="owners-secondary-button" onClick={() => plansQuery.refetch()}>Reintentar</button></div>
            : availablePlans.length === 0 ? <p>No hay planes comerciales activos disponibles para asignar.</p>
              : <button type="button" className="owners-secondary-button" onClick={openAssign}>Asignar suscripción</button>}
      </div> : <>
        {subscriptionQuery.isPending ? <div className="user-detail-state" role="status"><LoaderCircle size={17} className="spin" /> Cargando detalle de suscripción…</div>
          : subscriptionQuery.isError ? <div className="user-detail-error" role="alert">{getSubscriptionErrorMessage(subscriptionQuery.error)} <button type="button" className="owners-secondary-button" onClick={() => subscriptionQuery.refetch()}><RefreshCw size={14} /> Reintentar</button></div>
            : subscription && <>
              <dl className="business-subscription-grid">
                <Field label="Estado">{statusLabels[subscription.status] || subscription.status}</Field>
                <Field label="Plan">{subscription.nombrePlan}</Field>
                <Field label="Tipo de plan">{business.tipoPlan}</Field>
                <Field label="Precio contratado">{subscription.precioContratado == null ? '—' : money.format(Number(subscription.precioContratado))}</Field>
                <Field label="Inicio">{formatDate(subscription.fechaInicio)}</Field>
                <Field label="Vencimiento">{formatDate(subscription.fechaVencimiento)}</Field>
                <Field label="Plazo">{business.diasRestantes != null ? `${business.diasRestantes} días restantes` : business.diasAtraso != null ? `${business.diasAtraso} días de atraso` : '—'}</Field>
                <Field label="Habilitación por suscripción">{business.suscripcionHabilitada ? 'Habilitada' : 'No habilitada'}</Field>
              </dl>
              {subscription.status === 'SUSPENDED' && !canActivate && <p className="business-subscription-rule" role="status">La reactivación está disponible desde el inicio del período y antes de su vencimiento.</p>}
              {subscription.status === 'EXPIRED' && <p className="business-subscription-rule" role="status">Una suscripción vencida no se puede activar; solo puede cancelarse desde este panel.</p>}
              <div className="business-detail-actions business-subscription-actions">
                <span className="user-detail-field-label">Acciones de suscripción</span>
                <div className="businesses-row-actions">
                  {statusActions.map(([action, label]) => <button key={action} type="button" className="owners-secondary-button" onClick={() => openStatusAction(action)} disabled={pending}>{label}</button>)}
                  <button type="button" className="owners-secondary-button" onClick={openRegularize} disabled={pending}>Regularizar datos</button>
                  <button type="button" className="owners-secondary-button" onClick={() => historyQuery.refetch()} disabled={historyQuery.isFetching}><RefreshCw size={14} /> Actualizar historial</button>
                </div>
              </div>
              {historyQuery.isPending ? <p className="business-subscription-history-state" role="status"><LoaderCircle size={15} className="spin" /> Cargando historial…</p>
                : historyQuery.isError ? <div className="user-detail-error" role="alert">{getSubscriptionErrorMessage(historyQuery.error)} <button type="button" className="owners-secondary-button" onClick={() => historyQuery.refetch()}>Reintentar historial</button></div>
                  : <div className="business-subscription-history"><h3>Historial de eventos</h3>{historyQuery.data?.length ? <ol>{historyQuery.data.map((item) => <li key={item.historialSuscripcionId}><Clock3 size={15} /><div><strong>{eventLabels[item.tipoEvento] || item.tipoEvento}</strong><span>{eventDate(item.fechaEvento)}{item.actorNombre ? ` · ${item.actorNombre}` : ''}</span>{item.observacion && <p>{item.observacion}</p>}</div></li>)}</ol> : <p>No hay eventos registrados para esta suscripción.</p>}</div>}
            </>}
      </>}

      {dialog && <div className="business-subscription-dialog-backdrop">
        <form className="business-subscription-dialog" role="dialog" aria-modal="true" aria-labelledby="subscription-dialog-title" onSubmit={submit}>
          <div className="business-subscription-dialog-heading"><h3 id="subscription-dialog-title">{dialog.type === 'assign' ? 'Asignar suscripción' : dialog.type === 'regularize' ? 'Regularizar suscripción' : ({ activate: 'Activar suscripción', suspend: 'Suspender suscripción', cancel: 'Cancelar suscripción' }[dialog.action])}</h3><button type="button" className="icon-button" onClick={() => setDialog(null)} disabled={pending} aria-label="Cerrar diálogo"><X size={17} /></button></div>
          {dialog.type === 'status' && <p className="business-subscription-dialog-copy">Confirmá la operación para el negocio “{business.nombre}”.</p>}
          {dialog.type === 'assign' && <>
            <label className="owner-form-field"><span>Plan comercial activo <b>*</b></span><select required value={planId} onChange={(event) => setPlanId(event.target.value)} disabled={pending}><option value="">Seleccionar plan</option>{availablePlans.map((plan) => <option key={plan.planId} value={plan.planId}>{plan.nombre} · {money.format(Number(plan.precioMensual))}</option>)}</select></label>
            <div className="business-subscription-form-grid"><label className="owner-form-field"><span>Fecha de inicio <b>*</b></span><input required type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} disabled={pending} /></label><label className="owner-form-field"><span>Fecha de vencimiento <b>*</b></span><input required type="date" value={dueDate} onChange={(event) => setDueDate(event.target.value)} disabled={pending} /></label></div>
            <label className="owner-form-field"><span>Estado inicial <b>*</b></span><select required value={initialStatus} onChange={(event) => setInitialStatus(event.target.value)} disabled={pending}><option value="ACTIVE">Activa</option><option value="SUSPENDED">Suspendida</option><option value="EXPIRED">Vencida</option></select><small>El backend valida que el estado coincida con las fechas y que la duración sea válida.</small></label>
          </>}
          {dialog.type === 'regularize' && <>
            <p className="business-subscription-dialog-copy">La versión actual {subscription.version} se enviará automáticamente como <code>versionEsperada</code>. No modifiques estados ni extiendas el vencimiento desde esta acción.</p>
            <label className="owner-form-field"><span>Plan comercial activo</span><select value={regularize.planId} onChange={(event) => setRegularize({ ...regularize, planId: event.target.value })} disabled={pending}><option value="">Sin cambios</option>{availablePlans.map((plan) => <option key={plan.planId} value={plan.planId}>{plan.nombre}</option>)}</select></label>
            <div className="business-subscription-form-grid"><label className="owner-form-field"><span>Fecha de inicio</span><input type="date" value={regularize.fechaInicio} onChange={(event) => setRegularize({ ...regularize, fechaInicio: event.target.value })} disabled={pending} /></label><label className="owner-form-field"><span>Fecha de vencimiento</span><input type="date" value={regularize.fechaVencimiento} onChange={(event) => setRegularize({ ...regularize, fechaVencimiento: event.target.value })} disabled={pending} /><small>El backend no permite extender el vencimiento.</small></label></div>
            <label className="owner-form-field"><span>Precio contratado</span><input type="number" min="0" step="0.01" value={regularize.precioContratado} onChange={(event) => setRegularize({ ...regularize, precioContratado: event.target.value })} disabled={pending} /></label>
          </>}
          {dialog.type !== 'regularize' && <label className="owner-form-field"><span>Observación</span><textarea maxLength={1000} value={observation} onChange={(event) => setObservation(event.target.value)} disabled={pending} /><small>{observation.length}/1000</small></label>}
          {dialog.type === 'regularize' && <label className="owner-form-field"><span>Observación</span><textarea maxLength={1000} value={observation} onChange={(event) => setObservation(event.target.value)} disabled={pending} /><small>{observation.length}/1000</small></label>}
          <div className="business-subscription-dialog-actions"><button type="button" className="owners-secondary-button" onClick={() => setDialog(null)} disabled={pending}>Volver</button><button type="submit" className="owners-secondary-button" disabled={pending || (dialog.type === 'assign' && !planId)}>{pending && <LoaderCircle size={14} className="spin" />}{dialog.type === 'status' ? 'Confirmar' : dialog.type === 'assign' ? 'Asignar' : 'Guardar cambios'}</button></div>
        </form>
      </div>}
    </section>
  )
}
