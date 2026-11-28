import { useState } from 'react'
import { CircleAlert, Eye, LoaderCircle, RefreshCw } from 'lucide-react'
import { getOwnerLiquidacionesErrorMessage } from '../api/ownerRemuneracionesApi.js'
import { useOwnerLiquidaciones } from '../hooks/useOwnerRemuneraciones.js'
import OwnerLiquidacionDetailDrawer from './OwnerLiquidacionDetailDrawer.jsx'

const pageSize = 20

function money(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value))
}

function dateOnly(value) {
  if (!value) return '—'
  const parsed = new Date(`${value}T12:00:00`)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium' }).format(parsed)
}

const methodLabels = {
  EFECTIVO: 'Efectivo', TRANSFERENCIA_QR: 'Transferencia / QR', DEBITO: 'Débito', CREDITO: 'Crédito', OTRO: 'Otro',
  CASH: 'Efectivo (histórico)', DEBIT_CARD: 'Débito (histórico)', CREDIT_CARD: 'Crédito (histórico)',
  TRANSFER: 'Transferencia (histórico)', MERCADO_PAGO: 'Mercado Pago (histórico)',
}

export default function OwnerLiquidacionesSection({ negocioId, empleados }) {
  const [empleadoId, setEmpleadoId] = useState('')
  const [page, setPage] = useState(0)
  const [liquidacionId, setLiquidacionId] = useState(null)
  const query = useOwnerLiquidaciones({ negocioId, empleadoId, page, size: pageSize })
  const result = query.data
  const liquidaciones = Array.isArray(result?.content) ? result.content : []
  const currentPage = typeof result?.number === 'number' ? result.number : page
  const totalPages = typeof result?.totalPages === 'number' ? result.totalPages : null

  return <section className="owner-remuneraciones-panel" aria-label="Historial de liquidaciones">
    <div className="owner-remuneraciones-panel-heading"><div><h2>Historial de liquidaciones</h2><p>Consultá los pagos registrados para los empleados del negocio.</p></div></div>
    <div className="owner-liquidaciones-filter"><label className="owner-form-field"><span>Empleado</span><select value={empleadoId} onChange={(event) => { setEmpleadoId(event.target.value); setPage(0) }}><option value="">Todos los empleados</option>{empleados.map((employee) => <option key={employee.empleadoId} value={String(employee.empleadoId)}>{employee.nombre || `Empleado #${employee.empleadoId}`}</option>)}</select></label></div>
    <div className="owner-remuneraciones-results" aria-busy={query.isFetching}>
      {query.isError ? <div className="owner-remuneraciones-state is-error" role="alert"><CircleAlert size={17} /><span>{getOwnerLiquidacionesErrorMessage(query.error)}</span><button type="button" className="owners-secondary-button" onClick={() => void query.refetch()} disabled={query.isFetching}><RefreshCw size={14} /> Reintentar</button></div>
        : query.isPending ? <div className="owner-remuneraciones-state" role="status"><LoaderCircle size={18} className="spin" /> Cargando liquidaciones…</div>
          : liquidaciones.length === 0 ? <div className="owner-remuneraciones-state">{empleadoId ? 'Este empleado todavía no tiene liquidaciones registradas.' : 'Todavía no hay liquidaciones registradas para este negocio.'}</div>
            : <>
              <div className="owner-remuneraciones-result-count">{typeof result?.totalElements === 'number' ? `${result.totalElements} resultados` : ''}</div>
              <div className="owner-remuneraciones-table-wrap"><table className="owners-table owner-liquidaciones-table"><thead><tr><th>Fecha efectiva</th><th>Empleado</th><th>Importe total</th><th>Medio de pago</th><th>Estado</th><th>Detalle</th></tr></thead><tbody>{liquidaciones.map((liquidacion) => <tr key={liquidacion.liquidacionId}>
                <td data-label="Fecha efectiva">{dateOnly(liquidacion.fechaEfectiva)}</td>
                <td data-label="Empleado">{liquidacion.empleadoNombre || `#${liquidacion.empleadoId}`}</td>
                <td data-label="Importe total" className="owner-remuneraciones-money">{money(liquidacion.importeTotal)}</td>
                <td data-label="Medio de pago">{methodLabels[liquidacion.metodoPago] || liquidacion.metodoPago || '—'}{liquidacion.metodoPago === 'OTRO' && liquidacion.descripcionMetodo ? <small className="owner-liquidaciones-description">{liquidacion.descripcionMetodo}</small> : null}</td>
                <td data-label="Estado"><span className={`owner-remuneraciones-status ${liquidacion.estado === 'ANULADA' ? 'liquidacion-anulada' : 'liquidacion-vigente'}`}>{liquidacion.estado === 'ANULADA' ? 'Anulada' : liquidacion.estado === 'VIGENTE' ? 'Vigente' : liquidacion.estado || '—'}</span></td>
                <td data-label="Detalle"><button type="button" className="owners-secondary-button owner-remuneracion-detail-button" onClick={() => setLiquidacionId(liquidacion.liquidacionId)} aria-label={`Ver detalle de liquidación #${liquidacion.liquidacionId}`}><Eye size={14} /> Ver</button></td>
              </tr>)}</tbody></table></div>
              <nav className="owner-remuneraciones-pagination" aria-label="Paginación de liquidaciones"><span>{typeof result?.totalElements === 'number' ? `${result.totalElements} resultados` : ''}</span><div><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={currentPage === 0 || query.isFetching}>Anterior</button><span>{totalPages != null ? `Página ${currentPage + 1} de ${totalPages}` : `Página ${currentPage + 1}`}</span><button type="button" className="owners-secondary-button" onClick={() => setPage((current) => current + 1)} disabled={query.isFetching || (totalPages != null ? currentPage + 1 >= totalPages : liquidaciones.length < pageSize)}>Siguiente</button></div></nav>
            </>}
    </div>
    {liquidacionId != null && <OwnerLiquidacionDetailDrawer key={`${negocioId}-${liquidacionId}`} negocioId={negocioId} liquidacionId={liquidacionId} onClose={() => setLiquidacionId(null)} />}
  </section>
}
