import { useEffect, useMemo, useRef, useState } from 'react'
import { Ban, Bell, CalendarDays, Check, CheckCircle2, Clock3, History, LoaderCircle, RefreshCw, UserRound, UserRoundX, UsersRound, X } from 'lucide-react'
import { getEmployeeAppointmentActionErrorMessage } from '../api/employeeApi.js'
import { useEmployeeAppointmentAction, useEmployeeAppointments, useEmployeeBusiness, useEmployeeProfile } from '../hooks/useEmployeeDashboard.js'
import './EmployeeHomePage.css'

const ACTIVE_UPCOMING_STATUSES = new Set(['PENDING', 'CONFIRMED'])
const AVAILABLE_ACTIONS = {
  PENDING: ['confirmar', 'cancelar'],
  CONFIRMED: ['cancelar', 'completar', 'no-show'],
  IN_PROGRESS: ['cancelar', 'completar'],
}
const ACTION_DETAILS = {
  confirmar: { label: 'Confirmar', icon: CheckCircle2, style: 'is-primary', success: 'Turno confirmado.' },
  cancelar: { label: 'Cancelar', icon: Ban, style: 'is-danger', success: 'Turno cancelado.' },
  completar: { label: 'Completar', icon: Check, style: 'is-secondary', success: 'Turno completado.' },
  'no-show': { label: 'Marcar ausencia', icon: UserRoundX, style: 'is-danger', success: 'Ausencia marcada.' },
}

function localDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatTime(value) {
  if (!value) return 'Hora no disponible'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return 'Hora no disponible'
  return new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(parsed)
}

function formatDuration(start, end) {
  const startTime = new Date(start).getTime()
  const endTime = new Date(end).getTime()
  const minutes = Math.round((endTime - startTime) / 60000)
  if (!Number.isFinite(minutes) || minutes <= 0) return null
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  const remainder = minutes % 60
  return remainder ? `${hours} h ${remainder} min` : `${hours} h`
}

function clientName(turn) {
  return [turn.clienteNombre, turn.clienteApellido].filter(Boolean).join(' ') || 'Cliente'
}

function statusLabel(status) {
  if (status === 'PENDING') return 'Pendiente'
  if (status === 'CONFIRMED') return 'Confirmado'
  if (status === 'IN_PROGRESS') return 'En curso'
  if (status === 'COMPLETED') return 'Completado'
  if (status === 'CANCELLED') return 'Cancelado'
  if (status === 'NO_SHOW') return 'Ausencia'
  if (status === 'REJECTED') return 'Rechazado'
  return status || 'Estado no disponible'
}

function DashboardSkeleton() {
  return <div className="employee-skeleton" role="status" aria-label="Cargando el dashboard">
    <span /><span /><span /><span />
  </div>
}

function MetricCard({ label, value, detail, icon: Icon, tone = '' }) {
  return <article className={`employee-metric ${tone}`}>
    <div className="employee-metric-top"><span>{label}</span><span className="employee-metric-icon"><Icon size={17} /></span></div>
    <strong>{value}</strong>
    <small>{detail}</small>
  </article>
}

function AppointmentRow({ turn, isProcessing, activeAction, feedback, onAction, onRetry, onDismissFeedback }) {
  const actions = AVAILABLE_ACTIONS[turn.status] || []
  return <li className={`employee-appointment-row${actions.length ? ' has-actions' : ''}`}>
    <time className="employee-appointment-time">{formatTime(turn.fechaHoraInicio)}</time>
    <span className="employee-appointment-marker" aria-hidden="true" />
    <div className="employee-appointment-details">
      <strong>{clientName(turn)}</strong>
      <span>{turn.servicioNombre || 'Servicio no disponible'}</span>
    </div>
    <span className={`employee-status employee-status-${turn.status?.toLowerCase() || 'unknown'}`}>{statusLabel(turn.status)}</span>
    {actions.length > 0 && <div className="employee-appointment-actions" aria-label={`Acciones para ${clientName(turn)}`}>
      {actions.map((action) => {
        const details = ACTION_DETAILS[action]
        const Icon = isProcessing && activeAction === action ? LoaderCircle : details.icon
        return <button key={action} type="button" className={`employee-action-button ${details.style}`} onClick={() => onAction(turn, action)} disabled={isProcessing}>
          <Icon size={14} className={isProcessing && activeAction === action ? 'employee-action-spinner' : ''} />
          {details.label}
        </button>
      })}
    </div>}
    {feedback && <div className={`employee-action-feedback is-${feedback.type}`} role={feedback.type === 'error' ? 'alert' : 'status'}>
      <span>{feedback.message}</span>
      {feedback.type === 'error' && feedback.retryable && <button type="button" onClick={() => onRetry(turn, feedback.action)} disabled={isProcessing}><RefreshCw size={13} /> Reintentar</button>}
      <button type="button" className="employee-feedback-dismiss" aria-label="Cerrar mensaje" onClick={() => onDismissFeedback(turn.turnoId)}><X size={14} /></button>
    </div>}
  </li>
}

