import { useEffect, useMemo, useState } from 'react'
import { CalendarDays, ChevronLeft, ChevronRight, History, RefreshCw, RotateCcw } from 'lucide-react'
import { useEmployeeAppointmentHistory } from '../hooks/useEmployeeDashboard.js'
import { employeeClientName, employeeStatusLabel } from '../components/employeeAppointmentPresentation.js'
import './EmployeeHomePage.css'
import './EmployeeAgendaPage.css'
import './EmployeeHistoryPage.css'

const STATUS_OPTIONS = [
  ['', 'Todos'], ['PENDING', 'Pendiente'], ['CONFIRMED', 'Confirmado'], ['IN_PROGRESS', 'En curso'],
  ['COMPLETED', 'Completado'], ['CANCELLED', 'Cancelado'], ['NO_SHOW', 'Ausente'], ['REJECTED', 'Rechazado'],
]
const HISTORY_PAGE_SIZE = 10

function historyStatusLabel(status) {
  return status === 'NO_SHOW' ? 'Ausente' : employeeStatusLabel(status)
}

function parseLocalDateTime(value) {
  if (!value) return null
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function formatDate(value) {
  const parsed = parseLocalDateTime(value)
  return parsed ? new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'long', year: 'numeric' }).format(parsed) : 'Fecha no disponible'
}

function formatTime(value) {
  const parsed = parseLocalDateTime(value)
  return parsed ? new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(parsed) : '--:--'
}

function HistorySkeleton() {
  return <ul className="employee-history-skeleton" role="status" aria-label="Cargando historial"><li /><li /><li /></ul>
}

function HistoryCard({ turn }) {
  return <article className="employee-history-card">
    <div className="employee-history-card-top">
      <div className="employee-history-date"><strong>{formatDate(turn.fechaHoraInicio)}</strong><span>{formatTime(turn.fechaHoraInicio)} – {formatTime(turn.fechaHoraFin)}</span></div>
      <span className={`employee-status employee-status-${turn.status?.toLowerCase() || 'unknown'}`}>{historyStatusLabel(turn.status)}</span>
    </div>
    <div className="employee-history-details">
      <div><span>Cliente</span><strong>{employeeClientName(turn)}</strong></div>
      <div><span>Servicio</span><strong>{turn.servicioNombre || 'Servicio no disponible'}</strong></div>
      {turn.precio != null && <div><span>Precio</span><strong>${Number(turn.precio).toLocaleString('es-AR')}</strong></div>}
    </div>
    {turn.observaciones && <p className="employee-history-notes"><span>Observaciones</span>{turn.observaciones}</p>}
  </article>
}

