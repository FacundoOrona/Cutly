import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Activity, Building2, CalendarDays, RefreshCw, ShieldCheck, UsersRound } from 'lucide-react'
import useAuth from '../auth/useAuth.js'
import apiClient from '../services/apiClient.js'
import './DashboardPage.css'

function dateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function defaultPeriod() {
  const hasta = new Date()
  const today = dateKey(hasta)
  return { desde: today, hasta: today }
}

function displayDate(value) {
  return value ? value.split('-').reverse().join('-') : ''
}

function number(value) {
  return new Intl.NumberFormat('es-AR').format(value ?? 0)
}

function Distribution({ title, values }) {
  const entries = Object.entries(values || {})
  const max = Math.max(0, ...entries.map(([, value]) => value))
  return <section className="admin-stats-panel">
    <h2>{title}</h2>
    {entries.length === 0 ? <p className="admin-stats-muted">Sin datos para mostrar.</p> : <ul className="admin-stats-distribution">
      {entries.map(([label, value]) => <li key={label}>
        <div><span>{label.replaceAll('_', ' ')}</span><strong>{number(value)}</strong></div>
        <span className="admin-stats-track"><span style={{ width: `${max ? (value / max) * 100 : 0}%` }} /></span>
      </li>)}
    </ul>}
  </section>
}

function DailyActivity({ values }) {
  const entries = Object.entries(values || {}).sort(([a], [b]) => a.localeCompare(b))
  const max = Math.max(0, ...entries.map(([, value]) => value))
  return <section className="admin-stats-panel admin-stats-daily">
    <div className="admin-stats-panel-heading"><h2>Turnos por día</h2><span>Período seleccionado</span></div>
    {entries.length === 0 || max === 0 ? <p className="admin-stats-muted">No hay turnos registrados en este período.</p> : <div className="admin-stats-chart" role="img" aria-label="Cantidad de turnos por día">
      {entries.map(([date, value]) => <div className="admin-stats-chart-column" key={date} title={`${date}: ${number(value)} turnos`}>
        <strong>{number(value)}</strong><span style={{ height: `${Math.max(5, (value / max) * 100)}%` }} /><small>{date.slice(5)}</small>
      </div>)}
    </div>}
  </section>
}

export default function DashboardPage() {
  const { user } = useAuth()
  const [initialPeriod] = useState(defaultPeriod)
  const [draft, setDraft] = useState(initialPeriod)
  const [period, setPeriod] = useState(initialPeriod)
  const query = useQuery({
    queryKey: ['admin', 'indicators', period.desde, period.hasta],
    queryFn: async () => {
      const { data } = await apiClient.get('/api/admin/estadisticas/indicadores', { params: period })
      return data
    },
    enabled: period.desde <= period.hasta,
  })

  const indicators = query.data
  const cards = indicators ? [
    { label: 'Propietarios', value: indicators.totalPropietarios, detail: `${number(indicators.propietariosActivos)} activos · ${number(indicators.propietariosInactivos)} inactivos · ${number(indicators.propietariosBloqueados)} bloqueados`, icon: UsersRound },
    { label: 'Negocios', value: indicators.totalNegocios, detail: `${number(indicators.negociosActivos)} activos · ${number(indicators.negociosInactivos)} inactivos · ${number(indicators.negociosSuspendidos)} suspendidos`, icon: Building2 },
    { label: 'Turnos en el período', value: indicators.turnosEnPeriodo, detail: `${number(indicators.negociosConActividad)} negocios con actividad`, icon: CalendarDays },
    { label: 'Suscripciones próximas a vencer', value: indicators.suscripcionesProximasAVencer, detail: `${number(indicators.suscripcionesVencidas)} vencidas`, icon: ShieldCheck },
  ] : []

  function updateDraft(field, value) {
    setDraft((current) => ({ ...current, [field]: value }))
  }

  function applyPeriod(event) {
    event.preventDefault()
    if (draft.desde && draft.hasta && draft.desde <= draft.hasta) setPeriod(draft)
  }

  return <section className="admin-dashboard" aria-labelledby="admin-dashboard-title">
    <header className="admin-dashboard-heading">
      <div><span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO ADMINISTRATIVO</span>
        <h1 id="admin-dashboard-title">Hola, <span>{user?.nombre || 'bienvenido'}.</span></h1>
        <p className="dashboard-lead">Indicadores generales de Cutly.</p>
      </div>
      <Activity className="admin-dashboard-icon" aria-hidden="true" />
    </header>

    <form className="admin-stats-period" onSubmit={applyPeriod}>
      <label>Desde<span className="admin-stats-date-input"><input type="date" value={draft.desde} max={draft.hasta} onChange={(event) => updateDraft('desde', event.target.value)} required /><span aria-hidden="true">{displayDate(draft.desde)}</span></span></label>
      <label>Hasta<span className="admin-stats-date-input"><input type="date" value={draft.hasta} min={draft.desde} onChange={(event) => updateDraft('hasta', event.target.value)} required /><span aria-hidden="true">{displayDate(draft.hasta)}</span></span></label>
      <button type="submit" disabled={!draft.desde || !draft.hasta || draft.desde > draft.hasta || query.isFetching}>
        <RefreshCw size={15} className={query.isFetching ? 'admin-stats-spinning' : ''} />
        {query.isFetching ? 'Actualizando…' : 'Aplicar período'}
      </button>
    </form>

    {query.isPending ? <div className="admin-stats-state" role="status">Cargando indicadores…</div>
      : query.isError ? <div className="admin-stats-state is-error" role="alert">
        <strong>No pudimos cargar los indicadores.</strong><p>Revisá el período o intentá nuevamente.</p>
        <button type="button" onClick={() => query.refetch()} disabled={query.isFetching}>Reintentar</button>
      </div>
        : indicators && <>
          <p className="admin-stats-period-caption">Del {indicators.desde} al {indicators.hasta}</p>
          <div className="admin-stats-cards">{cards.map(({ label, value, detail, icon: Icon }) => <article className="admin-stats-card" key={label}>
            <span className="admin-stats-card-icon"><Icon size={18} /></span><small>{label}</small><strong>{number(value)}</strong><span>{detail}</span>
          </article>)}</div>
          <p className="admin-stats-subscriptions-note">Negocios con suscripción vigente: <strong>{number(indicators.negociosConSuscripcionVigente)}</strong> · Sin suscripción habilitada: <strong>{number(indicators.negociosSinSuscripcionHabilitada)}</strong></p>
          <div className="admin-stats-grid">
            <Distribution title="Turnos por estado" values={indicators.turnosPorEstado} />
            <Distribution title="Suscripciones por estado" values={indicators.suscripcionesPorEstado} />
            <Distribution title="Owners por plan" values={indicators.suscripcionesPorPlan} />
            <DailyActivity values={indicators.turnosPorDia} />
          </div>
        </>}
  </section>
}
