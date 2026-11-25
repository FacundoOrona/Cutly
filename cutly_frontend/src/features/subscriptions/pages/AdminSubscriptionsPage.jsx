import { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Building2, LoaderCircle, RefreshCw, Search, ShieldCheck, UserRound, X } from 'lucide-react'
import { fetchUsers } from '../../users/api/usersApi.js'
import { useAdminPlans } from '../../subscriptionPlans/hooks/useAdminPlans.js'
import OwnerSubscriptionManagement from '../../owners/components/OwnerSubscriptionManagement.jsx'
import { getAdminSubscriptionsErrorMessage } from '../api/adminSubscriptionsApi.js'
import { useAdminSubscriptions, useAdminSubscriptionStats } from '../hooks/useAdminSubscriptions.js'
import './adminSubscriptions.css'

const PAGE_SIZE = 20
const EMPTY_FILTERS = { status: '', planId: '', ownerId: '' }
const STATUSES = [
  ['PENDING', 'Pendiente'], ['TRIAL', 'Prueba'], ['ACTIVE', 'Activa'],
  ['SUSPENDED', 'Suspendida'], ['EXPIRED', 'Vencida'], ['CANCELLED', 'Cancelada'],
]
const STATUS_LABELS = Object.fromEntries(STATUSES)
const MONEY = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

function dateValue(value) {
  return value ? new Date(`${value}T00:00:00`) : null
}

function formatDate(value) {
  const date = dateValue(value)
  return date ? date.toLocaleDateString('es-AR') : '—'
}

function remainingDays(value) {
  const due = dateValue(value)
  if (!due) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((due - today) / 86400000)
}

function OwnerFilter({ value, selectedOwner, onSelect }) {
  const [field, setField] = useState('nombre')
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const ownerQuery = useQuery({
    queryKey: ['admin', 'subscription-owner-options', field, debouncedSearch],
    enabled: debouncedSearch.trim().length >= 2,
    queryFn: () => fetchUsers({
      filters: { nombre: field === 'nombre' ? debouncedSearch.trim() : '', email: field === 'email' ? debouncedSearch.trim() : '', rol: 'OWNER' },
      page: 0,
      size: 10,
      sort: { field: 'usuarioId', direction: 'DESC' },
    }),
  })
  const owners = ownerQuery.data?.content || []

  useEffect(() => {
    const timeout = window.setTimeout(() => setDebouncedSearch(search), 300)
    return () => window.clearTimeout(timeout)
  }, [search])

  useEffect(() => {
    setSearch('')
    setDebouncedSearch('')
  }, [field])

  return <div className="subscription-owner-filter">
    <label className="subscription-field"><span>Buscar OWNER por</span><select value={field} onChange={event => { onSelect('', null); setField(event.target.value) }}><option value="nombre">Nombre</option><option value="email">Email</option></select></label>
    <div className="subscription-owner-search-wrap">
      <label className="subscription-field"><span>OWNER</span><div className="subscription-search"><Search size={15} aria-hidden="true" /><input type="search" value={search} onChange={event => { setSearch(event.target.value); if (value) onSelect('', null) }} placeholder={field === 'nombre' ? 'Nombre o apellido' : 'Email'} /></div></label>
      {value && selectedOwner && <div className="subscription-selected-owner"><span>{[selectedOwner.nombre, selectedOwner.apellido].filter(Boolean).join(' ') || selectedOwner.email}</span><button type="button" onClick={() => onSelect('', null)} aria-label="Quitar filtro de OWNER"><X size={13} /></button></div>}
      {debouncedSearch.trim().length >= 2 && <ul className="subscription-owner-options" aria-label="Resultados de búsqueda de OWNER">
        {ownerQuery.isFetching ? <li role="status">Buscando OWNER…</li>
          : ownerQuery.isError ? <li role="alert">No se pudieron buscar los OWNER. <button type="button" onClick={() => ownerQuery.refetch()}>Reintentar</button></li>
            : owners.length ? owners.map(owner => <li key={owner.usuarioId}><button type="button" onClick={() => { onSelect(String(owner.usuarioId), owner); setSearch(''); setDebouncedSearch('') }}>
              <strong>{[owner.nombre, owner.apellido].filter(Boolean).join(' ') || 'OWNER'}</strong><span>{owner.email}</span>
            </button></li>) : <li>No hay OWNER que coincidan con la búsqueda.</li>}
      </ul>}
    </div>
  </div>
}