function NextAppointment({ turn }) {
  if (!turn) return <div className="employee-next-empty">
    <span className="employee-next-empty-icon"><Check size={19} /></span>
    <div><strong>No tenés próximos turnos por hoy</strong><p>Cuando haya un turno pendiente o confirmado, lo vas a ver acá.</p></div>
  </div>

  const duration = formatDuration(turn.fechaHoraInicio, turn.fechaHoraFin)
  return <div className="employee-next-card">
    <div className="employee-next-time"><Clock3 size={17} /><span>{formatTime(turn.fechaHoraInicio)}</span></div>
    <div className="employee-next-info">
      <strong>{clientName(turn)}</strong>
      <span>{turn.servicioNombre || 'Servicio no disponible'}{duration ? ` · ${duration}` : ''}</span>
    </div>
    <span className={`employee-status employee-status-${turn.status.toLowerCase()}`}>{statusLabel(turn.status)}</span>
  </div>
}

function UpcomingBoundary({ turns, onBoundary }) {
  useEffect(() => {
    const now = Date.now()
    const nextStart = turns
      .filter((turn) => ACTIVE_UPCOMING_STATUSES.has(turn.status))
      .map((turn) => new Date(turn.fechaHoraInicio).getTime())
      .filter((time) => Number.isFinite(time) && time > now)
      .sort((a, b) => a - b)[0]
    const nextMidnight = new Date()
    nextMidnight.setHours(24, 0, 0, 0)
    const delay = Math.max(1, Math.min(nextStart ?? Infinity, nextMidnight.getTime()) - now)
    const timeout = window.setTimeout(onBoundary, delay)
    return () => window.clearTimeout(timeout)
  }, [turns, onBoundary])

  return null
}

function ProfileError({ onRetry, isFetching }) {
  return <div className="employee-inline-error" role="alert">
    <p>No pudimos cargar tu perfil. Reintentá para ver tu información personal.</p>
    <button type="button" onClick={onRetry} disabled={isFetching}><RefreshCw size={14} /> Reintentar</button>
  </div>
}

function TurnosError({ onRetry, isFetching }) {
  return <div className="employee-query-error" role="alert">
    <CalendarDays size={22} />
    <strong>No pudimos cargar los turnos de hoy</strong>
    <p>Las estadísticas y la agenda no están disponibles hasta que se complete la consulta.</p>
    <button type="button" onClick={onRetry} disabled={isFetching}><RefreshCw size={14} /> Reintentar</button>
  </div>
}

function ComingSoonCard({ icon: Icon, title, detail }) {
  return <div className="employee-shortcut" aria-disabled="true">
    <span className="employee-shortcut-icon"><Icon size={17} /></span>
    <span className="employee-shortcut-copy"><strong>{title}</strong><small>{detail}</small></span>
    <span className="employee-coming-soon">Próximamente</span>
  </div>
}

