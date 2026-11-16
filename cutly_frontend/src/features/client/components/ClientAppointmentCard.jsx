import { CalendarDays, Clock3, Scissors } from 'lucide-react'
import { formatClientAppointmentDate, formatClientAppointmentTime } from '../utils/clientAppointmentDateTime.js'
import './ClientAppointmentCard.css'

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

function formatPrice(value) {
  const amount = Number(value)
  return value !== null && value !== undefined && Number.isFinite(amount)
    ? currencyFormatter.format(amount)
    : 'No disponible'
}

export default function ClientAppointmentCard({ appointment }) {
  const status = appointment.status
  const statusClass = statusLabels[status] ? status.toLowerCase().replace(/_/g, '-') : 'unknown'
  const professional = [appointment.empleadoNombre, appointment.empleadoApellido]
    .filter((part) => typeof part === 'string' && part.trim())
    .map((part) => part.trim())
    .join(' ')

  return (
    <article className="client-appointment-card">
      <div className="client-appointment-card-main">
        <div className="client-appointment-card-heading">
          <div className="client-appointment-card-title">
            <span className="client-appointment-card-icon" aria-hidden="true"><Scissors size={17} /></span>
            <div>
              <h3>{appointment.negocioNombre || 'Negocio'}</h3>
              <p>{appointment.servicioNombre || 'Servicio'}</p>
            </div>
          </div>
          <span className={`client-appointment-status client-appointment-status-${statusClass}`}>
            {statusLabels[status] || status || 'Estado no disponible'}
          </span>
        </div>

        <div className="client-appointment-card-details">
          <div className="client-appointment-detail">
            <CalendarDays size={15} aria-hidden="true" />
            <span>{formatClientAppointmentDate(appointment.fechaHoraInicio)}</span>
          </div>
          <div className="client-appointment-detail">
            <Clock3 size={15} aria-hidden="true" />
            <span>{formatClientAppointmentTime(appointment.fechaHoraInicio)}–{formatClientAppointmentTime(appointment.fechaHoraFin)}</span>
          </div>
          {professional && <div className="client-appointment-detail client-appointment-professional"><span>Profesional</span><strong>{professional}</strong></div>}
          <div className="client-appointment-price">{formatPrice(appointment.precio)}</div>
        </div>
      </div>
    </article>
  )
}