function Summary({ stats, unavailable }) {
  const items = [
    ['Suscripciones', stats?.totalSuscripciones, 'En todo el sistema'],
    ['Activas', stats?.activas, 'Estado registrado ACTIVE'],
    ['Pendientes', stats?.pendientes, 'A la espera de confirmación'],
    ['Vencidas', stats?.vencidas, 'Estado registrado EXPIRED'],
  ]
  return <div className="subscription-summary" aria-label="Resumen de suscripciones">
    {items.map(([label, value, detail]) => <div className="subscription-summary-item" key={label}>
      <span>{label}</span><strong>{unavailable ? '—' : new Intl.NumberFormat('es-AR').format(value ?? 0)}</strong><small>{detail}</small>
    </div>)}
  </div>
}

export default function AdminSubscriptionsPage() {
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [page, setPage] = useState(0)
  const [managedSubscription, setManagedSubscription] = useState(null)
  const [selectedOwner, setSelectedOwner] = useState(null)
  const plansQuery = useAdminPlans()
  const statsQuery = useAdminSubscriptionStats()
  const subscriptionsQuery = useAdminSubscriptions(filters, page, PAGE_SIZE)
  const data = subscriptionsQuery.data
  const subscriptions = data?.content || []
  const plans = useMemo(() => plansQuery.data || [], [plansQuery.data])

  useEffect(() => {
    if (!managedSubscription) return undefined
    function closeOnEscape(event) {
      if (event.key === 'Escape') setManagedSubscription(null)
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [managedSubscription])

  function setFilter(field, value) {
    setPage(0)
    setFilters(current => ({ ...current, [field]: value }))
  }

  function clearFilters() {
    setFilters(EMPTY_FILTERS)
    setSelectedOwner(null)
    setPage(0)
  }

  return <section className="admin-subscriptions-page">
    <span className="eyebrow"><span className="eyebrow-dot" /> ADMINISTRACIÓN</span>
    <header className="subscription-page-heading">
      <div><h1>Suscripciones</h1><p>Administrá la vigencia y capacidad contratada por cada OWNER.</p></div>
      <span className="subscription-heading-note"><ShieldCheck size={15} /> Pagos externos · gestión SUPER_ADMIN</span>
    </header>

    <Summary stats={statsQuery.data} unavailable={statsQuery.isPending || statsQuery.isError} />
    {statsQuery.isError && <p className="subscription-stats-error" role="status">No se pudo actualizar el resumen de estados.</p>}

    <section className="subscription-filter-panel" aria-label="Filtros de suscripciones">
      <div className="subscription-filter-grid">
        <label className="subscription-field"><span>Estado</span><select value={filters.status} onChange={event => setFilter('status', event.target.value)}><option value="">Todos los estados</option>{STATUSES.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
        <label className="subscription-field"><span>Plan</span><select value={filters.planId} onChange={event => setFilter('planId', event.target.value)}><option value="">Todos los planes</option>{plans.map(plan => <option value={plan.planId} key={plan.planId}>{plan.nombre}</option>)}</select></label>
        <OwnerFilter value={filters.ownerId} selectedOwner={selectedOwner} onSelect={(ownerId, owner) => { setSelectedOwner(owner || null); setFilter('ownerId', ownerId) }} />
      </div>
      <button type="button" className="subscription-clear-filters" onClick={clearFilters} disabled={!filters.status && !filters.planId && !filters.ownerId}>Limpiar filtros</button>
    </section>

    <section className="subscription-results" aria-label="Listado de suscripciones" aria-busy={subscriptionsQuery.isFetching}>
      <div className="subscription-results-heading"><div><h2>Listado general</h2><p>{typeof data?.totalElements === 'number' ? `${new Intl.NumberFormat('es-AR').format(data.totalElements)} resultados` : 'Suscripciones de los OWNER'}</p></div>{subscriptionsQuery.isFetching && data && <span role="status"><LoaderCircle size={14} className="spin" /> Actualizando</span>}</div>
      {subscriptionsQuery.isError && !data ? <div className="subscription-state subscription-error" role="alert"><p>{getAdminSubscriptionsErrorMessage(subscriptionsQuery.error)}</p><button className="owners-secondary-button" type="button" onClick={() => subscriptionsQuery.refetch()}><RefreshCw size={14} /> Reintentar</button></div>
        : !data ? <div className="subscription-state" role="status"><LoaderCircle size={18} className="spin" /> Cargando suscripciones…</div>
          : <>
            {subscriptionsQuery.isError && <p className="subscription-inline-error" role="alert">No se pudo actualizar la lista. {getAdminSubscriptionsErrorMessage(subscriptionsQuery.error)}</p>}
            {subscriptions.length ? <div className="subscription-table-wrap"><table className="subscription-table"><thead><tr><th>OWNER</th><th>Plan contratado</th><th>Estado y vigencia</th><th>Período</th><th>Capacidad</th><th>Acceso</th></tr></thead><tbody>
              {subscriptions.map(subscription => {
                const remaining = remainingDays(subscription.fechaVencimiento)
                const overdue = ['ACTIVE', 'TRIAL'].includes(subscription.status) && remaining !== null && remaining < 0
                return <tr key={subscription.suscripcionId}>
                  <td data-label="OWNER"><strong>{subscription.nombreOwner || `OWNER #${subscription.ownerId}`}</strong><span>{subscription.emailOwner || 'Sin email'}</span></td>
                  <td data-label="Plan contratado"><strong>{subscription.nombrePlan || 'Plan sin nombre'}</strong><span>{subscription.precioContratado == null ? '—' : MONEY.format(Number(subscription.precioContratado))}</span></td>
                  <td data-label="Estado y vigencia"><span className={`subscription-status status-${String(subscription.status).toLowerCase()}`}>{STATUS_LABELS[subscription.status] || subscription.status}</span>{overdue ? <small className="subscription-expiry is-overdue">Vencida hace {Math.abs(remaining)} {Math.abs(remaining) === 1 ? 'día' : 'días'} · estado {subscription.status}</small> : remaining === 0 ? <small className="subscription-expiry is-today">Vence hoy</small> : remaining > 0 && ['ACTIVE', 'TRIAL'].includes(subscription.status) ? <small className="subscription-expiry">{remaining} {remaining === 1 ? 'día restante' : 'días restantes'}</small> : null}</td>
                  <td data-label="Período"><span>Inicio {formatDate(subscription.fechaInicio)}</span><small>Vence {formatDate(subscription.fechaVencimiento)}</small></td>
                  <td data-label="Capacidad"><strong><Building2 size={14} /> {subscription.cantidadNegocios} de {subscription.maxNegocios ?? '—'}</strong><small>{subscription.capacidadDisponible ?? 0} disponibles</small></td>
                  <td data-label="Acceso"><div className="subscription-row-actions"><button type="button" className="owners-secondary-button" onClick={() => setManagedSubscription(subscription)}>Gestionar</button><Link className="subscription-owner-link" to={`/admin/owners/${subscription.ownerId}`} title="Abrir detalle del OWNER" aria-label={`Abrir detalle del OWNER ${subscription.nombreOwner || subscription.ownerId}`}><UserRound size={15} aria-hidden="true" /></Link></div></td>
                </tr>
              })}
            </tbody></table></div> : <div className="subscription-state"><ShieldCheck size={21} /><p>No hay suscripciones que coincidan con estos filtros.</p></div>}
            {subscriptions.length > 0 && <nav className="subscription-pagination" aria-label="Paginación de suscripciones"><span>{data.number * data.size + 1}–{Math.min((data.number + 1) * data.size, data.totalElements ?? (data.number + 1) * data.size)} de {data.totalElements ?? '—'}</span><div><button type="button" className="owners-secondary-button" onClick={() => setPage(current => Math.max(0, current - 1))} disabled={page === 0 || subscriptionsQuery.isFetching}>Anterior</button><span>Página {page + 1}{data.totalPages ? ` de ${data.totalPages}` : ''}</span><button type="button" className="owners-secondary-button" onClick={() => setPage(current => current + 1)} disabled={subscriptionsQuery.isFetching || (data.totalPages ? page + 1 >= data.totalPages : data.last)}>Siguiente</button></div></nav>}
          </>}
    </section>

    {managedSubscription && <div className="subscription-dialog-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) setManagedSubscription(null) }}>
      <section className="subscription-dialog" role="dialog" aria-modal="true" aria-labelledby="subscription-dialog-title">
        <header><div><span>GESTIÓN ADMINISTRATIVA</span><h2 id="subscription-dialog-title">{managedSubscription.nombreOwner || 'Suscripción del OWNER'}</h2><p>{managedSubscription.emailOwner}</p></div><button type="button" className="icon-button" onClick={() => setManagedSubscription(null)} aria-label="Cerrar gestión"><X size={17} /></button></header>
        <OwnerSubscriptionManagement owner={{ ownerId: managedSubscription.ownerId, nombre: managedSubscription.nombreOwner, email: managedSubscription.emailOwner, suscripcion: managedSubscription }} />
        <footer><Link className="owners-secondary-button" to={`/admin/owners/${managedSubscription.ownerId}`}>Abrir detalle del OWNER</Link><button type="button" className="owners-secondary-button" onClick={() => setManagedSubscription(null)}>Cerrar</button></footer>
      </section>
    </div>}
  </section>
}
