import { Link } from 'react-router-dom'
import BusinessStatusActions from './BusinessStatusActions.jsx'

const statusLabels = { ACTIVE: 'Activo', INACTIVE: 'Inactivo', SUSPENDED: 'Suspendido' }

export default function BusinessesTable({ businesses, pendingBusinessId, onChangeStatus, businessesListState }) {
  return (
    <div className="owners-table-wrap">
      <table className="owners-table businesses-table">
        <thead><tr><th scope="col">Nombre</th><th scope="col">Owner</th><th scope="col">Email</th><th scope="col">Teléfono</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
        <tbody>{businesses.map((business) => (
          <tr key={business.negocioId}>
            <td data-label="Nombre"><Link className="business-detail-link" to={`/admin/businesses/${business.negocioId}`} state={{ businessesListState }}>{business.nombre}</Link></td>
            <td data-label="Owner">{[business.ownerNombre, business.ownerApellido].filter(Boolean).join(' ') || '—'}</td>
            <td data-label="Email" className="owner-email">{business.email || '—'}</td>
            <td data-label="Teléfono">{business.telefono || '—'}</td>
            <td data-label="Estado"><span className={`owner-status owner-status-${String(business.status).toLowerCase()}`}>{statusLabels[business.status] || business.status || 'Sin estado'}</span></td>
            <td data-label="Acciones" className="owner-row-actions"><BusinessStatusActions business={business} pending={pendingBusinessId === business.negocioId} onChangeStatus={onChangeStatus} /></td>
          </tr>
        ))}</tbody>
      </table>
    </div>
  )
}
