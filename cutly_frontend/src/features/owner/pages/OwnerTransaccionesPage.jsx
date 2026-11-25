import { useEffect, useMemo, useState } from 'react'
import { Building2, CircleAlert, Clock3, CreditCard, LoaderCircle, RefreshCw, Search, Wallet } from 'lucide-react'
import { getOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'
import { getOwnerTransaccionesErrorMessage } from '../api/ownerTransaccionesApi.js'
import { useOwnerBusinesses } from '../hooks/useOwnerBusinesses.js'
import { useOwnerTransacciones } from '../hooks/useOwnerTransacciones.js'
import { useActiveOwnerBusiness } from '../context/OwnerBusinessContext.jsx'

const pageSize = 20
function localDateString(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const paymentMethods = [
  ['EFECTIVO', 'Efectivo'],
  ['TRANSFERENCIA_QR', 'Transferencia / QR'],
  ['DEBITO', 'Débito'],
  ['CREDITO', 'Crédito'],
  ['OTRO', 'Otro'],
]
const paymentLabels = Object.fromEntries(paymentMethods)
Object.assign(paymentLabels, {
  CASH: 'Efectivo (histórico)',
  DEBIT_CARD: 'Tarjeta de débito (histórico)',
  CREDIT_CARD: 'Tarjeta de crédito (histórico)',
  TRANSFER: 'Transferencia (histórico)',
  MERCADO_PAGO: 'Mercado Pago (histórico)',
})
const typeLabels = { COBRO_DIRECTO: 'Cobro directo', PAGO_CUENTA_CORRIENTE: 'Pago de cuenta corriente' }

function money(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value))
}

function dateTime(value) {
  if (!value) return '—'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium', timeStyle: 'short' }).format(parsed)
}

