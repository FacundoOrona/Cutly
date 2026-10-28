import { useState } from 'react'
import { CheckCircle2, Clock3, LoaderCircle, Pencil, Plus, RefreshCw, ToggleLeft, ToggleRight, X } from 'lucide-react'
import { getOwnerBusinessServicesErrorMessage } from '../api/ownerBusinessServicesApi.js'
import { useOwnerBusinessServices, useUpdateOwnerBusinessServiceStatus } from '../hooks/useOwnerBusinessServices.js'
import OwnerBusinessServiceForm from './OwnerBusinessServiceForm.jsx'

const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export default function OwnerBusinessServicesSection({ negocioId }) {
  const query = useOwnerBusinessServices(negocioId)
  const statusMutation = useUpdateOwnerBusinessServiceStatus()
  const [editingServiceId, setEditingServiceId] = useState(null)
  const [isCreating, setIsCreating] = useState(false)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const [pendingServiceId, setPendingServiceId] = useState(null)

  async function handleStatusChange(service) {
    const activate = !service.activo
    if (!activate && !window.confirm(`¿Desactivar «${service.nombre}»?`)) return
    setError('')
    setPendingServiceId(service.servicioId)
    try {
      await statusMutation.mutateAsync({ id: negocioId, servicioId: service.servicioId, activo: activate })
      setNotice(activate ? 'El servicio se activó correctamente.' : 'El servicio se desactivó correctamente.')
    } catch (statusError) {
      setError(getOwnerBusinessServicesErrorMessage(statusError, activate ? 'activar' : 'desactivar'))
    } finally {
      setPendingServiceId(null)
    }
  }

  function handleSaved(message) {
    setIsCreating(false)
    setEditingServiceId(null)
    setError('')
    setNotice(message)
  }

  return <section className="create-owner-panel owner-services-panel" aria-labelledby={`owner-services-title-${negocioId}`}>
    <div className="create-owner-heading">
      <div><span className="create-owner-icon"><Clock3 size={17} /></span><div><h2 id={`owner-services-title-${negocioId}`}>Servicios</h2><p>Servicios que ofrecés en este negocio.</p></div></div>
      {query.isSuccess && !isCreating && editingServiceId === null && <button type="button" className="owner-create-button" onClick={() => { setNotice(''); setIsCreating(true) }}><Plus size={15} /> Agregar servicio</button>}
    </div>
    {notice && <div className="owner-created-notice" role="status"><CheckCircle2 size={17} /><span>{notice}</span><button type="button" className="icon-button" onClick={() => setNotice('')} aria-label="Cerrar confirmación"><X size={16} /></button></div>}
    {error && <p className="owner-form-submit-error" role="alert">{error}</p>}
    {query.isPending ? <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando servicios…</div>
      : query.isError ? <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessServicesErrorMessage(query.error)}</p><button type="button" className="owners-secondary-button" onClick={() => query.refetch()} disabled={query.isFetching}><RefreshCw size={15} /> Reintentar</button></div>
        : <>
          {isCreating && <OwnerBusinessServiceForm key={`create-${negocioId}`} id={negocioId} onCancel={() => setIsCreating(false)} onSaved={handleSaved} />}
          {!query.data.length && !isCreating ? <div className="owner-business-state"><p>Todavía no hay servicios registrados.</p><button type="button" className="owner-create-button" onClick={() => { setNotice(''); setIsCreating(true) }}><Plus size={15} /> Crear primer servicio</button></div>
            : <ul className="owner-services-list">{query.data.map((service) => <li className="owner-service-card" key={service.servicioId}>
              {editingServiceId === service.servicioId ? <OwnerBusinessServiceForm key={`edit-${negocioId}-${service.servicioId}`} id={negocioId} service={service} onCancel={() => setEditingServiceId(null)} onSaved={handleSaved} /> : <>
                <div className="owner-service-main">
                  <div className="owner-service-heading"><h3>{service.nombre}</h3><span className={service.activo ? 'owner-service-status is-active' : 'owner-service-status'}>{service.activo ? 'Activo' : 'Inactivo'}</span></div>
                  {service.descripcion && <p className="owner-service-description">{service.descripcion}</p>}
                  <div className="owner-service-details"><strong>{currencyFormatter.format(Number(service.precio))}</strong><span>{service.duracionMinutos} min</span></div>
                </div>
                <div className="owner-service-actions">
                  <button type="button" className="owners-secondary-button" onClick={() => { setNotice(''); setEditingServiceId(service.servicioId) }} disabled={pendingServiceId !== null}><Pencil size={14} /> Editar</button>
                  <button type="button" className="owners-secondary-button" onClick={() => void handleStatusChange(service)} disabled={pendingServiceId !== null}>
                    {pendingServiceId === service.servicioId ? <LoaderCircle size={14} className="spin" /> : service.activo ? <ToggleLeft size={15} /> : <ToggleRight size={15} />}
                    {pendingServiceId === service.servicioId ? 'Actualizando…' : service.activo ? 'Desactivar' : 'Activar'}
                  </button>
                </div>
              </>}
            </li>)}</ul>}
        </>}
  </section>
}
