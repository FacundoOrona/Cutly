import { useEffect, useMemo, useState } from 'react'
import { LoaderCircle, RefreshCw, Users } from 'lucide-react'
import UserFilters from '../components/UserFilters.jsx'
import UsersTable from '../components/UsersTable.jsx'
import { getUsersErrorMessage } from '../api/usersApi.js'
import useUsers from '../hooks/useUsers.js'

const PAGE_SIZE = 20
const DEBOUNCE_MS = 350
const EMPTY_FILTERS = { nombre: '', apellido: '', email: '', dni: '', rol: '', status: '' }
const DEFAULT_SORT = { field: 'usuarioId', direction: 'DESC' }

export default function UsersPage() {
  const [filterInputs, setFilterInputs] = useState(EMPTY_FILTERS)
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [sort, setSort] = useState(DEFAULT_SORT)
  const [page, setPage] = useState(0)
  const params = useMemo(() => ({ filters, page, size: PAGE_SIZE, sort }), [filters, page, sort])
  const query = useUsers(params)

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setFilters((current) => {
        const next = Object.fromEntries(Object.entries(filterInputs).map(([key, value]) => [key, value.trim()]))
        if (Object.keys(next).every((key) => next[key] === current[key])) return current
        setPage(0)
        return next
      })
    }, DEBOUNCE_MS)
    return () => window.clearTimeout(timeout)
  }, [filterInputs])

  function changeFilter(name, value) {
    setFilterInputs((current) => ({ ...current, [name]: value }))
  }

  function changeSort(value) {
    const [field, direction] = value.split(',')
    setSort({ field, direction })
    setPage(0)
  }

  function clearFilters() {
    setFilterInputs(EMPTY_FILTERS)
    setFilters(EMPTY_FILTERS)
    setPage(0)
  }

  const data = query.data
  const users = data?.content || []
  const totalElements = data?.totalElements
  const totalPages = data?.totalPages || 0
  const hasFilters = Object.values(filterInputs).some(Boolean)

  return (
    <>
      <span className="eyebrow"><span className="eyebrow-dot" /> ADMINISTRACIÓN</span>
      <div className="owners-heading">
        <div><h1>Usuarios</h1><p className="owners-description">Consultá, filtrá y ordená las cuentas registradas en Cutly.</p></div>
        <div className="owners-count"><Users size={17} />{typeof totalElements === 'number' ? `${totalElements} ${totalElements === 1 ? 'usuario' : 'usuarios'}` : 'Listado'}</div>
      </div>
      <UserFilters values={filterInputs} sort={sort} onChange={changeFilter} onSortChange={changeSort} onClear={clearFilters} hasFilters={hasFilters} />
      <section className="owners-results" aria-label="Listado de usuarios" aria-busy={query.isFetching}>
        {query.isError && !data ? (
          <div className="owners-message owners-error" role="alert"><p>{getUsersErrorMessage(query.error)}</p><button type="button" className="owners-secondary-button" onClick={() => query.refetch()}><RefreshCw size={15} /> Reintentar</button></div>
        ) : !data ? (
          <div className="owners-message" role="status"><LoaderCircle size={19} className="spin" /> Cargando usuarios…</div>
        ) : (
          <>
            {query.isError && <p className="owners-inline-error" role="alert">No se pudo actualizar el listado. {getUsersErrorMessage(query.error)}</p>}
            {query.isFetching && <p className="owners-updating" role="status"><LoaderCircle size={15} className="spin" /> Actualizando listado…</p>}
            {users.length ? <UsersTable users={users} /> : <div className="owners-message"><Users size={20} /><p>No hay usuarios que coincidan con estos filtros.</p></div>}
            {totalElements > 0 && <div className="owners-pagination"><span>{data.number * data.size + 1}–{Math.min((data.number + 1) * data.size, totalElements)} de {totalElements}</span><div><button type="button" className="owners-secondary-button" onClick={() => setPage(Math.max(0, data.number - 1))} disabled={data.number === 0 || query.isFetching}>Anterior</button><span className="owners-page-number">Página {data.number + 1}{totalPages ? ` de ${totalPages}` : ''}</span><button type="button" className="owners-secondary-button" onClick={() => setPage(data.number + 1)} disabled={query.isFetching || data.number + 1 >= totalPages}>Siguiente</button></div></div>}
          </>
        )}
      </section>
    </>
  )
}
