import { useEffect, useMemo, useRef, useState } from 'react'
import { CalendarDays, CheckCircle2, Clock3, LoaderCircle, Scissors, Search, UserRoundPlus, X } from 'lucide-react'
import { getEmployeeAppointmentCreateErrorMessage } from '../api/employeeApi.js'
import {
  useCreateEmployeeAppointment,
  useEmployeeAvailability,
  useEmployeeLocalClients,
  useEmployeeServices,
} from '../hooks/useEmployeeTurnoCreation.js'
import './EmployeeAppointmentCreateForm.css'

const initialClient = { nombre: '', apellido: '', dni: '', telefono: '' }
const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

function localDateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function timeMinutes(value) {
  const match = String(value || '').match(/T(\d{2}):(\d{2})/)
  return match ? Number(match[1]) * 60 + Number(match[2]) : null
}

function formatTime(value) {
  return `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(value % 60).padStart(2, '0')}`
}

function availableTimes(ranges, duration, fecha) {
  if (!Number.isFinite(duration) || duration <= 0) return []
  const times = new Set()
  const now = new Date()
  const today = localDateKey(now)

  for (const range of ranges) {
    if (range.fecha !== fecha) continue
    const start = timeMinutes(range.fechaHoraInicio)
    const end = timeMinutes(range.fechaHoraFin)
    if (start == null || end == null || end <= start) continue
    for (let minute = Math.ceil(start / 15) * 15; minute + duration <= end; minute += 15) {
      const time = formatTime(minute)
      if (fecha === today) {
        const [hour, min] = time.split(':').map(Number)
        if (new Date(now.getFullYear(), now.getMonth(), now.getDate(), hour, min).getTime() <= now.getTime()) continue
      }
      times.add(time)
    }
  }
  return [...times].sort()
}

function dateLabel(fecha) {
  if (!fecha) return ''
  const [year, month, day] = fecha.split('-').map(Number)
  return new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'long', year: 'numeric' })
    .format(new Date(year, month - 1, day, 12))
}

function validateClient(client) {
  const errors = {}
  for (const [field, label] of [['nombre', 'El nombre'], ['apellido', 'El apellido']]) {
    const value = client[field].trim()
    if (!value) errors[field] = `${label} es obligatorio.`
    else if (value.length < 2 || value.length > 20) errors[field] = `${label} debe contener entre 2 y 20 caracteres.`
  }
  const dni = client.dni.trim()
  if (!dni) errors.dni = 'El DNI es obligatorio.'
  else if (dni.length > 20 || !/^[0-9.\-\s]+$/.test(dni) || !/^\d{7,8}$/.test(dni.replace(/[.\-\s]/g, ''))) {
    errors.dni = 'Ingresá un DNI de 7 u 8 dígitos. Podés usar puntos o guiones.'
  }
  const telefono = client.telefono.trim()
  if (!telefono) errors.telefono = 'El teléfono es obligatorio.'
  else if (telefono.length > 30) errors.telefono = 'El teléfono no puede superar los 30 caracteres.'
  return errors
}

function validateForm({ servicio, fecha, hora, clientMode, selectedClient, client }) {
  const errors = {}
  if (!servicio) errors.servicioId = 'Seleccioná un servicio activo.'
  if (!fecha) errors.fecha = 'Seleccioná una fecha.'
  if (!hora) errors.hora = 'Seleccioná un horario disponible.'
  if (clientMode === 'existing' && !selectedClient?.id) errors.clienteLocalId = 'Buscá y seleccioná un cliente.'
  if (clientMode === 'new') Object.assign(errors, validateClient(client))
  return errors
}

function clientName(client) {
  return [client?.nombre, client?.apellido].filter(Boolean).join(' ')
}

