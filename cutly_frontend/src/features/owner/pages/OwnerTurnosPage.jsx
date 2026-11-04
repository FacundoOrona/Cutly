import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { ArrowLeft, ArrowRight, Ban, CalendarDays, Check, CircleAlert, Ellipsis, Eye, LoaderCircle, Plus, RefreshCw, SlidersHorizontal, UserRoundX, X } from 'lucide-react'
import { Navigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { getOwnerBusinessEmployeesErrorMessage } from '../api/ownerBusinessEmployeesApi.js'
import { getOwnerBusinessServicesErrorMessage } from '../api/ownerBusinessServicesApi.js'
import { completeOwnerTurno, getCompleteOwnerTurnoErrorMessage, getOwnerTurnosErrorMessage, getUpdateOwnerTurnoErrorMessage, updateOwnerTurnoStatus } from '../api/ownerTurnosApi.js'
import { useActiveOwnerBusiness } from '../context/OwnerBusinessContext.jsx'
import { useOwnerBusinessEmployees } from '../hooks/useOwnerBusinessEmployees.js'
import { useOwnerBusinessServices } from '../hooks/useOwnerBusinessServices.js'
import { ownerTurnoDetailQueryKey, ownerTurnosQueryKey, useOwnerTurnos } from '../hooks/useOwnerTurnos.js'
import OwnerTurnoCreateForm from '../components/OwnerTurnoCreateForm.jsx'
import OwnerTurnoDetailDrawer from '../components/OwnerTurnoDetailDrawer.jsx'
import OwnerTurnoFinancialDrawer from '../components/OwnerTurnoFinancialDrawer.jsx'

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

function shiftDate(value, days) {
  if (!validDate(value)) return value
  const [year, month, day] = value.split('-').map(Number)
  const shifted = new Date(Date.UTC(year, month - 1, day + days))
  return `${shifted.getUTCFullYear()}-${String(shifted.getUTCMonth() + 1).padStart(2, '0')}-${String(shifted.getUTCDate()).padStart(2, '0')}`
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
  return new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(parsed)
}

function money(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value))
}

const paymentMethodLabels = { EFECTIVO: 'Efectivo', TRANSFERENCIA: 'Transferencia', TARJETA: 'Tarjeta', OTRO: 'Otro' }

function financialSummary(turno) {
  const summary = turno.resumenFinanciero
  if (turno.status === 'COMPLETED' && !turno.regularizadoFinancieramente) {
    return <div className="owner-turno-financial"><strong>{money(turno.precio)}</strong><span className="owner-turno-financial-pending">Pendiente de regularización</span></div>
  }
  if (!summary) return '—'
  if (summary.decisionFinanciera === 'COBRO_DIRECTO') return <div className="owner-turno-financial"><strong>{money(summary.monto)}</strong><span>Cobrado · {paymentMethodLabels[summary.metodoPago] || summary.metodoPago || 'Método no disponible'}</span></div>
  if (summary.decisionFinanciera === 'CUENTA_CORRIENTE') {
    const agreed = summary.montoAcordado ?? summary.monto
    const paid = summary.totalAbonado
    const balance = summary.saldoPendiente
    const debtStatus = balance === 0 ? 'Deuda saldada' : paid === 0 ? 'Sin abonos' : paid != null ? 'Abono parcial' : 'Cuenta corriente'
    return <div className="owner-turno-financial"><strong>{debtStatus}</strong><span>Importe acordado: {money(agreed)}</span><span>Total abonado: {money(paid)}</span><span>Saldo pendiente: {money(balance)}</span></div>
  }
  if (summary.decisionFinanciera === 'GRATUIDAD') return <div className="owner-turno-financial"><strong>Gratis</strong></div>
  return '—'
}

function turnoStatusLabel(turno) {
  if (turno.status === 'COMPLETED') return turno.regularizadoFinancieramente ? 'Completado y regularizado' : 'Pendiente de regularización'
  return statusLabels[turno.status] || turno.status || 'Sin estado'
}

function errorCanRetry(error) {
  return ![400, 401, 403, 404].includes(error.response?.status)
}

export function LegacyOwnerTurnosRedirect() {
  const { negocioId } = useParams()
  return <Navigate to={`/owner/turnos?negocioId=${encodeURIComponent(negocioId || '')}`} replace />
}

