import { useEffect, useRef, useState } from 'react'
import { Ban, Check, CheckCircle2, LoaderCircle, RefreshCw, UserRoundX, X } from 'lucide-react'
import { getEmployeeAppointmentActionErrorMessage } from '../api/employeeApi.js'
import { useEmployeeAppointmentAction } from '../hooks/useEmployeeDashboard.js'
import { EMPLOYEE_APPOINTMENT_ACTIONS, employeeClientName, employeeStatusLabel } from './employeeAppointmentPresentation.js'
import '../pages/EmployeeHomePage.css'

const ACTION_DETAILS = {
  confirmar: { label: 'Confirmar', icon: CheckCircle2, style: 'is-primary', success: 'Turno confirmado.' },
  cancelar: { label: 'Cancelar', icon: Ban, style: 'is-danger', success: 'Turno cancelado.' },
  completar: { label: 'Completar', icon: Check, style: 'is-secondary', success: 'Turno completado.' },
  'no-show': { label: 'Marcar ausencia', icon: UserRoundX, style: 'is-danger', success: 'Ausencia marcada.' },
}

function formatTime(value, agenda) {
  if (!value) return agenda ? '—' : 'Hora no disponible'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return agenda ? '—' : 'Hora no disponible'
  return new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(parsed)
}

function AppointmentRow({ turn, isProcessing, activeAction, feedback, onAction, onRetry, onDismissFeedback, agenda }) {
  const actions = EMPLOYEE_APPOINTMENT_ACTIONS[turn.status] || []
  const actionControls = (agenda || actions.length > 0) && <div className="employee-appointment-actions" aria-label={actions.length > 0 ? `Acciones para ${employeeClientName(turn)}` : undefined} aria-hidden={actions.length === 0 ? 'true' : undefined}>
    {actions.map((action) => {
      const details = ACTION_DETAILS[action]
      const Icon = isProcessing && activeAction === action ? LoaderCircle : details.icon
      return <button key={action} type="button" className={`employee-action-button ${details.style}`} onClick={() => onAction(turn, action)} disabled={isProcessing}>
        <Icon size={14} className={isProcessing && activeAction === action ? 'employee-action-spinner' : ''} />{details.label}
      </button>
    })}
  </div>
  const statusBadge = <span className={`employee-status employee-status-${turn.status?.toLowerCase() || 'unknown'}`}>{employeeStatusLabel(turn.status)}</span>
  return <li className={`employee-appointment-row${actions.length ? ' has-actions' : ''}${agenda ? ' employee-agenda-row' : ''}`}>
    <time className="employee-appointment-time">{formatTime(turn.fechaHoraInicio, agenda)}{agenda && <span className="employee-agenda-end-time">– {formatTime(turn.fechaHoraFin, agenda)}</span>}</time>
    {!agenda && <span className="employee-appointment-marker" aria-hidden="true" />}
    <div className="employee-appointment-details">
      <strong>{employeeClientName(turn)}</strong>
      <span>{turn.servicioNombre || 'Servicio no disponible'}{agenda && turn.precio != null ? ` · $${Number(turn.precio).toLocaleString('es-AR')}` : ''}</span>
    </div>
    {agenda ? <>{actionControls}{statusBadge}</> : <>{statusBadge}{actionControls}</>}
    {feedback && <div className={`employee-action-feedback is-${feedback.type}`} role={feedback.type === 'error' ? 'alert' : 'status'}>
      <span>{feedback.message}</span>
      {feedback.type === 'error' && feedback.retryable && <button type="button" onClick={() => onRetry(turn, feedback.action)} disabled={isProcessing}><RefreshCw size={13} /> Reintentar</button>}
      <button type="button" className="employee-feedback-dismiss" aria-label="Cerrar mensaje" onClick={() => onDismissFeedback(turn.turnoId)}><X size={14} /></button>
    </div>}
  </li>
}

