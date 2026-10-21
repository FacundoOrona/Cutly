import { LoaderCircle, Power, PowerOff, ShieldAlert } from 'lucide-react'

const statusLabels = { ACTIVE: 'Activo', INACTIVE: 'Inactivo', SUSPENDED: 'Suspendido' }

export default function BusinessesTable({ businesses, pendingBusinessId, onChangeStatus }) {
  return (
    <div className="owners-table-wrap">
      <table className="owners-table businesses-table">
        <thead><tr><th scope="col">Nombre</th><th scope="col">Email</th><th scope="col">Teléfono</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
        <tbody>{businesses.map((business) => (
          <tr key={business.negocioId}>
            <td data-label="Nombre">{business.nombre}</td>
            <td data-label="Email" className="owner-email">{business.email || '—'}</td>
            <td data-label="Teléfono">{business.telefono || '—'}</td>
            <td data-label="Estado"><span className={`owner-status owner-status-${String(business.status).toLowerCase()}`}>{statusLabels[business.status] || business.status || 'Sin estado'}</span></td>
            <td data-label="Acciones" className="owner-row-actions businesses-row-actions">
              {business.status === 'ACTIVE' && <>
                <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(business, 'suspend')} disabled={pendingBusinessId === business.negocioId} aria-label={`Suspender el negocio ${business.nombre}`}>
                  {pendingBusinessId === business.negocioId ? <LoaderCircle size={14} className="spin" /> : <ShieldAlert size={14} />} Suspender
                </button>
                <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(business, 'deactivate')} disabled={pendingBusinessId === business.negocioId} aria-label={`Desactivar el negocio ${business.nombre}`}>
                  {pendingBusinessId === business.negocioId ? <LoaderCircle size={14} className="spin" /> : <PowerOff size={14} />} Desactivar
                </button>
              </>}
              {business.status === 'SUSPENDED' && <>
                <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(business, 'reactivate')} disabled={pendingBusinessId === business.negocioId} aria-label={`Reactivar el negocio ${business.nombre}`}>
                  {pendingBusinessId === business.negocioId ? <LoaderCircle size={14} className="spin" /> : <Power size={14} />} Reactivar
                </button>
                <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(business, 'deactivate')} disabled={pendingBusinessId === business.negocioId} aria-label={`Desactivar el negocio ${business.nombre}`}>
                  {pendingBusinessId === business.negocioId ? <LoaderCircle size={14} className="spin" /> : <PowerOff size={14} />} Desactivar
                </button>
              </>}
              {business.status === 'INACTIVE' && <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(business, 'reactivate')} disabled={pendingBusinessId === business.negocioId} aria-label={`Reactivar el negocio ${business.nombre}`}>
                {pendingBusinessId === business.negocioId ? <LoaderCircle size={14} className="spin" /> : <Power size={14} />} Reactivar
              </button>}
            </td>
          </tr>
        ))}</tbody>
      </table>
    </div>
  )
}
