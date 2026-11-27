import { useState } from 'react'
import { CheckCircle2, Clock3, LoaderCircle, Plus, RefreshCw, Scissors, Trash2, Percent } from 'lucide-react'
import { getOwnerBusinessEmployeesErrorMessage } from '../api/ownerBusinessEmployeesApi.js'
import { getOwnerBusinessServicesErrorMessage } from '../api/ownerBusinessServicesApi.js'
import { useAssignOwnerBusinessEmployeeService, useOwnerBusinessEmployeeServices, useRemoveOwnerBusinessEmployeeService } from '../hooks/useOwnerBusinessEmployees.js'
import { useOwnerBusinessServices } from '../hooks/useOwnerBusinessServices.js'
import { useOwnerBusinessEmployeeCompensation, useUpdateOwnerBusinessEmployeeGeneralCompensation, useUpdateOwnerBusinessEmployeeServiceCompensation, useRemoveOwnerBusinessEmployeeServiceCompensation } from '../hooks/useOwnerBusinessEmployees.js'

const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export default function OwnerBusinessEmployeeServices({ negocioId, empleadoId }) {
  const assignments = useOwnerBusinessEmployeeServices(negocioId, empleadoId)
  const services = useOwnerBusinessServices(negocioId)
  const assignMutation = useAssignOwnerBusinessEmployeeService()
  const removeMutation = useRemoveOwnerBusinessEmployeeService()
  const compensation = useOwnerBusinessEmployeeCompensation(negocioId, empleadoId)
  const saveGeneral = useUpdateOwnerBusinessEmployeeGeneralCompensation()
  const saveService = useUpdateOwnerBusinessEmployeeServiceCompensation()
  const removeException = useRemoveOwnerBusinessEmployeeServiceCompensation()
  const [pendingServiceId, setPendingServiceId] = useState(null)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const [generalValue, setGeneralValue] = useState('')
  const [serviceValues, setServiceValues] = useState({})

  const assignedIds = new Set((assignments.data || []).map((service) => service.servicioId))
  const availableServices = (services.data || []).filter((service) => !assignedIds.has(service.servicioId))

  async function handleAssign(service) {
    if (pendingServiceId !== null || assignedIds.has(service.servicioId)) return
    setPendingServiceId(service.servicioId)
    setError('')
    setNotice('')
    try {
      await assignMutation.mutateAsync({ id: negocioId, empleadoId, servicioId: service.servicioId })
      await compensation.refetch()
      setNotice(`«${service.nombre}» se asignó correctamente.`)
    } catch (mutationError) {
      setError(getOwnerBusinessEmployeesErrorMessage(mutationError, 'asignar'))
      if (mutationError.response?.status === 409) {
        await assignments.refetch()
      }
    } finally {
      setPendingServiceId(null)
    }
  }

  async function handleRemove(service) {
    if (pendingServiceId !== null || !window.confirm(`¿Quitar «${service.nombre}» de este empleado?`)) return
    setPendingServiceId(service.servicioId)
    setError('')
    setNotice('')
    try {
      await removeMutation.mutateAsync({ id: negocioId, empleadoId, servicioId: service.servicioId })
      await compensation.refetch()
      setNotice(`Se quitó «${service.nombre}» de este empleado.`)
    } catch (mutationError) {
      setError(getOwnerBusinessEmployeesErrorMessage(mutationError, 'quitar'))
    } finally {
      setPendingServiceId(null)
    }
  }

  function porcentajeValido(value) {
    return value.trim() !== '' && Number.isFinite(Number(value)) && Number(value) >= 0 && Number(value) <= 100
  }

  async function guardarGeneral(event) {
    event.preventDefault()
    if (!porcentajeValido(generalValue)) { setError('Ingresá un porcentaje entre 0 y 100.'); return }
    setError('')
    try {
      await saveGeneral.mutateAsync({ id: negocioId, empleadoId, porcentaje: generalValue })
      setGeneralValue('')
      setNotice('La comisión general se guardó correctamente.')
    } catch (e) { setError(getOwnerBusinessEmployeesErrorMessage(e, 'guardar la comisión')) }
  }

  async function guardarServicio(service) {
    const value = serviceValues[service.servicioId] ?? ''
    if (!porcentajeValido(value)) { setError('Ingresá un porcentaje entre 0 y 100.'); return }
    setError('')
    try {
      await saveService.mutateAsync({ id: negocioId, empleadoId, servicioId: service.servicioId, porcentaje: value })
      setServiceValues((values) => ({ ...values, [service.servicioId]: '' }))
      setNotice(`La comisión de «${service.nombre}» se guardó correctamente.`)
    } catch (e) { setError(getOwnerBusinessEmployeesErrorMessage(e, 'guardar la comisión')) }
  }

  async function volverGeneral(service) {
    if (!window.confirm(`¿Eliminar la excepción de «${service.nombre}» y volver a usar la comisión general?`)) return
    setError('')
    try {
      await removeException.mutateAsync({ id: negocioId, empleadoId, servicioId: service.servicioId })
      setServiceValues((values) => ({ ...values, [service.servicioId]: '' }))
      setNotice(`«${service.nombre}» vuelve a usar la comisión general.`)
    } catch (e) { setError(getOwnerBusinessEmployeesErrorMessage(e, 'eliminar la excepción')) }
  }

  const loading = assignments.isPending || services.isPending || compensation.isPending
  const queryError = assignments.isError || services.isError || compensation.isError

  return <div className="owner-employee-services" aria-label="Servicios asignados al empleado">
    <div className="owner-employee-services-heading"><h4><Scissors size={14} /> Servicios asignados</h4></div>
    {notice && <p className="owner-assignment-notice" role="status"><CheckCircle2 size={14} />{notice}</p>}
    {error && <p className="owner-form-submit-error" role="alert">{error}</p>}
    {loading ? <div className="owner-assignment-state" role="status"><LoaderCircle size={16} className="spin" /> Cargando servicios…</div>
      : queryError ? <div className="owner-assignment-error" role="alert">
        <p>{compensation.isError ? getOwnerBusinessEmployeesErrorMessage(compensation.error) : assignments.isError ? getOwnerBusinessEmployeesErrorMessage(assignments.error) : getOwnerBusinessServicesErrorMessage(services.error)}</p>
        <button type="button" className="owners-secondary-button" onClick={() => { if (assignments.isError) void assignments.refetch(); if (services.isError) void services.refetch(); if (compensation.isError) void compensation.refetch() }} disabled={assignments.isFetching || services.isFetching || compensation.isFetching}><RefreshCw size={14} /> Reintentar</button>
      </div> : <>
        <div className="owner-commission-box">
          <form className="owner-commission-general" onSubmit={guardarGeneral}>
            <label htmlFor={`employee-${empleadoId}-commission-general`}><Percent size={14} /> Comisión general</label>
            <span>{compensation.data.porcentajeGeneral}%</span>
            <input id={`employee-${empleadoId}-commission-general`} type="number" min="0" max="100" step="0.01" inputMode="decimal" placeholder="Nuevo %" value={generalValue} onChange={(e) => setGeneralValue(e.target.value)} aria-label="Porcentaje general" />
            <button type="submit" className="owners-secondary-button" disabled={saveGeneral.isPending}>{saveGeneral.isPending ? 'Guardando…' : 'Guardar'}</button>
          </form>
          <p className="owner-assignment-empty">Las excepciones por servicio reemplazan este porcentaje.</p>
        </div>
        {!assignments.data.length ? <p className="owner-assignment-empty">Este empleado todavía no tiene servicios asignados.</p>
          : <ul className="owner-assigned-services">{assignments.data.map((service) => {
            const commission = compensation.data.servicios.find((item) => item.servicioId === service.servicioId)
            return <li className="owner-assigned-service" key={service.servicioId}>
            <div className="owner-assigned-service-main">
              <div className="owner-service-heading"><strong>{service.nombre}</strong><span className={service.activo ? 'owner-service-status is-active' : 'owner-service-status'}>{service.activo ? 'Activo' : 'Inactivo'}</span></div>
              <div className="owner-service-details"><span>{currencyFormatter.format(Number(service.precio))}</span><span><Clock3 size={12} /> {service.duracionMinutos} min</span></div>
            </div>
            <div className="owner-service-commission">
              <span>{commission?.usaPorcentajeGeneral ? `Usa comisión general (${commission.porcentajeAplicado}%)` : `Excepción: ${commission?.porcentajeAplicado}%`}</span>
              <input type="number" min="0" max="100" step="0.01" inputMode="decimal" placeholder="Porcentaje" aria-label={`Comisión particular para ${service.nombre}`} value={serviceValues[service.servicioId] ?? (commission?.usaPorcentajeGeneral ? '' : String(commission?.porcentajeExcepcion ?? ''))} onChange={(e) => setServiceValues((values) => ({ ...values, [service.servicioId]: e.target.value }))} />
              <button type="button" className="owners-secondary-button" onClick={() => void guardarServicio(service)} disabled={saveService.isPending}>{saveService.isPending ? 'Guardando…' : 'Guardar'}</button>
              {!commission?.usaPorcentajeGeneral && <button type="button" className="owners-secondary-button" onClick={() => void volverGeneral(service)} disabled={removeException.isPending}>Usar general</button>}
            </div>
            <button type="button" className="owners-secondary-button owner-assignment-remove" onClick={() => void handleRemove(service)} disabled={pendingServiceId !== null}>
              {pendingServiceId === service.servicioId ? <LoaderCircle size={14} className="spin" /> : <Trash2 size={14} />} Quitar
            </button>
          </li>})}</ul>}
        <div className="owner-assignment-picker">
          <label htmlFor={`employee-${negocioId}-${empleadoId}-service`}><Plus size={14} /> Asignar servicio disponible</label>
          {availableServices.length ? <select id={`employee-${negocioId}-${empleadoId}-service`} value="" onChange={(event) => {
            const service = availableServices.find((item) => item.servicioId === Number(event.target.value))
            if (service) void handleAssign(service)
          }} disabled={pendingServiceId !== null}>
            <option value="" disabled>Seleccioná un servicio…</option>
            {availableServices.map((service) => <option key={service.servicioId} value={service.servicioId}>{service.nombre} · {currencyFormatter.format(Number(service.precio))} · {service.duracionMinutos} min · {service.activo ? 'Activo' : 'Inactivo'}</option>)}
          </select> : <p className="owner-assignment-empty">{services.data.length ? 'Todos los servicios del negocio ya están asignados.' : 'Este negocio todavía no tiene servicios.'}</p>}
          {pendingServiceId !== null && <span className="owner-assignment-pending"><LoaderCircle size={13} className="spin" /> Actualizando asignación…</span>}
        </div>
      </>}
  </div>
}
