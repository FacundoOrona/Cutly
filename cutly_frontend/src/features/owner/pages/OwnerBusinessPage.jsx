import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Building2, CheckCircle2, LoaderCircle, Pencil, RefreshCw, X } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { getOwnerBusiness, getOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'
import EditOwnerBusinessForm from '../components/EditOwnerBusinessForm.jsx'
import { useQueryClient } from '@tanstack/react-query'
import { ownerBusinessDetailQueryKey } from '../hooks/useOwnerBusinesses.js'
import OwnerBusinessAddressSection from '../components/OwnerBusinessAddressSection.jsx'
import OwnerBusinessHoursSection from '../components/OwnerBusinessHoursSection.jsx'
import OwnerBusinessServicesSection from '../components/OwnerBusinessServicesSection.jsx'

export default function OwnerBusinessPage() {
  const { negocioId } = useParams()
  const validId = typeof negocioId === 'string' && /^[1-9]\d*$/.test(negocioId) && Number.isSafeInteger(Number(negocioId))
  const [business, setBusiness] = useState(null)
  const [isLoading, setIsLoading] = useState(validId)
  const [error, setError] = useState(validId ? null : new Error('invalid id'))
  const [isEditing, setIsEditing] = useState(false)
  const [showUpdatedNotice, setShowUpdatedNotice] = useState(false)
  const queryClient = useQueryClient()

  const loadBusiness = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await getOwnerBusiness(negocioId)
      setBusiness(data)
      queryClient.setQueryData(ownerBusinessDetailQueryKey(negocioId), data)
    } catch (loadError) {
      setError(loadError)
    } finally {
      setIsLoading(false)
    }
  }, [negocioId, queryClient])

  useEffect(() => {
    if (!validId) return undefined
    let active = true
    Promise.resolve().then(async () => {
      if (!active) return
      await loadBusiness()
    })
    return () => { active = false }
  }, [loadBusiness, validId])

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
        <div className="user-detail-heading-actions"><Link className="owners-secondary-button user-back-button" to="/owner"><ArrowLeft size={15} /> Volver a negocios</Link>{business && !isEditing && <button type="button" className="owner-create-button" onClick={() => { setShowUpdatedNotice(false); setIsEditing(true) }}><Pencil size={15} /> Editar negocio</button>}</div>
      </div>
      {!validId || error?.message === 'invalid id' ? (
        <div className="owner-business-state user-detail-error" role="alert"><p>El identificador del negocio no es válido.</p></div>
      ) : isLoading ? (
        <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocio…</div>
      ) : error ? (
        <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessErrorMessage(error)}</p>{error.response?.status !== 404 && error.response?.status !== 400 && error.response?.status !== 401 && error.response?.status !== 403 && <button type="button" className="owners-secondary-button" onClick={loadBusiness}><RefreshCw size={15} /> Reintentar</button>}</div>
      ) : business ? (
        <>
          {showUpdatedNotice && <div className="owner-created-notice" role="status"><CheckCircle2 size={17} /><span>Los datos de «{business.nombre}» se actualizaron correctamente.</span><button type="button" className="icon-button" onClick={() => setShowUpdatedNotice(false)} aria-label="Cerrar confirmación"><X size={16} /></button></div>}
          {isEditing ? (
            <EditOwnerBusinessForm business={business} onCancel={() => setIsEditing(false)} onUpdated={(updatedBusiness) => { setBusiness(updatedBusiness); setIsEditing(false); setShowUpdatedNotice(true) }} />
          ) : (
            <section className="user-detail-card" aria-label="Datos del negocio">
              <div className="user-detail-card-heading"><span className="user-detail-icon"><Building2 size={18} /></span><div><h2>{business.nombre || 'Negocio'}</h2><p>{business.email || 'Sin email'}</p></div></div>
              <dl className="user-detail-grid">{fields.map(([label, value]) => <div className="user-detail-field" key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}</dl>
            </section>
          )}
          <OwnerBusinessAddressSection negocioId={negocioId} />
          <OwnerBusinessHoursSection negocioId={negocioId} />
          <OwnerBusinessServicesSection key={negocioId} negocioId={negocioId} />
        </>
      ) : (
        <div className="owner-business-state"><p>No hay datos disponibles para este negocio.</p></div>
      )}
    </>
  )
}
