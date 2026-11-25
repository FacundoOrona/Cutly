import { useState } from 'react'
import { ArrowRight, Building2, CheckCircle2, LoaderCircle, Plus, RefreshCw, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'
import CreateOwnerBusinessForm from '../components/CreateOwnerBusinessForm.jsx'
import { useActiveOwnerBusiness } from '../context/OwnerBusinessContext.jsx'
import OwnerOnboardingPage from './OwnerOnboardingPage.jsx'

const statusLabels = { ACTIVE: 'Activo', INACTIVE: 'Inactivo', SUSPENDED: 'Suspendido' }

export default function OwnerBusinessesPage() {
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [createdBusiness, setCreatedBusiness] = useState(null)
  const { businesses, businessesQuery, subscription, subscriptionQuery, isOnboarding, trialActivationPending } = useActiveOwnerBusiness()
  if (isOnboarding || trialActivationPending) return <OwnerOnboardingPage />
  const puedeCrear = subscription?.suscripcionId != null
    && ['ACTIVE', 'TRIAL'].includes(subscription.status)
    && subscription.capacidadDisponible > 0

  async function handleBusinessCreated(business) {
    setCreatedBusiness(business)
    setIsCreateOpen(false)
    await businessesQuery.refetch()
  }

  return (
    <>
      <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
      <div className="owners-heading"><div><h1>Tus negocios</h1><p className="owners-description">Elegí un negocio para entrar a su espacio.</p></div><button type="button" className="owner-create-button" disabled={subscriptionQuery.isLoading || !puedeCrear} onClick={() => { setCreatedBusiness(null); setIsCreateOpen((open) => !open) }}><Plus size={15} /> Crear negocio</button></div>
      {createdBusiness && <div className="owner-created-notice" role="status"><CheckCircle2 size={17} /><span>Se creó «{createdBusiness.nombre}» correctamente.</span><Link className="owners-secondary-button" to={`/owner/negocios/${createdBusiness.negocioId}`}>Ir al negocio <ArrowRight size={14} /></Link><button type="button" className="icon-button" onClick={() => setCreatedBusiness(null)} aria-label="Cerrar confirmación"><X size={16} /></button></div>}
      {isCreateOpen && <CreateOwnerBusinessForm onCancel={() => setIsCreateOpen(false)} onCreated={handleBusinessCreated} />}
      {businessesQuery.isLoading ? (
        <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocios…</div>
      ) : businessesQuery.isError ? (
        <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessErrorMessage(businessesQuery.error)}</p><button type="button" className="owners-secondary-button" onClick={() => void businessesQuery.refetch()} disabled={businessesQuery.isFetching}><RefreshCw size={15} /> Reintentar</button></div>
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
