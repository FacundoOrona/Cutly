import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, Building2, CircleAlert, Eye, LoaderCircle, RefreshCw, Search } from 'lucide-react'
import { getOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'
import { useOwnerBusinesses } from '../hooks/useOwnerBusinesses.js'
import { useCreateOwnerAbono, useOwnerDeudas, useOwnerDeudasClientes } from '../hooks/useOwnerDeudas.js'
import OwnerDeudaDetailDrawer from '../components/OwnerDeudaDetailDrawer.jsx'
import { useActiveOwnerBusiness } from '../context/OwnerBusinessContext.jsx'

const pageSize = 20
const detailPageSize = 100
const statusLabels = { PENDIENTE: 'Pendiente', PARCIAL: 'Parcial', PAGADA: 'Pagada' }
const pendingStates = ['PENDIENTE', 'PARCIAL']

function money(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value))
}

function dateTime(value) {
  if (!value) return '—'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium' }).format(parsed)
}

function clientName(client) {
  return [client.nombreCliente, client.apellidoCliente].filter(Boolean).join(' ') || '—'
}

function sameClient(deuda, client) {
  return client.clienteId != null
    ? String(deuda.clienteId) === String(client.clienteId)
    : client.clienteLocalId != null && String(deuda.clienteLocalId) === String(client.clienteLocalId)
}

