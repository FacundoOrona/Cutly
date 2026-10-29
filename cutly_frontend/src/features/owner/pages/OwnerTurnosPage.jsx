import { useMemo, useRef, useState } from 'react'
import { Ban, CalendarDays, Check, CircleAlert, LoaderCircle, Plus, RefreshCw, SlidersHorizontal, UserRoundX, X } from 'lucide-react'
import { Navigate, useParams, useSearchParams } from 'react-router-dom'
import { useMutation, useQueries, useQueryClient } from '@tanstack/react-query'
import { getOwnerBusinessEmployeesErrorMessage } from '../api/ownerBusinessEmployeesApi.js'
import { getOwnerBusinessServicesErrorMessage } from '../api/ownerBusinessServicesApi.js'
import { getOwnerTurnos, getOwnerTurnosErrorMessage, getUpdateOwnerTurnoErrorMessage, updateOwnerTurnoStatus } from '../api/ownerTurnosApi.js'
import { useOwnerBusinesses } from '../hooks/useOwnerBusinesses.js'
import { useOwnerBusinessEmployees } from '../hooks/useOwnerBusinessEmployees.js'
import { useOwnerBusinessServices } from '../hooks/useOwnerBusinessServices.js'
import { ownerTurnosQueryKey } from '../hooks/useOwnerTurnos.js'
import OwnerTurnoCreateForm from '../components/OwnerTurnoCreateForm.jsx'

const statusLabels = {
  PENDING: 'Pendiente', CONFIRMED: 'Confirmado', IN_PROGRESS: 'En curso',
  COMPLETED: 'Completado', CANCELLED: 'Cancelado', NO_SHOW: 'Ausencia', REJECTED: 'Rechazado',
}

const availableActions = {
  PENDING: ['confirmar', 'rechazar', 'cancelar'],
  CONFIRMED: ['cancelar', 'completar', 'no-show'],
  IN_PROGRESS: ['cancelar', 'completar'],
}

