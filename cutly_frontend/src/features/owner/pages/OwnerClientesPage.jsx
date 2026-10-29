import { useEffect, useMemo, useState } from 'react'
import { Building2, CheckCircle2, CircleAlert, LoaderCircle, RefreshCw, Search, UsersRound } from 'lucide-react'
import { getOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'
import { getOwnerClientesLocalesErrorMessage } from '../api/ownerTurnosApi.js'
import OwnerClienteLocalForm from '../components/OwnerClienteLocalForm.jsx'
import { useOwnerBusinesses } from '../hooks/useOwnerBusinesses.js'
import { useOwnerClientesLocales } from '../hooks/useOwnerTurnos.js'

const clientLabel = (client) => [client.nombre, client.apellido].filter(Boolean).join(' ')

export default function OwnerClientesPage() {
  const businessesQuery = useOwnerBusinesses()
  const businesses = useMemo(() => Array.isArray(businessesQuery.data) ? businessesQuery.data : [], [businessesQuery.data])
  const [selectedBusinessId, setSelectedBusinessId] = useState('')
  const [searchText, setSearchText] = useState('')
  const [debouncedText, setDebouncedText] = useState('')
  const [createdClient, setCreatedClient] = useState(null)

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
  const clientsQuery = useOwnerClientesLocales(validBusinessId, debouncedText, { enabled: searchReady })
  const clients = Array.isArray(clientsQuery.data) ? clientsQuery.data : []
  const shownClients = !isWaitingForDebounce && clientsQuery.dataUpdatedAt > 0 ? clients : []

  function changeBusiness(event) {
    setSelectedBusinessId(event.target.value)
    setSearchText('')
    setDebouncedText('')
    setCreatedClient(null)
  }

  const createdName = createdClient ? [createdClient.nombre, createdClient.apellido].filter(Boolean).join(' ') : ''
  const newlyCreatedMatchesSearch = Boolean(createdClient && debouncedText && createdName.toLocaleLowerCase().includes(debouncedText.toLocaleLowerCase()))

  function handleClientCreated(client) {
    const currentSearch = searchText.trim()
    const fullName = [client.nombre, client.apellido].filter(Boolean).join(' ')
    const matchesCurrentSearch = currentSearch && fullName.toLocaleLowerCase().includes(currentSearch.toLocaleLowerCase())
    setCreatedClient(client)
    if (!matchesCurrentSearch) {
      setSearchText(fullName)
      setDebouncedText(fullName)
    }
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
          <label className="owner-form-field" htmlFor="owner-clients-search"><span>Nombre o apellido</span><input id="owner-clients-search" type="search" value={searchText} maxLength={100} onChange={(event) => { setSearchText(event.target.value); setCreatedClient(null) }} placeholder="Escribí un nombre o apellido" /></label>
          <p className="owner-clients-limit-note">Se muestran como máximo 50 coincidencias. Los resultados no representan necesariamente todos los clientes del negocio. La búsqueda no admite DNI.</p>
          {searchText.trim().length < 2 ? <p className="owner-clients-search-state">Escribí al menos 2 caracteres para buscar.</p>
            : isWaitingForDebounce || clientsQuery.isFetching ? <p className="owner-clients-search-state" role="status"><LoaderCircle size={15} className="spin" /> Buscando clientes…</p>
              : clientsQuery.isError ? <div className="owner-clients-error" role="alert"><CircleAlert size={17} /><span>{getOwnerClientesLocalesErrorMessage(clientsQuery.error)}</span><button type="button" className="owners-secondary-button" onClick={() => void clientsQuery.refetch()} disabled={clientsQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div>
                : shownClients.length === 0 ? <p className="owner-clients-search-state">No se encontraron coincidencias.</p>
                  : <div className="owner-clients-table-wrap"><table className="owner-clients-table"><caption>Coincidencias (hasta 50)</caption><thead><tr><th>Nombre</th><th>Apellido</th><th>DNI</th><th>Teléfono</th></tr></thead><tbody>{shownClients.map((client) => <tr key={client.clienteLocalId}><td data-label="Nombre">{client.nombre}</td><td data-label="Apellido">{client.apellido}</td><td data-label="DNI">{client.dniPresentado}</td><td data-label="Teléfono">{client.telefono}</td></tr>)}</tbody></table></div>}
        </section>

        <section className="owner-client-card">
          <div className="owner-client-card-heading"><span className="user-detail-icon"><UsersRound size={17} /></span><div><h2>Registrar cliente</h2><p>El DNI debe contener 7 u 8 dígitos; se permiten puntos, guiones y espacios.</p></div></div>
          <OwnerClienteLocalForm key={validBusinessId} negocioId={validBusinessId} onCreated={handleClientCreated} />
          {createdClient && <div className="owner-created-notice" role="status"><CheckCircle2 size={17} /><span>Se registró a {clientLabel(createdClient)} correctamente.{searchReady && !newlyCreatedMatchesSearch ? ' La búsqueda actual no coincide: cambiá el texto por su nombre o apellido para encontrarlo.' : searchReady ? ' Actualizamos la búsqueda para incluirlo.' : ' Escribí su nombre o apellido para buscarlo.'}</span></div>}
        </section>
      </> : <div className="owner-business-state"><Building2 size={22} /><p>Seleccioná uno de tus negocios para buscar o registrar clientes.</p></div>}
    </>}
  </>
}
