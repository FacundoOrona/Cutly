import { Search } from 'lucide-react'

const roles = [
  ['SUPER_ADMIN', 'Super administrador'],
  ['OWNER', 'Owner'],
  ['EMPLOYEE', 'Empleado'],
  ['CLIENT', 'Cliente'],
]
const statuses = [['ACTIVE', 'Activo'], ['INACTIVE', 'Inactivo'], ['BLOCKED', 'Bloqueado']]

export default function UserFilters({ values, sort, onChange, onSortChange, onClear, hasFilters }) {
  function textField(name, label) {
    return (
      <label className="owners-search" htmlFor={`users-${name}`}>
        <span>{label}</span>
        <div className="owners-filter-input"><Search size={16} aria-hidden="true" /><input id={`users-${name}`} type="search" value={values[name]} onChange={(event) => onChange(name, event.target.value)} /></div>
      </label>
    )
  }

  return (
    <section className="users-filters" aria-label="Filtros de usuarios">
      {textField('nombre', 'Nombre')}
      {textField('apellido', 'Apellido')}
      {textField('email', 'Email')}
      {textField('dni', 'DNI')}
      <label className="owners-status-filter" htmlFor="users-role"><span>Rol</span><select id="users-role" value={values.rol} onChange={(event) => onChange('rol', event.target.value)}><option value="">Todos los roles</option>{roles.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <label className="owners-status-filter" htmlFor="users-status"><span>Estado</span><select id="users-status" value={values.status} onChange={(event) => onChange('status', event.target.value)}><option value="">Todos los estados</option>{statuses.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <label className="owners-status-filter users-sort" htmlFor="users-sort"><span>Ordenar por</span><select id="users-sort" value={`${sort.field},${sort.direction}`} onChange={(event) => onSortChange(event.target.value)}>
        {['usuarioId', 'nombre', 'apellido', 'dni', 'email', 'rol', 'status'].flatMap((field) => [
          <option key={`${field},ASC`} value={`${field},ASC`}>{field === 'usuarioId' ? 'ID' : field[0].toUpperCase() + field.slice(1)} · ascendente</option>,
          <option key={`${field},DESC`} value={`${field},DESC`}>{field === 'usuarioId' ? 'ID' : field[0].toUpperCase() + field.slice(1)} · descendente</option>,
        ])}
      </select></label>
      <div className="users-filter-actions"><button type="button" className="owners-secondary-button" onClick={onClear} disabled={!hasFilters}>Limpiar filtros</button></div>
    </section>
  )
}
