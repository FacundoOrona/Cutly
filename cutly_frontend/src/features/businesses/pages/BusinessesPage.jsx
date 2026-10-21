import { useMemo, useState } from 'react'
import { CheckCircle2, LoaderCircle, RefreshCw, Search, Store, X } from 'lucide-react'
import BusinessesTable from '../components/BusinessesTable.jsx'
import { getBusinessesErrorMessage, getUpdateBusinessStatusErrorMessage } from '../api/businessesApi.js'
import useBusinesses, { useUpdateBusinessStatus } from '../hooks/useBusinesses.js'

const statusOptions = [
  ['', 'Todos los estados'],
  ['ACTIVE', 'Activos'],
  ['INACTIVE', 'Inactivos'],
  ['SUSPENDED', 'Suspendidos'],
]
const EMPTY_BUSINESSES = []

export default function BusinessesPage() {
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [statusMessage, setStatusMessage] = useState(null)
  const query = useBusinesses()
  const statusMutation = useUpdateBusinessStatus()
  const businesses = query.data ?? EMPTY_BUSINESSES
  const filteredBusinesses = useMemo(() => {
    const term = search.trim().toLocaleLowerCase()
    return businesses.filter((business) => {
      const matchesSearch = !term || [business.nombre, business.email]
        .some((value) => value?.toLocaleLowerCase().includes(term))
      return matchesSearch && (!status || business.status === status)
    })
  }, [businesses, search, status])

  async function handleBusinessStatusChange(business, action) {
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

  return (
    <>
      <span className="eyebrow"><span className="eyebrow-dot" /> ADMINISTRACIÓN</span>
      <div className="owners-heading">
        <div><h1>Negocios</h1><p className="owners-description">Consultá y buscá los negocios registrados en Cutly.</p></div>
        <div className="owners-count"><Store size={17} />{query.data ? `${businesses.length} ${businesses.length === 1 ? 'negocio' : 'negocios'}` : 'Listado'}</div>
      </div>
      <section className="owners-filters businesses-filters" aria-label="Filtros de negocios">
        <label className="owners-search" htmlFor="business-search"><span>Buscar por nombre o email</span><div className="owners-filter-input"><Search size={16} aria-hidden="true" /><input id="business-search" type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Nombre o email" /></div></label>
        <label className="owners-status-filter" htmlFor="business-status"><span>Estado</span><select id="business-status" value={status} onChange={(event) => setStatus(event.target.value)}>{statusOptions.map(([value, label]) => <option key={value || 'all'} value={value}>{label}</option>)}</select></label>
      </section>
      {statusMessage && <div className={`owner-created-notice${statusMessage.type === 'error' ? ' owner-status-error' : ''}`} role={statusMessage.type === 'error' ? 'alert' : 'status'}>{statusMessage.type === 'success' && <CheckCircle2 size={17} />}{statusMessage.text}{statusMessage.retry && <button type="button" className="owners-secondary-button businesses-retry-button" onClick={() => handleBusinessStatusChange(statusMessage.retry.business, statusMessage.retry.action)}>Reintentar</button>}<button type="button" className="icon-button" onClick={() => setStatusMessage(null)} aria-label="Cerrar mensaje"><X size={16} /></button></div>}
      <section className="owners-results" aria-label="Listado de negocios" aria-busy={query.isFetching}>
        {query.isError && !query.data ? (
          <div className="owners-message owners-error" role="alert"><p>{getBusinessesErrorMessage(query.error)}</p><button type="button" className="owners-secondary-button" onClick={() => query.refetch()}><RefreshCw size={15} /> Reintentar</button></div>
        ) : !query.data ? (
          <div className="owners-message" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocios…</div>
        ) : (
          <>
            {query.isError && <p className="owners-inline-error" role="alert">No se pudo actualizar el listado. {getBusinessesErrorMessage(query.error)}</p>}
            {query.isFetching && <p className="owners-updating" role="status"><LoaderCircle size={15} className="spin" /> Actualizando listado…</p>}
            {filteredBusinesses.length ? <BusinessesTable businesses={filteredBusinesses} pendingBusinessId={statusMutation.isPending ? statusMutation.variables?.id : null} onChangeStatus={handleBusinessStatusChange} /> : <div className="owners-message"><Store size={20} /><p>{businesses.length ? 'No hay negocios que coincidan con estos filtros.' : 'Todavía no hay negocios registrados.'}</p></div>}
          </>
        )}
      </section>
    </>
  )
}
