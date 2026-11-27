import { useState } from 'react'
import { Building2, CircleAlert, LoaderCircle, RefreshCw, Wallet } from 'lucide-react'
import { useActiveOwnerBusiness } from '../context/OwnerBusinessContext.jsx'
import { getOwnerRemuneracionesErrorMessage } from '../api/ownerRemuneracionesApi.js'
import { useOwnerRemuneraciones, useOwnerRemuneracionesResumen } from '../hooks/useOwnerRemuneraciones.js'
import './OwnerRemuneracionesPage.css'

const pageSize = 20
const tabs = [['resumen', 'Resumen'], ['remuneraciones', 'Remuneraciones'], ['liquidaciones', 'Liquidaciones']]
const remunerationStates = [
  ['PENDIENTE_DEFINICION_FINANCIERA', 'Pendiente de definición financiera'],
  ['PENDIENTE_REVISION_MANUAL', 'Pendiente de revisión manual'],
  ['CONFIRMADA', 'Confirmada'],
  ['SIN_REMUNERACION', 'Sin remuneración'],
]
const remunerationLabels = Object.fromEntries(remunerationStates)
const paymentLabels = { PENDIENTE: 'Pendiente de pago', PARCIAL: 'Parcialmente pagada', PAGADA: 'Totalmente pagada', NO_PAGABLE: 'No pagable' }

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

function StateMessage({ error, loading, empty, onRetry, retrying }) {
  if (error) return <div className="owner-remuneraciones-state is-error" role="alert"><CircleAlert size={17} /><span>{error}</span>{onRetry && <button type="button" className="owners-secondary-button" onClick={onRetry} disabled={retrying}><RefreshCw size={14} /> Reintentar</button>}</div>
  if (loading) return <div className="owner-remuneraciones-state" role="status"><LoaderCircle size={18} className="spin" /> Cargando…</div>
  return <div className="owner-remuneraciones-state">{empty}</div>
}

export default function OwnerRemuneracionesPage() {
  const { activeBusiness } = useActiveOwnerBusiness()
  const negocioId = String(activeBusiness?.negocioId || '')

  if (!negocioId) return <><span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span><div className="owner-remuneraciones-state"><Building2 size={20} /> Seleccioná un negocio para consultar sus remuneraciones.</div></>
  return <OwnerRemuneracionesContent key={negocioId} negocioId={negocioId} negocioNombre={activeBusiness?.nombre} />
}

