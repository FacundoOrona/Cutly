import { useState } from 'react'
import { CheckCircle2, LoaderCircle, Pencil, Plus, RefreshCw, Scissors, ToggleLeft, ToggleRight, UserRound, X } from 'lucide-react'
import { getOwnerBusinessEmployeesErrorMessage } from '../api/ownerBusinessEmployeesApi.js'
import { useOwnerBusinessEmployees, useUpdateOwnerBusinessEmployeeStatus } from '../hooks/useOwnerBusinessEmployees.js'
import OwnerBusinessEmployeeForm from './OwnerBusinessEmployeeForm.jsx'
import OwnerBusinessEmployeeServices from './OwnerBusinessEmployeeServices.jsx'

export default function OwnerBusinessEmployeesSection({ negocioId }) {
  const query = useOwnerBusinessEmployees(negocioId)
  const statusMutation = useUpdateOwnerBusinessEmployeeStatus()
  const [editingEmployeeId, setEditingEmployeeId] = useState(null)
  const [isCreating, setIsCreating] = useState(false)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const [pendingEmployeeId, setPendingEmployeeId] = useState(null)
  const [servicesEmployeeId, setServicesEmployeeId] = useState(null)

  async function handleStatusChange(employee) {
    const activate = !employee.activo
    if (!activate && !window.confirm(`¿Desactivar a ${employee.nombre} ${employee.apellido}?`)) return
    setError('')
    setPendingEmployeeId(employee.empleadoId)
    try {
      await statusMutation.mutateAsync({ id: negocioId, empleadoId: employee.empleadoId, activo: activate })
      setNotice(activate ? 'El empleado se activó correctamente.' : 'El empleado se desactivó correctamente.')
    } catch (statusError) {
      setError(getOwnerBusinessEmployeesErrorMessage(statusError, activate ? 'activar' : 'desactivar'))
    } finally {
      setPendingEmployeeId(null)
    }
  }

  function handleSaved(message) {
    setIsCreating(false)
    setEditingEmployeeId(null)
    setError('')
    setNotice(message)
  }

  return <section className="create-owner-panel owner-employees-panel" aria-labelledby={`owner-employees-title-${negocioId}`}>
    <div className="create-owner-heading">
      <div><span className="create-owner-icon"><UserRound size={17} /></span><div><h2 id={`owner-employees-title-${negocioId}`}>Empleados</h2><p>Personas que trabajan en este negocio.</p></div></div>
      {query.isSuccess && !isCreating && editingEmployeeId === null && <button type="button" className="owner-create-button" onClick={() => { setNotice(''); setIsCreating(true) }}><Plus size={15} /> Agregar empleado</button>}
    </div>
    {notice && <div className="owner-created-notice" role="status"><CheckCircle2 size={17} /><span>{notice}</span><button type="button" className="icon-button" onClick={() => setNotice('')} aria-label="Cerrar confirmación"><X size={16} /></button></div>}
    {error && <p className="owner-form-submit-error" role="alert">{error}</p>}
    {query.isPending ? <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando empleados…</div>
      : query.isError ? <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessEmployeesErrorMessage(query.error)}</p><button type="button" className="owners-secondary-button" onClick={() => query.refetch()} disabled={query.isFetching}><RefreshCw size={15} /> Reintentar</button></div>
        : <>
          {isCreating && <OwnerBusinessEmployeeForm key={`create-${negocioId}`} id={negocioId} onCancel={() => setIsCreating(false)} onSaved={handleSaved} />}
          {!query.data.length && !isCreating ? <div className="owner-business-state"><p>Todavía no hay empleados registrados.</p><button type="button" className="owner-create-button" onClick={() => { setNotice(''); setIsCreating(true) }}><Plus size={15} /> Agregar primer empleado</button></div>
            : <ul className="owner-employees-list">{query.data.map((employee) => <li className="owner-employee-card" key={employee.empleadoId}>
              {editingEmployeeId === employee.empleadoId ? <OwnerBusinessEmployeeForm key={`edit-${negocioId}-${employee.empleadoId}`} id={negocioId} employee={employee} onCancel={() => setEditingEmployeeId(null)} onSaved={handleSaved} /> : <>
                <div className="owner-employee-main">
                  <div className="owner-service-heading"><h3>{employee.nombre} {employee.apellido}</h3><span className={employee.activo ? 'owner-service-status is-active' : 'owner-service-status'}>{employee.activo ? 'Activo' : 'Inactivo'}</span></div>
                  <div className="owner-employee-details"><span>{employee.email}</span><span>DNI: {employee.dni}</span></div>
                </div>
                <div className="owner-service-actions">
                  <button type="button" className="owners-secondary-button" onClick={() => { setNotice(''); setEditingEmployeeId(employee.empleadoId) }} disabled={pendingEmployeeId !== null}><Pencil size={14} /> Editar</button>
                  <button type="button" className="owners-secondary-button" aria-expanded={servicesEmployeeId === employee.empleadoId} aria-controls={`employee-services-${negocioId}-${employee.empleadoId}`} onClick={() => setServicesEmployeeId((current) => current === employee.empleadoId ? null : employee.empleadoId)}><Scissors size={14} /> Servicios</button>
                  <button type="button" className="owners-secondary-button" onClick={() => void handleStatusChange(employee)} disabled={pendingEmployeeId !== null}>
                    {pendingEmployeeId === employee.empleadoId ? <LoaderCircle size={14} className="spin" /> : employee.activo ? <ToggleLeft size={15} /> : <ToggleRight size={15} />}
                    {pendingEmployeeId === employee.empleadoId ? 'Actualizando…' : employee.activo ? 'Desactivar' : 'Activar'}
                  </button>
                </div>
                {servicesEmployeeId === employee.empleadoId && <div id={`employee-services-${negocioId}-${employee.empleadoId}`} className="owner-employee-services-wrap"><OwnerBusinessEmployeeServices key={`${negocioId}-${employee.empleadoId}`} negocioId={negocioId} empleadoId={employee.empleadoId} /></div>}
              </>}
            </li>)}</ul>}
        </>}
  </section>
}
