import { AlertCircle, ArrowLeft, CalendarDays, Clock3, LoaderCircle, MapPin, Scissors, UserRound } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { getClientAppointmentDetailErrorMessage } from '../api/clientApi.js'
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

export default function ClientAppointmentDetailPage() {
  const { turnoId } = useParams()
  const validId = validAppointmentId(turnoId)
  const query = useClientAppointmentDetail(turnoId)
  const appointment = query.data

  if (!validId) {
    return <DetailUnavailable title="Turno no disponible" message="El identificador del turno no es válido." />
  }
  if (query.isPending) return <AppointmentDetailLoading />
  if (query.isError) {
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

  return (
    <section className="client-appointment-detail-page">
      <Link className="client-appointment-detail-back" to="/client/turnos"><ArrowLeft size={16} aria-hidden="true" /> Volver a Mis turnos</Link>

      <header className="client-appointment-detail-hero">
        <div className="client-appointment-detail-hero-icon" aria-hidden="true"><Scissors size={21} /></div>
        <div className="client-appointment-detail-hero-copy">
          <span className="client-appointment-detail-eyebrow">DETALLE DE TURNO</span>
          <h1>Turno #{appointment.turnoId}</h1>
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

    </section>
  )
}
