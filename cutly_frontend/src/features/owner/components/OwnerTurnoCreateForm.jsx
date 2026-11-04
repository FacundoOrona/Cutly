import { useEffect, useMemo, useState } from 'react'
import { useQueries } from '@tanstack/react-query'
import { CheckCircle2, ChevronDown, Clock3, LoaderCircle, Scissors, X } from 'lucide-react'
import useAuth from '../../../auth/useAuth.js'
import { getCreateOwnerTurnoErrorMessage, getOwnerClientesLocalesErrorMessage, getOwnerTurnos } from '../api/ownerTurnosApi.js'
import { getOwnerBusinessEmployeeServices } from '../api/ownerBusinessEmployeesApi.js'
import { useActiveOwnerBusiness } from '../context/OwnerBusinessContext.jsx'
import { useOwnerBusinessHours } from '../hooks/useOwnerBusinessHours.js'
import { ownerBusinessEmployeeServicesQueryKey, useOwnerBusinessEmployees } from '../hooks/useOwnerBusinessEmployees.js'
import { useOwnerBusinessServices } from '../hooks/useOwnerBusinessServices.js'
import { ownerTurnosQueryKey, useCreateOwnerTurno, useOwnerClientesLocales } from '../hooks/useOwnerTurnos.js'

const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const initialClient = { nombre: '', apellido: '', dni: '', telefono: '' }

function todayLocal() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function validate(values, clientMode, selectedClient) {
  const errors = {}
  if (!values.servicioId) errors.servicioId = 'Seleccioná un servicio.'
  if (!values.responsable) errors.responsable = 'Seleccioná un responsable.'
  if (!values.fecha) errors.fecha = 'Seleccioná una fecha.'
  if (!values.hora) errors.hora = 'Seleccioná una hora.'
  if (clientMode === 'existing' && !selectedClient?.clienteLocalId) errors.clienteLocalId = 'Buscá y seleccioná un cliente.'
  if (clientMode === 'new') {
    for (const [field, label] of [['nombre', 'El nombre'], ['apellido', 'El apellido']]) {
      const value = values.clienteNuevo[field].trim()
      if (!value) errors[field] = `${label} es obligatorio.`
      else if (value.length < 2 || value.length > 20) errors[field] = `${label} debe contener entre 2 y 20 caracteres.`
    }
    const dni = values.clienteNuevo.dni.trim()
    if (!dni) errors.dni = 'El DNI es obligatorio.'
    else if (dni.length > 20) errors.dni = 'El DNI no puede superar los 20 caracteres.'
    const telefono = values.clienteNuevo.telefono.trim()
    if (!telefono) errors.telefono = 'El teléfono es obligatorio.'
    else if (telefono.length > 30) errors.telefono = 'El teléfono no puede superar los 30 caracteres.'
  }
  if (values.observaciones.length > 500) errors.observaciones = 'Las observaciones no pueden superar los 500 caracteres.'
  return errors
}

function clientLabel(client) {
  return [client.nombre, client.apellido].filter(Boolean).join(' ') || `Cliente ${client.clienteLocalId}`
}

const weekDays = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

function minuteOfDay(value) {
  if (typeof value !== 'string') return null
  const [hour, minute] = value.split(':').map(Number)
  return Number.isFinite(hour) && Number.isFinite(minute) ? hour * 60 + minute : null
}

