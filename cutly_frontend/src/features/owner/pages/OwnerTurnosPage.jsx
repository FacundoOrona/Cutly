import { useMemo, useState } from 'react'
import { CalendarDays, LoaderCircle, RefreshCw, SlidersHorizontal } from 'lucide-react'
import { Navigate, useParams, useSearchParams } from 'react-router-dom'
import { useQueries } from '@tanstack/react-query'
import { getOwnerBusinessEmployeesErrorMessage } from '../api/ownerBusinessEmployeesApi.js'
import { getOwnerBusinessServicesErrorMessage } from '../api/ownerBusinessServicesApi.js'
import { getOwnerTurnos, getOwnerTurnosErrorMessage } from '../api/ownerTurnosApi.js'
import { useOwnerBusinesses } from '../hooks/useOwnerBusinesses.js'
import { useOwnerBusinessEmployees } from '../hooks/useOwnerBusinessEmployees.js'
import { useOwnerBusinessServices } from '../hooks/useOwnerBusinessServices.js'
import { ownerTurnosQueryKey } from '../hooks/useOwnerTurnos.js'

const statusLabels = {
  PENDING: 'Pendiente', CONFIRMED: 'Confirmado', IN_PROGRESS: 'En curso',
  COMPLETED: 'Completado', CANCELLED: 'Cancelado', NO_SHOW: 'Ausencia', REJECTED: 'Rechazado',
}

function todayLocal() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function validDate(value) {
  if (!value) return true
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const [year, month, day] = value.split('-').map(Number)
  const parsed = new Date(year, month - 1, day)
  return parsed.getFullYear() === year && parsed.getMonth() === month - 1 && parsed.getDate() === day
}

function dateTime(value) {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium', timeStyle: 'short' }).format(parsed)
}

function money(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value))
}

function errorCanRetry(error) {
  return ![400, 401, 403, 404].includes(error.response?.status)
}

export function LegacyOwnerTurnosRedirect() {
  const { negocioId } = useParams()
  return <Navigate to={`/owner/turnos?negocioId=${encodeURIComponent(negocioId || '')}`} replace />
}

