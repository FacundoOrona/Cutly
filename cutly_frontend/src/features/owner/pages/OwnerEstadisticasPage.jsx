import { useMemo, useState } from 'react'
import { BarChart3, Building2, CalendarDays, CircleAlert, CreditCard, LoaderCircle, RefreshCw, Wallet } from 'lucide-react'
import { useActiveOwnerBusiness } from '../context/OwnerBusinessContext.jsx'
import { getOwnerEstadisticasErrorMessage } from '../api/ownerEstadisticasApi.js'
import {
  useOwnerEstadisticasEmpleados,
  useOwnerEstadisticasResumen,
  useOwnerEstadisticasServicios,
  useOwnerEstadisticasTurnos,
} from '../hooks/useOwnerEstadisticas.js'
import './OwnerEstadisticasPage.css'

const periods = [
  ['today', 'Hoy'], ['7days', 'Últimos 7 días'], ['30days', 'Últimos 30 días'],
  ['thisMonth', 'Este mes'], ['lastMonth', 'Mes anterior'], ['custom', 'Personalizado'],
]

function localDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function shiftedDate(date, days) {
  const result = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  result.setDate(result.getDate() + days)
  return result
}

function datesForPeriod(period, now = new Date()) {
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  if (period === 'today') return { desde: localDate(today), hasta: localDate(today) }
  if (period === '7days') return { desde: localDate(shiftedDate(today, -6)), hasta: localDate(today) }
  if (period === '30days') return { desde: localDate(shiftedDate(today, -29)), hasta: localDate(today) }
  if (period === 'lastMonth') return {
    desde: localDate(new Date(today.getFullYear(), today.getMonth() - 1, 1)),
    hasta: localDate(new Date(today.getFullYear(), today.getMonth(), 0)),
  }
  return { desde: localDate(new Date(today.getFullYear(), today.getMonth(), 1)), hasta: localDate(today) }
}

function dateLabel(value) {
  if (!value) return '—'
  const [year, month, day] = value.split('-').map(Number)
  return new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'short', year: 'numeric' })
    .format(new Date(year, month - 1, day))
}

function money(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 2 }).format(Number(value))
}

function number(value) {
  return new Intl.NumberFormat('es-AR').format(Number(value) || 0)
}

function MetricCard({ icon: Icon, label, value, detail, prominent = false }) {
  return <article className={`owner-stats-metric${prominent ? ' owner-stats-metric-primary' : ''}`}>
    <span className="owner-stats-metric-icon"><Icon size={17} /></span>
    <span className="owner-stats-metric-label">{label}</span>
    <strong>{value}</strong>
    {detail && <small>{detail}</small>}
  </article>
}

function SectionState({ query, empty, onRetry, disabled = false }) {
  if (disabled) return null
  if (query.isPending) return <div className="owner-stats-state" role="status"><LoaderCircle size={17} className="spin" /> Cargando…</div>
  if (query.isError) return <div className="owner-stats-state owner-stats-state-error" role="alert"><CircleAlert size={17} /><span>{getOwnerEstadisticasErrorMessage(query.error)}</span><button className="owners-secondary-button" type="button" onClick={onRetry}><RefreshCw size={14} /> Reintentar</button></div>
  if (empty) return <div className="owner-stats-state">{empty}</div>
  return null
}

const turnStatuses = [
  ['pendientes', 'Pendientes'], ['confirmados', 'Confirmados'], ['enCurso', 'En curso'],
  ['completados', 'Completados'], ['cancelados', 'Cancelados'], ['ausencias', 'Ausencias'], ['rechazados', 'Rechazados'],
]

function Distribution({ query, onRetry, disabled }) {
  const values = turnStatuses.map(([key, label]) => ({ label, value: Number(query.data?.[key]) || 0 }))
  const max = Math.max(1, ...values.map((item) => item.value))
  return <>
    <SectionState query={query} onRetry={onRetry} disabled={disabled} />
    {!disabled && !query.isPending && !query.isError && <div className="owner-stats-bars" aria-label="Cantidad de turnos por estado">
      {values.map(({ label, value }) => <div className="owner-stats-bar-row" key={label}>
        <span>{label}</span><div className="owner-stats-bar-track"><span style={{ width: `${value ? Math.max(4, value / max * 100) : 0}%` }} /></div><strong>{number(value)}</strong>
      </div>)}
      {!values.some((item) => item.value > 0) && <p className="owner-stats-empty-note">No hay turnos registrados en este período.</p>}
    </div>}
  </>
}

