const roleLabels = { SUPER_ADMIN: 'Super administrador', OWNER: 'Owner', EMPLOYEE: 'Empleado', CLIENT: 'Cliente' }
const statusLabels = { ACTIVE: 'Activo', INACTIVE: 'Inactivo', BLOCKED: 'Bloqueado' }

export default function UsersTable({ users }) {
  return (
    <div className="owners-table-wrap">
      <table className="owners-table users-table">
        <thead><tr><th scope="col">ID</th><th scope="col">Nombre</th><th scope="col">Apellido</th><th scope="col">Email</th><th scope="col">DNI</th><th scope="col">Rol</th><th scope="col">Estado</th></tr></thead>
        <tbody>{users.map((user) => (
          <tr key={user.usuarioId}>
            <td data-label="ID">{user.usuarioId}</td>
            <td data-label="Nombre">{user.nombre}</td>
            <td data-label="Apellido">{user.apellido}</td>
            <td data-label="Email" className="owner-email">{user.email}</td>
            <td data-label="DNI">{user.dni}</td>
            <td data-label="Rol"><span className="user-role-label">{roleLabels[user.rol] || user.rol}</span></td>
            <td data-label="Estado"><span className={`owner-status owner-status-${String(user.status).toLowerCase()}`}>{statusLabels[user.status] || user.status || 'Sin estado'}</span></td>
          </tr>
        ))}</tbody>
      </table>
    </div>
  )
}