export default function EmployeeAppointmentCreateForm({ onCancel, onCreated, onPendingChange }) {
  const [serviceId, setServiceId] = useState('')
  const [fecha, setFecha] = useState(() => localDateKey(new Date()))
  const [hora, setHora] = useState('')
  const [clientMode, setClientMode] = useState('existing')
  const [searchText, setSearchText] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [selectedClient, setSelectedClient] = useState(null)
  const [client, setClient] = useState(initialClient)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const submitLock = useRef(false)

  const servicesQuery = useEmployeeServices()
  const availabilityQuery = useEmployeeAvailability(fecha)
  const clientsQuery = useEmployeeLocalClients(debouncedSearch, clientMode === 'existing')
  const createMutation = useCreateEmployeeAppointment()
  const services = useMemo(() => (Array.isArray(servicesQuery.data) ? servicesQuery.data : [])
    .filter((service) => service.activo === true), [servicesQuery.data])
  const selectedService = services.find((service) => String(service.servicioId) === serviceId)
  const ranges = Array.isArray(availabilityQuery.data) ? availabilityQuery.data : []
  const times = useMemo(() => availableTimes(ranges, Number(selectedService?.duracionMinutos), fecha),
    [ranges, selectedService?.duracionMinutos, fecha])
  const timesKey = times.join('|')
  const clients = Array.isArray(clientsQuery.data) ? clientsQuery.data : []
  const normalizedSearch = searchText.trim()
  const searchReady = debouncedSearch === normalizedSearch
    && normalizedSearch.replace(/\s/g, '').length >= 2

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedSearch(searchText.trim()), 300)
    return () => window.clearTimeout(timer)
  }, [searchText])

  useEffect(() => {
    if (availabilityQuery.isSuccess && hora && !timesKey.split('|').includes(hora)) setHora('')
  }, [availabilityQuery.isSuccess, hora, timesKey])

  function changeClientMode(mode) {
    setClientMode(mode)
    setSelectedClient(null)
    setSearchText('')
    setDebouncedSearch('')
    setFieldErrors((current) => ({ ...current, clienteLocalId: '', nombre: '', apellido: '', dni: '', telefono: '' }))
    setSubmitError('')
  }

  async function submit(event) {
    event.preventDefault()
    if (createMutation.isPending || submitLock.current) return
    const errors = validateForm({ servicio: selectedService, fecha, hora, clientMode, selectedClient, client })
    if (hora && !times.includes(hora)) errors.hora = 'Ese horario ya no está disponible. Elegí otro.'
    setFieldErrors(errors)
    setSubmitError('')
    if (Object.keys(errors).length) return

    const request = {
      servicioId: Number(selectedService.servicioId),
      fechaHoraInicio: `${fecha}T${hora}:00`,
      ...(clientMode === 'existing'
        ? { clienteLocalId: Number(selectedClient.id) }
        : { clienteNuevo: Object.fromEntries(Object.entries(client).map(([key, value]) => [key, value.trim()])) }),
    }

    submitLock.current = true
    onPendingChange(true)
    try {
      const created = await createMutation.mutateAsync(request)
      onCreated(created)
    } catch (error) {
      setSubmitError(getEmployeeAppointmentCreateErrorMessage(error))
    } finally {
      submitLock.current = false
      onPendingChange(false)
    }
  }

  const selectedClientName = clientMode === 'existing' ? clientName(selectedClient) : clientName(client)
  const price = selectedService?.precio == null ? '—' : currencyFormatter.format(Number(selectedService.precio))

  return <section id="employee-create-turno-panel" className="employee-create-turno" aria-labelledby="employee-create-turno-title" aria-busy={createMutation.isPending}>
    <div className="employee-create-turno-heading">
      <div><span className="employee-create-turno-icon"><Scissors size={17} /></span><div><h2 id="employee-create-turno-title">Nuevo turno</h2><p>Elegí cliente, servicio y horario.</p></div></div>
      <button type="button" className="employee-create-turno-close" aria-label="Cerrar formulario de nuevo turno" onClick={onCancel} disabled={createMutation.isPending}><X size={17} /></button>
    </div>

    {servicesQuery.isPending ? <p className="employee-create-turno-state" role="status"><LoaderCircle size={15} className="employee-action-spinner" /> Cargando servicios…</p>
      : servicesQuery.isError ? <div className="employee-create-turno-error" role="alert"><span>{getEmployeeAppointmentCreateErrorMessage(servicesQuery.error)}</span><button type="button" onClick={() => void servicesQuery.refetch()} disabled={servicesQuery.isFetching}>Reintentar</button></div>
        : !services.length ? <p className="employee-create-turno-state">No tenés servicios activos asignados.</p>
          : <form className="employee-create-turno-form" onSubmit={submit} noValidate>
            <div className="employee-create-turno-grid">
              <label className="employee-create-turno-field" htmlFor="employee-create-service"><span>Servicio *</span>
                <select id="employee-create-service" value={serviceId} onChange={(event) => { setServiceId(event.target.value); setHora(''); setSubmitError(''); setFieldErrors((current) => ({ ...current, servicioId: '', hora: '' })) }} disabled={createMutation.isPending} aria-invalid={Boolean(fieldErrors.servicioId)}>
                  <option value="">Seleccioná un servicio…</option>
                  {services.map((service) => <option key={service.servicioId} value={service.servicioId}>{service.nombre} · {service.duracionMinutos} min · {currencyFormatter.format(Number(service.precio))}</option>)}
                </select>
                {fieldErrors.servicioId && <small role="alert">{fieldErrors.servicioId}</small>}
              </label>

              <label className="employee-create-turno-field" htmlFor="employee-create-date"><span>Fecha *</span>
                <span className="employee-create-turno-date-input"><CalendarDays size={15} /><input id="employee-create-date" type="date" value={fecha} min={localDateKey(new Date())} onChange={(event) => { setFecha(event.target.value); setHora(''); setFieldErrors((current) => ({ ...current, fecha: '', hora: '' })); setSubmitError('') }} disabled={createMutation.isPending} aria-invalid={Boolean(fieldErrors.fecha)} /></span>
                {fieldErrors.fecha && <small role="alert">{fieldErrors.fecha}</small>}
              </label>

              <label className="employee-create-turno-field" htmlFor="employee-create-time"><span>Horario *</span>
                <span className="employee-create-turno-time-input"><Clock3 size={15} /><select id="employee-create-time" value={hora} onChange={(event) => { setHora(event.target.value); setFieldErrors((current) => ({ ...current, hora: '' })); setSubmitError('') }} disabled={!selectedService || availabilityQuery.isFetching || availabilityQuery.isError || !times.length || createMutation.isPending} aria-invalid={Boolean(fieldErrors.hora)}>
                  <option value="">Seleccioná una hora…</option>{times.map((time) => <option key={time} value={time}>{time}</option>)}
                </select></span>
                {availabilityQuery.isFetching && <small role="status">Consultando disponibilidad…</small>}
                {availabilityQuery.isError && <small className="is-error" role="alert">{getEmployeeAppointmentCreateErrorMessage(availabilityQuery.error)} <button type="button" onClick={() => void availabilityQuery.refetch()} disabled={availabilityQuery.isFetching}>Reintentar</button></small>}
                {availabilityQuery.isSuccess && selectedService && !times.length && <small>No hay horarios disponibles para este servicio y fecha.</small>}
                {fieldErrors.hora && <small role="alert">{fieldErrors.hora}</small>}
              </label>
            </div>

            <fieldset className="employee-create-turno-client">
              <legend>Cliente *</legend>
              <div className="employee-create-turno-client-modes">
                <label><input type="radio" name="employee-client-mode" value="existing" checked={clientMode === 'existing'} onChange={() => changeClientMode('existing')} disabled={createMutation.isPending} /> Cliente existente</label>
                <label><input type="radio" name="employee-client-mode" value="new" checked={clientMode === 'new'} onChange={() => changeClientMode('new')} disabled={createMutation.isPending} /> Cliente nuevo</label>
              </div>

              {clientMode === 'existing' ? <div className="employee-create-turno-client-search">
                <label className="employee-create-turno-field" htmlFor="employee-client-search"><span>Buscar por nombre o apellido</span>
                  <span className="employee-create-turno-search-input"><Search size={15} /><input id="employee-client-search" type="search" value={searchText} maxLength={100} onChange={(event) => { setSearchText(event.target.value); setSelectedClient(null); setFieldErrors((current) => ({ ...current, clienteLocalId: '' })); setSubmitError('') }} placeholder="Escribí al menos 2 caracteres" disabled={createMutation.isPending} /></span>
                </label>
                {normalizedSearch.replace(/\s/g, '').length >= 2 && !searchReady && <small className="employee-create-turno-help" role="status">Buscando…</small>}
                {searchReady && clientsQuery.isFetching && <small className="employee-create-turno-help" role="status">Buscando clientes…</small>}
                {clientsQuery.isError && searchReady && <div className="employee-create-turno-error" role="alert"><span>{getEmployeeAppointmentCreateErrorMessage(clientsQuery.error)}</span><button type="button" onClick={() => void clientsQuery.refetch()} disabled={clientsQuery.isFetching}>Reintentar</button></div>}
                {searchReady && !clientsQuery.isFetching && !clientsQuery.isError && clients.length === 0 && <small className="employee-create-turno-help">No se encontraron clientes con esa búsqueda.</small>}
                {searchReady && !clientsQuery.isError && clients.length > 0 && <ul className="employee-create-turno-results" aria-label="Resultados de clientes">{clients.map((result) => <li key={result.id}><button type="button" className={String(selectedClient?.id) === String(result.id) ? 'is-selected' : ''} onClick={() => { setSelectedClient(result); setSearchText(clientName(result)) }} aria-pressed={String(selectedClient?.id) === String(result.id)}><span>{clientName(result)}</span>{String(selectedClient?.id) === String(result.id) && <CheckCircle2 size={16} aria-hidden="true" />}</button></li>)}</ul>}
                {selectedClient && <p className="employee-create-turno-selected" role="status">Cliente seleccionado: <strong>{clientName(selectedClient)}</strong></p>}
                {fieldErrors.clienteLocalId && <small className="is-error" role="alert">{fieldErrors.clienteLocalId}</small>}
              </div> : <div className="employee-create-turno-grid employee-create-turno-new-client">
                {[
                  ['nombre', 'Nombre'], ['apellido', 'Apellido'], ['dni', 'DNI'], ['telefono', 'Teléfono'],
                ].map(([field, label]) => <label className="employee-create-turno-field" htmlFor={`employee-client-${field}`} key={field}><span>{label} *</span>
                  <input id={`employee-client-${field}`} name={field} type={field === 'telefono' ? 'tel' : 'text'} value={client[field]} maxLength={field === 'nombre' || field === 'apellido' ? 20 : field === 'dni' ? 20 : 30} onChange={(event) => { setClient((current) => ({ ...current, [field]: event.target.value })); setFieldErrors((current) => ({ ...current, [field]: '' })); setSubmitError('') }} disabled={createMutation.isPending} aria-invalid={Boolean(fieldErrors[field])} autoComplete={field === 'nombre' ? 'given-name' : field === 'apellido' ? 'family-name' : field === 'telefono' ? 'tel' : 'off'} />
                  {fieldErrors[field] && <small role="alert">{fieldErrors[field]}</small>}
                </label>)}
              </div>}
            </fieldset>

            {selectedService && hora && <section className="employee-create-turno-summary" aria-labelledby="employee-create-summary-title">
              <h3 id="employee-create-summary-title">Resumen del turno</h3>
              <dl><div><dt>Cliente</dt><dd>{selectedClientName || '—'}</dd></div><div><dt>Servicio</dt><dd>{selectedService.nombre}</dd></div><div><dt>Duración</dt><dd>{selectedService.duracionMinutos} min</dd></div><div><dt>Fecha</dt><dd>{dateLabel(fecha)}</dd></div><div><dt>Hora</dt><dd>{hora}</dd></div><div><dt>Precio</dt><dd>{price}</dd></div></dl>
            </section>}

            {submitError && <p className="employee-create-turno-submit-error" role="alert">{submitError}</p>}
            <div className="employee-create-turno-actions">
              <button type="button" className="employee-action-button is-secondary" onClick={onCancel} disabled={createMutation.isPending}>Cancelar</button>
              <button type="submit" className="employee-action-button is-primary" disabled={createMutation.isPending || servicesQuery.isPending || availabilityQuery.isFetching || availabilityQuery.isError || !selectedService || !hora}>
                {createMutation.isPending ? <><LoaderCircle size={15} className="employee-action-spinner" /> Guardando…</> : <><UserRoundPlus size={15} /> Confirmar turno</>}
              </button>
            </div>
          </form>}
  </section>
}
