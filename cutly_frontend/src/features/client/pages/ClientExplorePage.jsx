import { useEffect, useMemo, useState } from 'react'
import { AlertCircle, LoaderCircle, Search, X } from 'lucide-react'
import { useLocation } from 'react-router-dom'
import ClientBusinessCard from '../components/ClientBusinessCard.jsx'
import { getClientBusinessesErrorMessage } from '../api/clientApi.js'
import useClientBusinesses from '../hooks/useClientBusinesses.js'
import './ClientExplorePage.css'

const PAGE_SIZE = 20
const DEBOUNCE_MS = 350
const SORT = 'nombre,asc'

function BusinessSkeleton() {
  return <div className="client-business-skeleton" aria-hidden="true"><span /><div><i /><i /><i /></div></div>
}

export default function ClientExplorePage() {
  const location = useLocation()
  const restoredState = location.state?.catalogState
  const [searchInput, setSearchInput] = useState(() => restoredState?.searchInput || '')
  const [search, setSearch] = useState(() => restoredState?.search || '')
  const [page, setPage] = useState(() => restoredState?.page || 0)

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      const nextSearch = searchInput.trim()
      setPage(0)
      setSearch((current) => current === nextSearch ? current : nextSearch)
    }, DEBOUNCE_MS)
    return () => window.clearTimeout(timeout)
  }, [searchInput])

  const params = useMemo(() => ({ nombre: search, page, size: PAGE_SIZE, sort: SORT }), [search, page])
  const query = useClientBusinesses(params)
  const data = query.data
  const businesses = data?.content || []
  const pageNumber = data?.number ?? page
  const totalPages = data?.totalPages ?? 0
  const first = data?.first ?? pageNumber === 0
  const last = data?.last ?? true
  const showing = data?.numberOfElements ?? businesses.length
  const total = data?.totalElements
  const start = total && showing ? pageNumber * (data?.size || PAGE_SIZE) + 1 : 0

  return (
    <section className="client-explore-page">
      <header className="client-explore-heading">
        <span className="client-explore-eyebrow">DESCUBRÍ CUTLY</span>
        <h1>Explorá negocios</h1>
        <p>Encontrá negocios y descubrí los servicios disponibles cerca tuyo.</p>
      </header>

      <div className="client-explore-search-wrap">
        <Search size={19} aria-hidden="true" />
        <input
          type="search"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
          placeholder="Buscar por nombre"
          aria-label="Buscar negocios por nombre"
        />
        {searchInput && <button type="button" onClick={() => setSearchInput('')} aria-label="Limpiar búsqueda"><X size={17} /></button>}
      </div>

      {query.isPending ? (
        <div className="client-business-grid" aria-label="Cargando negocios"><BusinessSkeleton /><BusinessSkeleton /><BusinessSkeleton /><BusinessSkeleton /></div>
      ) : query.isError ? (
        <div className="client-explore-message client-explore-error" role="alert">
          <AlertCircle size={22} aria-hidden="true" />
          <p>{getClientBusinessesErrorMessage(query.error)}</p>
          <button type="button" onClick={() => query.refetch()} disabled={query.isFetching}>Reintentar</button>
        </div>
      ) : businesses.length === 0 ? (
        <div className="client-explore-message client-explore-empty">
          <span className="client-explore-empty-icon"><Search size={21} aria-hidden="true" /></span>
          <h2>{search ? 'No encontramos negocios con ese nombre' : 'Todavía no hay negocios disponibles'}</h2>
          <p>{search ? 'Probá con otro nombre para ampliar la búsqueda.' : 'Volvé a consultar más tarde para descubrir nuevos negocios.'}</p>
        </div>
      ) : (
        <>
          <div className="client-explore-results-heading" aria-live="polite" aria-busy={query.isFetching}>
            <h2>{search ? 'Resultados de búsqueda' : 'Negocios disponibles'}</h2>
            <span>{start ? `Mostrando ${start}–${start + showing - 1} de ${total} negocios` : `${total} negocios`}</span>
          </div>
          <div className={`client-business-grid${query.isFetching ? ' client-business-grid-loading' : ''}`} aria-busy={query.isFetching}>
            {businesses.map((business) => <ClientBusinessCard key={business.negocioId} business={business} catalogState={{ searchInput, search, page }} />)}
          </div>
          <nav className="client-explore-pagination" aria-label="Paginación de negocios">
            <button type="button" onClick={() => setPage(Math.max(0, pageNumber - 1))} disabled={first || query.isFetching}>Anterior</button>
            <span>{query.isFetching && query.isPlaceholderData ? 'Cargando página…' : `Página ${pageNumber + 1}${totalPages ? ` de ${totalPages}` : ''}`}</span>
            <button type="button" onClick={() => setPage(pageNumber + 1)} disabled={last || query.isFetching}>Siguiente</button>
          </nav>
        </>
      )}
      {query.isFetching && data && <span className="client-explore-fetching"><LoaderCircle size={14} aria-hidden="true" /> Actualizando resultados</span>}
    </section>
  )
}
