import { Search } from 'lucide-react'

export default function OwnerFilters({ nombre, email, status, onNombreChange, onEmailChange, onStatusChange }) {
  return (
    <section className="owners-filters" aria-label="Filtros de owners">
      <label className="owners-search"><span>Nombre</span><div className="owners-filter-input"><Search size={16} aria-hidden="true" /><input type="search" value={nombre} onChange={(event) => onNombreChange(event.target.value)} placeholder="Buscar por nombre" /></div></label>
      <label className="owners-search"><span>Email</span><div className="owners-filter-input"><Search size={16} aria-hidden="true" /><input type="search" value={email} onChange={(event) => onEmailChange(event.target.value)} placeholder="Buscar por email" /></div></label>
      <label className="owners-status-filter"><span>Estado</span><select value={status} onChange={(event) => onStatusChange(event.target.value)}><option value="">Todos</option><option value="ACTIVE">Activo</option><option value="INACTIVE">Inactivo</option><option value="BLOCKED">Bloqueado</option></select></label>
    </section>
  )
}
