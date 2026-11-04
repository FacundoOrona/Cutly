import { useRef, useState } from 'react'
import { Building2, CheckCircle2, LoaderCircle, RefreshCw, UserRound, X } from 'lucide-react'
import { getEmployeeBusinessErrorMessage, getEmployeeProfileUpdateError } from '../api/employeeApi.js'
import { useEmployeeBusiness, useEmployeeProfile, useUpdateEmployeeProfile } from '../hooks/useEmployeeDashboard.js'
import './EmployeeHomePage.css'
import './EmployeeProfilePage.css'

const FIELDS = [
  { key: 'nombre', label: 'Nombre', type: 'text', maxLength: 20 },
  { key: 'apellido', label: 'Apellido', type: 'text', maxLength: 20 },
  { key: 'dni', label: 'DNI', type: 'text', minLength: 7, maxLength: 9 },
  { key: 'email', label: 'Email', type: 'email', maxLength: 150 },
]

function valuesFrom(profile) {
  return Object.fromEntries(FIELDS.map(({ key }) => [key, profile?.[key] ?? '']))
}

function validate(values) {
  const errors = {}
  for (const key of ['nombre', 'apellido']) {
    const label = key === 'nombre' ? 'El nombre' : 'El apellido'
    const value = values[key]
    if (!value.trim()) errors[key] = `${label} es obligatorio.`
    else if (value.trim().length < 2 || value.length > 20) errors[key] = `${label} debe tener entre 2 y 20 caracteres.`
  }
  if (!values.dni.trim()) errors.dni = 'El DNI es obligatorio.'
  else if (values.dni.length < 7 || values.dni.length > 9) errors.dni = 'El DNI debe tener entre 7 y 9 caracteres.'
  if (!values.email.trim()) errors.email = 'El email es obligatorio.'
  else if (values.email.length > 150 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email)) errors.email = 'Ingresá un email válido de hasta 150 caracteres.'
  return errors
}

function ProfileSkeleton() {
  return <div className="employee-profile-skeleton" role="status" aria-label="Cargando perfil"><span /><span /><span /><span /></div>
}

function FieldValue({ label, value }) {
  return <div className="employee-profile-value"><dt>{label}</dt><dd>{value || '—'}</dd></div>
}

function BusinessSkeleton() {
  return <div className="employee-business-skeleton" role="status" aria-label="Cargando información laboral"><span /><span /><span /></div>
}

