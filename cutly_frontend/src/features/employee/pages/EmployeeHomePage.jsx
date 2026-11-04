import { useEffect, useMemo, useState } from 'react'
import { Bell, CalendarDays, Check, Clock3, History, RefreshCw, UserRound, UsersRound } from 'lucide-react'
import { useEmployeeAppointments, useEmployeeBusiness, useEmployeeProfile } from '../hooks/useEmployeeDashboard.js'
import './EmployeeHomePage.css'

const ACTIVE_UPCOMING_STATUSES = new Set(['PENDING', 'CONFIRMED'])

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

function AppointmentRow({ turn }) {
  return <li className="employee-appointment-row">
    <time className="employee-appointment-time">{formatTime(turn.fechaHoraInicio)}</time>
    <span className="employee-appointment-marker" aria-hidden="true" />
    <div className="employee-appointment-details">
      <strong>{clientName(turn)}</strong>
      <span>{turn.servicioNombre || 'Servicio no disponible'}</span>
    </div>
    <span className={`employee-status employee-status-${turn.status?.toLowerCase() || 'unknown'}`}>{statusLabel(turn.status)}</span>
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
  const refreshTemporalState = useMemo(() => () => setNow(new Date()), [])
  const today = localDateKey(now)
  const profileQuery = useEmployeeProfile()
  const businessQuery = useEmployeeBusiness()
  const appointmentsQuery = useEmployeeAppointments(today)
  const turns = appointmentsQuery.data ?? []

  const futureTurns = useMemo(() => turns
    .filter((turn) => ACTIVE_UPCOMING_STATUSES.has(turn.status))
    .filter((turn) => new Date(turn.fechaHoraInicio).getTime() > now.getTime())
    .sort((a, b) => new Date(a.fechaHoraInicio).getTime() - new Date(b.fechaHoraInicio).getTime()), [turns, now])

  const dateLabel = new Intl.DateTimeFormat('es-AR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(now)
  const profile = profileQuery.data
  const business = businessQuery.data

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
      <div className="employee-section-heading employee-upcoming-heading"><div><span className="employee-section-kicker">AGENDA DE HOY</span><h2 id="employee-upcoming-title">Próximos turnos</h2></div>
        {!appointmentsQuery.isPending && !appointmentsQuery.isError && <span className="employee-upcoming-count">{futureTurns.length} {futureTurns.length === 1 ? 'turno' : 'turnos'}</span>}
      </div>
      {appointmentsQuery.isPending ? <div className="employee-list-skeleton" role="status" aria-label="Cargando agenda"><span /><span /><span /></div>
        : appointmentsQuery.isError ? null
          : futureTurns.length === 0 ? <div className="employee-empty-state"><CalendarDays size={20} /><strong>No hay próximos turnos</strong><p>Tu agenda de hoy está al día.</p></div>
            : <ul className="employee-appointments-list">{futureTurns.map((turn) => <AppointmentRow key={turn.turnoId} turn={turn} />)}</ul>}
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
  </section>
}