function availableTimes({ date, duration, hours, turns, empleadoId }) {
  if (!date || !Number.isFinite(duration) || duration <= 0) return []
  const day = weekDays[new Date(`${date}T12:00:00`).getDay()]
  const intervals = hours.filter((item) => item.diaSemana === day && !item.cerrado)
    .map((item) => [minuteOfDay(item.horaApertura), minuteOfDay(item.horaCierre)])
    .filter(([start, end]) => start != null && end != null && end > start)
  const relevantTurns = turns.filter((turno) => turno.status !== 'CANCELLED' && turno.status !== 'REJECTED'
    && (empleadoId == null ? turno.tipoResponsable === 'OWNER' : String(turno.empleadoId) === String(empleadoId)))
  const now = new Date()
  const today = todayLocal()
  const currentMinute = now.getHours() * 60 + now.getMinutes()
  const slots = new Set()

  for (const [opening, closing] of intervals) {
    const first = Math.ceil(opening / 15) * 15
    for (let start = first; start + duration <= closing; start += 15) {
      if (date < today || (date === today && start <= currentMinute)) continue
      const end = start + duration
      const time = `${String(Math.floor(start / 60)).padStart(2, '0')}:${String(start % 60).padStart(2, '0')}`
      const startStamp = `${date}T${time}`
      const endStamp = `${date}T${String(Math.floor(end / 60)).padStart(2, '0')}:${String(end % 60).padStart(2, '0')}`
      const overlaps = relevantTurns.some((turno) => startStamp < String(turno.fechaHoraFin || '').slice(0, 16)
        && endStamp > String(turno.fechaHoraInicio || '').slice(0, 16))
      if (!overlaps) slots.add(time)
    }
  }
  return [...slots].sort()
}

function successMessage(turno, service, client) {
  const start = turno?.fechaHoraInicio
  const date = start ? new Date(start) : null
  const when = date && !Number.isNaN(date.getTime())
    ? new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' }).format(date)
    : null
  const serviceName = turno?.servicioNombre || service?.nombre
  const turnoClientName = [turno?.clienteNombre, turno?.clienteApellido].filter(Boolean).join(' ')
  const parts = [when, serviceName, turnoClientName || client]
  return `Turno creado correctamente${parts.filter(Boolean).length ? `: ${parts.filter(Boolean).join(' · ')}` : '.'}`
}

function ClientFields({ errors, negocioId, selectedClient, onSelectClient }) {
  const [searchText, setSearchText] = useState('')
  const [debouncedText, setDebouncedText] = useState('')
  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedText(searchText.trim()), 300)
    return () => window.clearTimeout(timer)
  }, [searchText])
  const query = useOwnerClientesLocales(negocioId, debouncedText)
  const clients = Array.isArray(query.data) ? query.data : []

  return <div className="owner-turno-client-fields">
    <label className="owner-form-field" htmlFor="owner-turno-client-search"><span>Buscar cliente local</span><input id="owner-turno-client-search" type="search" value={searchText} maxLength={100} onChange={(event) => { setSearchText(event.target.value); onSelectClient(null) }} placeholder="Nombre o apellido" aria-describedby="owner-turno-client-search-help" /></label>
    <small id="owner-turno-client-search-help" className="owner-turno-helper">Escribí al menos 2 caracteres para buscar por nombre o apellido. Seleccioná un resultado.</small>
    {query.isFetching && <p className="owner-turno-search-state" role="status"><LoaderCircle size={14} className="spin" /> Buscando clientes…</p>}
    {query.isError && debouncedText === searchText.trim() && <div className="owner-inline-query-error" role="alert"><span>{getOwnerClientesLocalesErrorMessage(query.error)}</span><button type="button" className="owners-secondary-button" onClick={() => void query.refetch()} disabled={query.isFetching}>Reintentar</button></div>}
    {!query.isFetching && !query.isError && debouncedText === searchText.trim() && debouncedText.length >= 2 && clients.length === 0 && <p className="owner-turno-search-state">No se encontraron clientes con esa búsqueda.</p>}
    {!query.isFetching && debouncedText === searchText.trim() && clients.length > 0 && <ul className="owner-turno-client-results" aria-label="Resultados de clientes">{clients.map((client) => <li key={client.clienteLocalId}><button type="button" className={String(selectedClient?.clienteLocalId) === String(client.clienteLocalId) ? 'owner-turno-client-option is-selected' : 'owner-turno-client-option'} onClick={() => { onSelectClient(client); setSearchText(clientLabel(client)) }} aria-pressed={String(selectedClient?.clienteLocalId) === String(client.clienteLocalId)}><span>{clientLabel(client)}</span><small>{client.dniPresentado || ''}{client.telefono ? ` · ${client.telefono}` : ''}</small></button></li>)}</ul>}
    {selectedClient && <p className="owner-turno-selected-client" role="status">Seleccionado: {clientLabel(selectedClient)}{selectedClient.dniPresentado ? ` · DNI ${selectedClient.dniPresentado}` : ''}</p>}
    {errors.clienteLocalId && <small className="owner-field-error" role="alert">{errors.clienteLocalId}</small>}
  </div>
}

