import { useEffect, useMemo, useState } from 'react'
import { Building2, CheckCircle2, CircleAlert, LoaderCircle, RefreshCw, Search, UsersRound } from 'lucide-react'
import { getOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'
import { getOwnerClientesLocalesErrorMessage } from '../api/ownerTurnosApi.js'
import OwnerClienteLocalForm from '../components/OwnerClienteLocalForm.jsx'
import { useOwnerBusinesses } from '../hooks/useOwnerBusinesses.js'
import { useOwnerClientesLocalesPage } from '../hooks/useOwnerTurnos.js'

const clientLabel = (client) => [client.nombre, client.apellido].filter(Boolean).join(' ')

export default function OwnerClientesPage() {
  const businessesQuery = useOwnerBusinesses()
  const businesses = useMemo(() => Array.isArray(businessesQuery.data) ? businessesQuery.data : [], [businessesQuery.data])
  const [selectedBusinessId, setSelectedBusinessId] = useState('')
  const [searchText, setSearchText] = useState('')
  const [debouncedText, setDebouncedText] = useState('')
  const [page, setPage] = useState(0)
  const [createdClient, setCreatedClient] = useState(null)
  const [createdClientMatchedPreviousSearch, setCreatedClientMatchedPreviousSearch] = useState(true)

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedText(searchText.trim()), 300)
    return () => window.clearTimeout(timer)
  }, [searchText])

  const selectedBusiness = businesses.find((business) => String(business.negocioId) === selectedBusinessId)
  const validBusinessId = selectedBusiness
    ? selectedBusinessId
    : businesses.length === 1 ? String(businesses[0].negocioId) : ''
  const searchReady = debouncedText.trim().length >= 2
  const isWaitingForDebounce = searchText.trim().length >= 2 && debouncedText !== searchText.trim()
  const effectiveQuery = searchReady ? debouncedText : ''
  const clientsQuery = useOwnerClientesLocalesPage(validBusinessId, page, 10, effectiveQuery, { enabled: !isWaitingForDebounce })
  const result = !isWaitingForDebounce && !clientsQuery.isFetching ? clientsQuery.data : null
  const clients = Array.isArray(result?.content) ? result.content : []
  const totalElements = result?.totalElements
  const totalPages = result?.totalPages

  function changeBusiness(event) {
    setSelectedBusinessId(event.target.value)
    setPage(0)
    setSearchText('')
    setDebouncedText('')
    setCreatedClient(null)
  }

  function handleClientCreated(client) {
    const currentSearch = searchText.trim()
    const fullName = [client.nombre, client.apellido].filter(Boolean).join(' ')
    const matchesCurrentSearch = currentSearch && fullName.toLocaleLowerCase().includes(currentSearch.toLocaleLowerCase())
    setCreatedClient(client)
    setCreatedClientMatchedPreviousSearch(Boolean(matchesCurrentSearch))
    setSearchText(fullName)
    setDebouncedText(fullName)
    setPage(0)
  }

  return <>
    <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
    <div className="owners-heading"><div><h1>Clientes</h1><p className="owners-description">Buscá y registrá clientes locales de tus negocios.</p></div></div>

    {businessesQuery.isLoading ? (
      <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocios…</div>
    ) : businessesQuery.isError ? (
      <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessErrorMessage(businessesQuery.error)}</p><button type="button" className="owners-secondary-button" onClick={() => void businessesQuery.refetch()} disabled={businessesQuery.isFetching}><RefreshCw size={15} /> Reintentar</button></div>
    ) : businesses.length === 0 ? (
      <div className="owner-business-state"><Building2 size={22} /><p>Todavía no tenés negocios registrados.</p></div>
    ) : <>
      <section className="owner-clients-business-panel" aria-label="Negocio seleccionado">
        {businesses.length === 1 ? <div className="owner-clients-selected-business"><Building2 size={17} /><span><small>Negocio</small><strong>{businesses[0].nombre || 'Negocio'}</strong></span></div> : <label className="owner-form-field" htmlFor="owner-clients-business"><span>Negocio</span><select id="owner-clients-business" value={validBusinessId} onChange={changeBusiness}><option value="">Seleccioná un negocio…</option>{businesses.map((business) => <option key={business.negocioId} value={String(business.negocioId)}>{business.nombre || `Negocio ${business.negocioId}`}</option>)}</select></label>}
      </section>

      {validBusinessId ? <>
        <section className="owner-client-card">
          <div className="owner-client-card-heading"><span className="user-detail-icon"><Search size={17} /></span><div><h2>Buscar clientes</h2><p>La búsqueda considera nombre y apellido, con dos caracteres como mínimo.</p></div></div>
          <label className="owner-form-field" htmlFor="owner-clients-search"><span>Nombre o apellido</span><input id="owner-clients-search" type="search" value={searchText} maxLength={100} onChange={(event) => { const value = event.target.value; setSearchText(value); setCreatedClient(null); if (value.trim().length < 2 || value.trim() !== searchText.trim()) setPage(0) }} placeholder="Escribí un nombre o apellido" /></label>
          <p className="owner-clients-limit-note">Mostramos 10 clientes por página. La búsqueda es opcional, considera nombre y apellido, y no admite DNI.</p>
          {clientsQuery.isError ? <div className="owner-clients-error" role="alert"><CircleAlert size={17} /><span>{getOwnerClientesLocalesErrorMessage(clientsQuery.error)}</span><button type="button" className="owners-secondary-button" onClick={() => void clientsQuery.refetch()} disabled={clientsQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div>
            : isWaitingForDebounce || clientsQuery.isFetching ? <p className="owner-clients-search-state" role="status"><LoaderCircle size={15} className="spin" /> Cargando clientes…</p>
                : clients.length === 0 ? <p className="owner-clients-search-state">{effectiveQuery ? 'No se encontraron coincidencias.' : 'Todavía no hay clientes registrados en este negocio.'}</p>
                  : <div className="owner-clients-table-wrap"><table className="owner-clients-table"><caption>{typeof totalElements === 'number' ? `${totalElements} clientes · Página ${page + 1}${typeof totalPages === 'number' ? ` de ${totalPages}` : ''}` : `Clientes · Página ${page + 1}`}</caption><thead><tr><th>Nombre</th><th>Apellido</th><th>DNI</th><th>Teléfono</th></tr></thead><tbody>{clients.map((client) => <tr key={client.clienteLocalId}><td data-label="Nombre">{client.nombre}</td><td data-label="Apellido">{client.apellido}</td><td data-label="DNI">{client.dniPresentado}</td><td data-label="Teléfono">{client.telefono}</td></tr>)}</tbody></table><nav className="owner-clients-pagination" aria-label="Paginación de clientes"><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={page === 0 || clientsQuery.isFetching}>Anterior</button><span>{typeof totalPages === 'number' ? `Página ${page + 1} de ${totalPages}` : `Página ${page + 1}`}</span><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => current + 1)} disabled={clientsQuery.isFetching || (typeof totalPages === 'number' ? page + 1 >= totalPages : clients.length < 10)}>Siguiente</button></nav></div>}
        </section>

        <section className="owner-client-card">
          <div className="owner-client-card-heading"><span className="user-detail-icon"><UsersRound size={17} /></span><div><h2>Registrar cliente</h2><p>El DNI debe contener 7 u 8 dígitos; se permiten puntos, guiones y espacios.</p></div></div>
          <OwnerClienteLocalForm key={validBusinessId} negocioId={validBusinessId} onCreated={handleClientCreated} />
          {createdClient && <div className="owner-created-notice" role="status"><CheckCircle2 size={17} /><span>Se registró a {clientLabel(createdClient)} correctamente.{createdClientMatchedPreviousSearch ? ' Actualizamos la búsqueda para incluirlo.' : ' Actualizamos la búsqueda con su nombre.'}</span></div>}
        </section>
      </> : <div className="owner-business-state"><Building2 size={22} /><p>Seleccioná uno de tus negocios para buscar o registrar clientes.</p></div>}
    </>}
  </>
}