const actionDetails = {
  confirmar: { label: 'Confirmar', verb: 'confirmar', icon: Check, className: 'owner-turno-action-primary' },
  rechazar: { label: 'Rechazar', verb: 'rechazar', icon: X, className: 'owner-turno-action-danger', confirm: true },
  cancelar: { label: 'Cancelar', verb: 'cancelar', icon: Ban, className: 'owner-turno-action-danger', confirm: true },
  completar: { label: 'Completar', verb: 'completar', icon: Check, className: 'owner-turno-action-secondary' },
  'no-show': { label: 'Marcar ausencia', verb: 'marcar como ausencia', icon: UserRoundX, className: 'owner-turno-action-danger', confirm: true },
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

function time(value) {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit' }).format(parsed)
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
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [createBusinessId, setCreateBusinessId] = useState('')
  const [createNotice, setCreateNotice] = useState('')
  const [pendingActionKeys, setPendingActionKeys] = useState(() => new Set())
  const [actionErrors, setActionErrors] = useState({})
  const pendingActionKeysRef = useRef(new Set())
  const queryClient = useQueryClient()

  const updateTurnoMutation = useMutation({
    mutationFn: updateOwnerTurnoStatus,
    onSuccess: (updatedTurno, variables) => {
      const queryPrefix = ['ownerBusinesses', 'detail', String(variables.negocioId), 'turnos']
      for (const [queryKey, cachedTurnos] of queryClient.getQueriesData({ queryKey: queryPrefix })) {
        if (!Array.isArray(cachedTurnos)) continue
        const queryFilters = queryKey[4] || {}
        const nextTurnos = cachedTurnos.reduce((result, turno) => {
          if (String(turno.turnoId) !== String(updatedTurno.turnoId)) {
            result.push(turno)
          } else if (!queryFilters.status || queryFilters.status === updatedTurno.status) {
            result.push(updatedTurno)
          }
          return result
        }, [])
        queryClient.setQueryData(queryKey, nextTurnos)
      }
      const actionKey = `${variables.negocioId}:${variables.turnoId}`
      setActionErrors((current) => {
        const next = { ...current }
        delete next[actionKey]
        return next
      })
    },
    onError: (error, variables) => {
      const action = actionDetails[variables.action]
      const actionKey = `${variables.negocioId}:${variables.turnoId}`
      const statusCode = error.response?.status
      setActionErrors((current) => ({
        ...current,
        [actionKey]: {
          message: getUpdateOwnerTurnoErrorMessage(error, action.verb),
          retryable: ![400, 401, 403, 404, 409].includes(statusCode),
          action: variables.action,
          actionLabel: action.label,
          negocioId: variables.negocioId,
          turnoId: variables.turnoId,
        },
      }))
      if (statusCode === 409) {
        queryClient.invalidateQueries({ queryKey: ['ownerBusinesses', 'detail', String(variables.negocioId), 'turnos'] })
      }
    },
  })

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

  async function runTurnoAction(turno, action) {
    const actionKey = `${turno.negocioId}:${turno.turnoId}`
    if (pendingActionKeysRef.current.has(actionKey)) return
    const details = actionDetails[action]
    if (details.confirm) {
      const clientName = [turno.clienteNombre, turno.clienteApellido].filter(Boolean).join(' ') || 'el cliente'
      if (!window.confirm(`¿Querés ${details.verb} el turno de ${clientName} del ${dateTime(turno.fechaHoraInicio)}?`)) return
    }

    pendingActionKeysRef.current.add(actionKey)
    setPendingActionKeys(new Set(pendingActionKeysRef.current))
    try {
      await updateTurnoMutation.mutateAsync({ negocioId: turno.negocioId, turnoId: turno.turnoId, action })
    } catch {
      // El error se presenta junto al turno desde onError.
    } finally {
      pendingActionKeysRef.current.delete(actionKey)
      setPendingActionKeys(new Set(pendingActionKeysRef.current))
    }
  }

  function renderTurnoActions(turno) {
    const actionKey = `${turno.negocioId}:${turno.turnoId}`
    const turnIsPending = pendingActionKeys.has(actionKey)
    const actions = availableActions[turno.status] || []
    return <div className="owner-turno-row-actions">
      {actions.map((action) => {
        const details = actionDetails[action]
        const Icon = details.icon
        return <button key={action} type="button" className={`owners-secondary-button owner-turno-action ${details.className}`} onClick={() => runTurnoAction(turno, action)} disabled={turnIsPending} aria-label={`${details.label}: ${turno.clienteNombre || 'turno'} ${dateTime(turno.fechaHoraInicio)}`}><Icon size={13} />{details.label}</button>
      })}
      {turnIsPending && <span className="owner-turno-action-pending" role="status"><LoaderCircle size={14} className="spin" /> Procesando…</span>}
    </div>
  }

  if (businessesQuery.isPending) return <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando tus negocios…</div>
  if (businessesQuery.isError) return <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessesErrorMessage(businessesQuery.error)}</p>{errorCanRetry(businessesQuery.error) && <button className="owners-secondary-button" type="button" onClick={() => businessesQuery.refetch()}><RefreshCw size={15} /> Reintentar</button>}</div>

  return <>
    <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
    <div className="owners-heading">
      <div><h1>Turnos</h1><p className="owners-description">Consultá los turnos diarios de tus negocios.</p></div>
      <button className="owner-create-button" type="button" onClick={() => { setCreateNotice(''); setCreateBusinessId(individualBusinessId); setShowCreateForm((current) => !current) }} aria-expanded={showCreateForm}><Plus size={15} /> Nuevo turno</button>
    </div>

    {createNotice && <div className="owner-created-notice" role="status"><Check size={16} /><span>{createNotice}</span><button type="button" className="icon-button" aria-label="Cerrar confirmación" onClick={() => setCreateNotice('')}><X size={15} /></button></div>}
    {showCreateForm && !createBusinessId && <section className="create-owner-panel owner-turno-create-panel" aria-label="Seleccionar negocio para el nuevo turno"><div className="create-owner-heading"><div><span className="create-owner-icon"><CalendarDays size={17} /></span><div><h2>Nuevo turno</h2><p>Seleccioná el negocio donde se reservará el turno.</p></div></div><button type="button" className="icon-button owner-turno-close" aria-label="Cerrar formulario de nuevo turno" onClick={() => setShowCreateForm(false)}><X size={17} /></button></div><label className="owner-form-field" htmlFor="owner-turno-business"><span>Negocio *</span><select id="owner-turno-business" value={createBusinessId} onChange={(event) => setCreateBusinessId(event.target.value)}><option value="">Seleccioná un negocio…</option>{businesses.map((business) => <option key={business.negocioId} value={business.negocioId}>{business.nombre}</option>)}</select></label></section>}
    {showCreateForm && createBusinessId && <OwnerTurnoCreateForm
      key={`create-${createBusinessId}`}
      negocioId={createBusinessId}
      onCancel={() => setShowCreateForm(false)}
      onCreated={(turno, summary) => {
        setCreateNotice(summary)
        setShowCreateForm(false)
      }}
    />}

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
    {Object.entries(actionErrors).map(([key, error]) => {
      const turno = turnos.find((item) => String(item.negocioId) === String(error.negocioId) && String(item.turnoId) === String(error.turnoId))
      return <div className="owner-created-notice owner-turno-action-error" role="alert" key={key}><CircleAlert size={15} /><span>{error.message}</span>{error.retryable && turno && <button type="button" className="owners-secondary-button" onClick={() => runTurnoAction(turno, error.action)} disabled={pendingActionKeys.has(key)}><RefreshCw size={12} /> Reintentar {error.actionLabel.toLowerCase()}</button>}<button type="button" className="icon-button" aria-label="Cerrar mensaje" onClick={() => setActionErrors((current) => { const next = { ...current }; delete next[key]; return next })}><X size={15} /></button></div>
    })}
    {!dateIsValid ? <div className="owners-results owners-message owners-error" role="alert">La fecha seleccionada no es válida. Elegí una fecha válida para consultar los turnos.</div> : businesses.length === 0 ? <div className="owners-business-state"><CalendarDays size={22} /><p>Todavía no tenés negocios registrados.</p></div> : (
      <section className="owners-results owner-turnos-results" aria-label="Listado de turnos" aria-busy={isLoadingTurnos}>
        {isLoadingTurnos && turnos.length === 0 ? <div className="owners-message owner-turnos-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando turnos…</div> : failedQueries.length > 0 ? <div className="owners-message owners-error owner-turnos-state" role="alert"><p>No se pudo completar la consulta; ocultamos los resultados parciales.</p><ul className="owner-turnos-failed-businesses">{failedQueries.map(({ query, business }) => <li key={business.negocioId}>{business.nombre || `Negocio ${business.negocioId}`}: {getOwnerTurnosErrorMessage(query.error)}</li>)}</ul><button className="owners-secondary-button" type="button" onClick={retryTurnos}><RefreshCw size={14} /> Reintentar negocios fallidos</button></div> : <>
          {isLoadingTurnos && turnos.length > 0 && <div className="owners-updating" role="status"><LoaderCircle size={14} className="spin" /> Actualizando turnos…</div>}
          {employeesQuery.isLoading && individualBusinessId && <div className="owners-updating" role="status">Cargando empleados y servicios…</div>}
          <div className="owner-turnos-count" aria-live="polite">{visibleTurnos.length} {visibleTurnos.length === 1 ? 'turno' : 'turnos'}{servicioId ? ' para el servicio seleccionado' : ''}</div>
          {visibleTurnos.length === 0 ? <div className="owners-message owner-turnos-state"><CalendarDays size={21} /><p>{turnos.length ? 'No hay turnos para el servicio seleccionado.' : 'No hay turnos para los filtros elegidos.'}</p></div> : <div className="owners-table-wrap owner-turnos-table-wrap"><table className="owners-table owner-turnos-table"><thead><tr>{allBusinesses && <th>Negocio</th>}<th>Cliente</th><th>Empleado</th><th>Servicio</th><th>Inicio</th><th>Fin</th><th>Precio</th><th>Estado</th><th>Observaciones</th><th>Acciones</th></tr></thead><tbody>{visibleTurnos.map((turno) => <tr key={`${turno.negocioId}-${turno.turnoId}`}>{allBusinesses && <td data-label="Negocio">{turno.negocioNombre}</td>}<td data-label="Cliente">{[turno.clienteNombre, turno.clienteApellido].filter(Boolean).join(' ') || '—'}</td><td data-label="Empleado">{[turno.empleadoNombre, turno.empleadoApellido].filter(Boolean).join(' ') || '—'}</td><td data-label="Servicio">{turno.servicioNombre || '—'}</td><td data-label="Inicio">{time(turno.fechaHoraInicio)}</td><td data-label="Fin">{time(turno.fechaHoraFin)}</td><td data-label="Precio">{money(turno.precio)}</td><td data-label="Estado"><span className={`owner-status owner-turno-status owner-turno-status-${String(turno.status || '').toLowerCase()}`}>{statusLabels[turno.status] || turno.status || 'Sin estado'}</span></td><td className="owner-turnos-observations" data-label="Observaciones">{turno.observaciones || '—'}</td><td className="owner-turnos-actions-cell" data-label="Acciones">{renderTurnoActions(turno)}</td></tr>)}</tbody></table></div>}
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
