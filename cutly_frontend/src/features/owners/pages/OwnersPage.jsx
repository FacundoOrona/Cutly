import { useEffect, useState } from 'react'
import { LoaderCircle, RefreshCw, Users } from 'lucide-react'
import OwnerFilters from '../components/OwnerFilters.jsx'
import OwnersTable from '../components/OwnersTable.jsx'
import { getOwnersErrorMessage } from '../api/ownersApi.js'
import useOwners from '../hooks/useOwners.js'

const PAGE_SIZE = 20
const DEBOUNCE_MS = 350

export default function OwnersPage() {
  const [nombreInput, setNombreInput] = useState('')
  const [emailInput, setEmailInput] = useState('')
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const query = useOwners({ nombre, email, status, page, size: PAGE_SIZE })

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setNombre(nombreInput)
      setEmail(emailInput)
      setPage(0)
    }, DEBOUNCE_MS)
    return () => window.clearTimeout(timeout)
  }, [nombreInput, emailInput])

  function changeStatus(value) {
    setStatus(value)
    setPage(0)
  }

  const data = query.data
  const owners = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements

  return (
    <>
      <span className="eyebrow"><span className="eyebrow-dot" /> ADMINISTRACIÓN</span>
      <div className="owners-heading"><div><h1>Owners</h1><p className="owners-description">Consultá y filtrá las cuentas de propietarios registradas.</p></div><div className="owners-count"><Users size={17} />{typeof totalElements === 'number' ? `${totalElements} ${totalElements === 1 ? 'owner' : 'owners'}` : 'Listado'}</div></div>
      <OwnerFilters nombre={nombreInput} email={emailInput} status={status} onNombreChange={setNombreInput} onEmailChange={setEmailInput} onStatusChange={changeStatus} />
      <section className="owners-results" aria-label="Listado de owners" aria-busy={query.isFetching}>
        {query.isError && !data ? (
          <div className="owners-message owners-error" role="alert"><p>{getOwnersErrorMessage(query.error)}</p><button className="owners-secondary-button" onClick={() => query.refetch()}><RefreshCw size={15} /> Reintentar</button></div>
        ) : !data ? (
          <div className="owners-message" role="status"><LoaderCircle size={19} className="spin" /> Cargando owners…</div>
        ) : (
          <>
            {query.isError && <p className="owners-inline-error" role="alert">No se pudo actualizar el listado. {getOwnersErrorMessage(query.error)}</p>}
            {query.isFetching && <p className="owners-updating" role="status"><LoaderCircle size={15} className="spin" /> Actualizando listado…</p>}
            {owners.length ? <OwnersTable owners={owners} /> : <div className="owners-message"><Users size={20} /><p>No hay owners que coincidan con estos filtros.</p></div>}
            {owners.length > 0 && <div className="owners-pagination"><span>{data.number * data.size + 1}–{Math.min((data.number + 1) * data.size, totalElements ?? (data.number + 1) * data.size)}{typeof totalElements === 'number' ? ` de ${totalElements}` : ''}</span><div><button className="owners-secondary-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={page === 0 || query.isFetching}>Anterior</button><span className="owners-page-number">Página {page + 1}{totalPages ? ` de ${totalPages}` : ''}</span><button className="owners-secondary-button" onClick={() => setPage((current) => current + 1)} disabled={query.isFetching || (totalPages ? page + 1 >= totalPages : !data.last)}>Siguiente</button></div></div>}
          </>
        )}
      </section>
    </>
  )
}
