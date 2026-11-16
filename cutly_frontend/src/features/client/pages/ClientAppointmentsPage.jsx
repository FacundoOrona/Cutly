import { useEffect, useState } from 'react'
import { AlertCircle, CalendarDays, Compass, LoaderCircle } from 'lucide-react'
import { Link } from 'react-router-dom'
import ClientAppointmentCard from '../components/ClientAppointmentCard.jsx'
import { getClientAppointmentsErrorMessage } from '../api/clientApi.js'
import useClientAppointments from '../hooks/useClientAppointments.js'
import { parseClientLocalDateTime } from '../utils/clientAppointmentDateTime.js'
import './ClientAppointmentsPage.css'

export function classifyClientAppointments(appointments, now = new Date()) {
  const upcoming = []
  const history = []

  for (const appointment of appointments) {
    const status = appointment.status
    const start = parseClientLocalDateTime(appointment.fechaHoraInicio)
    const isUpcoming = status === 'IN_PROGRESS'
      || ((status === 'PENDING' || status === 'CONFIRMED') && start !== null && start.getTime() > now.getTime())

    if (isUpcoming) upcoming.push(appointment)
    else history.push(appointment)
  }

  upcoming.sort((first, second) => String(first.fechaHoraInicio || '').localeCompare(String(second.fechaHoraInicio || '')))
  history.sort((first, second) => String(second.fechaHoraInicio || '').localeCompare(String(first.fechaHoraInicio || '')))
  return { upcoming, history }
}

function AppointmentSkeleton() {
  return <div className="client-appointments-skeleton" aria-hidden="true"><span /><div><i /><i /><i /></div></div>
}

function AppointmentSection({ title, count, type, appointments }) {
  return (
    <section className="client-appointments-section" aria-labelledby={`client-appointments-${type}-title`}>
      <header className="client-appointments-section-heading">
        <div className="client-appointments-section-mark" aria-hidden="true"><CalendarDays size={17} /></div>
        <h2 id={`client-appointments-${type}-title`}>{title}</h2>
        <span className="client-appointments-count" aria-label={`${count} turnos`}>{count}</span>
      </header>
      {appointments.length > 0 ? (
        <div className="client-appointments-list">
          {appointments.map((appointment) => <ClientAppointmentCard key={appointment.turnoId} appointment={appointment} />)}
        </div>
      ) : type === 'upcoming' ? (
        <div className="client-appointments-empty">
          <span className="client-appointments-empty-icon" aria-hidden="true"><Compass size={20} /></span>
          <h3>No tenés próximos turnos</h3>
          <p>Explorá los negocios disponibles y encontrá un horario para tu próxima visita.</p>
          <Link to="/client/explorar">Explorar negocios</Link>
        </div>
      ) : (
        <div className="client-appointments-empty client-appointments-history-empty">
          <p>Cuando tengas turnos anteriores o finalizados, los vas a encontrar acá.</p>
        </div>
      )}
    </section>
  )
}

export default function ClientAppointmentsPage() {
  const query = useClientAppointments()
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const interval = window.setInterval(() => setNow(new Date()), 60_000)
    return () => window.clearInterval(interval)
  }, [])

  const sections = classifyClientAppointments(query.data || [], now)

  return (
    <section className="client-appointments-page">
      <header className="client-appointments-heading">
        <span className="client-appointments-eyebrow">TU AGENDA EN CUTLY</span>
        <h1>Mis turnos</h1>
        <p>Consultá tus próximas visitas y revisá el historial de tus reservas.</p>
      </header>

      {query.isPending ? (
        <div className="client-appointments-loading" role="status" aria-label="Cargando tus turnos">
          <span className="client-appointments-loading-label"><LoaderCircle size={17} aria-hidden="true" /> Cargando tus turnos…</span>
          <AppointmentSkeleton /><AppointmentSkeleton />
        </div>
      ) : query.isError ? (
        <div className="client-appointments-error" role="alert">
          <AlertCircle size={21} aria-hidden="true" />
          <div><h2>No pudimos cargar tus turnos</h2><p>{getClientAppointmentsErrorMessage(query.error)}</p></div>
          <button type="button" onClick={() => query.refetch()} disabled={query.isFetching}>
            {query.isFetching ? 'Reintentando…' : 'Reintentar'}
          </button>
        </div>
      ) : (
        <div className="client-appointments-sections" aria-busy={query.isFetching}>
          <AppointmentSection title="Próximos turnos" type="upcoming" appointments={sections.upcoming} count={sections.upcoming.length} />
          <AppointmentSection title="Historial" type="history" appointments={sections.history} count={sections.history.length} />
        </div>
      )}
    </section>
  )
}
