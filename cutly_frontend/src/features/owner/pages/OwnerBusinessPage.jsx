import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Building2, CheckCircle2, ChevronDown, LoaderCircle, Pencil, RefreshCw, X } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { useActiveOwnerBusiness } from '../context/OwnerBusinessContext.jsx'
import { getOwnerBusiness, getOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'
import EditOwnerBusinessForm from '../components/EditOwnerBusinessForm.jsx'
import { useQueryClient } from '@tanstack/react-query'
import { ownerBusinessDetailQueryKey } from '../hooks/useOwnerBusinesses.js'
import OwnerBusinessAddressSection from '../components/OwnerBusinessAddressSection.jsx'
import OwnerBusinessHoursSection from '../components/OwnerBusinessHoursSection.jsx'
import OwnerBusinessServicesSection from '../components/OwnerBusinessServicesSection.jsx'
import OwnerBusinessEmployeesSection from '../components/OwnerBusinessEmployeesSection.jsx'

export default function OwnerBusinessPage() {
  const { negocioId: routeBusinessId } = useParams()
  const { activeBusinessId, setActiveBusinessId, businesses, businessesQuery } = useActiveOwnerBusiness()
  const validRouteId = typeof routeBusinessId === 'string' && /^[1-9]\d*$/.test(routeBusinessId) && Number.isSafeInteger(Number(routeBusinessId))
  const validId = validRouteId && routeBusinessId === activeBusinessId
  const [business, setBusiness] = useState(null)
  const [isLoading, setIsLoading] = useState(validRouteId)
  const [error, setError] = useState(validRouteId ? null : new Error('invalid id'))
  const [isEditing, setIsEditing] = useState(false)
  const [showUpdatedNotice, setShowUpdatedNotice] = useState(false)
  const queryClient = useQueryClient()

  const loadBusiness = useCallback(async (isCurrent = () => true) => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await getOwnerBusiness(activeBusinessId)
      queryClient.setQueryData(ownerBusinessDetailQueryKey(activeBusinessId), data)
      if (isCurrent()) setBusiness(data)
    } catch (loadError) {
      if (isCurrent()) setError(loadError)
    } finally {
      if (isCurrent()) setIsLoading(false)
    }
  }, [activeBusinessId, queryClient])

  useEffect(() => {
    if (!validRouteId) return undefined
    if (routeBusinessId !== activeBusinessId) {
      if (businessesQuery.isSuccess && !businesses.some((item) => String(item.negocioId) === routeBusinessId)) {
        setError(new Error('invalid id'))
        setIsLoading(false)
        return undefined
      }
      setIsLoading(true)
      setActiveBusinessId(routeBusinessId)
      return undefined
    }
    let active = true
    Promise.resolve().then(async () => {
      if (!active) return
      await loadBusiness(() => active)
    })
    return () => { active = false }
  }, [activeBusinessId, businesses, businessesQuery.isSuccess, loadBusiness, routeBusinessId, setActiveBusinessId, validRouteId])

  const fields = business ? [
    ['ID', business.negocioId],
    ['Nombre', business.nombre],
    ['Descripción', business.descripcion],
    ['Teléfono', business.telefono],
    ['Email', business.email],
    ['Estado', business.status],
  ] : []

  return (
    <>
      <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
      <div className="user-detail-heading">
        <div><h1>{business?.nombre || 'Espacio del negocio'}</h1><p className="owners-description">Información básica de tu negocio.</p></div>
        <div className="user-detail-heading-actions"><Link className="owners-secondary-button user-back-button" to="/owner"><ArrowLeft size={15} /> Volver a negocios</Link></div>
      </div>
      {!validRouteId || error?.message === 'invalid id' ? (
        <div className="owner-business-state user-detail-error" role="alert"><p>El identificador del negocio no es válido.</p></div>
      ) : isLoading || !validId || String(business?.negocioId) !== activeBusinessId ? (
        <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocio…</div>
      ) : error ? (
        <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessErrorMessage(error)}</p>{error.response?.status !== 404 && error.response?.status !== 400 && error.response?.status !== 401 && error.response?.status !== 403 && <button type="button" className="owners-secondary-button" onClick={loadBusiness}><RefreshCw size={15} /> Reintentar</button>}</div>
      ) : business ? (
        <>
          {showUpdatedNotice && <div className="owner-created-notice" role="status"><CheckCircle2 size={17} /><span>Los datos de «{business.nombre}» se actualizaron correctamente.</span><button type="button" className="icon-button" onClick={() => setShowUpdatedNotice(false)} aria-label="Cerrar confirmación"><X size={16} /></button></div>}
          {isEditing ? (
            <EditOwnerBusinessForm business={business} onCancel={() => setIsEditing(false)} onUpdated={(updatedBusiness) => { setBusiness(updatedBusiness); setIsEditing(false); setShowUpdatedNotice(true) }} />
          ) : (
            <div className="owner-business-detail-sections">
              <AccordionPanel title="Datos del negocio">
              <section className="user-detail-card" aria-label="Datos del negocio">
                <div className="user-detail-card-heading"><div className="user-detail-card-heading-copy"><span className="user-detail-icon"><Building2 size={18} /></span><div><h2>{business.nombre || 'Negocio'}</h2><p>{business.email || 'Sin email'}</p></div></div>{!isEditing && <button type="button" className="owner-create-button" onClick={() => { setShowUpdatedNotice(false); setIsEditing(true) }}><Pencil size={15} /> Editar negocio</button>}</div>
                <dl className="user-detail-grid">{fields.map(([label, value]) => <div className="user-detail-field" key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}</dl>
              </section>
              </AccordionPanel>
              <AccordionPanel title="Dirección">
              <OwnerBusinessAddressSection negocioId={activeBusinessId} />
              </AccordionPanel>
              <AccordionPanel title="Horarios de atención">
              <OwnerBusinessHoursSection negocioId={activeBusinessId} />
              </AccordionPanel>
              <AccordionPanel title="Servicios">
              <OwnerBusinessServicesSection key={`services-${activeBusinessId}`} negocioId={activeBusinessId} />
              </AccordionPanel>
              <AccordionPanel title="Empleados">
              <OwnerBusinessEmployeesSection key={`employees-${activeBusinessId}`} negocioId={activeBusinessId} />
              </AccordionPanel>
            </div>
          )}
        </>
      ) : (
        <div className="owner-business-state"><p>No hay datos disponibles para este negocio.</p></div>
      )}
    </>
  )
}

function AccordionPanel({ title, children }) {
  const [isOpen, setIsOpen] = useState(false)
  const panelId = `owner-detail-${title.toLowerCase().replaceAll(' ', '-')}`

  return <section className="owner-detail-accordion">
    <button type="button" className="owner-detail-accordion-toggle" aria-expanded={isOpen} aria-controls={panelId} onClick={() => setIsOpen((open) => !open)}>
      <span>{title}</span><ChevronDown size={17} aria-hidden="true" />
    </button>
    <div id={panelId} className="owner-detail-accordion-content" hidden={!isOpen}>{children}</div>
  </section>
}