export default function OwnerDeudasPage() {
  const businessesQuery = useOwnerBusinesses()
  const businesses = useMemo(() => Array.isArray(businessesQuery.data) ? businessesQuery.data : [], [businessesQuery.data])
  const { activeBusiness } = useActiveOwnerBusiness()
  const [searchText, setSearchText] = useState('')
  const [debouncedText, setDebouncedText] = useState('')
  const [page, setPage] = useState(0)
  const [selectedClient, setSelectedClient] = useState(null)
  const [selectedDeuda, setSelectedDeuda] = useState(null)
  const abonoMutation = useCreateOwnerAbono()

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedText(searchText.trim()), 300)
    return () => window.clearTimeout(timer)
  }, [searchText])

  useEffect(() => { setPage(0); setSelectedClient(null); setSelectedDeuda(null) }, [activeBusiness?.negocioId])

  const validBusinessId = String(activeBusiness?.negocioId || '')
  const isWaitingForDebounce = searchText.trim() !== debouncedText
  const clientsQuery = useOwnerDeudasClientes({ negocioId: validBusinessId, page, size: pageSize, q: debouncedText }, { enabled: !isWaitingForDebounce && !selectedClient })
  const clientsResult = !isWaitingForDebounce && !clientsQuery.isFetching ? clientsQuery.data : null
  const clients = Array.isArray(clientsResult?.content) ? clientsResult.content : []
  const currentPage = typeof clientsResult?.page === 'number' ? clientsResult.page : page
  const detailQueries = pendingStates.map((estado) => useOwnerDeudas({
    negocioId: validBusinessId,
    page: 0,
    size: detailPageSize,
    q: selectedClient ? clientName(selectedClient) : '',
    estado,
  }, { enabled: Boolean(selectedClient && validBusinessId) }))
  const clientDeudas = detailQueries.flatMap((query) => Array.isArray(query.data?.content) ? query.data.content : [])
    .filter((deuda, index, all) => sameClient(deuda, selectedClient || {}) && all.findIndex((item) => String(item.deudaId) === String(deuda.deudaId)) === index)
  const detailsLoading = detailQueries.some((query) => query.isFetching)
  const detailsError = detailQueries.find((query) => query.isError)

  function changeSearch(event) {
    setSearchText(event.target.value)
    setPage(0)
  }

  function closeDeudaDetail() {
    if (!abonoMutation.isPending) {
      setSelectedDeuda(null)
      abonoMutation.reset()
    }
  }

  function registerAbono(operation) {
    if (!selectedDeuda || abonoMutation.isPending) return
    void abonoMutation.mutateAsync({
      negocioId: selectedDeuda.negocioId,
      deudaId: selectedDeuda.deudaId,
      body: operation.body,
      idempotencyKey: operation.idempotencyKey,
    }).catch(() => {})
  }

  return <>
    <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
    <div className="owners-heading"><div><h1>Cuentas corrientes</h1><p className="owners-description">Seguimiento de saldos pendientes y pagos de clientes.</p></div></div>

    {businessesQuery.isLoading ? (
      <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocios…</div>
    ) : businessesQuery.isError ? (
      <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessErrorMessage(businessesQuery.error)}</p><button type="button" className="owners-secondary-button" onClick={() => void businessesQuery.refetch()} disabled={businessesQuery.isFetching}><RefreshCw size={15} /> Reintentar</button></div>
    ) : businesses.length === 0 ? (
      <div className="owner-business-state"><Building2 size={22} /><p>Todavía no tenés negocios registrados.</p></div>
    ) : <>

      {validBusinessId ? <section className="owner-client-card owner-deudas-card" aria-label={selectedClient ? 'Deudas pendientes del cliente' : 'Cuentas corrientes por cliente'}>
        <div className="owner-client-card-heading"><span className="user-detail-icon"><Search size={17} /></span><div><h2>{selectedClient ? `Deudas de ${clientName(selectedClient)}` : 'Cuentas corrientes por cliente'}</h2><p>{selectedClient ? 'Deudas pendientes y parciales asociadas a este cliente.' : 'Buscá por nombre o apellido para consultar saldos agrupados.'}</p></div></div>

        {!selectedClient ? <label className="owner-form-field owner-deudas-client-search" htmlFor="owner-deudas-search"><span>Buscar cliente</span><input id="owner-deudas-search" type="search" value={searchText} maxLength={100} onChange={changeSearch} placeholder="Nombre o apellido" /></label> : <button type="button" className="owners-secondary-button owner-deudas-back" onClick={() => setSelectedClient(null)}><ArrowLeft size={14} /> Volver a clientes</button>}

        <div className="owner-deudas-results" aria-busy={isWaitingForDebounce || clientsQuery.isFetching || detailsLoading}>
          {!selectedClient ? clientsQuery.isError ? <div className="owner-deudas-state owner-deudas-error" role="alert"><CircleAlert size={17} /><span>No pudimos cargar las cuentas corrientes. Intentá nuevamente.</span><button type="button" className="owners-secondary-button" onClick={() => void clientsQuery.refetch()} disabled={clientsQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div>
            : isWaitingForDebounce || clientsQuery.isFetching ? <div className="owner-deudas-state" role="status"><LoaderCircle size={17} className="spin" /> Cargando clientes…</div>
              : clients.length === 0 ? <div className="owner-deudas-state">{debouncedText ? 'No hay clientes con deudas pendientes para esa búsqueda.' : 'Todavía no hay deudas pendientes para este negocio.'}</div>
                : <>
                  <div className="owner-deudas-count" aria-live="polite">{typeof clientsResult?.totalElements === 'number' ? `${clientsResult.totalElements} ${clientsResult.totalElements === 1 ? 'cliente' : 'clientes'}` : 'Clientes'}</div>
                  <div className="owner-deudas-table-wrap"><table className="owners-table owner-deudas-table owner-deudas-clients-table"><thead><tr><th>Cliente</th><th>Tipo</th><th>Deudas pendientes</th><th>Saldo pendiente</th><th>Deuda más antigua</th><th>Detalle</th></tr></thead><tbody>{clients.map((client) => <tr key={client.clienteId != null ? `usuario-${client.clienteId}` : `local-${client.clienteLocalId}`}>
                    <td data-label="Cliente" className="owner-deudas-client">{clientName(client)}</td>
                    <td data-label="Tipo">{client.tipoCliente === 'LOCAL' ? 'Local' : 'Registrado'}</td>
                    <td data-label="Deudas pendientes">{client.cantidadDeudasPendientes}</td>
                    <td data-label="Saldo pendiente" className="owner-deudas-balance">{money(client.saldoPendienteTotal)}</td>
                    <td data-label="Deuda más antigua">{dateTime(client.fechaDeudaMasAntigua)}</td>
                    <td data-label="Detalle"><button type="button" className="owners-secondary-button owner-deudas-detail" onClick={() => setSelectedClient(client)}><Eye size={14} /><span>Ver deudas</span></button></td>
                  </tr>)}</tbody></table></div>
                  <nav className="owner-clients-pagination owner-deudas-pagination" aria-label="Paginación de cuentas corrientes"><span>{typeof clientsResult?.totalElements === 'number' ? `${clientsResult.totalElements} resultados` : ''}</span><div><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={currentPage === 0 || clientsQuery.isFetching}>Anterior</button><span>{typeof clientsResult?.totalPages === 'number' ? `Página ${currentPage + 1} de ${clientsResult.totalPages}` : `Página ${currentPage + 1}`}</span><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => current + 1)} disabled={clientsQuery.isFetching || (typeof clientsResult?.totalPages === 'number' ? currentPage + 1 >= clientsResult.totalPages : clients.length < pageSize)}>Siguiente</button></div></nav>
                </> : detailsLoading ? <div className="owner-deudas-state" role="status"><LoaderCircle size={17} className="spin" /> Cargando deudas…</div>
            : detailsError ? <div className="owner-deudas-state owner-deudas-error" role="alert"><CircleAlert size={17} /><span>No pudimos cargar las deudas de este cliente.</span><button type="button" className="owners-secondary-button" onClick={() => detailQueries.forEach((query) => void query.refetch())}><RefreshCw size={14} /> Reintentar</button></div>
              : clientDeudas.length === 0 ? <div className="owner-deudas-state">Este cliente no tiene deudas pendientes.</div>
                : <><div className="owner-deudas-count" aria-live="polite">{clientDeudas.length} {clientDeudas.length === 1 ? 'deuda pendiente' : 'deudas pendientes'}</div>
                  <div className="owner-deudas-table-wrap"><table className="owners-table owner-deudas-table"><thead><tr><th>Turno</th><th>Fecha de origen</th><th>Importe original</th><th>Total abonado</th><th>Saldo pendiente</th><th>Estado</th><th>Detalle</th></tr></thead><tbody>{clientDeudas.map((deuda) => <tr key={deuda.deudaId}>
                    <td data-label="Turno">#{deuda.turnoId ?? '—'}</td><td data-label="Fecha de origen">{dateTime(deuda.fechaOrigen)}</td><td data-label="Importe original">{money(deuda.montoOriginal)}</td><td data-label="Total abonado">{money(deuda.totalAbonado)}</td><td data-label="Saldo pendiente" className="owner-deudas-balance">{money(deuda.saldo)}</td><td data-label="Estado"><span className={`owner-status owner-deuda-status owner-deuda-status-${String(deuda.estado || '').toLowerCase()}`}>{statusLabels[deuda.estado] || deuda.estado || '—'}</span></td><td data-label="Detalle"><button type="button" className="owners-secondary-button owner-deudas-detail" onClick={() => { abonoMutation.reset(); setSelectedDeuda({ negocioId: validBusinessId, deudaId: deuda.deudaId }) }}><Eye size={14} /><span>Ver detalle</span></button></td>
                  </tr>)}</tbody></table></div></>}
        </div>
      </section> : null}
    </>}
    {selectedDeuda && <OwnerDeudaDetailDrawer negocioId={selectedDeuda.negocioId} deudaId={selectedDeuda.deudaId} abonoMutation={abonoMutation} onClose={closeDeudaDetail} onAbonar={registerAbono} />}
  </>
}