export default function OwnerTurnoCreateForm({ negocioId, onCancel, onCreated }) {
  const { user } = useAuth()
  const { businesses } = useActiveOwnerBusiness()
  const servicesQuery = useOwnerBusinessServices(negocioId)
  const employeesQuery = useOwnerBusinessEmployees(negocioId)
  const mutation = useCreateOwnerTurno()
  const [values, setValues] = useState(() => ({ servicioId: '', fecha: todayLocal(), hora: '', responsable: '', observaciones: '', clienteNuevo: initialClient }))
  const hoursQuery = useOwnerBusinessHours(negocioId)
  const availabilityBusinessIds = useMemo(() => [...new Set([...(businesses || []).map((business) => String(business.negocioId)), String(negocioId)])].filter(Boolean), [businesses, negocioId])
  const turnsQueries = useQueries({ queries: availabilityBusinessIds.map((id) => ({
    queryKey: ownerTurnosQueryKey({ negocioId: id, fecha: values.fecha }),
    queryFn: () => getOwnerTurnos({ negocioId: id, fecha: values.fecha }),
  })) })
  const [clientMode, setClientMode] = useState('existing')
  const [selectedClient, setSelectedClient] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const [submitNotice, setSubmitNotice] = useState('')

  const services = useMemo(() => (Array.isArray(servicesQuery.data) ? servicesQuery.data : []).filter((service) => service.activo), [servicesQuery.data])
  const activeEmployees = useMemo(() => (Array.isArray(employeesQuery.data) ? employeesQuery.data : []).filter((employee) => employee.activo), [employeesQuery.data])
  const ownerName = [user?.nombre, user?.apellido].filter(Boolean).join(' ')
  const ownerEmployeeId = user?.userId
  const chosenService = services.find((service) => String(service.servicioId) === values.servicioId)
  const hours = Array.isArray(hoursQuery.data) ? hoursQuery.data : []
  const turns = turnsQueries.flatMap((query) => Array.isArray(query.data) ? query.data : [])
  const availabilityLoading = hoursQuery.isPending || hoursQuery.isFetching || turnsQueries.some((query) => query.isPending || query.isFetching)
  const availabilityError = hoursQuery.isError || turnsQueries.some((query) => query.isError)
  const employeeServicesQueries = useQueries({ queries: activeEmployees.map((employee) => ({
    queryKey: ownerBusinessEmployeeServicesQueryKey(negocioId, employee.empleadoId),
    queryFn: () => getOwnerBusinessEmployeeServices({ id: negocioId, empleadoId: employee.empleadoId }),
    enabled: Boolean(values.servicioId),
  })) })
  const eligibleEmployees = useMemo(() => {
    if (!chosenService || availabilityLoading || availabilityError) return []
    return activeEmployees.filter((employee, index) => String(employee.usuarioId) !== String(ownerEmployeeId)
      && Array.isArray(employeeServicesQueries[index]?.data)
      && employeeServicesQueries[index].data.some((service) => String(service.servicioId) === values.servicioId && service.activo)
      && availableTimes({ date: values.fecha, duration: chosenService.duracionMinutos, hours, turns, empleadoId: employee.empleadoId }).length > 0)
  }, [activeEmployees, availabilityError, availabilityLoading, chosenService, employeeServicesQueries, hours, ownerEmployeeId, turns, values.fecha, values.servicioId])
  const ownerTimes = useMemo(() => chosenService && !availabilityLoading && !availabilityError
    ? availableTimes({ date: values.fecha, duration: chosenService.duracionMinutos, hours, turns, empleadoId: null }) : [],
  [availabilityError, availabilityLoading, chosenService, hours, turns, values.fecha])
  const selectedEmployeeId = values.responsable.startsWith('employee:') ? values.responsable.slice('employee:'.length) : null
  const selectedTimes = values.responsable === 'OWNER' ? ownerTimes : selectedEmployeeId
    ? availableTimes({ date: values.fecha, duration: chosenService?.duracionMinutos, hours, turns, empleadoId: selectedEmployeeId }) : []
  const hasResponsibleOptions = Boolean(ownerName && ownerTimes.length > 0) || eligibleEmployees.length > 0
  const [timePickerOpen, setTimePickerOpen] = useState(false)
  const assignmentError = employeeServicesQueries.some((query) => query.isError)
  const showAssignmentLoading = employeeServicesQueries.some((query) => query.isPending || query.isFetching)

  useEffect(() => {
    if (!values.responsable || !values.servicioId || showAssignmentLoading || availabilityLoading) return
    const stillAvailable = values.responsable === 'OWNER'
      ? ownerTimes.length > 0
      : eligibleEmployees.some((employee) => `employee:${employee.empleadoId}` === values.responsable)
    if (!stillAvailable) setValues((current) => ({ ...current, responsable: '', hora: '' }))
  }, [availabilityLoading, eligibleEmployees, ownerTimes.length, showAssignmentLoading, values.responsable, values.servicioId])

  function changeField(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value, ...(name === 'fecha' || name === 'servicioId' || name === 'responsable' ? { hora: '' } : {}) }))
    if (name === 'fecha' || name === 'servicioId' || name === 'responsable') setTimePickerOpen(false)
    setFieldErrors((current) => ({ ...current, [name]: '' }))
    setSubmitError('')
    setSubmitNotice('')
  }

  function changeClientField(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, clienteNuevo: { ...current.clienteNuevo, [name]: value } }))
    setFieldErrors((current) => ({ ...current, [name]: '' }))
    setSubmitError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (mutation.isPending) return
    const errors = validate(values, clientMode, selectedClient)
    setFieldErrors(errors)
    setSubmitError('')
    setSubmitNotice('')
    if (Object.keys(errors).length) return
    if (!selectedTimes.includes(values.hora)) {
      setFieldErrors((current) => ({ ...current, hora: 'Elegí un horario disponible para este responsable.' }))
      return
    }

    const turno = {
      servicioId: Number(values.servicioId),
      fechaHoraInicio: `${values.fecha}T${values.hora.length === 5 ? `${values.hora}:00` : values.hora}`,
    }
    if (values.observaciones.trim()) turno.observaciones = values.observaciones.trim()
    if (selectedEmployeeId) turno.empleadoId = Number(selectedEmployeeId)
    if (clientMode === 'existing') turno.clienteLocalId = Number(selectedClient.clienteLocalId)
    else turno.clienteNuevo = Object.fromEntries(Object.entries(values.clienteNuevo).map(([key, value]) => [key, value.trim()]))

    try {
      const created = await mutation.mutateAsync({ negocioId, turno })
      const notice = successMessage(created, chosenService, clientMode === 'existing' ? clientLabel(selectedClient) : `${values.clienteNuevo.nombre.trim()} ${values.clienteNuevo.apellido.trim()}`)
      setSubmitNotice(notice)
      onCreated(created, notice)
      setValues({ servicioId: '', fecha: todayLocal(), hora: '', responsable: '', observaciones: '', clienteNuevo: initialClient })
      setSelectedClient(null)
    } catch (error) {
      setSubmitError(getCreateOwnerTurnoErrorMessage(error))
    }
  }

  const idFor = (field) => `owner-turno-${field}`
  const inputFields = [['nombre', 'Nombre'], ['apellido', 'Apellido'], ['dni', 'DNI'], ['telefono', 'Teléfono']]
  const showEmployeesLoading = employeesQuery.isPending || showAssignmentLoading

  return <section className="create-owner-panel owner-turno-create-panel" aria-labelledby="owner-turno-create-title">
    <div className="create-owner-heading"><div><span className="create-owner-icon"><Scissors size={17} /></span><div><h2 id="owner-turno-create-title">Nuevo turno</h2><p>Completá los datos para reservar un horario.</p></div></div><button type="button" className="icon-button owner-turno-close" aria-label="Cerrar formulario de nuevo turno" onClick={onCancel} disabled={mutation.isPending}><X size={17} /></button></div>
    {servicesQuery.isPending || employeesQuery.isPending ? <p className="owner-turno-search-state" role="status"><LoaderCircle size={15} className="spin" /> Cargando servicios y empleados…</p>
      : servicesQuery.isError || employeesQuery.isError ? <p className="owner-form-submit-error" role="alert">{servicesQuery.isError ? 'No se pudieron cargar los servicios de este negocio.' : 'No se pudieron cargar los empleados de este negocio.'} Cerrá y volvé a abrir el formulario para reintentar.</p>
        : <form className="owner-form owner-turno-form" onSubmit={handleSubmit} noValidate>
          <div className="owner-form-grid">
            <label className="owner-form-field" htmlFor={idFor('servicio')}><span>Servicio *</span><select id={idFor('servicio')} name="servicioId" value={values.servicioId} onChange={changeField} aria-invalid={Boolean(fieldErrors.servicioId)} aria-describedby={fieldErrors.servicioId ? `${idFor('servicio')}-error` : undefined}><option value="">Seleccioná un servicio…</option>{services.map((service) => <option key={service.servicioId} value={service.servicioId}>{service.nombre}{service.precio != null ? ` · ${currencyFormatter.format(Number(service.precio))}` : ''}{service.duracionMinutos != null ? ` · ${service.duracionMinutos} min` : ''}</option>)}</select>{fieldErrors.servicioId && <small id={`${idFor('servicio')}-error`} className="owner-field-error">{fieldErrors.servicioId}</small>}{!services.length && <small className="owner-turno-helper">No hay servicios activos disponibles.</small>}{assignmentError && <small className="owner-field-error" role="alert">No se pudieron comprobar todos los servicios asignados; se muestran solo los responsables comprobados.</small>}</label>
            <label className="owner-form-field" htmlFor={idFor('responsable')}><span>Responsable *</span><select id={idFor('responsable')} name="responsable" value={values.responsable} onChange={changeField} disabled={!values.servicioId || showEmployeesLoading || availabilityLoading || availabilityError || !hasResponsibleOptions} aria-invalid={Boolean(fieldErrors.responsable)} aria-describedby={fieldErrors.responsable ? `${idFor('responsable')}-error` : undefined}><option value="">Seleccione un responsable a cargo</option>{ownerName && ownerTimes.length > 0 && <option value="OWNER">{ownerName}</option>}{eligibleEmployees.map((employee) => <option key={employee.empleadoId} value={`employee:${employee.empleadoId}`}>{employee.nombre} {employee.apellido}</option>)}</select>{showEmployeesLoading && values.servicioId && <small className="owner-turno-helper">Comprobando servicios asignados…</small>}{availabilityLoading && values.servicioId && <small className="owner-turno-helper">Comprobando horarios disponibles…</small>}{availabilityError && <small className="owner-field-error" role="alert">No se pudo comprobar la disponibilidad. Volvé a abrir el formulario para reintentar.</small>}{assignmentError && <button type="button" className="owners-secondary-button" onClick={() => employeeServicesQueries.filter((query) => query.isError).forEach((query) => void query.refetch())}>Reintentar asignaciones</button>}{values.servicioId && !showEmployeesLoading && !availabilityLoading && !availabilityError && !hasResponsibleOptions && <small className="owner-turno-helper">No hay responsables disponibles para este servicio en la fecha elegida.</small>}{fieldErrors.responsable && <small id={`${idFor('responsable')}-error`} className="owner-field-error">{fieldErrors.responsable}</small>}</label>
            <label className="owner-form-field" htmlFor={idFor('fecha')}><span>Fecha *</span><input id={idFor('fecha')} name="fecha" type="date" value={values.fecha} onChange={changeField} min={todayLocal()} aria-invalid={Boolean(fieldErrors.fecha)} aria-describedby={fieldErrors.fecha ? `${idFor('fecha')}-error` : undefined} />{fieldErrors.fecha && <small id={`${idFor('fecha')}-error`} className="owner-field-error">{fieldErrors.fecha}</small>}</label>
            <div className="owner-form-field owner-turno-time-field"><span>Hora de inicio *</span><button id={idFor('hora')} type="button" className="owner-turno-time-trigger" aria-expanded={timePickerOpen} aria-invalid={Boolean(fieldErrors.hora)} aria-describedby={fieldErrors.hora ? `${idFor('hora')}-error` : undefined} disabled={!values.servicioId || !values.responsable || availabilityLoading || availabilityError || !selectedTimes.length} onClick={() => setTimePickerOpen((open) => !open)}>{values.hora || 'Seleccioná un horario'}<ChevronDown size={15} /></button>{timePickerOpen && <div className="owner-turno-time-popover"><div className="owner-turno-time-grid" role="group" aria-label="Horarios disponibles">{selectedTimes.map((time) => <button key={time} type="button" aria-pressed={values.hora === time} className={values.hora === time ? 'is-selected' : ''} onClick={() => { setValues((current) => ({ ...current, hora: time })); setFieldErrors((current) => ({ ...current, hora: '' })); setSubmitError(''); setSubmitNotice(''); setTimePickerOpen(false) }}>{time}</button>)}</div></div>}{values.responsable && !availabilityLoading && !availabilityError && !selectedTimes.length && <small className="owner-turno-helper">No hay horarios disponibles para este responsable en la fecha elegida.</small>}{fieldErrors.hora && <small id={`${idFor('hora')}-error`} className="owner-field-error">{fieldErrors.hora}</small>}</div>
            <label className="owner-form-field owner-turno-observations-field" htmlFor={idFor('observaciones')}><span>Observaciones (opcional)</span><textarea id={idFor('observaciones')} name="observaciones" value={values.observaciones} onChange={changeField} maxLength={500} rows={3} aria-invalid={Boolean(fieldErrors.observaciones)} aria-describedby={fieldErrors.observaciones ? `${idFor('observaciones')}-error` : `${idFor('observaciones')}-count`} />{fieldErrors.observaciones && <small id={`${idFor('observaciones')}-error`} className="owner-field-error">{fieldErrors.observaciones}</small>}<small id={`${idFor('observaciones')}-count`} className="owner-turno-helper">{values.observaciones.length}/500</small></label>
          </div>
          <fieldset className="owner-turno-client-section"><legend>Cliente *</legend><div className="owner-turno-client-mode"><label><input type="radio" name="clientMode" value="existing" checked={clientMode === 'existing'} onChange={() => { setClientMode('existing'); setSelectedClient(null); setFieldErrors((current) => ({ ...current, clienteLocalId: '' })) }} /> Cliente existente</label><label><input type="radio" name="clientMode" value="new" checked={clientMode === 'new'} onChange={() => { setClientMode('new'); setSelectedClient(null); setFieldErrors((current) => ({ ...current, clienteLocalId: '' })) }} /> Nuevo cliente</label></div>
            {clientMode === 'existing' ? <ClientFields errors={fieldErrors} negocioId={negocioId} selectedClient={selectedClient} onSelectClient={setSelectedClient} /> : <div className="owner-form-grid">{inputFields.map(([name, label]) => <label className="owner-form-field" htmlFor={idFor(`cliente-${name}`)} key={name}><span>{label} *</span><input id={idFor(`cliente-${name}`)} name={name} value={values.clienteNuevo[name]} onChange={changeClientField} maxLength={name === 'dni' ? 20 : name === 'telefono' ? 30 : 20} autoComplete={name === 'telefono' ? 'tel' : name} inputMode={name === 'dni' || name === 'telefono' ? 'text' : undefined} aria-invalid={Boolean(fieldErrors[name])} aria-describedby={fieldErrors[name] ? `${idFor(`cliente-${name}`)}-error` : undefined} />{fieldErrors[name] && <small id={`${idFor(`cliente-${name}`)}-error`} className="owner-field-error">{fieldErrors[name]}</small>}</label>)}</div>}
          </fieldset>
          {submitError && <p className="owner-form-submit-error" role="alert">{submitError}</p>}
          {submitNotice && <div className="owner-created-notice" role="status"><CheckCircle2 size={16} /><span>{submitNotice}</span></div>}
          <div className="owner-form-actions"><button type="button" className="owners-secondary-button" onClick={onCancel} disabled={mutation.isPending}>Cancelar</button><button type="submit" className="owner-create-button" disabled={mutation.isPending || !services.length || availabilityLoading || availabilityError || (selectedEmployeeId && (showEmployeesLoading || assignmentError))}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Creando…</> : <><Clock3 size={15} /> Crear turno</>}</button></div>
        </form>}
  </section>
}
