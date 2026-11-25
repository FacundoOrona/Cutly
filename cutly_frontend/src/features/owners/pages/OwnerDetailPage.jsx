import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Building2, LoaderCircle, RefreshCw, UserRound } from 'lucide-react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { fetchOwnerById, getOwnerDetailErrorMessage } from '../api/ownersApi.js'
import './OwnerDetailPage.css'
import OwnerSubscriptionManagement from '../components/OwnerSubscriptionManagement.jsx'

const ownerStatusLabels = { ACTIVE: 'Activo', INACTIVE: 'Inactivo', BLOCKED: 'Bloqueado' }
const businessStatusLabels = { ACTIVE: 'Activo', INACTIVE: 'Inactivo', SUSPENDED: 'Suspendido' }

function formatDate(value) {
  if (!value) return '—'
  const [year, month, day] = value.split('-')
  return `${day}/${month}/${year}`
}

export default function OwnerDetailPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const validId = typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))
  const query = useQuery({
    queryKey: ['admin', 'owner', id],
    queryFn: () => fetchOwnerById(id),
    enabled: validId,
  })
  const owner = query.data

  function backToList() {
    navigate('/admin/owners', { state: location.state?.ownersListState ? { ownersListState: location.state.ownersListState } : null })
  }

  return <>
    <span className="eyebrow"><span className="eyebrow-dot" /> ADMINISTRACIÓN</span>
    <div className="user-detail-heading">
      <div><h1>Detalle de owner</h1><p className="owners-description">Cuenta y negocios asociados.</p></div>
      <button type="button" className="owners-secondary-button user-back-button" onClick={backToList}><ArrowLeft size={15} /> Volver al listado</button>
    </div>
    {!validId ? <div className="user-detail-state" role="alert"><UserRound size={21} /><p>El identificador del owner no es válido.</p></div>
      : query.isPending ? <div className="user-detail-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando owner…</div>
        : query.isError ? <div className="user-detail-state user-detail-error" role="alert"><p>{getOwnerDetailErrorMessage(query.error)}</p>{![400, 401, 403, 404].includes(query.error.response?.status) && <button type="button" className="owners-secondary-button" onClick={() => query.refetch()}><RefreshCw size={15} /> Reintentar</button>}</div>
          : owner && <>
            <section className="user-detail-card" aria-label="Datos del owner">
              <div className="user-detail-card-heading"><span className="user-detail-icon"><UserRound size={18} /></span><div className="owner-detail-identity"><h2>{[owner.nombre, owner.apellido].filter(Boolean).join(' ') || 'Owner'}</h2><p>{owner.email || 'Sin email'}</p></div><span className={`owner-status owner-status-${String(owner.status).toLowerCase()}`}>{ownerStatusLabels[owner.status] || owner.status}</span></div>
              <dl className="user-detail-grid"><div className="user-detail-field"><dt>ID</dt><dd>{owner.ownerId}</dd></div><div className="user-detail-field"><dt>DNI</dt><dd>{owner.dni || '—'}</dd></div><div className="user-detail-field"><dt>Negocios asociados</dt><dd>{owner.cantidadNegocios}</dd></div></dl>
            </section>
            <OwnerSubscriptionManagement owner={owner} />
            <section className="owner-detail-businesses" aria-labelledby="owner-detail-businesses-title">
              <header><div><h2 id="owner-detail-businesses-title">Negocios</h2><p>Recursos operativos administrados por este OWNER.</p></div><span>{owner.cantidadNegocios}</span></header>
              {owner.negocios.length === 0 ? <div className="user-detail-state">Este owner todavía no tiene negocios asociados.</div> : <div className="owner-detail-business-list">
                {owner.negocios.map((business) => <article className="owner-detail-business" key={business.negocioId}>
                  <div className="owner-detail-business-heading"><div className="owner-detail-business-name"><span className="user-detail-icon"><Building2 size={17} /></span><div><h3>{business.nombre}</h3><p>{business.email || 'Sin email'}</p></div></div><span className={`owner-status owner-status-${String(business.status).toLowerCase()}`}>{businessStatusLabels[business.status] || business.status || 'Sin estado'}</span></div>
                </article>)}
              </div>}
            </section>
          </>}
  </>
}
