import { useEffect, useRef, useState } from 'react'
import { AlertCircle, ArrowLeft, Ban, CalendarDays, Clock3, LoaderCircle, MapPin, Scissors, UserRound, X } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { getClientAppointmentCancellationErrorMessage, getClientAppointmentDetailErrorMessage } from '../api/clientApi.js'
import useCancelClientAppointment from '../hooks/useCancelClientAppointment.js'
import useClientAppointmentDetail from '../hooks/useClientAppointmentDetail.js'
import { formatClientAppointmentDate, formatClientAppointmentTime } from '../utils/clientAppointmentDateTime.js'
import './ClientAppointmentDetailPage.css'

const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

const statusLabels = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmado',
  IN_PROGRESS: 'En curso',
  COMPLETED: 'Completado',
  CANCELLED: 'Cancelado',
  NO_SHOW: 'Ausente',
  REJECTED: 'Rechazado',
}
const cancellableStatuses = new Set(['PENDING', 'CONFIRMED', 'IN_PROGRESS'])

const validAppointmentId = (id) => typeof id === 'string'
  && /^[1-9]\d*$/.test(id)
  && Number.isSafeInteger(Number(id))

function displayName(value) {
  return typeof value === 'string' && value.trim() ? value.trim() : 'No disponible'
}

function formatPrice(value) {
  const amount = Number(value)
  return value !== null && value !== undefined && Number.isFinite(amount)
    ? currencyFormatter.format(amount)
    : 'No disponible'
}

function formatStatus(status) {
  return statusLabels[status] || (typeof status === 'string' && status.trim() ? status : 'Estado no disponible')
}

function AppointmentDetailLoading() {
  return (
    <div className="client-appointment-detail-loading" role="status" aria-label="Cargando el detalle del turno">
      <LoaderCircle size={21} aria-hidden="true" />
      <span>Cargando el detalle del turno…</span>
    </div>
  )
}

function DetailUnavailable({ title, message, onRetry, retryDisabled }) {
  return (
    <section className="client-appointment-detail-message" role="status">
      <span className="client-appointment-detail-message-icon" aria-hidden="true"><AlertCircle size={21} /></span>
      <h1>{title}</h1>
      <p>{message}</p>
      {onRetry && <button type="button" onClick={onRetry} disabled={retryDisabled}>{retryDisabled ? 'Reintentando…' : 'Reintentar'}</button>}
      <Link className="client-appointment-detail-back" to="/client/turnos"><ArrowLeft size={16} aria-hidden="true" /> Volver a Mis turnos</Link>
    </section>
  )
}

function DetailField({ label, children }) {
  return <div className="client-appointment-detail-field"><dt>{label}</dt><dd>{children}</dd></div>
}

function trapDialogFocus(event, dialog) {
  if (event.key !== 'Tab') return
  const focusable = [...dialog.querySelectorAll('button:not([disabled])')]
  if (focusable.length === 0) {
    event.preventDefault()
    return
  }

  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog)) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && (document.activeElement === last || document.activeElement === dialog)) {
    event.preventDefault()
    first.focus()
  }
}

