import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Building2, LoaderCircle, RefreshCw } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { getOwnerBusiness, getOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'

export default function OwnerBusinessPage() {
  const { negocioId } = useParams()
  const validId = typeof negocioId === 'string' && /^[1-9]\d*$/.test(negocioId) && Number.isSafeInteger(Number(negocioId))
  const [business, setBusiness] = useState(null)
  const [isLoading, setIsLoading] = useState(validId)
  const [error, setError] = useState(validId ? null : new Error('invalid id'))

  const loadBusiness = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      setBusiness(await getOwnerBusiness(negocioId))
    } catch (loadError) {
      setError(loadError)
    } finally {
      setIsLoading(false)
    }
  }, [negocioId])

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
        <Link className="owners-secondary-button user-back-button" to="/owner"><ArrowLeft size={15} /> Volver a negocios</Link>
      </div>
      {!validId || error?.message === 'invalid id' ? (
        <div className="owner-business-state user-detail-error" role="alert"><p>El identificador del negocio no es válido.</p></div>
      ) : isLoading ? (
        <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocio…</div>
      ) : error ? (
        <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessErrorMessage(error)}</p>{error.response?.status !== 404 && error.response?.status !== 400 && error.response?.status !== 401 && error.response?.status !== 403 && <button type="button" className="owners-secondary-button" onClick={loadBusiness}><RefreshCw size={15} /> Reintentar</button>}</div>
      ) : business ? (
        <section className="user-detail-card" aria-label="Datos del negocio">
          <div className="user-detail-card-heading"><span className="user-detail-icon"><Building2 size={18} /></span><div><h2>{business.nombre || 'Negocio'}</h2><p>{business.email || 'Sin email'}</p></div></div>
          <dl className="user-detail-grid">{fields.map(([label, value]) => <div className="user-detail-field" key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}</dl>
        </section>
      ) : (
        <div className="owner-business-state"><p>No hay datos disponibles para este negocio.</p></div>
      )}
    </>
  )
}