export default function EmployeeAppointmentsList({ turns, agenda = false }) {
  const [pendingTurnIds, setPendingTurnIds] = useState(() => new Set())
  const [pendingActions, setPendingActions] = useState({})
  const [actionFeedback, setActionFeedback] = useState({})
  const [confirmation, setConfirmation] = useState(null)
  const pendingTurnIdsRef = useRef(new Set())
  const actionMutation = useEmployeeAppointmentAction()
  const confirmationTurnId = confirmation ? String(confirmation.turn.turnoId) : ''
  const confirmationIsProcessing = confirmationTurnId ? pendingTurnIds.has(confirmationTurnId) : false

  useEffect(() => {
    if (!confirmation) return undefined
    function closeOnEscape(event) {
      if (event.key === 'Escape' && !pendingTurnIdsRef.current.has(String(confirmation.turn.turnoId))) setConfirmation(null)
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
      setActionFeedback((current) => ({ ...current, [turnId]: { type: 'error', message: getEmployeeAppointmentActionErrorMessage(error), action, retryable: ![400, 401, 403, 404, 409].includes(status) } }))
    } finally {
      pendingTurnIdsRef.current.delete(turnId)
      setPendingTurnIds(new Set(pendingTurnIdsRef.current))
      setPendingActions((current) => { const next = { ...current }; delete next[turnId]; return next })
      if (confirmation && String(confirmation.turn.turnoId) === turnId) setConfirmation(null)
    }
  }

  function requestAction(turn, action) {
    if (['cancelar', 'no-show', 'completar'].includes(action)) setConfirmation({ turn, action })
    else void performAction(turn, action)
  }

  return <>
    <ul className={`employee-appointments-list${agenda ? ' employee-agenda-list' : ''}`}>{turns.map((turn) => {
      const turnId = String(turn.turnoId)
      const feedback = actionFeedback[turnId]
      return <AppointmentRow key={turn.turnoId} turn={turn} agenda={agenda} isProcessing={pendingTurnIds.has(turnId)} activeAction={pendingActions[turnId]} feedback={feedback} onAction={requestAction} onRetry={performAction}
        onDismissFeedback={(id) => setActionFeedback((current) => { const next = { ...current }; delete next[String(id)]; return next })} />
    })}</ul>
    {confirmation && <div className="employee-confirm-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !confirmationIsProcessing) setConfirmation(null) }}>
      <section className="employee-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="employee-confirm-title" aria-describedby="employee-confirm-description">
        <div className="employee-confirm-heading"><span className={`employee-confirm-icon is-${confirmation.action}`}><ConfirmationIcon action={confirmation.action} /></span>
          <button type="button" className="employee-confirm-close" aria-label="Cerrar diálogo" onClick={() => setConfirmation(null)} disabled={confirmationIsProcessing}><X size={17} /></button></div>
        <h2 id="employee-confirm-title">{confirmation.action === 'cancelar' ? '¿Cancelar este turno?' : confirmation.action === 'completar' ? '¿Completar este turno?' : '¿Marcar al cliente como ausente?'}</h2>
        <p id="employee-confirm-description">{confirmation.action === 'cancelar' ? 'El turno pasará a estado CANCELLED. Esta acción no podrá revertirse desde tu cuenta de empleado.' : confirmation.action === 'completar' ? 'El turno pasará a estado COMPLETED. Confirmá que el servicio fue realizado correctamente.' : 'El turno pasará a estado NO_SHOW. Esta acción no podrá revertirse desde tu cuenta de empleado.'}</p>
        <div className="employee-confirm-actions">
          <button type="button" className="employee-action-button is-secondary" onClick={() => setConfirmation(null)} disabled={confirmationIsProcessing}>Volver</button>
          <button type="button" className={`employee-action-button ${confirmation.action === 'completar' ? 'is-primary' : 'is-danger'}`} onClick={() => void performAction(confirmation.turn, confirmation.action)} disabled={confirmationIsProcessing}>
            {confirmationIsProcessing && <LoaderCircle size={14} className="employee-action-spinner" />}
            {confirmation.action === 'cancelar' ? confirmationIsProcessing ? 'Cancelando…' : 'Sí, cancelar turno' : confirmation.action === 'completar' ? confirmationIsProcessing ? 'Completando…' : 'Sí, completar turno' : confirmationIsProcessing ? 'Marcando ausencia…' : 'Sí, marcar ausencia'}
          </button>
        </div>
      </section>
    </div>}
  </>
}

function ConfirmationIcon({ action }) {
  const Icon = action === 'cancelar' ? Ban : action === 'completar' ? CheckCircle2 : UserRoundX
  return <Icon size={20} />
}