export default function OwnerTransaccionesPage() {
  const businessesQuery = useOwnerBusinesses()
  const businesses = useMemo(() => Array.isArray(businessesQuery.data) ? businessesQuery.data : [], [businessesQuery.data])
  const { activeBusiness } = useActiveOwnerBusiness()
  const [desde, setDesde] = useState(localDateString)
  const [hasta, setHasta] = useState(localDateString)
  const [metodoPago, setMetodoPago] = useState([])
  const [tipo, setTipo] = useState('')
  const [searchText, setSearchText] = useState('')
  const [debouncedText, setDebouncedText] = useState('')
  const [page, setPage] = useState(0)

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedText(searchText.trim()), 300)
    return () => window.clearTimeout(timer)
  }, [searchText])

  useEffect(() => { setPage(0) }, [activeBusiness?.negocioId])

  const validBusinessId = String(activeBusiness?.negocioId || '')
  const isWaitingForDebounce = searchText.trim() !== debouncedText
  const dateRangeInvalid = Boolean(desde && hasta && desde > hasta)
  const transactionsQuery = useOwnerTransacciones({
    negocioId: validBusinessId,
    desde,
    hasta,
    metodoPago,
    tipo,
    q: debouncedText,
    page,
    size: pageSize,
  }, { enabled: !isWaitingForDebounce && !dateRangeInvalid })
  const result = transactionsQuery.isPlaceholderData ? null : transactionsQuery.data
  const transactions = Array.isArray(result?.items) ? result.items : []
  const summary = result?.resumen
  const currentPage = typeof result?.page === 'number' ? result.page : page
  const totalPages = typeof result?.totalPages === 'number' ? result.totalPages : null
  const hasFilters = Boolean(desde || hasta || metodoPago.length || tipo || searchText.trim())

  function resetPage() { setPage(0) }

  function togglePaymentMethod(value) {
    setMetodoPago((current) => current.includes(value) ? current.filter((item) => item !== value) : [...current, value])
    resetPage()
  }

  function clearFilters() {
    const today = localDateString()
    setDesde(today)
    setHasta(today)
    setMetodoPago([])
    setTipo('')
    setSearchText('')
    setDebouncedText('')
    setPage(0)
  }

  return <>
    <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
    <div className="owners-heading"><div><h1>Transacciones</h1><p className="owners-description">Historial de pagos efectivamente recibidos por tus negocios.</p></div></div>

    {businessesQuery.isLoading ? (
      <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocios…</div>
    ) : businessesQuery.isError ? (
      <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessErrorMessage(businessesQuery.error)}</p><button type="button" className="owners-secondary-button" onClick={() => void businessesQuery.refetch()} disabled={businessesQuery.isFetching}><RefreshCw size={15} /> Reintentar</button></div>
    ) : businesses.length === 0 ? (
      <div className="owner-business-state"><Building2 size={22} /><p>Todavía no tenés negocios registrados.</p></div>
    ) : <>

      {validBusinessId ? <>
        <section className="owner-client-card owner-transactions-filter-card" aria-label="Filtros de transacciones">
          <div className="owner-client-card-heading"><span className="user-detail-icon"><Search size={17} /></span><div><h2>Filtrar transacciones</h2><p>Buscá los pagos recibidos por fecha, método, tipo o cliente.</p></div></div>
          <div className="owner-transactions-filters">
            <label className="owner-form-field"><span>Desde</span><input type="date" value={desde} max={hasta || undefined} onChange={(event) => { setDesde(event.target.value); resetPage() }} /></label>
            <label className="owner-form-field"><span>Hasta</span><input type="date" value={hasta} min={desde || undefined} onChange={(event) => { setHasta(event.target.value); resetPage() }} /></label>
            <label className="owner-form-field"><span>Tipo de transacción</span><select value={tipo} onChange={(event) => { setTipo(event.target.value); resetPage() }}><option value="">Todos los tipos</option><option value="COBRO_DIRECTO">Cobro directo</option><option value="PAGO_CUENTA_CORRIENTE">Pago de cuenta corriente</option></select></label>
            <label className="owner-form-field owner-transactions-search"><span>Cliente</span><input type="search" value={searchText} maxLength={100} onChange={(event) => { setSearchText(event.target.value); resetPage() }} placeholder="Nombre o apellido" /></label>
            <fieldset className="owner-transactions-methods"><legend>Métodos de pago</legend><div>{paymentMethods.map(([value, label]) => <label key={value}><input type="checkbox" checked={metodoPago.includes(value)} onChange={() => togglePaymentMethod(value)} /><span>{label}</span></label>)}</div></fieldset>
            <div className="owner-transactions-filter-actions"><span>{metodoPago.length ? `${metodoPago.length} ${metodoPago.length === 1 ? 'método seleccionado' : 'métodos seleccionados'}` : 'Podés seleccionar varios métodos'}</span><button type="button" className="owners-secondary-button" onClick={clearFilters} disabled={!hasFilters}>Limpiar filtros</button></div>
          </div>
          {dateRangeInvalid && <p className="owner-transactions-validation" role="alert">La fecha “Desde” no puede ser posterior a “Hasta”.</p>}
        </section>

        {summary && <section className="owner-transactions-summary" aria-label="Resumen financiero de resultados filtrados" aria-busy={transactionsQuery.isFetching}>
          <article className="owner-transactions-stat owner-transactions-stat-primary"><span className="owner-transactions-stat-icon"><Wallet size={18} /></span><small>Total ingresado</small><strong>{money(summary.totalIngresado)}</strong></article>
          <article className="owner-transactions-stat"><span className="owner-transactions-stat-icon"><CreditCard size={18} /></span><small>Transacciones</small><strong>{summary.cantidadTransacciones ?? '—'}</strong></article>
          <article className="owner-transactions-stat"><span className="owner-transactions-stat-icon"><Wallet size={18} /></span><small>Cobros directos</small><strong>{money(summary.totalCobrosDirectos)}</strong></article>
          <article className="owner-transactions-stat"><span className="owner-transactions-stat-icon"><CreditCard size={18} /></span><small>Pagos de cuenta corriente</small><strong>{money(summary.totalPagosCuentaCorriente)}</strong></article>
        </section>}

        <section className="owner-client-card owner-transactions-history" aria-label="Historial de transacciones">
          <div className="owner-client-card-heading"><span className="user-detail-icon"><Clock3 size={17} /></span><div><h2>Historial de pagos</h2><p>Pagos recibidos del negocio seleccionado.</p></div></div>
          <div className="owner-deudas-results" aria-busy={isWaitingForDebounce || transactionsQuery.isFetching}>
            {transactionsQuery.isError ? <div className="owner-deudas-state owner-deudas-error" role="alert"><CircleAlert size={17} /><span>{getOwnerTransaccionesErrorMessage(transactionsQuery.error)}</span><button type="button" className="owners-secondary-button" onClick={() => void transactionsQuery.refetch()} disabled={transactionsQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div>
              : isWaitingForDebounce || transactionsQuery.isLoading ? <div className="owner-deudas-state" role="status"><LoaderCircle size={17} className="spin" /> Cargando transacciones…</div>
                : dateRangeInvalid ? <div className="owner-deudas-state">Corregí el rango de fechas para consultar las transacciones.</div>
                  : transactions.length === 0 ? <div className="owner-deudas-state">{hasFilters ? 'No hay transacciones para los filtros aplicados.' : 'Todavía no hay transacciones registradas para este negocio.'}</div>
                    : <>
                      <div className="owner-deudas-count" aria-live="polite">{typeof result?.totalItems === 'number' ? `${result.totalItems} ${result.totalItems === 1 ? 'transacción' : 'transacciones'}` : 'Transacciones'} · Página {currentPage + 1}{totalPages != null ? ` de ${totalPages}` : ''}</div>
                      <div className="owner-deudas-table-wrap owner-transactions-table-wrap"><table className="owners-table owner-deudas-table owner-transactions-table"><thead><tr><th>Fecha y hora</th><th>Cliente</th><th>Tipo</th><th>Método de pago</th><th>Importe</th></tr></thead><tbody>{transactions.map((transaction) => {
                        const client = [transaction.nombre, transaction.apellido].filter(Boolean).join(' ') || 'Cliente no informado'
                        const paymentLabel = paymentLabels[transaction.metodoPago] || transaction.metodoPago || 'Método no informado'
                        return <tr key={transaction.pagoId}>
                          <td data-label="Fecha y hora">{dateTime(transaction.fechaPago)}<small className="owner-transactions-payment-id">Pago #{transaction.pagoId}</small></td>
                          <td data-label="Cliente" className="owner-deudas-client">{client}</td>
                          <td data-label="Tipo">{typeLabels[transaction.tipo] || transaction.tipo || '—'}</td>
                          <td data-label="Método de pago">{paymentLabel}{transaction.metodoPago === 'OTRO' && transaction.descripcionOtro ? <small className="owner-transactions-payment-note">{transaction.descripcionOtro}</small> : null}</td>
                          <td data-label="Importe" className="owner-transactions-amount">{money(transaction.monto)}</td>
                        </tr>
                      })}</tbody></table></div>
                      <nav className="owner-clients-pagination owner-deudas-pagination owner-transactions-pagination" aria-label="Paginación de transacciones"><span>{typeof result?.totalItems === 'number' ? `${result.totalItems} resultados` : ''}</span><div><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={currentPage === 0 || transactionsQuery.isFetching}>Anterior</button><span>{totalPages != null ? `Página ${currentPage + 1} de ${totalPages}` : `Página ${currentPage + 1}`}</span><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => current + 1)} disabled={transactionsQuery.isFetching || (totalPages != null ? currentPage + 1 >= totalPages : transactions.length < pageSize)}>Siguiente</button></div></nav>
                    </>}
          </div>
        </section>
      </> : null}
    </>}
  </>
}
