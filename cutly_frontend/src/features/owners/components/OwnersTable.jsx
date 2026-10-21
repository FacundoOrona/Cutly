function statusLabel(status) {
  return { ACTIVE: 'Activo', INACTIVE: 'Inactivo', BLOCKED: 'Bloqueado' }[status] || status || 'Sin estado'
}

export default function OwnersTable({ owners }) {
  return (
    <div className="owners-table-wrap">
      <table className="owners-table">
        <thead><tr><th scope="col">ID</th><th scope="col">Nombre</th><th scope="col">Apellido</th><th scope="col">DNI</th><th scope="col">Email</th><th scope="col">Estado</th></tr></thead>
        <tbody>{owners.map((owner) => (
          <tr key={owner.usuarioId}>
            <td data-label="ID">{owner.usuarioId}</td><td data-label="Nombre">{owner.nombre}</td><td data-label="Apellido">{owner.apellido}</td><td data-label="DNI">{owner.dni}</td><td data-label="Email" className="owner-email">{owner.email}</td>
            <td data-label="Estado"><span className={`owner-status owner-status-${String(owner.status).toLowerCase()}`}>{statusLabel(owner.status)}</span></td>
          </tr>
        ))}</tbody>
      </table>
    </div>
  )
}