export default function EmployeeHistoryPage() {
  const [fecha, setFecha] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(1)
  const historyQuery = useEmployeeAppointmentHistory(fecha, status)
  const turns = useMemo(() => [...(historyQuery.data ?? [])].sort((a, b) => {
    const startDifference = (parseLocalDateTime(b.fechaHoraInicio)?.getTime() ?? 0) - (parseLocalDateTime(a.fechaHoraInicio)?.getTime() ?? 0)
    return startDifference || Number(b.turnoId) - Number(a.turnoId)
  }), [historyQuery.data])
  const totalPages = Math.max(1, Math.ceil(turns.length / HISTORY_PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageTurns = useMemo(() => turns.slice((currentPage - 1) * HISTORY_PAGE_SIZE, currentPage * HISTORY_PAGE_SIZE), [turns, currentPage])
  useEffect(() => {
    if (page !== currentPage) setPage(currentPage)
  }, [page, currentPage])
  const filtersActive = Boolean(fecha || status)
  const selectedStatus = STATUS_OPTIONS.find(([value]) => value === status)?.[1]

  function resetFilters() {
    setFecha('')
    setStatus('')
    setPage(1)
  }

  return <section className="employee-dashboard employee-history-page" aria-labelledby="employee-history-title">
    <header className="employee-dashboard-heading employee-agenda-heading">
      <div><span className="eyebrow">MI ESPACIO</span><h1 id="employee-history-title">Historial de turnos</h1><p className="employee-date">Consultá tus turnos anteriores y revisá cómo finalizaron.</p></div>
      <span className="employee-heading-icon"><History size={21} /></span>
    </header>

    <section className="employee-agenda-controls employee-history-controls" aria-label="Filtros del historial">
      <div className="employee-history-date-control"><label className="employee-agenda-date-picker employee-history-date-picker"><CalendarDays size={16} /><input type="date" value={fecha} onChange={(event) => { setFecha(event.target.value); setPage(1) }} aria-label="Filtrar por fecha" /></label><button type="button" className="employee-history-clear" onClick={() => { setFecha(''); setPage(1) }} disabled={!fecha}>Limpiar fecha</button></div>
      <label className="employee-agenda-filter"><span>Estado</span><select value={status} onChange={(event) => { setStatus(event.target.value); setPage(1) }} aria-label="Filtrar turnos por estado">
        {STATUS_OPTIONS.map(([value, optionLabel]) => <option key={value || 'all'} value={value}>{value ? historyStatusLabel(value) : optionLabel}</option>)}
      </select></label>
      <button type="button" className="employee-history-reset" onClick={resetFilters} disabled={!filtersActive}><RotateCcw size={14} /> Restablecer filtros</button>
    </section>

    <section className="employee-section employee-history-results" aria-labelledby="employee-history-results-title" aria-busy={historyQuery.isPending || historyQuery.isFetching}>
      <div className="employee-section-heading employee-upcoming-heading">
        <div><span className="employee-section-kicker">CONSULTA</span><h2 id="employee-history-results-title">Tus turnos</h2></div>
        {historyQuery.isSuccess && !historyQuery.isPlaceholderData && <span className="employee-upcoming-count">{turns.length} {turns.length === 1 ? 'turno' : 'turnos'}</span>}
        {historyQuery.isFetching && historyQuery.isSuccess && <span className="employee-refreshing">Actualizando…</span>}
      </div>
      {historyQuery.isPending ? <HistorySkeleton />
        : historyQuery.isError ? <div className="employee-query-error" role="alert"><History size={22} /><strong>No pudimos cargar tu historial</strong><p>Revisá tu conexión e intentá nuevamente.</p><button type="button" onClick={() => historyQuery.refetch()} disabled={historyQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div>
          : turns.length === 0 ? <div className="employee-empty-state"><History size={20} /><strong>{filtersActive ? 'No hay turnos que coincidan con estos filtros' : 'Todavía no hay turnos en tu historial'}</strong><p>{filtersActive ? `No encontramos turnos ${status ? `${selectedStatus.toLowerCase()} ` : ''}${fecha ? 'para la fecha seleccionada' : 'con los filtros seleccionados'}.` : 'Cuando tengas turnos para consultar, van a aparecer acá.'}</p>{filtersActive && <button type="button" className="employee-history-reset is-empty-reset" onClick={resetFilters}><RotateCcw size={14} /> Limpiar filtros</button>}</div>
            : <>
              <div className="employee-history-list">{pageTurns.map((turn) => <HistoryCard key={turn.turnoId} turn={turn} />)}</div>
              {turns.length > HISTORY_PAGE_SIZE && <nav className="employee-history-pagination" aria-label="Paginación del historial">
                <span className="employee-history-range">Mostrando {(currentPage - 1) * HISTORY_PAGE_SIZE + 1}-{Math.min(currentPage * HISTORY_PAGE_SIZE, turns.length)} de {turns.length} {turns.length === 1 ? 'turno' : 'turnos'}</span>
                <div className="employee-history-pagination-controls">
                  <button type="button" onClick={() => setPage((current) => Math.max(1, current - 1))} disabled={currentPage === 1}><ChevronLeft size={15} /> Anterior</button>
                  <span aria-live="polite">Página {currentPage} de {totalPages}</span>
                  <button type="button" onClick={() => setPage((current) => Math.min(totalPages, current + 1))} disabled={currentPage === totalPages}>Siguiente <ChevronRight size={15} /></button>
                </div>
              </nav>}
            </>}
    </section>
  </section>
}
