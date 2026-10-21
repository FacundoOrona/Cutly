import { useCallback, useEffect, useState } from 'react'
import { ArrowRight, Building2, LoaderCircle, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getOwnerBusinesses, getOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'

const statusLabels = { ACTIVE: 'Activo', INACTIVE: 'Inactivo', SUSPENDED: 'Suspendido' }

export default function OwnerBusinessesPage() {
  const [businesses, setBusinesses] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadBusinesses = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await getOwnerBusinesses()
      if (!Array.isArray(data)) throw new Error('La respuesta del servidor no contiene una lista de negocios.')
      setBusinesses(data)
    } catch (loadError) {
      setError(loadError)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    Promise.resolve().then(async () => {
      if (!active) return
      await loadBusinesses()
    })
    return () => { active = false }
  }, [loadBusinesses])

  return (
    <>
      <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
      <div className="owners-heading"><div><h1>Tus negocios</h1><p className="owners-description">Elegí un negocio para entrar a su espacio.</p></div></div>
      {isLoading ? (
        <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocios…</div>
      ) : error ? (
        <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessErrorMessage(error)}</p><button type="button" className="owners-secondary-button" onClick={loadBusinesses}><RefreshCw size={15} /> Reintentar</button></div>
      ) : businesses.length === 0 ? (
        <div className="owner-business-state"><Building2 size={22} /><p>Todavía no tenés negocios registrados.</p></div>
      ) : (
        <section className="owner-business-list" aria-label="Tus negocios">
          {businesses.map((business) => (
            <Link className="owner-business-card" key={business.negocioId} to={`/owner/negocios/${business.negocioId}`}>
              <span className="user-detail-icon"><Building2 size={18} /></span>
              <span className="owner-business-card-main"><strong>{business.nombre || 'Negocio'}</strong><small>{business.descripcion || 'Sin descripción'}</small></span>
              <span className={`owner-status owner-status-${String(business.status || '').toLowerCase()}`}>{statusLabels[business.status] || business.status || 'Sin estado'}</span>
              <ArrowRight className="owner-business-arrow" size={17} />
            </Link>
          ))}
        </section>
      )}
    </>
  )
}