function OwnerRemuneracionesContent({ negocioId, negocioNombre }) {
  const [tab, setTab] = useState('resumen')
  const [empleadoId, setEmpleadoId] = useState('')
  const [estado, setEstado] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [page, setPage] = useState(0)
  const summaryQuery = useOwnerRemuneracionesResumen(negocioId)
  const summary = Array.isArray(summaryQuery.data) ? summaryQuery.data : []
  const dateRangeInvalid = Boolean(desde && hasta && desde > hasta)
  const listQuery = useOwnerRemuneraciones({ negocioId, empleadoId, estado, desde, hasta, page, size: pageSize }, { enabled: tab === 'remuneraciones' && !dateRangeInvalid })
  const result = listQuery.data
  const remuneraciones = Array.isArray(result?.content) ? result.content : []
  const currentPage = typeof result?.number === 'number' ? result.number : page
  const totalPages = typeof result?.totalPages === 'number' ? result.totalPages : null

  function updateFilter(setter, value) {
    setter(value)
    setPage(0)
  }

  function filterEmployee(id) {
    setEmpleadoId(String(id))
    setPage(0)
    setTab('remuneraciones')
  }

  return <>
    <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO OWNER</span>
    <div className="owners-heading"><div><h1>Remuneraciones</h1><p className="owners-description">Consultá las remuneraciones generadas por los turnos de {negocioNombre || 'el negocio seleccionado'}.</p></div></div>

    <nav className="owner-remuneraciones-tabs" aria-label="Vistas de remuneraciones" role="tablist">
      {tabs.map(([value, label]) => <button key={value} type="button" role="tab" aria-selected={tab === value} className={tab === value ? 'is-active' : ''} onClick={() => setTab(value)}>{label}</button>)}
    </nav>

    {tab === 'resumen' && <section className="owner-remuneraciones-panel" aria-label="Resumen de remuneraciones por empleado">
      <div className="owner-remuneraciones-panel-heading"><div><h2>Resumen por empleado</h2><p>Importes y cantidades informados por el sistema para el negocio activo.</p></div></div>
      {summaryQuery.isError ? <StateMessage error={getOwnerRemuneracionesErrorMessage(summaryQuery.error)} onRetry={() => void summaryQuery.refetch()} retrying={summaryQuery.isFetching} />
        : summaryQuery.isPending ? <StateMessage loading />
          : summary.length === 0 ? <StateMessage empty="Todavía no hay empleados ni remuneraciones para este negocio." />
            : <div className="owner-remuneraciones-summary-grid">{summary.map((employee) => <article className="owner-remuneraciones-employee" key={employee.empleadoId}>
              <div className="owner-remuneraciones-employee-heading"><Wallet size={17} /><h3>{employee.nombre || `Empleado #${employee.empleadoId}`}</h3></div>
              <dl className="owner-remuneraciones-totals">
                <div><dt>Total confirmado</dt><dd>{money(employee.totalConfirmado)}</dd></div>
                <div><dt>Total pagado</dt><dd>{money(employee.totalPagado)}</dd></div>
                <div className="is-balance"><dt>Total pendiente</dt><dd>{money(employee.totalPendiente)}</dd></div>
              </dl>
              <div className="owner-remuneraciones-counts">
                <span>Pendientes <b>{employee.pendientes}</b></span><span>Parciales <b>{employee.parcialmentePagadas}</b></span><span>Pagadas <b>{employee.totalmentePagadas}</b></span>
                <span>Revisión manual <b>{employee.pendientesRevisionManual}</b></span><span>Definición financiera <b>{employee.pendientesDefinicionFinanciera}</b></span>
              </div>
              <button type="button" className="owners-secondary-button" onClick={() => filterEmployee(employee.empleadoId)}>Ver remuneraciones</button>
            </article>)}</div>}
    </section>}

    {tab === 'remuneraciones' && <section className="owner-remuneraciones-panel" aria-label="Listado de remuneraciones">
      <div className="owner-remuneraciones-panel-heading"><div><h2>Remuneraciones</h2><p>El período se filtra según la fecha de inicio del turno.</p></div></div>
      <div className="owner-remuneraciones-filters">
        <label className="owner-form-field"><span>Empleado</span><select value={empleadoId} onChange={(event) => updateFilter(setEmpleadoId, event.target.value)}><option value="">Todos los empleados</option>{summary.map((employee) => <option key={employee.empleadoId} value={String(employee.empleadoId)}>{employee.nombre || `Empleado #${employee.empleadoId}`}</option>)}</select></label>
        <label className="owner-form-field"><span>Estado</span><select value={estado} onChange={(event) => updateFilter(setEstado, event.target.value)}><option value="">Todos los estados</option>{remunerationStates.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label className="owner-form-field"><span>Desde</span><input type="date" value={desde} max={hasta || undefined} onChange={(event) => updateFilter(setDesde, event.target.value)} /></label>
        <label className="owner-form-field"><span>Hasta</span><input type="date" value={hasta} min={desde || undefined} onChange={(event) => updateFilter(setHasta, event.target.value)} /></label>
      </div>
      <div className="owner-remuneraciones-results" aria-busy={listQuery.isFetching}>
        {dateRangeInvalid ? <StateMessage empty="Corregí el rango de fechas para consultar las remuneraciones." />
          : listQuery.isError ? <StateMessage error={getOwnerRemuneracionesErrorMessage(listQuery.error)} onRetry={() => void listQuery.refetch()} retrying={listQuery.isFetching} />
            : listQuery.isPending ? <StateMessage loading />
              : remuneraciones.length === 0 ? <StateMessage empty="No hay remuneraciones para los filtros seleccionados." />
                : <>
                  <div className="owner-remuneraciones-result-count">{typeof result?.totalElements === 'number' ? `${result.totalElements} resultados` : ''}</div>
                  <div className="owner-remuneraciones-table-wrap"><table className="owners-table owner-remuneraciones-table"><thead><tr><th>Turno</th><th>Empleado</th><th>Servicio</th><th>Cliente</th><th>Inicio del turno</th><th>Remuneración</th><th>Pagado</th><th>Saldo pendiente</th><th>Estado</th><th>Estado de pago</th></tr></thead><tbody>{remuneraciones.map((item) => <tr key={item.remuneracionId}>
                    <td data-label="Turno">#{item.turnoId ?? '—'}</td><td data-label="Empleado">{summary.find((employee) => String(employee.empleadoId) === String(item.empleadoId))?.nombre || `#${item.empleadoId}`}</td><td data-label="Servicio">{item.servicioNombre || '—'}</td><td data-label="Cliente">{item.clienteNombre || '—'}</td><td data-label="Inicio del turno">{dateTime(item.fechaHoraInicio)}</td><td data-label="Remuneración" className="owner-remuneraciones-money">{money(item.importeConfirmado)}</td><td data-label="Pagado" className="owner-remuneraciones-money">{money(item.totalPagado)}</td><td data-label="Saldo pendiente" className="owner-remuneraciones-money">{money(item.saldoPendiente)}</td>
                    <td data-label="Estado"><span className={`owner-remuneraciones-status state-${String(item.estado || '').toLowerCase()}`}>{remunerationLabels[item.estado] || item.estado || '—'}</span></td><td data-label="Estado de pago"><span className={`owner-remuneraciones-status payment-${String(item.estadoPago || '').toLowerCase()}`}>{paymentLabels[item.estadoPago] || item.estadoPago || '—'}</span></td>
                  </tr>)}</tbody></table></div>
                  <nav className="owner-remuneraciones-pagination" aria-label="Paginación de remuneraciones"><span>{typeof result?.totalElements === 'number' ? `${result.totalElements} resultados` : ''}</span><div><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={currentPage === 0 || listQuery.isFetching}>Anterior</button><span>{totalPages != null ? `Página ${currentPage + 1} de ${totalPages}` : `Página ${currentPage + 1}`}</span><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => current + 1)} disabled={listQuery.isFetching || (totalPages != null ? currentPage + 1 >= totalPages : remuneraciones.length < pageSize)}>Siguiente</button></div></nav>
                </>}
      </div>
    </section>}

    {tab === 'liquidaciones' && <section className="owner-remuneraciones-panel owner-remuneraciones-pending" aria-label="Liquidaciones pendientes"><Wallet size={22} /><h2>Liquidaciones</h2><p>Esta vista estará disponible en una próxima etapa.</p></section>}
  </>
}
