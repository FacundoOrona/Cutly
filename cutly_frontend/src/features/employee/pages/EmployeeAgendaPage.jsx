import { useMemo, useState } from 'react'
import { CalendarDays, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { useEmployeeAppointments } from '../hooks/useEmployeeDashboard.js'
import EmployeeAppointmentsList from '../components/EmployeeAppointmentsList.jsx'
import { employeeStatusLabel } from '../components/employeeAppointmentPresentation.js'
import './EmployeeHomePage.css'
import './EmployeeAgendaPage.css'

const STATUS_OPTIONS = [
  ['', 'Todos'], ['PENDING', 'Pendientes'], ['CONFIRMED', 'Confirmados'], ['IN_PROGRESS', 'En curso'],
  ['COMPLETED', 'Completados'], ['CANCELLED', 'Cancelados'], ['NO_SHOW', 'Ausentes'], ['REJECTED', 'Rechazados'],
]

function localDateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function shiftDate(dateKey, days) {
  const [year, month, day] = dateKey.split('-').map(Number)
  return localDateKey(new Date(year, month - 1, day + days, 12))
}

function dateLabel(dateKey) {
  const [year, month, day] = dateKey.split('-').map(Number)
  return new Intl.DateTimeFormat('es-AR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(year, month - 1, day, 12))
}

function AgendaSkeleton() {
  return <ul className="employee-agenda-skeleton" role="status" aria-label="Cargando agenda"><li /><li /><li /></ul>
}

export default function EmployeeAgendaPage() {
  const today = localDateKey(new Date())
  const [fecha, setFecha] = useState(today)
  const [status, setStatus] = useState('')
  const appointmentsQuery = useEmployeeAppointments(fecha, status)
  const turns = useMemo(() => [...(appointmentsQuery.data ?? [])].sort((a, b) => new Date(a.fechaHoraInicio) - new Date(b.fechaHoraInicio) || a.turnoId - b.turnoId), [appointmentsQuery.data])
  const label = dateLabel(fecha)
  const formattedDate = label.charAt(0).toUpperCase() + label.slice(1)
  const selectedStatusLabel = STATUS_OPTIONS.find(([value]) => value === status)?.[1]

  return <section className="employee-dashboard employee-agenda-page" aria-labelledby="employee-agenda-title">
    <header className="employee-dashboard-heading employee-agenda-heading">
      <div><span className="eyebrow">MI ESPACIO</span><h1 id="employee-agenda-title">Agenda</h1><p className="employee-date">{formattedDate}</p></div>
      <span className="employee-heading-icon"><CalendarDays size={21} /></span>
    </header>

    <section className="employee-agenda-controls" aria-label="Controles de agenda">
      <div className="employee-agenda-date-controls">
        <button type="button" className="employee-agenda-nav" aria-label="Día anterior" onClick={() => setFecha((current) => shiftDate(current, -1))}><ChevronLeft size={18} /></button>
        <label className="employee-agenda-date-picker"><CalendarDays size={16} /><input type="date" value={fecha} onChange={(event) => event.target.value && setFecha(event.target.value)} aria-label="Seleccionar fecha" /></label>
        <button type="button" className="employee-agenda-nav" aria-label="Día siguiente" onClick={() => setFecha((current) => shiftDate(current, 1))}><ChevronRight size={18} /></button>
        <button type="button" className="employee-agenda-today" onClick={() => setFecha(localDateKey(new Date()))}>Hoy</button>
      </div>
      <label className="employee-agenda-filter"><span>Estado</span><select value={status} onChange={(event) => setStatus(event.target.value)} aria-label="Filtrar turnos por estado">
        {STATUS_OPTIONS.map(([value, optionLabel]) => <option key={value || 'all'} value={value}>{optionLabel}</option>)}
      </select></label>
    </section>

    <section className="employee-section employee-agenda-results" aria-labelledby="employee-agenda-results-title" aria-busy={appointmentsQuery.isPending || appointmentsQuery.isFetching}>
      <div className="employee-section-heading employee-upcoming-heading">
        <div><span className="employee-section-kicker">{status ? `FILTRADO: ${employeeStatusLabel(status).toUpperCase()}` : 'TURNOS DEL DÍA'} </span><h2 id="employee-agenda-results-title">{formattedDate}</h2></div>
        {appointmentsQuery.isSuccess && <span className="employee-upcoming-count">{turns.length} {turns.length === 1 ? 'turno' : 'turnos'}</span>}
        {appointmentsQuery.isFetching && appointmentsQuery.isSuccess && <span className="employee-refreshing">Actualizando…</span>}
      </div>
      {appointmentsQuery.isPending ? <AgendaSkeleton />
        : appointmentsQuery.isError ? <div className="employee-query-error" role="alert"><CalendarDays size={22} /><strong>No pudimos cargar la agenda</strong><p>Revisá tu conexión e intentá nuevamente.</p><button type="button" onClick={() => appointmentsQuery.refetch()} disabled={appointmentsQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div>
          : turns.length === 0 ? <div className="employee-empty-state"><CalendarDays size={20} /><strong>{status ? `No hay turnos ${selectedStatusLabel.toLowerCase()} para esta fecha` : 'No hay turnos para esta fecha'}</strong><p>Probá con otro día o estado para consultar tu agenda.</p></div>
            : <EmployeeAppointmentsList turns={turns} agenda />}
    </section>
  </section>
}
