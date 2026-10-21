import { Ban, LoaderCircle, Power, PowerOff, Unlock } from 'lucide-react'
import { generatePath, Link } from 'react-router-dom'

const roleLabels = { SUPER_ADMIN: 'Super administrador', OWNER: 'Owner', EMPLOYEE: 'Empleado', CLIENT: 'Cliente' }
const statusLabels = { ACTIVE: 'Activo', INACTIVE: 'Inactivo', BLOCKED: 'Bloqueado' }

export default function UsersTable({ users, currentUserId, pendingUserId, onChangeStatus, usersListState }) {
  return (
    <div className="owners-table-wrap">
      <table className="owners-table users-table">
        <thead><tr><th scope="col">ID</th><th scope="col">Nombre</th><th scope="col">Apellido</th><th scope="col">Email</th><th scope="col">DNI</th><th scope="col">Rol</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
        <tbody>{users.map((user) => (
          <tr key={user.usuarioId}>
            <td data-label="ID">{user.usuarioId}</td>
            <td data-label="Nombre">{user.nombre}</td>
            <td data-label="Apellido">{user.apellido}</td>
            <td data-label="Email" className="owner-email">{user.email}</td>
            <td data-label="DNI">{user.dni}</td>
            <td data-label="Rol"><span className="user-role-label">{roleLabels[user.rol] || user.rol}</span></td>
            <td data-label="Estado"><span className={`owner-status owner-status-${String(user.status).toLowerCase()}`}>{statusLabels[user.status] || user.status || 'Sin estado'}</span></td>
            <td data-label="Acciones" className="owner-row-actions users-row-actions">
              {Number.isSafeInteger(Number(user.usuarioId)) && Number(user.usuarioId) > 0
                ? <Link className="owners-secondary-button owner-status-action" to={generatePath('/admin/users/:id', { id: String(user.usuarioId) })} state={{ usersListState }} aria-label={`Ver detalle de ${user.email}`}>Ver detalle</Link>
                : <span className="owner-action-note">ID no disponible</span>}
              {user.status === 'ACTIVE' && <>
                <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(user, 'deactivate')} disabled={pendingUserId === user.usuarioId || (user.rol === 'SUPER_ADMIN' && user.usuarioId === currentUserId)} aria-label={`Desactivar a ${user.email}`}>
                  {pendingUserId === user.usuarioId ? <LoaderCircle size={14} className="spin" /> : <PowerOff size={14} />} Desactivar
                </button>
                <button type="button" className="owners-secondary-button owner-status-action owner-block-action" onClick={() => onChangeStatus(user, 'block')} disabled={pendingUserId === user.usuarioId || (user.rol === 'SUPER_ADMIN' && user.usuarioId === currentUserId)} aria-label={`Bloquear a ${user.email}`}>
                  {pendingUserId === user.usuarioId ? <LoaderCircle size={14} className="spin" /> : <Ban size={14} />} Bloquear
                </button>
              </>}
              {user.status === 'INACTIVE' && <>
                <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(user, 'activate')} disabled={pendingUserId === user.usuarioId} aria-label={`Activar a ${user.email}`}>
                  {pendingUserId === user.usuarioId ? <LoaderCircle size={14} className="spin" /> : <Power size={14} />} Activar
                </button>
              </>}
              {user.status === 'BLOCKED' && <>
                <button type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(user, 'unblock')} disabled={pendingUserId === user.usuarioId} aria-label={`Desbloquear a ${user.email}`}>
                  {pendingUserId === user.usuarioId ? <LoaderCircle size={14} className="spin" /> : <Unlock size={14} />} Desbloquear
                </button>
              </>}
            </td>
          </tr>
        ))}</tbody>
      </table>
    </div>
  )
}