function Ranking({ query, rows, nameOf, onRetry, empty, className = '', disabled }) {
  const max = Math.max(1, ...rows.map((row) => Number(row.totalTurnos) || 0))
  return <>
    <SectionState query={query} empty={!query.isPending && !query.isError && rows.length === 0 ? empty : ''} onRetry={onRetry} disabled={disabled} />
    {!disabled && !query.isPending && !query.isError && rows.length > 0 && <div className={`owner-stats-ranking ${className}`}>
      {rows.map((row) => <article className="owner-stats-rank-row" key={row.empleadoId ?? row.servicioId}>
        <div className="owner-stats-rank-heading"><strong>{nameOf(row)}</strong><span>{number(row.totalTurnos)} turnos</span></div>
        <div className="owner-stats-rank-track"><span style={{ width: `${Math.max(4, (Number(row.totalTurnos) || 0) / max * 100)}%` }} /></div>
        <div className="owner-stats-rank-details"><span>{number(row.turnosCompletados)} completados</span><span>{number(row.turnosCancelados)} cancelados</span></div>
      </article>)}
    </div>}
  </>
}

export default function OwnerEstadisticasPage() {
  const { activeBusiness, businessesQuery } = useActiveOwnerBusiness()
  const [period, setPeriod] = useState('thisMonth')
  const [customDates, setCustomDates] = useState(() => datesForPeriod('thisMonth'))
  const presetDates = useMemo(() => datesForPeriod(period), [period])
  const dates = period === 'custom' ? customDates : presetDates
  const invalidDates = Boolean(dates.desde && dates.hasta && dates.desde > dates.hasta)
  const filters = { negocioId: String(activeBusiness?.negocioId || ''), ...dates }
  const options = { enabled: !invalidDates }
  const resumen = useOwnerEstadisticasResumen(filters, options)
  const turnos = useOwnerEstadisticasTurnos(filters, options)
  const empleadosQuery = useOwnerEstadisticasEmpleados(filters, options)
  const serviciosQuery = useOwnerEstadisticasServicios(filters, options)
  const empleados = useMemo(() => [...(Array.isArray(empleadosQuery.data) ? empleadosQuery.data : [])]
    .sort((a, b) => (Number(b.totalTurnos) || 0) - (Number(a.totalTurnos) || 0)), [empleadosQuery.data])
  const servicios = useMemo(() => [...(Array.isArray(serviciosQuery.data) ? serviciosQuery.data : [])]
    .sort((a, b) => (Number(b.totalTurnos) || 0) - (Number(a.totalTurnos) || 0)), [serviciosQuery.data])
  const queries = [resumen, turnos, empleadosQuery, serviciosQuery]
  const retryAll = () => queries.filter((query) => query.isError).forEach((query) => void query.refetch())
  const selectedLabel = periods.find(([value]) => value === period)?.[1]
  const summary = resumen.data
  const hasBusinesses = Array.isArray(businessesQuery.data) && businessesQuery.data.length > 0

  function changePeriod(value) {
    setPeriod(value)
    if (value !== 'custom') setCustomDates(datesForPeriod(value))
  }

  return <main className="owner-stats-page">
    <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
    <div className="owners-heading owner-stats-heading"><div><h1>Estadísticas</h1><p className="owners-description">Una mirada clara a la actividad y las finanzas de tu negocio.</p></div>
      {activeBusiness && <span className="owner-stats-business"><Building2 size={15} /> {activeBusiness.nombre}</span>}
    </div>

    {businessesQuery.isLoading ? <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando negocios…</div>
      : businessesQuery.isError ? <div className="owner-business-state user-detail-error" role="alert"><p>No pudimos cargar tus negocios.</p><button type="button" className="owners-secondary-button" onClick={() => void businessesQuery.refetch()}><RefreshCw size={14} /> Reintentar</button></div>
        : !hasBusinesses ? <div className="owner-business-state"><Building2 size={22} /><p>Todavía no tenés negocios registrados.</p></div>
          : <>
            <section className="owner-client-card owner-stats-period" aria-label="Período de estadísticas">
              <div className="owner-stats-period-title"><span className="user-detail-icon"><CalendarDays size={17} /></span><div><h2>Período</h2><p>{selectedLabel}: {dateLabel(dates.desde)} – {dateLabel(dates.hasta)}</p></div></div>
              <div className="owner-stats-period-controls"><label className="owner-form-field"><span>Seleccionar período</span><select value={period} onChange={(event) => changePeriod(event.target.value)}>{periods.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                {period === 'custom' && <>
                  <label className="owner-form-field"><span>Desde</span><input type="date" value={customDates.desde} max={customDates.hasta || undefined} onChange={(event) => setCustomDates((current) => ({ ...current, desde: event.target.value }))} /></label>
                  <label className="owner-form-field"><span>Hasta</span><input type="date" value={customDates.hasta} min={customDates.desde || undefined} onChange={(event) => setCustomDates((current) => ({ ...current, hasta: event.target.value }))} /></label>
                </>}
              </div>
              {invalidDates && <p className="owner-stats-date-error" role="alert">La fecha “Desde” no puede ser posterior a “Hasta”.</p>}
            </section>

            <section className="owner-stats-group" aria-labelledby="owner-stats-activity-title">
              <div className="owner-stats-group-heading"><span className="user-detail-icon"><CalendarDays size={17} /></span><div><h2 id="owner-stats-activity-title">Actividad</h2><p>Turnos del período seleccionado</p></div></div>
              <div className="owner-stats-metrics owner-stats-activity-metrics">
                <MetricCard icon={CalendarDays} label="Turnos registrados" value={summary ? number(summary.totalTurnos) : '—'} />
                <MetricCard icon={BarChart3} label="Turnos completados" value={summary ? number(summary.turnosCompletados) : '—'} />
              </div>
              {(resumen.isPending || resumen.isError) && <SectionState query={resumen} onRetry={retryAll} disabled={invalidDates} />}
            </section>

            <section className="owner-stats-group" aria-labelledby="owner-stats-finance-title">
              <div className="owner-stats-group-heading"><span className="user-detail-icon"><Wallet size={17} /></span><div><h2 id="owner-stats-finance-title">Finanzas</h2><p>Importes efectivamente recibidos en el período</p></div></div>
              <div className="owner-stats-metrics owner-stats-finance-metrics">
                <MetricCard icon={Wallet} label="Ingresos recibidos" value={summary ? money(summary.ingresosTotales) : '—'} prominent />
                <MetricCard icon={CreditCard} label="Pagos recibidos" value={summary ? number(summary.cantidadPagos) : '—'} />
                <MetricCard icon={Wallet} label="Cobros directos" value={summary ? money(summary.ingresosCobrosDirectos) : '—'} />
                <MetricCard icon={CreditCard} label="Abonos de cuentas corrientes" value={summary ? money(summary.ingresosAbonos) : '—'} />
                <MetricCard icon={CalendarDays} label="Turnos gratuitos" value={summary ? number(summary.turnosGratuitos) : '—'} />
              </div>
            </section>

            <section className="owner-stats-group" aria-labelledby="owner-stats-debts-title">
              <div className="owner-stats-group-heading"><span className="user-detail-icon"><CreditCard size={17} /></span><div><h2 id="owner-stats-debts-title">Deudas y regularizaciones</h2><p>Deudas según su fecha de origen; saldo pendiente separado de los ingresos</p></div></div>
              <div className="owner-stats-metrics owner-stats-debt-metrics">
                <MetricCard icon={Wallet} label="Saldo de deuda pendiente" value={summary ? money(summary.deudaPendienteTotal) : '—'} />
                <MetricCard icon={CreditCard} label="Deudas pendientes" value={summary ? number(summary.deudasPendientes) : '—'} />
                <MetricCard icon={CreditCard} label="Deudas parciales" value={summary ? number(summary.deudasParciales) : '—'} />
                <MetricCard icon={CreditCard} label="Deudas pagadas" value={summary ? number(summary.deudasPagadas) : '—'} />
                <MetricCard icon={CalendarDays} label="Turnos por regularizar" value={summary ? number(summary.turnosPendientesRegularizacion) : '—'} />
              </div>
            </section>

            <div className="owner-stats-panels">
              <section className="owner-client-card owner-stats-panel" aria-labelledby="owner-stats-turnos-title">
                <div className="owner-client-card-heading"><span className="user-detail-icon"><BarChart3 size={17} /></span><div><h2 id="owner-stats-turnos-title">Turnos por estado</h2><p>Distribución de la actividad operativa</p></div></div>
                <Distribution query={turnos} onRetry={() => void turnos.refetch()} disabled={invalidDates} />
              </section>
              <section className="owner-client-card owner-stats-panel" aria-labelledby="owner-stats-employees-title">
                <div className="owner-client-card-heading"><span className="user-detail-icon"><Building2 size={17} /></span><div><h2 id="owner-stats-employees-title">Rendimiento de empleados</h2><p>Ordenados por cantidad de turnos</p></div></div>
                <Ranking query={empleadosQuery} rows={empleados} nameOf={(row) => [row.nombre, row.apellido].filter(Boolean).join(' ') || 'Empleado'} onRetry={() => void empleadosQuery.refetch()} empty="No hay turnos asociados a empleados en este período." disabled={invalidDates} />
              </section>
            </div>

            <section className="owner-client-card owner-stats-services" aria-labelledby="owner-stats-services-title">
              <div className="owner-client-card-heading"><span className="user-detail-icon"><BarChart3 size={17} /></span><div><h2 id="owner-stats-services-title">Servicios más solicitados</h2><p>Demanda y turnos completados o cancelados</p></div></div>
              <Ranking query={serviciosQuery} rows={servicios} nameOf={(row) => row.nombre || 'Servicio'} onRetry={() => void serviciosQuery.refetch()} empty="No hay turnos asociados a servicios en este período." className="owner-stats-service-ranking" disabled={invalidDates} />
            </section>
          </>}
  </main>
}