export default function OwnerTurnosPage() {
  const { businesses, businessesQuery, activeBusiness, activeBusinessId } = useActiveOwnerBusiness()
  const [fecha, setFecha] = useState(todayLocal)
  const [empleadoId, setEmpleadoId] = useState('')
  const [status, setStatus] = useState('')
  const [servicioId, setServicioId] = useState('')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [createNotice, setCreateNotice] = useState('')
  const [pendingActionKeys, setPendingActionKeys] = useState(() => new Set())
  const [actionErrors, setActionErrors] = useState({})
  const [openActionsTurnoId, setOpenActionsTurnoId] = useState(null)
  const [selectedTurno, setSelectedTurno] = useState(null)
  const [financialTurno, setFinancialTurno] = useState(null)
  const [financialError, setFinancialError] = useState('')
  const [financialSuccessDecision, setFinancialSuccessDecision] = useState('')
  const financialOperationRef = useRef(null)
  const pendingActionKeysRef = useRef(new Set())
  const datePickerRef = useRef(null)
  const actionMenuTriggerRef = useRef(null)
  const actionMenuPopoverRef = useRef(null)
  const queryClient = useQueryClient()

  const updateTurnoMutation = useMutation({
    mutationFn: updateOwnerTurnoStatus,
    onSuccess: (updatedTurno, variables) => {
      queryClient.setQueryData(ownerTurnoDetailQueryKey({ negocioId: variables.negocioId, turnoId: updatedTurno.turnoId }), updatedTurno)
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

  const completeTurnoMutation = useMutation({
    mutationFn: completeOwnerTurno,
    onSuccess: (updatedTurno, variables) => {
      queryClient.setQueryData(ownerTurnoDetailQueryKey({ negocioId: variables.negocioId, turnoId: variables.turnoId }), updatedTurno)
      const prefix = ['ownerBusinesses', 'detail', String(variables.negocioId), 'turnos']
      for (const [queryKey, cached] of queryClient.getQueriesData({ queryKey: prefix })) {
        if (!Array.isArray(cached)) continue
        const filters = queryKey[4] || {}
        const without = cached.filter((turno) => String(turno.turnoId) !== String(updatedTurno.turnoId))
        const matches = (!filters.status || filters.status === updatedTurno.status) && (!filters.fecha || String(updatedTurno.fechaHoraInicio || '').slice(0, 10) === filters.fecha) && (!filters.empleadoId || String(updatedTurno.empleadoId) === String(filters.empleadoId))
        queryClient.setQueryData(queryKey, matches ? [...without, updatedTurno].sort((a, b) => String(a.fechaHoraInicio).localeCompare(String(b.fechaHoraInicio))) : without)
      }
      setFinancialSuccessDecision(variables.body.decisionFinanciera)
      financialOperationRef.current = null
      setFinancialError('')
    },
    onError: (error) => setFinancialError(getCompleteOwnerTurnoErrorMessage(error)),
  })
  const individualBusinessId = activeBusiness ? String(activeBusiness.negocioId) : ''
  const dateIsValid = validDate(fecha)

  useEffect(() => {
    setEmpleadoId('')
    setServicioId('')
    setSelectedTurno(null)
    setFinancialTurno(null)
    setActionErrors({})
    setPendingActionKeys(new Set())
    pendingActionKeysRef.current = new Set()
    financialOperationRef.current = null
  }, [activeBusinessId])

  const employeesQuery = useOwnerBusinessEmployees(individualBusinessId, { enabled: Boolean(individualBusinessId) })
  const servicesQuery = useOwnerBusinessServices(individualBusinessId, { enabled: Boolean(individualBusinessId) })
  const turnosQuery = useOwnerTurnos({ negocioId: individualBusinessId, fecha, empleadoId, status }, {
    enabled: Boolean(individualBusinessId) && businessesQuery.isSuccess && dateIsValid,
    retry: false,
  })
  const turnos = useMemo(() => (Array.isArray(turnosQuery.data) ? [...turnosQuery.data] : [])
    .sort((a, b) => String(a.fechaHoraInicio).localeCompare(String(b.fechaHoraInicio))), [turnosQuery.data])
  // La API no admite servicioId; este filtro se aplica sobre los turnos ya descargados.
  const visibleTurnos = servicioId ? turnos.filter((turno) => String(turno.servicioId) === servicioId) : turnos
  const isLoadingTurnos = turnosQuery.isPending || turnosQuery.isFetching

  function clearFilters() {
    setFecha(todayLocal())
    setEmpleadoId('')
    setStatus('')
    setServicioId('')
  }

  function openDatePicker() {
    const input = datePickerRef.current
    if (!input) return
    if (typeof input.showPicker === 'function') input.showPicker()
    else input.focus()
  }

  function retryTurnos() {
    void turnosQuery.refetch()
  }

  const closeTurnoDetail = useCallback(() => setSelectedTurno(null), [])

  useEffect(() => {
    if (openActionsTurnoId == null) return undefined
    const trigger = actionMenuTriggerRef.current
    const popover = actionMenuPopoverRef.current
    if (trigger && popover) {
      const triggerRect = trigger.getBoundingClientRect()
      const popoverRect = popover.getBoundingClientRect()
      const left = Math.max(8, Math.min(triggerRect.right - popoverRect.width, window.innerWidth - popoverRect.width - 8))
      const top = triggerRect.bottom + popoverRect.height <= window.innerHeight - 8
        ? triggerRect.bottom + 4
        : Math.max(8, triggerRect.top - popoverRect.height - 4)
      popover.style.left = `${left}px`
      popover.style.top = `${top}px`
    }
    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        setOpenActionsTurnoId(null)
        actionMenuTriggerRef.current?.focus()
      }
      if (event.key === 'Tab') setOpenActionsTurnoId(null)
    }
    function handleOutsideClick(event) {
      if (!event.target.closest('.owner-turno-actions-menu')) setOpenActionsTurnoId(null)
    }
    document.addEventListener('keydown', handleKeyDown)
    document.addEventListener('click', handleOutsideClick)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.removeEventListener('click', handleOutsideClick)
    }
  }, [openActionsTurnoId])

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
      await updateTurnoMutation.mutateAsync({ negocioId: activeBusinessId, turnoId: turno.turnoId, action })
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
      <button type="button" className="owners-secondary-button owner-turno-action owner-turno-detail-action" onClick={() => setSelectedTurno({ negocioId: activeBusinessId, turnoId: turno.turnoId })} aria-label={`Ver detalles del turno de ${turno.clienteNombre || 'cliente'} ${dateTime(turno.fechaHoraInicio)}`} title="Ver detalles"><Eye size={15} /></button>
      {actions.length > 0 && <div className="owner-turno-actions-menu">
        <button ref={openActionsTurnoId === actionKey ? actionMenuTriggerRef : null} type="button" className="owners-secondary-button owner-turno-action owner-turno-menu-trigger" aria-label={`Más acciones para el turno de ${turno.clienteNombre || 'cliente'}`} aria-haspopup="menu" aria-expanded={openActionsTurnoId === actionKey} onClick={() => setOpenActionsTurnoId((current) => current === actionKey ? null : actionKey)} disabled={turnIsPending}><Ellipsis size={16} /></button>
        {openActionsTurnoId === actionKey && <div ref={actionMenuPopoverRef} className="owner-turno-actions-popover" role="menu" aria-label="Acciones del turno">
          {actions.map((action) => {
            const details = actionDetails[action]
            const Icon = details.icon
            return <button key={action} type="button" role="menuitem" className={`owner-turno-menu-item ${details.className}`} onClick={() => { setOpenActionsTurnoId(null); if (action === 'completar') { financialOperationRef.current = null; setFinancialError(''); setFinancialTurno(turno) } else void runTurnoAction(turno, action) }} disabled={turnIsPending}><Icon size={14} />{details.label}</button>
          })}
        </div>}
      </div>}
      {turnIsPending && <span className="owner-turno-action-pending" role="status"><LoaderCircle size={14} className="spin" /> Procesando…</span>}
    </div>
  }

  if (businessesQuery.isPending) return <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando tus negocios…</div>
  if (businessesQuery.isError) return <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessesErrorMessage(businessesQuery.error)}</p>{errorCanRetry(businessesQuery.error) && <button className="owners-secondary-button" type="button" onClick={() => businessesQuery.refetch()}><RefreshCw size={15} /> Reintentar</button>}</div>

  return <>
    <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
    <div className="owners-heading">
      <div><h1>Turnos</h1><p className="owners-description">Consultá los turnos diarios de tus negocios.</p></div>
      <button className="owner-create-button" type="button" onClick={() => { setCreateNotice(''); setShowCreateForm((current) => !current) }} aria-expanded={showCreateForm}><Plus size={15} /> Nuevo turno</button>
    </div>

    {createNotice && <div className="owner-created-notice" role="status"><Check size={16} /><span>{createNotice}</span><button type="button" className="icon-button" aria-label="Cerrar confirmación" onClick={() => setCreateNotice('')}><X size={15} /></button></div>}
    {showCreateForm && individualBusinessId && <OwnerTurnoCreateForm
      key={`create-${individualBusinessId}`}
      negocioId={individualBusinessId}
      onCancel={() => setShowCreateForm(false)}
      onCreated={(turno, summary) => {
        setCreateNotice(summary)
        setShowCreateForm(false)
      }}
    />}

    <section className="owners-filters owner-turnos-filters" aria-label="Filtros de turnos">
      <div className="owner-turnos-filter-row">
        <div className="owner-turnos-filter-fields">
          <label className="owners-status-filter">Estado<select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Todos</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
          {<>
            <label className="owners-status-filter">Empleado<select value={empleadoId} onChange={(event) => setEmpleadoId(event.target.value)}><option value="">Todos</option>{(Array.isArray(employeesQuery.data) ? employeesQuery.data : []).map((employee) => <option key={employee.empleadoId} value={employee.empleadoId}>{employee.nombre} {employee.apellido}{employee.activo ? '' : ' (inactivo)'}</option>)}</select></label>
            <label className="owners-status-filter">Servicio<select value={servicioId} onChange={(event) => setServicioId(event.target.value)}><option value="">Todos</option>{(Array.isArray(servicesQuery.data) ? servicesQuery.data : []).map((service) => <option key={service.servicioId} value={service.servicioId}>{service.nombre}{service.activo ? '' : ' (inactivo)'}</option>)}</select></label>
          </>}
        </div>
        <button className="owners-secondary-button owner-turnos-clear-filters" type="button" onClick={clearFilters}><SlidersHorizontal size={14} /> Limpiar filtros</button>
      </div>
      <div className="owner-turnos-date-navigation" role="group" aria-label="Navegación por día">
        <button className="owners-secondary-button owner-turnos-day-button" type="button" aria-label="Día anterior" onClick={() => setFecha((current) => shiftDate(current, -1))} disabled={!dateIsValid}><ArrowLeft size={17} /></button>
        <div className="owner-turnos-date-display">
          <span className="owner-turnos-date-label">Fecha de agenda</span>
          <strong>{dateIsValid ? new Intl.DateTimeFormat('es-AR', { weekday: 'long', day: 'numeric', month: 'long', ...(fecha.split('-')[0] !== todayLocal().slice(0, 4) ? { year: 'numeric' } : {}) }).format(new Date(`${fecha}T12:00:00`)) : 'Fecha inválida'}</strong>
        </div>
        <button className="owners-secondary-button owner-turnos-day-button" type="button" aria-label="Día siguiente" onClick={() => setFecha((current) => shiftDate(current, 1))} disabled={!dateIsValid}><ArrowRight size={17} /></button>
        <button className="owners-secondary-button owner-turnos-today" type="button" onClick={() => setFecha(todayLocal())}>Hoy</button>
        <button className="owners-secondary-button owner-turnos-date-picker" type="button" aria-label="Elegir fecha" onClick={openDatePicker}>
          <CalendarDays size={17} aria-hidden="true" />
        </button>
        <input ref={datePickerRef} id="owner-turnos-date-picker" className="owner-turnos-date-input" type="date" value={fecha} aria-label="Fecha de agenda" aria-invalid={!dateIsValid} onChange={(event) => setFecha(event.target.value)} tabIndex={-1} />
      </div>
    </section>

    {employeesQuery.isError && <div className="owners-inline-error" role="alert"><span>{getOwnerBusinessEmployeesErrorMessage(employeesQuery.error)}</span><button className="owners-secondary-button" type="button" onClick={() => employeesQuery.refetch()}>Reintentar empleados</button></div>}
    {servicesQuery.isError && <div className="owners-inline-error" role="alert"><span>{getOwnerBusinessServicesErrorMessage(servicesQuery.error)}</span><button className="owners-secondary-button" type="button" onClick={() => servicesQuery.refetch()}>Reintentar servicios</button></div>}
    {Object.entries(actionErrors).map(([key, error]) => {
      const turno = turnos.find((item) => String(item.negocioId) === String(error.negocioId) && String(item.turnoId) === String(error.turnoId))
      return <div className="owner-created-notice owner-turno-action-error" role="alert" key={key}><CircleAlert size={15} /><span>{error.message}</span>{error.retryable && turno && <button type="button" className="owners-secondary-button" onClick={() => runTurnoAction(turno, error.action)} disabled={pendingActionKeys.has(key)}><RefreshCw size={12} /> Reintentar {error.actionLabel.toLowerCase()}</button>}<button type="button" className="icon-button" aria-label="Cerrar mensaje" onClick={() => setActionErrors((current) => { const next = { ...current }; delete next[key]; return next })}><X size={15} /></button></div>
    })}
    {!dateIsValid ? <div className="owners-results owners-message owners-error" role="alert">La fecha seleccionada no es válida. Elegí una fecha válida para consultar los turnos.</div> : businesses.length === 0 ? <div className="owners-business-state"><CalendarDays size={22} /><p>Todavía no tenés negocios registrados.</p></div> : (
      <section className="owners-results owner-turnos-results" aria-label="Listado de turnos" aria-busy={isLoadingTurnos}>
        {isLoadingTurnos && turnos.length === 0 ? <div className="owners-message owner-turnos-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando turnos…</div> : turnosQuery.isError ? <div className="owners-message owners-error owner-turnos-state" role="alert"><p>{getOwnerTurnosErrorMessage(turnosQuery.error)}</p><button className="owners-secondary-button" type="button" onClick={retryTurnos} disabled={turnosQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div> : <>
          {isLoadingTurnos && turnos.length > 0 && <div className="owners-updating" role="status"><LoaderCircle size={14} className="spin" /> Actualizando turnos…</div>}
          {employeesQuery.isLoading && individualBusinessId && <div className="owners-updating" role="status">Cargando empleados y servicios…</div>}
          <div className="owner-turnos-count" aria-live="polite">{visibleTurnos.length} {visibleTurnos.length === 1 ? 'turno' : 'turnos'}{servicioId ? ' para el servicio seleccionado' : ''}</div>
          {visibleTurnos.length === 0 ? <div className="owners-message owner-turnos-state"><CalendarDays size={21} /><p>{turnos.length ? 'No hay turnos para el servicio seleccionado.' : 'No hay turnos para los filtros elegidos.'}</p></div> : <div className="owner-turnos-table-wrap"><table className="owners-table owner-turnos-table"><thead><tr><th>Cliente</th><th>Responsable</th><th>Inicio</th><th>Servicio</th><th>Resumen económico</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{visibleTurnos.map((turno) => { const responsable = [turno.responsableNombre, turno.responsableApellido].filter(Boolean).join(' ').trim(); const tipoResponsable = turno.tipoResponsable === 'EMPLEADO' ? 'Empleado' : turno.tipoResponsable === 'OWNER' ? 'Owner' : null; return <tr key={`${turno.negocioId}-${turno.turnoId}`}><td className="owner-turnos-truncate" data-label="Cliente" title={[turno.clienteNombre, turno.clienteApellido].filter(Boolean).join(' ')}>{[turno.clienteNombre, turno.clienteApellido].filter(Boolean).join(' ') || '—'}</td><td className="owner-turnos-responsible-cell" data-label="Responsable"><span>{responsable || 'Sin asignar'}</span>{responsable && tipoResponsable && <small>{tipoResponsable}</small>}</td><td data-label="Inicio">{time(turno.fechaHoraInicio)}</td><td className="owner-turnos-truncate" data-label="Servicio" title={turno.servicioNombre}>{turno.servicioNombre || '—'}</td><td className="owner-turnos-economic-cell" data-label="Resumen económico">{financialSummary(turno)}</td><td data-label="Estado"><span className={`owner-status owner-turno-status owner-turno-table-status owner-turno-status-${String(turno.status || '').toLowerCase()}`}>{turnoStatusLabel(turno)}</span></td><td className="owner-turnos-actions-cell" data-label="Acciones">{renderTurnoActions(turno)}</td></tr> })}</tbody></table></div>}
        </>}
      </section>
    )}
    {selectedTurno && <OwnerTurnoDetailDrawer negocioId={selectedTurno.negocioId} turnoId={selectedTurno.turnoId} onClose={closeTurnoDetail} />}
    {financialTurno && <OwnerTurnoFinancialDrawer turno={financialTurno} error={financialError} isPending={completeTurnoMutation.isPending} successDecision={financialSuccessDecision} onCancel={() => { if (!completeTurnoMutation.isPending) { setFinancialTurno(null); setFinancialError(''); setFinancialSuccessDecision(''); financialOperationRef.current = null } }} onSubmit={(body) => {
      if (completeTurnoMutation.isPending) return
      if (financialOperationRef.current && !body) {
        const operation = financialOperationRef.current
        void completeTurnoMutation.mutateAsync({ negocioId: activeBusinessId, turnoId: financialTurno.turnoId, body: operation.body, idempotencyKey: operation.idempotencyKey }).catch(() => {})
        return
      }
      if (financialOperationRef.current && body && JSON.stringify(financialOperationRef.current.body) !== JSON.stringify(body)) return
      if (!financialOperationRef.current) financialOperationRef.current = { idempotencyKey: crypto.randomUUID(), body }
      const operation = financialOperationRef.current
      void completeTurnoMutation.mutateAsync({ negocioId: activeBusinessId, turnoId: financialTurno.turnoId, body: operation.body, idempotencyKey: operation.idempotencyKey }).catch(() => {})
    }} />}

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

