import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Clock3, LoaderCircle, RefreshCw } from 'lucide-react'
import apiClient from '../../../services/apiClient.js'
import './OwnerSubscriptionManagement.css'

const labels = { PENDING: 'Pendiente de pago', ACTIVE: 'Activa', TRIAL: 'Prueba gratuita', EXPIRED: 'Vencida', SUSPENDED: 'Suspendida', CANCELLED: 'Cancelada' }
const events = { SUBSCRIPTION_ASSIGNED: 'Suscripción asignada', TRIAL_CREATED: 'Prueba iniciada', ACTIVATED: 'Suscripción activada', RENEWED: 'Suscripción renovada', SUSPENDED: 'Suscripción suspendida', REACTIVATED: 'Suscripción reactivada', CANCELLED: 'Suscripción cancelada', ADMINISTRATIVE_CHANGE: 'Cambio administrativo', EXPIRED: 'Suscripción vencida' }
const money = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const date = value => value ? new Date(`${value}T00:00:00`).toLocaleDateString('es-AR') : '—'

export default function OwnerSubscriptionManagement({ owner }) {
  const queryClient = useQueryClient()
  const [planId, setPlanId] = useState('')
  const [regularize, setRegularize] = useState(null)
  const [notice, setNotice] = useState(null)
  const plans = useQuery({ queryKey: ['admin', 'subscriptions', 'plans'], queryFn: async () => (await apiClient.get('/api/admin/planes-suscripcion')).data })
  const subscriptionId = owner.suscripcion?.suscripcionId
  const subscriptionQuery = useQuery({
    queryKey: ['admin', 'subscription', subscriptionId],
    enabled: Boolean(subscriptionId),
    initialData: owner.suscripcion,
    queryFn: async () => (await apiClient.get(`/api/admin/suscripciones/${subscriptionId}`)).data,
  })
  const sub = subscriptionQuery.data ?? owner.suscripcion
  const history = useQuery({ queryKey: ['admin', 'subscription-history', sub?.suscripcionId], enabled: Boolean(sub), queryFn: async () => (await apiClient.get(`/api/admin/suscripciones/${sub.suscripcionId}/historial`)).data })
  const saveMutation = useMutation({
    mutationFn: async ({ action, body }) => {
      if (action === 'assign') return (await apiClient.post(`/api/admin/owners/${owner.ownerId}/suscripcion`, body)).data
      if (action === 'regularizar') return (await apiClient.put(`/api/admin/suscripciones/${sub.suscripcionId}/regularizar`, body)).data
      return (await apiClient.patch(`/api/admin/suscripciones/${sub.suscripcionId}/${action}`, body)).data
    },
    onSuccess: async (updatedSubscription) => {
      if (updatedSubscription?.suscripcionId) {
        queryClient.setQueryData(['admin', 'subscription', updatedSubscription.suscripcionId], updatedSubscription)
        queryClient.setQueryData(['admin', 'owner', String(owner.ownerId)], current => current ? { ...current, suscripcion: updatedSubscription } : current)
        queryClient.setQueryData(['admin', 'owner', owner.ownerId], current => current ? { ...current, suscripcion: updatedSubscription } : current)
      }
      setNotice({ type: 'success', text: 'La suscripción se actualizó correctamente.' })
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['admin', 'subscription', updatedSubscription?.suscripcionId] }),
        queryClient.invalidateQueries({ queryKey: ['admin', 'owner', String(owner.ownerId)] }),
        queryClient.invalidateQueries({ queryKey: ['admin', 'owner', owner.ownerId] }),
        queryClient.invalidateQueries({ queryKey: ['admin', 'subscription-history'] }),
        queryClient.invalidateQueries({ queryKey: ['admin', 'subscriptions'] }),
        queryClient.invalidateQueries({ queryKey: ['admin', 'subscription-stats'] }),
      ])
    },
  })
  const availablePlans = useMemo(() => (plans.data || []).filter(plan => plan.tipo === 'COMERCIAL' && plan.activo), [plans.data])
  const remaining = sub?.fechaVencimiento ? Math.ceil((new Date(`${sub.fechaVencimiento}T00:00:00`) - new Date(new Date().toDateString())) / 86400000) : null
  const used = Number(sub?.cantidadNegocios || 0)
  const capacity = Number(sub?.maxNegocios || 0)
  const progress = capacity > 0 ? Math.min(100, used / capacity * 100) : 0

  async function run(action, body = {}) {
    if (saveMutation.isPending) return
    setNotice(null)
    try {
      await saveMutation.mutateAsync({ action, body })
      if (action === 'regularizar') setRegularize(null)
    } catch (error) {
      setNotice({ type: 'error', text: error.response?.data?.mensaje || 'No se pudo actualizar la suscripción.' })
    }
  }

  function confirmAction(action) {
    if (saveMutation.isPending) return
    const prompts = {
      activar: 'Confirmá que recibiste el pago externo. La suscripción inicia hoy y dura 31 días.',
      suspender: 'El OWNER perderá el acceso operativo. El plazo de vigencia seguirá corriendo. ¿Querés suspender la suscripción?',
      cancelar: 'La cancelación es terminal y no se puede revertir. ¿Querés cancelar esta suscripción?',
    }
    if (prompts[action] && !window.confirm(prompts[action])) return
    if (action === 'suspender' || action === 'cancelar') return run(action)
    if (action === 'reactivar') {
      if (!window.confirm('La reactivación administrativa inicia un nuevo período de 31 días desde hoy. ¿Continuar?')) return
      return run('activar')
    }
    if (action === 'activar') {
      const observacion = window.prompt('Observación (opcional):')
      if (observacion === null) return
      return run(action, observacion.trim() ? { observacion: observacion.trim() } : {})
    }
    if (action === 'renovar') {
      if (!window.confirm('Confirmá que recibiste el pago externo. Se inicia hoy un nuevo período de 31 días.')) return
      const observacion = window.prompt('Observación (opcional):')
      if (observacion === null) return
      return run('activar', observacion.trim() ? { observacion: observacion.trim() } : {})
    }
  }

  const statusActions = sub ? [
    ...(sub.status === 'PENDING' ? [['activar', 'Activar']] : []),
    ...(sub.status === 'ACTIVE' ? [['renovar', 'Renovar'], ['suspender', 'Suspender']] : []),
    ...(sub.status === 'SUSPENDED' ? [['reactivar', 'Reactivar']] : []),
    ...(sub.status === 'EXPIRED' ? [['reactivar', 'Reactivar']] : []),
    ...(['PENDING', 'ACTIVE', 'SUSPENDED', 'EXPIRED'].includes(sub.status) ? [['cancelar', 'Cancelar']] : []),
  ] : []

  function beginRegularization() {
    setNotice(null)
    setRegularize({ planId: sub.planId != null && availablePlans.some(plan => String(plan.planId) === String(sub.planId)) ? String(sub.planId) : '', precioContratado: sub.status === 'TRIAL' || sub.precioContratado == null ? '' : String(sub.precioContratado), fechaInicio: sub.fechaInicio || '', fechaVencimiento: sub.fechaVencimiento || '' })
  }

  return <section className="user-detail-card owner-subscription-management" aria-label="Suscripción del OWNER">
    <div className="user-detail-card-heading"><div><h2>Suscripción del OWNER</h2><p>Pago externo; la activación y renovación las confirma SUPER_ADMIN.</p></div></div>
    {notice && <p className={`owner-subscription-notice is-${notice.type}`} role={notice.type === 'error' ? 'alert' : 'status'}>{notice.text}</p>}
    {saveMutation.isPending && <p className="owner-subscription-pending" role="status"><LoaderCircle size={15} className="spin" /> {saveMutation.variables?.action === 'regularizar' ? 'Guardando regularización…' : 'Actualizando suscripción…'}</p>}
    {sub ? <>
      <div className="owner-subscription-summary">
        <div className="owner-subscription-title"><div><span>Plan contratado</span><h3>{sub.nombrePlan || 'Plan sin nombre'}</h3></div><span className={`owner-subscription-badge status-${String(sub.status).toLowerCase()}`}>{labels[sub.status] || sub.status}</span></div>
        {sub.status === 'TRIAL' && <p className="owner-subscription-trial">El plan gratuito no admite activar, suspender ni cancelar. Regularizá la suscripción para pasar a un plan comercial.</p>}
        {sub.status === 'CANCELLED' && <p className="owner-subscription-terminal">Estado terminal: no es posible reactivar esta suscripción.</p>}
        <div className="owner-subscription-facts">
          <div><span>Precio contratado</span><strong>{sub.precioContratado == null ? '—' : money.format(Number(sub.precioContratado))}</strong></div>
          <div><span>Inicio</span><strong>{date(sub.fechaInicio)}</strong></div>
          <div><span>Vencimiento</span><strong>{date(sub.fechaVencimiento)}</strong></div>
          {remaining !== null && <div><span>Vigencia</span><strong>{remaining > 0 ? `${remaining} días restantes` : remaining === 0 ? 'Vence hoy' : `Vencida hace ${Math.abs(remaining)} días`}</strong></div>}
        </div>
        <div className="owner-subscription-capacity"><div><strong>{used} de {capacity || '—'} negocios utilizados</strong><span>{sub.capacidadDisponible ?? '—'} disponibles</span></div><div className="owner-subscription-progress" role="progressbar" aria-label="Uso de capacidad de negocios" aria-valuemin="0" aria-valuemax={capacity || 0} aria-valuenow={capacity ? Math.min(used, capacity) : 0}><span style={{ width: `${progress}%` }} /></div></div>
      </div>
      <div className="businesses-row-actions">{statusActions.map(([action, label]) => <button type="button" key={action} className="owners-secondary-button" disabled={saveMutation.isPending} onClick={() => confirmAction(action)}>{label}</button>)}{sub.status !== 'CANCELLED' && <button type="button" className="owners-secondary-button" disabled={saveMutation.isPending} onClick={beginRegularization}>Regularizar</button>}</div>
      {regularize && <form className="owner-form owner-subscription-regularize" onSubmit={event => { event.preventDefault(); run('regularizar', { planId: Number(regularize.planId), precioContratado: regularize.precioContratado === '' ? undefined : Number(regularize.precioContratado), fechaInicio: regularize.fechaInicio || undefined, fechaVencimiento: regularize.fechaVencimiento || undefined, versionEsperada: sub.version }) }}>
        <p className="owner-subscription-warning">Corrección administrativa: no puede extenderse el vencimiento ni activar o cancelar desde aquí. Usá las acciones específicas para esas transiciones.</p>
        <label className="owner-form-field"><span>Plan comercial activo</span><select className="owner-subscription-plan-select" required value={regularize.planId} onChange={event => setRegularize({ ...regularize, planId: event.target.value })}><option value="">Seleccionar plan</option><optgroup label="Planes comerciales">{availablePlans.map(plan => <option key={plan.planId} value={plan.planId}>{plan.nombre} · {money.format(Number(plan.precioMensual))} · hasta {plan.maxNegocios} negocios</option>)}</optgroup></select></label>
        <label className="owner-form-field"><span>Precio contratado</span><input type="number" min="0.01" step="0.01" value={regularize.precioContratado} onChange={event => setRegularize({ ...regularize, precioContratado: event.target.value })} /></label>
        <label className="owner-form-field"><span>Fecha de inicio</span><input type="date" value={regularize.fechaInicio} onChange={event => setRegularize({ ...regularize, fechaInicio: event.target.value })} /></label>
        <label className="owner-form-field"><span>Fecha de vencimiento</span><input type="date" max={sub.fechaVencimiento || undefined} value={regularize.fechaVencimiento} onChange={event => setRegularize({ ...regularize, fechaVencimiento: event.target.value })} /></label>
        <div className="owner-subscription-form-actions"><button type="button" className="owners-secondary-button" onClick={() => setRegularize(null)} disabled={saveMutation.isPending}>Cerrar</button><button className="owners-secondary-button" type="submit" disabled={saveMutation.isPending || !regularize.planId}>Guardar regularización</button></div>
      </form>}
      <div className="owner-subscription-history"><h3>Historial de eventos</h3>{history.isPending ? <p><LoaderCircle size={15} className="spin" /> Cargando historial…</p> : history.isError ? <div className="owner-history-error"><p>No se pudo cargar el historial.</p><button type="button" className="owners-secondary-button" onClick={() => history.refetch()}><RefreshCw size={14} /> Reintentar</button></div> : history.data?.length ? <ol>{history.data.map(item => <li key={item.historialSuscripcionId}><Clock3 size={15} /><div><strong>{events[item.tipoEvento] || item.tipoEvento}</strong><span>{item.fechaEvento ? new Date(item.fechaEvento).toLocaleString('es-AR') : 'Fecha no disponible'}{item.actorNombre ? ` · ${item.actorNombre}` : item.actorIdentificacion ? ` · ${item.actorIdentificacion}` : ''}</span><small>{[item.nombrePlanAnterior && item.nombrePlanNuevo ? `Plan: ${item.nombrePlanAnterior} → ${item.nombrePlanNuevo}` : null, item.maxNegociosAnterior != null || item.maxNegociosNuevo != null ? `Capacidad: ${item.maxNegociosAnterior ?? '—'} → ${item.maxNegociosNuevo ?? '—'}` : null, item.precioContratadoAnterior != null || item.precioContratadoNuevo != null ? `Precio: ${item.precioContratadoAnterior == null ? '—' : money.format(Number(item.precioContratadoAnterior))} → ${item.precioContratadoNuevo == null ? '—' : money.format(Number(item.precioContratadoNuevo))}` : null, item.estadoAnterior || item.estadoNuevo ? `Estado: ${labels[item.estadoAnterior] || item.estadoAnterior || '—'} → ${labels[item.estadoNuevo] || item.estadoNuevo || '—'}` : null, item.fechaInicioAnterior || item.fechaVencimientoAnterior || item.fechaInicioNueva || item.fechaVencimientoNueva ? `Período: ${date(item.fechaInicioAnterior)}–${date(item.fechaVencimientoAnterior)} → ${date(item.fechaInicioNueva)}–${date(item.fechaVencimientoNueva)}` : null].filter(Boolean).join(' · ')}</small>{item.observacion && <p>{item.observacion}</p>}</div></li>)}</ol> : <p>No hay eventos registrados.</p>}</div>
    </> : <div className="owner-subscription-assign"><p>Este OWNER todavía no tiene una suscripción asignada.</p>{plans.isPending ? <p><LoaderCircle size={15} className="spin" /> Cargando planes…</p> : plans.isError ? <div className="owner-history-error"><p>No se pudieron cargar los planes.</p><button type="button" className="owners-secondary-button" onClick={() => plans.refetch()}><RefreshCw size={14} /> Reintentar</button></div> : <><label className="owner-form-field owner-subscription-assign-field"><span>Plan comercial activo</span><select className="owner-subscription-plan-select" value={planId} onChange={event => setPlanId(event.target.value)}><option value="">Seleccionar plan</option><optgroup label="Planes comerciales">{availablePlans.map(plan => <option key={plan.planId} value={plan.planId}>{plan.nombre} · {money.format(Number(plan.precioMensual))} · hasta {plan.maxNegocios} negocios</option>)}</optgroup></select><small>La asignación crea una suscripción pendiente de confirmación de pago.</small></label><button type="button" className="owners-secondary-button" disabled={!planId || saveMutation.isPending} onClick={() => { const observacion = window.prompt('Observación (opcional):'); if (observacion !== null) run('assign', { planId: Number(planId), ...(observacion.trim() ? { observacion: observacion.trim() } : {}) }) }}>Asignar suscripción pendiente</button></>}</div>}
  </section>
}
