import { useEffect, useMemo, useState } from 'react'
import { Building2, CircleAlert, Eye, LoaderCircle, RefreshCw, Search } from 'lucide-react'
import { getOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'
import { useOwnerBusinesses } from '../hooks/useOwnerBusinesses.js'
import { useCreateOwnerAbono, useOwnerDeudas } from '../hooks/useOwnerDeudas.js'
import OwnerDeudaDetailDrawer from '../components/OwnerDeudaDetailDrawer.jsx'

const pageSize = 10
const statusLabels = { PENDIENTE: 'Pendiente', PARCIAL: 'Parcial', PAGADA: 'Pagada' }

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

export default function OwnerDeudasPage() {
  const businessesQuery = useOwnerBusinesses()
  const businesses = useMemo(() => Array.isArray(businessesQuery.data) ? businessesQuery.data : [], [businessesQuery.data])
  const [selectedBusinessId, setSelectedBusinessId] = useState('')
  const [searchText, setSearchText] = useState('')
  const [debouncedText, setDebouncedText] = useState('')
  const [estado, setEstado] = useState('')
  const [page, setPage] = useState(0)
  const [selectedDeuda, setSelectedDeuda] = useState(null)
  const abonoMutation = useCreateOwnerAbono()

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedText(searchText.trim()), 300)
    return () => window.clearTimeout(timer)
  }, [searchText])

  const selectedBusiness = businesses.find((business) => String(business.negocioId) === selectedBusinessId)
  const validBusinessId = selectedBusiness
    ? selectedBusinessId
    : businesses.length === 1 ? String(businesses[0].negocioId) : ''
  const isWaitingForDebounce = searchText.trim() !== debouncedText
  const deudasQuery = useOwnerDeudas({
    negocioId: validBusinessId,
    page,
    size: pageSize,
    q: debouncedText,
    estado,
  }, { enabled: !isWaitingForDebounce })
  const result = !isWaitingForDebounce && !deudasQuery.isFetching ? deudasQuery.data : null
  const deudas = Array.isArray(result?.content) ? result.content : []
  const totalElements = result?.totalElements
  const totalPages = result?.totalPages
  const currentPage = typeof result?.number === 'number' ? result.number : page

  function changeBusiness(event) {
    setSelectedBusinessId(event.target.value)
    setPage(0)
    setSearchText('')
    setDebouncedText('')
    setEstado('')
  }

  function changeSearch(event) {
    setSearchText(event.target.value)
    setPage(0)
  }

  function changeStatus(event) {
    setEstado(event.target.value)
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
    <div className="owners-heading">
      <div>
        <h1>Cuentas corrientes</h1>
        <p className="owners-description">Seguimiento de saldos pendientes y pagos de clientes.</p>
      </div>
    </div>

    {businessesQuery.isLoading ? (
      <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocios…</div>
    ) : businessesQuery.isError ? (
      <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessErrorMessage(businessesQuery.error)}</p><button type="button" className="owners-secondary-button" onClick={() => void businessesQuery.refetch()} disabled={businessesQuery.isFetching}><RefreshCw size={15} /> Reintentar</button></div>
    ) : businesses.length === 0 ? (
      <div className="owner-business-state"><Building2 size={22} /><p>Todavía no tenés negocios registrados.</p></div>
    ) : <>
      <section className="owner-clients-business-panel owner-deudas-business-panel" aria-label="Negocio seleccionado">
        {businesses.length === 1 ? <div className="owner-clients-selected-business"><Building2 size={17} /><span><small>Negocio</small><strong>{businesses[0].nombre || 'Negocio'}</strong></span></div> : <label className="owner-form-field" htmlFor="owner-deudas-business"><span>Negocio</span><select id="owner-deudas-business" value={validBusinessId} onChange={changeBusiness}><option value="">Seleccioná un negocio…</option>{businesses.map((business) => <option key={business.negocioId} value={String(business.negocioId)}>{business.nombre || `Negocio ${business.negocioId}`}</option>)}</select></label>}
      </section>

      {validBusinessId ? <section className="owner-client-card owner-deudas-card" aria-label="Listado de deudas">
        <div className="owner-client-card-heading"><span className="user-detail-icon"><Search size={17} /></span><div><h2>Deudas de clientes</h2><p>Buscá por nombre o apellido y filtrá por estado.</p></div></div>

        <div className="owner-deudas-filters">
          <label className="owner-form-field" htmlFor="owner-deudas-search"><span>Buscar cliente</span><input id="owner-deudas-search" type="search" value={searchText} maxLength={100} onChange={changeSearch} placeholder="Nombre o apellido" /></label>
          <label className="owner-form-field" htmlFor="owner-deudas-status"><span>Estado</span><select id="owner-deudas-status" value={estado} onChange={changeStatus}><option value="">Todos</option><option value="PENDIENTE">Pendiente</option><option value="PARCIAL">Parcial</option><option value="PAGADA">Pagada</option></select></label>
        </div>

        <div className="owner-deudas-results" aria-busy={isWaitingForDebounce || deudasQuery.isFetching}>
          {deudasQuery.isError ? <div className="owner-deudas-state owner-deudas-error" role="alert"><CircleAlert size={17} /><span>No pudimos cargar las cuentas corrientes. Intentá nuevamente.</span><button type="button" className="owners-secondary-button" onClick={() => void deudasQuery.refetch()} disabled={deudasQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div>
            : isWaitingForDebounce || deudasQuery.isFetching ? <div className="owner-deudas-state" role="status"><LoaderCircle size={17} className="spin" /> Cargando deudas…</div>
              : deudas.length === 0 ? <div className="owner-deudas-state">{debouncedText || estado ? 'No hay deudas para los filtros seleccionados.' : 'Todavía no hay deudas registradas para este negocio.'}</div>
                : <>
                  <div className="owner-deudas-count" aria-live="polite">{typeof totalElements === 'number' ? `${totalElements} ${totalElements === 1 ? 'deuda' : 'deudas'}` : 'Deudas'}</div>
                  <div className="owner-deudas-table-wrap"><table className="owners-table owner-deudas-table"><thead><tr><th>Cliente</th><th>Fecha de origen</th><th>Importe acordado</th><th>Total abonado</th><th>Saldo</th><th>Estado</th><th>Detalle</th></tr></thead><tbody>{deudas.map((deuda) => <tr key={deuda.deudaId}>
                    <td data-label="Cliente" className="owner-deudas-client">{[deuda.nombreCliente, deuda.apellidoCliente].filter(Boolean).join(' ') || '—'}</td>
                    <td data-label="Fecha de origen">{dateTime(deuda.fechaOrigen)}</td>
                    <td data-label="Importe acordado">{money(deuda.montoOriginal)}</td>
                    <td data-label="Total abonado">{money(deuda.totalAbonado)}</td>
                    <td data-label="Saldo" className="owner-deudas-balance">{money(deuda.saldo)}</td>
                    <td data-label="Estado"><span className={`owner-status owner-deuda-status owner-deuda-status-${String(deuda.estado || '').toLowerCase()}`}>{statusLabels[deuda.estado] || deuda.estado || '—'}</span></td>
                    <td data-label="Detalle"><button type="button" className="owners-secondary-button owner-deudas-detail" aria-label={`Consultar detalle de la deuda de ${[deuda.nombreCliente, deuda.apellidoCliente].filter(Boolean).join(' ') || 'cliente'}`} title="Consultar detalle" onClick={() => { abonoMutation.reset(); setSelectedDeuda({ negocioId: validBusinessId, deudaId: deuda.deudaId }) }}><Eye size={14} /><span>Ver detalle</span></button></td>
                  </tr>)}</tbody></table></div>
                  <nav className="owner-clients-pagination owner-deudas-pagination" aria-label="Paginación de cuentas corrientes">
                    <span>{typeof totalElements === 'number' ? `${totalElements} resultados` : ''}</span>
                    <div><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={currentPage === 0 || deudasQuery.isFetching}>Anterior</button><span>{typeof totalPages === 'number' ? `Página ${currentPage + 1} de ${totalPages}` : `Página ${currentPage + 1}`}</span><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => current + 1)} disabled={deudasQuery.isFetching || (typeof totalPages === 'number' ? currentPage + 1 >= totalPages : deudas.length < pageSize)}>Siguiente</button></div>
                  </nav>
                </>}
        </div>
      </section> : <div className="owner-business-state"><Building2 size={22} /><p>Seleccioná uno de tus negocios para consultar sus cuentas corrientes.</p></div>}
    </>}
    {selectedDeuda && <OwnerDeudaDetailDrawer negocioId={selectedDeuda.negocioId} deudaId={selectedDeuda.deudaId} abonoMutation={abonoMutation} onClose={closeDeudaDetail} onAbonar={registerAbono} />}
  </>
}