export default function ClientAppointmentDetailPage() {
  const { turnoId } = useParams()
  const validId = validAppointmentId(turnoId)
  const query = useClientAppointmentDetail(turnoId)
  const cancellation = useCancelClientAppointment()
  const [isCancelDialogOpen, setIsCancelDialogOpen] = useState(false)
  const [cancelSucceeded, setCancelSucceeded] = useState(false)
  const cancelTriggerRef = useRef(null)
  const detailHeadingRef = useRef(null)
  const dialogRef = useRef(null)
  const cancelPendingRef = useRef(false)
  cancelPendingRef.current = cancellation.isPending
  const appointment = query.data

  useEffect(() => {
    if (!isCancelDialogOpen) return undefined

    dialogRef.current?.focus()
    function handleDialogKeyDown(event) {
      if (event.key === 'Escape' && !cancelPendingRef.current) {
        setIsCancelDialogOpen(false)
        return
      }
      trapDialogFocus(event, dialogRef.current)
    }

    document.addEventListener('keydown', handleDialogKeyDown)
    return () => {
      document.removeEventListener('keydown', handleDialogKeyDown)
      if (cancelTriggerRef.current?.isConnected) cancelTriggerRef.current.focus()
      else if (detailHeadingRef.current?.isConnected) detailHeadingRef.current.focus()
    }
  }, [isCancelDialogOpen])

  if (!validId) {
    return <DetailUnavailable title="Turno no disponible" message="El identificador del turno no es válido." />
  }
  if (query.isPending) return <AppointmentDetailLoading />
  if (query.isError && (!query.data || [401, 403, 404].includes(query.error.response?.status))) {
    const status = query.error.response?.status
    const canRetry = status !== 401 && status !== 403 && status !== 404
    return (
      <DetailUnavailable
        title={status === 404 ? 'Turno no disponible' : 'No pudimos cargar el turno'}
        message={getClientAppointmentDetailErrorMessage(query.error)}
        onRetry={canRetry ? () => query.refetch() : undefined}
        retryDisabled={query.isFetching}
      />
    )
  }
  if (!appointment) {
    return <DetailUnavailable title="Turno no disponible" message="No encontramos la información de este turno." />
  }

  const statusClass = statusLabels[appointment.status]
    ? appointment.status.toLowerCase().replace(/_/g, '-')
    : 'unknown'
  const professionalName = [appointment.empleadoNombre, appointment.empleadoApellido]
    .filter((part) => typeof part === 'string' && part.trim())
    .map((part) => part.trim())
    .join(' ')
  const canCancel = cancellableStatuses.has(appointment.status)
  const cancellationErrorStatus = cancellation.error?.response?.status
  const canRetryCancellation = ![401, 403, 404].includes(cancellationErrorStatus)

  function openCancelDialog() {
    if (!canCancel || cancellation.isPending) return
    cancellation.reset()
    setCancelSucceeded(false)
    setIsCancelDialogOpen(true)
  }

  function closeCancelDialog() {
    if (cancellation.isPending) return
    setIsCancelDialogOpen(false)
  }

  async function confirmCancellation() {
    if (!canCancel || cancellation.isPending || !canRetryCancellation) return
    try {
      await cancellation.mutateAsync(turnoId)
      setCancelSucceeded(true)
      setIsCancelDialogOpen(false)
    } catch {
      // El error queda en la mutation para mostrarse en el diálogo.
    }
  }

  return (
    <section className="client-appointment-detail-page">
      <Link className="client-appointment-detail-back" to="/client/turnos"><ArrowLeft size={16} aria-hidden="true" /> Volver a Mis turnos</Link>

      <header className="client-appointment-detail-hero">
        <div className="client-appointment-detail-hero-icon" aria-hidden="true"><Scissors size={21} /></div>
        <div className="client-appointment-detail-hero-copy">
          <span className="client-appointment-detail-eyebrow">DETALLE DE TURNO</span>
          <h1 ref={detailHeadingRef} tabIndex="-1">Turno #{appointment.turnoId}</h1>
          <p>Información de tu reserva</p>
        </div>
        <span className={`client-appointment-status client-appointment-status-${statusClass}`}>
          {formatStatus(appointment.status)}
        </span>
      </header>

      <section className="client-appointment-detail-section" aria-labelledby="client-appointment-business-title">
        <div className="client-appointment-detail-section-heading">
          <MapPin size={17} aria-hidden="true" />
          <h2 id="client-appointment-business-title">Negocio</h2>
        </div>
        <p className="client-appointment-detail-primary-value">{displayName(appointment.negocioNombre)}</p>
      </section>

      <section className="client-appointment-detail-section" aria-labelledby="client-appointment-service-title">
        <div className="client-appointment-detail-section-heading">
          <Scissors size={17} aria-hidden="true" />
          <h2 id="client-appointment-service-title">Servicio</h2>
        </div>
        <dl className="client-appointment-detail-grid">
          <DetailField label="Nombre">{displayName(appointment.servicioNombre)}</DetailField>
          <DetailField label="Precio">{formatPrice(appointment.precio)}</DetailField>
        </dl>
      </section>

      <section className="client-appointment-detail-section" aria-labelledby="client-appointment-professional-title">
        <div className="client-appointment-detail-section-heading">
          <UserRound size={17} aria-hidden="true" />
          <h2 id="client-appointment-professional-title">Profesional</h2>
        </div>
        <p className="client-appointment-detail-primary-value">
          {professionalName || 'Profesional no disponible'}
        </p>
      </section>

      <section className="client-appointment-detail-section" aria-labelledby="client-appointment-schedule-title">
        <div className="client-appointment-detail-section-heading">
          <CalendarDays size={17} aria-hidden="true" />
          <h2 id="client-appointment-schedule-title">Fecha y horario</h2>
        </div>
        <dl className="client-appointment-detail-grid">
          <DetailField label="Fecha">{formatClientAppointmentDate(appointment.fechaHoraInicio)}</DetailField>
          <DetailField label="Hora de inicio">{formatClientAppointmentTime(appointment.fechaHoraInicio)}</DetailField>
          <DetailField label="Hora de finalización">{formatClientAppointmentTime(appointment.fechaHoraFin)}</DetailField>
        </dl>
      </section>

      {typeof appointment.observaciones === 'string' && appointment.observaciones.trim() && (
        <section className="client-appointment-detail-section" aria-labelledby="client-appointment-notes-title">
          <div className="client-appointment-detail-section-heading"><h2 id="client-appointment-notes-title">Observaciones</h2></div>
          <p className="client-appointment-detail-notes">{appointment.observaciones}</p>
        </section>
      )}

      {canCancel && (
        <section className="client-appointment-detail-section client-appointment-actions-section" aria-labelledby="client-appointment-actions-title">
          <div className="client-appointment-detail-section-heading">
            <Ban size={17} aria-hidden="true" />
            <h2 id="client-appointment-actions-title">Acciones</h2>
          </div>
          <button
            ref={cancelTriggerRef}
            className="client-appointment-cancel-trigger"
            type="button"
            onClick={openCancelDialog}
            disabled={cancellation.isPending}
          >
            Cancelar turno
          </button>
        </section>
      )}

      {cancelSucceeded && <div className="client-appointment-cancel-success" role="status">El turno fue cancelado.</div>}

      {isCancelDialogOpen && (
        <div
          className="client-appointment-cancel-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !cancellation.isPending) closeCancelDialog()
          }}
        >
          <section
            ref={dialogRef}
            className="client-appointment-cancel-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="client-appointment-cancel-title"
            aria-describedby="client-appointment-cancel-description"
            tabIndex="-1"
          >
            <div className="client-appointment-cancel-dialog-heading">
              <span className="client-appointment-cancel-dialog-icon" aria-hidden="true"><Ban size={19} /></span>
              <button type="button" className="client-appointment-cancel-close" aria-label="Cerrar diálogo" onClick={closeCancelDialog} disabled={cancellation.isPending}>
                <X size={17} />
              </button>
            </div>
            <h2 id="client-appointment-cancel-title">¿Cancelar este turno?</h2>
            <p id="client-appointment-cancel-description">Esta acción cancelará tu reserva. ¿Estás seguro de que deseas continuar?</p>
            {cancellation.error && (
              <p className="client-appointment-cancel-error" role="alert">
                {getClientAppointmentCancellationErrorMessage(cancellation.error)}
              </p>
            )}
            <div className="client-appointment-cancel-dialog-actions">
              <button type="button" className="client-appointment-cancel-secondary" onClick={closeCancelDialog} disabled={cancellation.isPending}>
                Volver
              </button>
              <button
                type="button"
                className="client-appointment-cancel-confirm"
                onClick={confirmCancellation}
                disabled={cancellation.isPending || !canCancel || !canRetryCancellation}
              >
                {cancellation.isPending && <LoaderCircle size={15} aria-hidden="true" />}
                {cancellation.isPending ? 'Cancelando…' : 'Sí, cancelar turno'}
              </button>
            </div>
          </section>
        </div>
      )}

    </section>
  )
}
