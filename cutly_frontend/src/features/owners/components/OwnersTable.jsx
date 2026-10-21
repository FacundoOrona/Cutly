import { Ban, LoaderCircle, Power, PowerOff, Unlock } from 'lucide-react'
import { generatePath, Link } from 'react-router-dom'

function statusLabel(status) {
  return { ACTIVE: 'Activo', INACTIVE: 'Inactivo', BLOCKED: 'Bloqueado' }[status] || status || 'Sin estado'
}

export default function OwnersTable({ owners, pendingOwnerId, onChangeStatus, ownersListState }) {
  return (
    <div className="owners-table-wrap">
      <table className="owners-table">
        <thead><tr><th scope="col">ID</th><th scope="col">Nombre</th><th scope="col">Apellido</th><th scope="col">DNI</th><th scope="col">Email</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
        <tbody>{owners.map((owner) => (
          <tr key={owner.usuarioId}>
            <td data-label="ID">{owner.usuarioId}</td><td data-label="Nombre"><Link className="business-detail-link" to={generatePath('/admin/users/:id', { id: String(owner.usuarioId) })} state={{ returnTo: '/admin/owners', ownersListState }} aria-label={`Ver detalle de ${owner.nombre} ${owner.apellido}`}>{owner.nombre}</Link></td><td data-label="Apellido">{owner.apellido}</td><td data-label="DNI">{owner.dni}</td><td data-label="Email" className="owner-email">{owner.email}</td>
            <td data-label="Estado"><span className={`owner-status owner-status-${String(owner.status).toLowerCase()}`}>{statusLabel(owner.status)}</span></td>
            <td data-label="Acciones" className="owner-row-actions">
              {owner.status === 'ACTIVE' && <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(owner, 'deactivate')} disabled={pendingOwnerId === owner.usuarioId} aria-label={`Desactivar a ${owner.email}`}>
                {pendingOwnerId === owner.usuarioId ? <LoaderCircle size={14} className="spin" /> : <PowerOff size={14} />} Desactivar
              </button>}
              {owner.status === 'INACTIVE' && <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(owner, 'activate')} disabled={pendingOwnerId === owner.usuarioId} aria-label={`Activar a ${owner.email}`}>
                {pendingOwnerId === owner.usuarioId ? <LoaderCircle size={14} className="spin" /> : <Power size={14} />} Activar
              </button>}
              {owner.status === 'ACTIVE' && <button type="button" className="owners-secondary-button owner-status-action owner-block-action" onClick={() => onChangeStatus(owner, 'block')} disabled={pendingOwnerId === owner.usuarioId} aria-label={`Bloquear a ${owner.email}`}>
                {pendingOwnerId === owner.usuarioId ? <LoaderCircle size={14} className="spin" /> : <Ban size={14} />} Bloquear
              </button>}
              {owner.status === 'BLOCKED' && <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(owner, 'unblock')} disabled={pendingOwnerId === owner.usuarioId} aria-label={`Desbloquear a ${owner.email}`}>
                {pendingOwnerId === owner.usuarioId ? <LoaderCircle size={14} className="spin" /> : <Unlock size={14} />} Desbloquear
              </button>}
            </td>
          </tr>
        ))}</tbody>
      </table>
    </div>
  )
}