export default function OwnerTurnosPage() {
  const businessesQuery = useOwnerBusinesses()
  const [searchParams] = useSearchParams()
  const initialBusinessId = searchParams.get('negocioId') || ''
  const [selectedBusinessId, setSelectedBusinessId] = useState(initialBusinessId)
  const [fecha, setFecha] = useState(todayLocal)
  const [empleadoId, setEmpleadoId] = useState('')
  const [status, setStatus] = useState('')
  const [servicioId, setServicioId] = useState('')

  const businesses = useMemo(() => Array.isArray(businessesQuery.data) ? businessesQuery.data : [], [businessesQuery.data])
  // Solo se acepta el negocio si pertenece a la respuesta autenticada de GET /api/owner/negocios.
  const selectedBusiness = businesses.find((business) => String(business.negocioId) === selectedBusinessId)
  const individualBusinessId = selectedBusiness ? String(selectedBusiness.negocioId) : ''
  const allBusinesses = !selectedBusiness
  const dateIsValid = validDate(fecha)

  const employeesQuery = useOwnerBusinessEmployees(individualBusinessId, { enabled: Boolean(individualBusinessId) })
  const servicesQuery = useOwnerBusinessServices(individualBusinessId, { enabled: Boolean(individualBusinessId) })
  const targetBusinesses = useMemo(() => allBusinesses ? businesses : selectedBusiness ? [selectedBusiness] : [], [allBusinesses, businesses, selectedBusiness])
  const turnosQueries = useQueries({
    queries: targetBusinesses.map((business) => {
      const filters = {
        negocioId: String(business.negocioId),
        fecha,
        empleadoId: allBusinesses ? '' : empleadoId,
        status,
      }
      return {
        queryKey: ownerTurnosQueryKey(filters),
        queryFn: () => getOwnerTurnos(filters),
        enabled: businessesQuery.isSuccess && dateIsValid,
        retry: false,
      }
    }),
  })

  const failedQueries = turnosQueries
    .map((query, index) => ({ query, business: targetBusinesses[index] }))
    .filter(({ query }) => query.isError)
  const turnos = useMemo(() => turnosQueries.flatMap((query, index) => {
    if (!Array.isArray(query.data)) return []
    const business = targetBusinesses[index]
    return query.data.map((turno) => ({
      ...turno,
      ...(allBusinesses ? { negocioNombre: business?.nombre || `Negocio ${business?.negocioId}` } : {}),
    }))
  }).sort((a, b) => String(a.fechaHoraInicio).localeCompare(String(b.fechaHoraInicio))), [turnosQueries, targetBusinesses, allBusinesses])
  // La API no admite servicioId; este filtro se aplica sobre los turnos ya descargados.
  const visibleTurnos = servicioId ? turnos.filter((turno) => String(turno.servicioId) === servicioId) : turnos
  const isLoadingTurnos = turnosQueries.some((query) => query.isPending || query.isFetching)

  function changeBusiness(nextId) {
    setSelectedBusinessId(nextId)
    setEmpleadoId('')
    setServicioId('')
  }

  function clearFilters() {
    setFecha(todayLocal())
    setEmpleadoId('')
    setStatus('')
    setServicioId('')
  }

  function retryTurnos() {
    for (const { query } of failedQueries) query.refetch()
  }

  if (businessesQuery.isPending) return <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando tus negocios…</div>
  if (businessesQuery.isError) return <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessesErrorMessage(businessesQuery.error)}</p>{errorCanRetry(businessesQuery.error) && <button className="owners-secondary-button" type="button" onClick={() => businessesQuery.refetch()}><RefreshCw size={15} /> Reintentar</button>}</div>

  return <>
    <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
    <div className="owners-heading">
      <div><h1>Turnos</h1><p className="owners-description">Consultá los turnos diarios de tus negocios.</p></div>
    </div>

    <section className="owners-filters owner-turnos-filters" aria-label="Filtros de turnos">
      <label className="owners-status-filter owner-turnos-business-filter">Negocio<select value={selectedBusiness ? individualBusinessId : ''} onChange={(event) => changeBusiness(event.target.value)}><option value="">Todos mis negocios</option>{businesses.map((business) => <option key={business.negocioId} value={business.negocioId}>{business.nombre}</option>)}</select></label>
      <label className="owners-search">Fecha<input className="owners-filter-input owner-turnos-date" type="date" value={fecha} aria-invalid={!dateIsValid} onChange={(event) => setFecha(event.target.value)} /></label>
      <label className="owners-status-filter">Estado<select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Todos</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      {!allBusinesses && <>
        <label className="owners-status-filter">Empleado<select value={empleadoId} onChange={(event) => setEmpleadoId(event.target.value)}><option value="">Todos</option>{(Array.isArray(employeesQuery.data) ? employeesQuery.data : []).map((employee) => <option key={employee.empleadoId} value={employee.empleadoId}>{employee.nombre} {employee.apellido}{employee.activo ? '' : ' (inactivo)'}</option>)}</select></label>
        <label className="owners-status-filter">Servicio <span className="owner-turnos-local-label">filtro local</span><select value={servicioId} onChange={(event) => setServicioId(event.target.value)}><option value="">Todos</option>{(Array.isArray(servicesQuery.data) ? servicesQuery.data : []).map((service) => <option key={service.servicioId} value={service.servicioId}>{service.nombre}{service.activo ? '' : ' (inactivo)'}</option>)}</select></label>
      </>}
      <div className="owner-turnos-filter-actions"><button className="owners-secondary-button" type="button" onClick={clearFilters}><SlidersHorizontal size={14} /> Limpiar filtros</button></div>
    </section>

    {employeesQuery.isError && <div className="owners-inline-error" role="alert"><span>{getOwnerBusinessEmployeesErrorMessage(employeesQuery.error)}</span><button className="owners-secondary-button" type="button" onClick={() => employeesQuery.refetch()}>Reintentar empleados</button></div>}
    {servicesQuery.isError && <div className="owners-inline-error" role="alert"><span>{getOwnerBusinessServicesErrorMessage(servicesQuery.error)}</span><button className="owners-secondary-button" type="button" onClick={() => servicesQuery.refetch()}>Reintentar servicios</button></div>}
    {!dateIsValid ? <div className="owners-results owners-message owners-error" role="alert">La fecha seleccionada no es válida. Elegí una fecha válida para consultar los turnos.</div> : businesses.length === 0 ? <div className="owners-business-state"><CalendarDays size={22} /><p>Todavía no tenés negocios registrados.</p></div> : (
      <section className="owners-results owner-turnos-results" aria-label="Listado de turnos" aria-busy={isLoadingTurnos}>
        {isLoadingTurnos && turnos.length === 0 ? <div className="owners-message" role="status"><LoaderCircle size={19} className="spin" /> Cargando turnos…</div> : failedQueries.length > 0 ? <div className="owners-message owners-error" role="alert"><p>No se pudo completar la consulta; ocultamos los resultados parciales.</p><ul className="owner-turnos-failed-businesses">{failedQueries.map(({ query, business }) => <li key={business.negocioId}>{business.nombre || `Negocio ${business.negocioId}`}: {getOwnerTurnosErrorMessage(query.error)}</li>)}</ul><button className="owners-secondary-button" type="button" onClick={retryTurnos}><RefreshCw size={14} /> Reintentar negocios fallidos</button></div> : <>
          {isLoadingTurnos && turnos.length > 0 && <div className="owners-updating" role="status"><LoaderCircle size={14} className="spin" /> Actualizando turnos…</div>}
          {employeesQuery.isLoading && individualBusinessId && <div className="owners-updating" role="status">Cargando empleados y servicios…</div>}
          <div className="owner-turnos-count" aria-live="polite">{visibleTurnos.length} {visibleTurnos.length === 1 ? 'turno' : 'turnos'}{servicioId ? ' para el servicio seleccionado' : ''}</div>
          {visibleTurnos.length === 0 ? <div className="owners-message"><CalendarDays size={21} /><p>{turnos.length ? 'No hay turnos para el servicio seleccionado.' : 'No hay turnos para los filtros elegidos.'}</p></div> : <div className="owners-table-wrap owner-turnos-table-wrap"><table className="owners-table owner-turnos-table"><thead><tr>{allBusinesses && <th>Negocio</th>}<th>Cliente</th><th>Empleado</th><th>Servicio</th><th>Inicio</th><th>Fin</th><th>Precio</th><th>Estado</th><th>Observaciones</th></tr></thead><tbody>{visibleTurnos.map((turno) => <tr key={`${turno.negocioId}-${turno.turnoId}`}>{allBusinesses && <td data-label="Negocio">{turno.negocioNombre}</td>}<td data-label="Cliente">{[turno.clienteNombre, turno.clienteApellido].filter(Boolean).join(' ') || '—'}</td><td data-label="Empleado">{[turno.empleadoNombre, turno.empleadoApellido].filter(Boolean).join(' ') || '—'}</td><td data-label="Servicio">{turno.servicioNombre || '—'}</td><td data-label="Inicio">{dateTime(turno.fechaHoraInicio)}</td><td data-label="Fin">{dateTime(turno.fechaHoraFin)}</td><td data-label="Precio">{money(turno.precio)}</td><td data-label="Estado"><span className={`owner-status owner-turno-status owner-turno-status-${String(turno.status || '').toLowerCase()}`}>{statusLabels[turno.status] || turno.status || 'Sin estado'}</span></td><td className="owner-turnos-observations" data-label="Observaciones">{turno.observaciones || '—'}</td></tr>)}</tbody></table></div>}
        </>}
      </section>
    )}
  </>
}

function getOwnerBusinessesErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene permiso para consultar sus negocios.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || 'No pudimos cargar tus negocios.'
}