export default function EmployeeProfilePage() {
  const profileQuery = useEmployeeProfile()
  const businessQuery = useEmployeeBusiness()
  const updateMutation = useUpdateEmployeeProfile()
  const pendingRef = useRef(false)
  const [editing, setEditing] = useState(false)
  const [values, setValues] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const [notice, setNotice] = useState('')
  const profile = profileQuery.data

  function beginEditing() {
    setValues(valuesFrom(profile))
    setFieldErrors({})
    setSubmitError('')
    setNotice('')
    setEditing(true)
  }

  function cancelEditing() {
    if (updateMutation.isPending) return
    setValues(null)
    setFieldErrors({})
    setSubmitError('')
    setEditing(false)
  }

  function changeField(key, value) {
    setValues((current) => ({ ...current, [key]: value }))
    setFieldErrors((current) => ({ ...current, [key]: undefined }))
    setSubmitError('')
  }

  async function save(event) {
    event.preventDefault()
    if (pendingRef.current) return
    const errors = validate(values)
    setFieldErrors(errors)
    setSubmitError('')
    setNotice('')
    if (Object.keys(errors).length) return

    const original = valuesFrom(profile)
    const changed = Object.fromEntries(FIELDS
      .filter(({ key }) => values[key] !== original[key])
      .map(({ key }) => [key, values[key]]))
    if (!Object.keys(changed).length) {
      setEditing(false)
      setNotice('No hay cambios para guardar.')
      return
    }

    pendingRef.current = true
    try {
      await updateMutation.mutateAsync(changed)
      setValues(null)
      setFieldErrors({})
      setEditing(false)
      setNotice(changed.email
        ? 'Tu perfil se actualizó. Iniciá sesión nuevamente para continuar con el email nuevo.'
        : 'Tu perfil se actualizó correctamente.')
    } catch (error) {
      const parsed = getEmployeeProfileUpdateError(error)
      setFieldErrors((current) => ({ ...current, ...parsed.fieldErrors }))
      setSubmitError(parsed.message)
    } finally {
      pendingRef.current = false
    }
  }

  const business = businessQuery.data
  const addressParts = business ? [business.calle, business.numero].filter((part) => part != null && part !== '') : []
  const hasAddress = business && [business.calle, business.numero, business.ciudad, business.provincia, business.codigoPostal].some((part) => part != null && part !== '')

  return <section className="employee-dashboard employee-profile-page" aria-labelledby="employee-profile-title">
    <header className="employee-dashboard-heading">
      <div><span className="eyebrow">MI ESPACIO</span><h1 id="employee-profile-title">Mi perfil</h1><p className="employee-date">Tu información personal y laboral</p></div>
      <span className="employee-heading-icon"><UserRound size={21} /></span>
    </header>

    {notice && <p className="employee-profile-notice" role="status"><CheckCircle2 size={17} />{notice}</p>}
    {profileQuery.isPending ? <ProfileSkeleton /> : profileQuery.isError ? <div className="employee-query-error" role="alert"><UserRound size={22} /><strong>No pudimos cargar tu perfil</strong><p>Revisá tu conexión e intentá nuevamente.</p><button type="button" onClick={() => profileQuery.refetch()} disabled={profileQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div> : profile && <>
      <section className="employee-profile-section" aria-labelledby="employee-personal-title">
        <div className="employee-profile-section-heading"><div><span className="employee-section-kicker">TU CUENTA</span><h2 id="employee-personal-title">Información personal</h2></div>
          {!editing && <button type="button" className="employee-action-button is-secondary" onClick={beginEditing}>Editar perfil</button>}
        </div>

        {editing ? <form className="employee-profile-card" onSubmit={save} noValidate>
          <div className="employee-profile-fields">
            {FIELDS.map(({ key, label, type, maxLength, minLength }) => {
              const errorId = `employee-profile-${key}-error`
              return <label className="employee-profile-field" htmlFor={`employee-profile-${key}`} key={key}>
                <span>{label}<b aria-hidden="true"> *</b></span>
                <input id={`employee-profile-${key}`} type={type} value={values[key]} maxLength={maxLength} minLength={minLength} required disabled={updateMutation.isPending} autoComplete={key === 'email' ? 'email' : key === 'nombre' ? 'given-name' : key === 'apellido' ? 'family-name' : 'off'} aria-invalid={Boolean(fieldErrors[key])} aria-describedby={fieldErrors[key] ? errorId : undefined} onChange={(event) => changeField(key, event.target.value)} />
                {fieldErrors[key] && <small className="employee-profile-field-error" id={errorId}>{fieldErrors[key]}</small>}
              </label>
            })}
          </div>
          {submitError && <p className="employee-profile-submit-error" role="alert">{submitError}</p>}
          <div className="employee-profile-actions">
            <button type="button" className="employee-action-button is-secondary" onClick={cancelEditing} disabled={updateMutation.isPending}><X size={15} /> Cancelar</button>
            <button type="submit" className="employee-action-button is-primary" disabled={updateMutation.isPending}>{updateMutation.isPending ? <><LoaderCircle size={15} className="employee-action-spinner" /> Guardando…</> : <><CheckCircle2 size={15} /> Guardar cambios</>}</button>
          </div>
        </form> : <dl className="employee-profile-card employee-profile-values">
          <FieldValue label="Nombre" value={profile.nombre} /><FieldValue label="Apellido" value={profile.apellido} />
          <FieldValue label="DNI" value={profile.dni} /><FieldValue label="Email" value={profile.email} />
        </dl>}
      </section>

      <section className="employee-profile-section" aria-labelledby="employee-work-title">
        <div className="employee-profile-section-heading"><div><span className="employee-section-kicker">TU TRABAJO</span><h2 id="employee-work-title">Información laboral</h2></div></div>
        <div className="employee-profile-card employee-work-card" aria-busy={businessQuery.isPending || businessQuery.isFetching}>
          <div className="employee-work-status">
            <span className="employee-work-icon"><Building2 size={18} /></span>
            <div><strong>Estado del perfil laboral</strong><span className={`employee-profile-badge${profile.activo ? ' is-active' : ''}`}>{profile.activo ? 'Activo' : 'Inactivo'}</span></div>
            <span className="employee-role-badge">{profile.rol || '—'}</span>
          </div>
          {businessQuery.isPending ? <BusinessSkeleton /> : businessQuery.isError ? <div className="employee-business-error" role="alert"><p>{getEmployeeBusinessErrorMessage(businessQuery.error)}</p><button type="button" onClick={() => businessQuery.refetch()} disabled={businessQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div> : business && <>
            <div className="employee-business-heading"><span>Negocio</span><strong>{business.nombre || '—'}</strong>{business.descripcion && <p>{business.descripcion}</p>}</div>
            <dl className="employee-business-values">
              <FieldValue label="Teléfono" value={business.telefono} />
              <FieldValue label="Email del negocio" value={business.email} />
              {hasAddress ? <>
                <FieldValue label="Calle y número" value={addressParts.join(' ') || null} />
                <FieldValue label="Ciudad" value={business.ciudad} />
                <FieldValue label="Provincia" value={business.provincia} />
                <FieldValue label="Código postal" value={business.codigoPostal} />
              </> : <p className="employee-no-address">El negocio todavía no tiene una dirección registrada.</p>}
            </dl>
          </>}
        </div>
      </section>
    </>}
  </section>
}