export default function EmployeeHomePage() {
  const [now, setNow] = useState(() => new Date())
  const [pendingTurnIds, setPendingTurnIds] = useState(() => new Set())
  const [pendingActions, setPendingActions] = useState({})
  const [actionFeedback, setActionFeedback] = useState({})
  const [confirmation, setConfirmation] = useState(null)
  const pendingTurnIdsRef = useRef(new Set())
  const refreshTemporalState = useMemo(() => () => setNow(new Date()), [])
  const today = localDateKey(now)
  const profileQuery = useEmployeeProfile()
  const businessQuery = useEmployeeBusiness()
  const appointmentsQuery = useEmployeeAppointments(today)
  const actionMutation = useEmployeeAppointmentAction()
  const turns = appointmentsQuery.data ?? []

  const futureTurns = useMemo(() => turns
    .filter((turn) => ACTIVE_UPCOMING_STATUSES.has(turn.status))
    .filter((turn) => new Date(turn.fechaHoraInicio).getTime() > now.getTime())
    .sort((a, b) => new Date(a.fechaHoraInicio).getTime() - new Date(b.fechaHoraInicio).getTime()), [turns, now])

  const dateLabel = new Intl.DateTimeFormat('es-AR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(now)
  const profile = profileQuery.data
  const business = businessQuery.data
  const confirmationTurnId = confirmation ? String(confirmation.turn.turnoId) : ''
  const confirmationIsProcessing = confirmationTurnId ? pendingTurnIds.has(confirmationTurnId) : false

  useEffect(() => {
    if (!confirmation) return undefined
    function closeOnEscape(event) {
      if (event.key === 'Escape' && !pendingTurnIdsRef.current.has(String(confirmation.turn.turnoId))) {
        setConfirmation(null)
      }
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [confirmation])

  async function performAction(turn, action) {
    const turnId = String(turn.turnoId)
    if (pendingTurnIdsRef.current.has(turnId)) return
    pendingTurnIdsRef.current.add(turnId)
    setPendingTurnIds(new Set(pendingTurnIdsRef.current))
    setPendingActions((current) => ({ ...current, [turnId]: action }))
    setActionFeedback((current) => { const next = { ...current }; delete next[turnId]; return next })
    try {
      await actionMutation.mutateAsync({ turnoId: turn.turnoId, action })
      setActionFeedback((current) => ({ ...current, [turnId]: { type: 'success', message: ACTION_DETAILS[action].success } }))
    } catch (error) {
      const status = error.response?.status
      setActionFeedback((current) => ({
        ...current,
        [turnId]: {
          type: 'error',
          message: getEmployeeAppointmentActionErrorMessage(error),
          action,
          retryable: ![400, 401, 403, 404, 409].includes(status),
        },
      }))
    } finally {
      pendingTurnIdsRef.current.delete(turnId)
      setPendingTurnIds(new Set(pendingTurnIdsRef.current))
      setPendingActions((current) => { const next = { ...current }; delete next[turnId]; return next })
      if (confirmation && String(confirmation.turn.turnoId) === turnId) setConfirmation(null)
    }
  }

  function requestAction(turn, action) {
    if (action === 'cancelar' || action === 'no-show' || action === 'completar') {
      setConfirmation({ turn, action })
      return
    }
    void performAction(turn, action)
  }

  return <section className="employee-dashboard" aria-labelledby="employee-dashboard-title">
    <UpcomingBoundary turns={turns} onBoundary={refreshTemporalState} />
    <header className="employee-dashboard-heading">
      <div>
        <span className="eyebrow">MI ESPACIO</span>
        {profileQuery.isPending ? <div className="employee-heading-skeleton" aria-label="Cargando perfil" />
          : <h1 id="employee-dashboard-title">{profile?.nombre ? `Hola, ${profile.nombre}` : 'Hola'}</h1>}
        <p className="employee-date">{dateLabel.charAt(0).toUpperCase() + dateLabel.slice(1)}{business?.nombre ? <><span aria-hidden="true"> · </span><span className="employee-business-name">{business.nombre}</span></> : null}</p>
      </div>
      <span className="employee-heading-icon"><CalendarDays size={21} /></span>
    </header>

    {profileQuery.isError && <ProfileError onRetry={() => profileQuery.refetch()} isFetching={profileQuery.isFetching} />}

    <section className="employee-section" aria-labelledby="employee-metrics-title">
      <div className="employee-section-heading"><div><span className="employee-section-kicker">RESUMEN</span><h2 id="employee-metrics-title">Tu jornada</h2></div>
        {appointmentsQuery.isFetching && appointmentsQuery.isSuccess && <span className="employee-refreshing">Actualizando…</span>}
      </div>
      {appointmentsQuery.isPending ? <DashboardSkeleton /> : appointmentsQuery.isError ? <TurnosError onRetry={() => appointmentsQuery.refetch()} isFetching={appointmentsQuery.isFetching} /> : <div className="employee-metrics-grid">
        <MetricCard label="Turnos de hoy" value={turns.length} detail="Todos los estados" icon={CalendarDays} />
        <MetricCard label="Pendientes" value={turns.filter((turn) => turn.status === 'PENDING').length} detail="Por confirmar" icon={Clock3} tone="is-pending" />
        <MetricCard label="En curso" value={turns.filter((turn) => turn.status === 'IN_PROGRESS').length} detail="En atención" icon={UsersRound} tone="is-active" />
        <MetricCard label="Completados" value={turns.filter((turn) => turn.status === 'COMPLETED').length} detail="Finalizados hoy" icon={Check} tone="is-completed" />
      </div>}
    </section>

    <section className="employee-section employee-agenda-section" aria-labelledby="employee-next-title">
      <div className="employee-section-heading"><div><span className="employee-section-kicker">A CONTINUACIÓN</span><h2 id="employee-next-title">Próximo turno</h2></div></div>
      {appointmentsQuery.isPending ? <div className="employee-panel-skeleton" role="status" aria-label="Cargando próximo turno" />
        : appointmentsQuery.isError ? null : <NextAppointment turn={futureTurns[0]} />}
    </section>

    <section className="employee-section" aria-labelledby="employee-upcoming-title">
      <div className="employee-section-heading employee-upcoming-heading"><div><span className="employee-section-kicker">AGENDA DE HOY</span><h2 id="employee-upcoming-title">Turnos de hoy</h2></div>
        {!appointmentsQuery.isPending && !appointmentsQuery.isError && <span className="employee-upcoming-count">{turns.length} {turns.length === 1 ? 'turno' : 'turnos'}</span>}
      </div>
      {appointmentsQuery.isPending ? <div className="employee-list-skeleton" role="status" aria-label="Cargando agenda"><span /><span /><span /></div>
        : appointmentsQuery.isError ? null
          : turns.length === 0 ? <div className="employee-empty-state"><CalendarDays size={20} /><strong>No hay turnos para hoy</strong><p>Tu agenda de hoy está al día.</p></div>
            : <ul className="employee-appointments-list">{turns.map((turn) => {
              const turnId = String(turn.turnoId)
              return <AppointmentRow
                key={turn.turnoId}
                turn={turn}
                isProcessing={pendingTurnIds.has(turnId)}
                activeAction={pendingActions[turnId]}
                feedback={actionFeedback[turnId]}
                onAction={requestAction}
                onRetry={performAction}
                onDismissFeedback={(id) => setActionFeedback((current) => { const next = { ...current }; delete next[String(id)]; return next })}
              />
            })}</ul>}
    </section>

    <section className="employee-section employee-shortcuts-section" aria-labelledby="employee-shortcuts-title">
      <div className="employee-section-heading"><div><span className="employee-section-kicker">TU ESPACIO</span><h2 id="employee-shortcuts-title">Accesos</h2></div></div>
      <div className="employee-shortcuts-grid">
        <ComingSoonCard icon={CalendarDays} title="Agenda" detail="Organizá tu jornada" />
        <ComingSoonCard icon={History} title="Historial" detail="Consultá turnos anteriores" />
        <ComingSoonCard icon={UserRound} title="Mi perfil" detail="Tus datos personales" />
        <ComingSoonCard icon={Bell} title="Notificaciones" detail="Novedades de tu cuenta" />
      </div>
    </section>
    {confirmation && <div className="employee-confirm-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !confirmationIsProcessing) setConfirmation(null) }}>
      <section className="employee-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="employee-confirm-title" aria-describedby="employee-confirm-description">
        <div className="employee-confirm-heading">
          <span className={`employee-confirm-icon is-${confirmation.action}`}><ConfirmationIcon action={confirmation.action} /></span>
          <button type="button" className="employee-confirm-close" aria-label="Cerrar diálogo" onClick={() => setConfirmation(null)} disabled={confirmationIsProcessing}><X size={17} /></button>
        </div>
        <h2 id="employee-confirm-title">{confirmation.action === 'cancelar' ? '¿Cancelar este turno?' : confirmation.action === 'completar' ? '¿Completar este turno?' : '¿Marcar al cliente como ausente?'}</h2>
        <p id="employee-confirm-description">{confirmation.action === 'cancelar'
          ? 'El turno pasará a estado CANCELLED. Esta acción no podrá revertirse desde tu cuenta de empleado.'
          : confirmation.action === 'completar'
            ? 'El turno pasará a estado COMPLETED. Confirmá que el servicio fue realizado correctamente.'
            : 'El turno pasará a estado NO_SHOW. Esta acción no podrá revertirse desde tu cuenta de empleado.'}</p>
        <div className="employee-confirm-actions">
          <button type="button" className="employee-action-button is-secondary" onClick={() => setConfirmation(null)} disabled={confirmationIsProcessing}>Volver</button>
          <button type="button" className={`employee-action-button ${confirmation.action === 'completar' ? 'is-primary' : 'is-danger'}`} onClick={() => void performAction(confirmation.turn, confirmation.action)} disabled={confirmationIsProcessing}>
            {confirmationIsProcessing ? <LoaderCircle size={14} className="employee-action-spinner" /> : null}
            {confirmation.action === 'cancelar'
              ? confirmationIsProcessing ? 'Cancelando…' : 'Sí, cancelar turno'
              : confirmation.action === 'completar'
                ? confirmationIsProcessing ? 'Completando…' : 'Sí, completar turno'
                : confirmationIsProcessing ? 'Marcando ausencia…' : 'Sí, marcar ausencia'}
          </button>
        </div>
      </section>
    </div>}
  </section>
}

function ConfirmationIcon({ action }) {
  const Icon = action === 'cancelar' ? Ban : action === 'completar' ? CheckCircle2 : UserRoundX
  return <Icon size={20} />
}
