import { useState } from 'react'
import { ArrowLeft, Building2, CheckCircle2, LoaderCircle, RefreshCw, X } from 'lucide-react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import BusinessStatusActions from '../components/BusinessStatusActions.jsx'
import { getBusinessErrorMessage, getUpdateBusinessStatusErrorMessage } from '../api/businessesApi.js'
import { useBusiness, useUpdateBusinessStatus } from '../hooks/useBusinesses.js'

const statusLabels = { ACTIVE: 'Activo', INACTIVE: 'Inactivo', SUSPENDED: 'Suspendido' }

export default function BusinessDetailPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const validId = typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))
  const query = useBusiness(id, validId)
  const statusMutation = useUpdateBusinessStatus()
  const [statusMessage, setStatusMessage] = useState(null)
  const returnState = location.state?.businessesListState

  function backToList() {
    navigate('/admin/businesses', { state: returnState ? { businessesListState: returnState } : null })
  }

  async function handleStatusChange(business, action) {
    const actionLabels = { suspend: 'suspender', deactivate: 'desactivar', reactivate: 'reactivar' }
    if (action !== 'reactivate' && !window.confirm(`¿Querés ${actionLabels[action]} el negocio "${business.nombre}"?`)) return
    setStatusMessage(null)
    try {
      await statusMutation.mutateAsync({ id: business.negocioId, action })
      setStatusMessage({ type: 'success', text: `El negocio "${business.nombre}" se ${actionLabels[action]} correctamente.` })
    } catch (error) {
      const retry = !error.response || error.response.status >= 500
      setStatusMessage({ type: 'error', text: getUpdateBusinessStatusErrorMessage(error), retry: retry ? { business, action } : null })
    }
  }

  const business = query.data
  const fields = business ? [
    ['ID', business.negocioId],
    ['Nombre', business.nombre],
    ['Owner', [business.ownerNombre, business.ownerApellido].filter(Boolean).join(' ')],
    ['Email del owner', business.ownerEmail],
    ['Descripción', business.descripcion],
    ['Teléfono', business.telefono],
    ['Email', business.email],
    ['Estado', statusLabels[business.status] || business.status || 'Sin estado'],
  ] : []

  return (
    <>
      <span className="eyebrow"><span className="eyebrow-dot" /> ADMINISTRACIÓN</span>
      <div className="user-detail-heading">
        <div><h1>Detalle de negocio</h1><p className="owners-description">Información básica del negocio registrado.</p></div>
        <button type="button" className="owners-secondary-button user-back-button" onClick={backToList}><ArrowLeft size={15} /> Volver al listado</button>
      </div>
      {statusMessage && <div className={`owner-created-notice${statusMessage.type === 'error' ? ' owner-status-error' : ''}`} role={statusMessage.type === 'error' ? 'alert' : 'status'}>{statusMessage.type === 'success' && <CheckCircle2 size={17} />}{statusMessage.text}{statusMessage.retry && <button type="button" className="owners-secondary-button businesses-retry-button" onClick={() => handleStatusChange(statusMessage.retry.business, statusMessage.retry.action)}>Reintentar</button>}<button type="button" className="icon-button" onClick={() => setStatusMessage(null)} aria-label="Cerrar mensaje"><X size={16} /></button></div>}
      {!validId ? (
        <div className="user-detail-state" role="alert"><Building2 size={21} /><p>El identificador del negocio no es válido.</p></div>
      ) : query.isPending ? (
        <div className="user-detail-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocio…</div>
      ) : query.isError ? (
        <div className="user-detail-state user-detail-error" role="alert"><p>{getBusinessErrorMessage(query.error)}</p>{query.error.response?.status !== 404 && query.error.response?.status !== 400 && query.error.response?.status !== 401 && query.error.response?.status !== 403 && <button type="button" className="owners-secondary-button" onClick={() => query.refetch()}><RefreshCw size={15} /> Reintentar</button>}</div>
      ) : business ? (
        <section className="user-detail-card" aria-label="Datos del negocio">
          <div className="user-detail-card-heading"><span className="user-detail-icon"><Building2 size={18} /></span><div><h2>{business.nombre || 'Negocio'}</h2><p>{business.email || 'Sin email'}</p></div></div>
          <dl className="user-detail-grid">{fields.map(([label, value]) => <div className="user-detail-field" key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}</dl>
          <div className="business-detail-actions"><span className="user-detail-field-label">Acciones de estado</span><BusinessStatusActions business={business} pending={statusMutation.isPending} onChangeStatus={handleStatusChange} /></div>
        </section>
      ) : (
        <div className="user-detail-state"><p>No hay datos disponibles para este negocio.</p><button type="button" className="owners-secondary-button" onClick={backToList}>Volver al listado</button></div>
      )}
    </>
  )
}
