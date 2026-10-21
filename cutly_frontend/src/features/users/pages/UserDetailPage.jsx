import { ArrowLeft, LoaderCircle, RefreshCw, UserRound } from 'lucide-react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { getUserErrorMessage } from '../api/usersApi.js'
import { useUser } from '../hooks/useUsers.js'

const roleLabels = { SUPER_ADMIN: 'Super administrador', OWNER: 'Owner', EMPLOYEE: 'Empleado', CLIENT: 'Cliente' }
const statusLabels = { ACTIVE: 'Activo', INACTIVE: 'Inactivo', BLOCKED: 'Bloqueado' }

function UserDetail({ user }) {
  const fields = [
    ['ID', user.usuarioId],
    ['Nombre', user.nombre],
    ['Apellido', user.apellido],
    ['Email', user.email],
    ['DNI', user.dni],
    ['Rol', roleLabels[user.rol] || user.rol],
    ['Estado', statusLabels[user.status] || user.status || 'Sin estado'],
  ]

  return (
    <dl className="user-detail-grid">
      {fields.map(([label, value]) => <div className="user-detail-field" key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}
    </dl>
  )
}

export default function UserDetailPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const query = useUser(id)
  const validId = typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))
  const returnState = location.state?.usersListState
  const ownerListState = location.state?.ownersListState
  const returnTo = location.state?.returnTo === '/admin/owners' ? '/admin/owners' : '/admin/users'

  function backToList() {
    navigate(returnTo, { state: returnState ? { usersListState: returnState } : ownerListState ? { ownersListState: ownerListState } : null })
  }

  return (
    <>
      <span className="eyebrow"><span className="eyebrow-dot" /> ADMINISTRACIÓN</span>
      <div className="user-detail-heading">
        <div><h1>Detalle de usuario</h1><p className="owners-description">Información básica de la cuenta registrada.</p></div>
        <button type="button" className="owners-secondary-button user-back-button" onClick={backToList}><ArrowLeft size={15} /> Volver al listado</button>
      </div>
      {!validId ? (
        <div className="user-detail-state" role="alert"><UserRound size={21} /><p>El identificador del usuario no es válido.</p></div>
      ) : query.isPending ? (
        <div className="user-detail-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando usuario…</div>
      ) : query.isError ? (
        <div className="user-detail-state user-detail-error" role="alert"><p>{getUserErrorMessage(query.error)}</p>{query.error.response?.status !== 404 && query.error.response?.status !== 400 && query.error.response?.status !== 401 && query.error.response?.status !== 403 && <button type="button" className="owners-secondary-button" onClick={() => query.refetch()}><RefreshCw size={15} /> Reintentar</button>}</div>
      ) : query.data ? (
        <section className="user-detail-card" aria-label="Datos del usuario">
          <div className="user-detail-card-heading"><span className="user-detail-icon"><UserRound size={18} /></span><div><h2>{[query.data.nombre, query.data.apellido].filter(Boolean).join(' ') || 'Usuario'}</h2><p>{query.data.email}</p></div></div>
          <UserDetail user={query.data} />
        </section>
      ) : (
        <div className="user-detail-state"><p>No hay datos disponibles para este usuario.</p><Link className="owners-secondary-button" to="/admin/users">Volver al listado</Link></div>
      )}
    </>
  )
}
