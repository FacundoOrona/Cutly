import { useRef, useState } from 'react'
import { CheckCircle2, Eye, EyeOff, LoaderCircle, LockKeyhole, RefreshCw, UserRound, X } from 'lucide-react'
import useAuth from '../../../auth/useAuth.js'
import { getClientPasswordError, getClientProfileError } from '../api/clientApi.js'
import useChangeClientPassword from '../hooks/useChangeClientPassword.js'
import useClientProfile from '../hooks/useClientProfile.js'
import useUpdateClientProfile from '../hooks/useUpdateClientProfile.js'
import './ClientProfilePage.css'

const FIELDS = [
  { key: 'nombre', label: 'Nombre', type: 'text', maxLength: 20, autoComplete: 'given-name' },
  { key: 'apellido', label: 'Apellido', type: 'text', maxLength: 20, autoComplete: 'family-name' },
  { key: 'dni', label: 'DNI', type: 'text', minLength: 7, maxLength: 9, autoComplete: 'off' },
  { key: 'email', label: 'Email', type: 'email', maxLength: 150, autoComplete: 'email' },
]

const emptyPassword = { contrasenaActual: '', nuevaContrasena: '', confirmarContrasena: '' }

function valuesFrom(profile) {
  return Object.fromEntries(FIELDS.map(({ key }) => [key, profile?.[key] ?? '']))
}

function validateProfile(values, emailConfirmation, originalEmail) {
  const errors = {}
  for (const key of ['nombre', 'apellido']) {
    const value = values[key].trim()
    const label = key === 'nombre' ? 'El nombre' : 'El apellido'
    if (!value) errors[key] = `${label} es obligatorio.`
    else if (value.length < 2 || value.length > 20) errors[key] = `${label} debe tener entre 2 y 20 caracteres.`
  }
  const dni = values.dni.trim()
  if (!dni) errors.dni = 'El DNI es obligatorio.'
  else if (dni.length < 7 || dni.length > 9) errors.dni = 'El DNI debe tener entre 7 y 9 caracteres.'
  const email = values.email.trim()
  if (!email) errors.email = 'El email es obligatorio.'
  else if (email.length > 150 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Ingresá un email válido de hasta 150 caracteres.'
  if (email !== originalEmail) {
    const confirmation = emailConfirmation.trim()
    if (!confirmation) errors.confirmarEmail = 'Confirmá tu nuevo email.'
    else if (email.toLowerCase() !== confirmation.toLowerCase()) errors.confirmarEmail = 'Los emails no coinciden.'
  }
  return errors
}

function ProfileSkeleton() {
  return <div className="client-profile-skeleton" role="status" aria-label="Cargando perfil"><span /><span /><span /><span /></div>
}

export default function ClientProfilePage() {
  const profileQuery = useClientProfile()
  const updateMutation = useUpdateClientProfile()
  const passwordMutation = useChangeClientPassword()
  const { signOut, updateUser } = useAuth()
  const profilePendingRef = useRef(false)
  const passwordPendingRef = useRef(false)
  const [editing, setEditing] = useState(false)
  const [values, setValues] = useState(null)
  const [emailConfirmation, setEmailConfirmation] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [profileError, setProfileError] = useState('')
  const [profileNotice, setProfileNotice] = useState('')
  const [passwordValues, setPasswordValues] = useState(emptyPassword)
  const [passwordErrors, setPasswordErrors] = useState({})
  const [passwordError, setPasswordError] = useState('')
  const [passwordNotice, setPasswordNotice] = useState('')
  const [visiblePasswords, setVisiblePasswords] = useState({})
  const profile = profileQuery.data

  function beginEditing() {
    setValues(valuesFrom(profile))
    setEmailConfirmation('')
    setFieldErrors({})
    setProfileError('')
    setProfileNotice('')
    setEditing(true)
  }

  function cancelEditing() {
    if (updateMutation.isPending) return
    setValues(null)
    setEmailConfirmation('')
    setFieldErrors({})
    setProfileError('')
    setEditing(false)
  }

  function changeProfileField(key, value) {
    setValues((current) => ({ ...current, [key]: value }))
    setFieldErrors((current) => ({ ...current, [key]: undefined }))
    if (key === 'email') {
      setEmailConfirmation('')
      setFieldErrors((current) => ({ ...current, confirmarEmail: undefined }))
    }
    setProfileError('')
    setProfileNotice('')
  }

  async function saveProfile(event) {
    event.preventDefault()
    if (profilePendingRef.current) return
    const trimmed = Object.fromEntries(FIELDS.map(({ key }) => [key, values[key].trim()]))
    const original = valuesFrom(profile)
    const originalEmail = original.email.trim()
    const errors = validateProfile(trimmed, emailConfirmation, originalEmail)
    setFieldErrors(errors)
    setProfileError('')
    setProfileNotice('')
    if (Object.keys(errors).length) return

    const changed = FIELDS.some(({ key }) => trimmed[key] !== (key === 'email' ? originalEmail : original[key]))
    if (!changed) {
      setEditing(false)
      setValues(null)
      setProfileNotice('No hay cambios para guardar.')
      return
    }

    profilePendingRef.current = true
    try {
      const updated = await updateMutation.mutateAsync(trimmed)
      updateUser({
        userId: updated.usuarioId,
        dni: updated.dni,
        nombre: updated.nombre,
        apellido: updated.apellido,
        email: updated.email,
        status: updated.status,
        rol: updated.rol,
      })
      setValues(null)
      setFieldErrors({})
      setEditing(false)
      if (updated.email !== originalEmail) {
        setProfileNotice('Tu email se actualizó. Vamos a cerrar tu sesión para que ingreses con el email nuevo.')
        try {
          await signOut()
        } catch {
          // signOut clears the local session and redirects even if token revocation fails.
        }
        return
      }
      setProfileNotice('Tus datos se actualizaron correctamente.')
    } catch (error) {
      const parsed = getClientProfileError(error, 'profile')
      setFieldErrors((current) => ({ ...current, ...parsed.fieldErrors }))
      setProfileError(parsed.message)
    } finally {
      profilePendingRef.current = false
    }
  }

  function changePasswordField(key, value) {
    setPasswordValues((current) => ({ ...current, [key]: value }))
    setPasswordErrors((current) => ({ ...current, [key]: undefined }))
    setPasswordError('')
    setPasswordNotice('')
  }

  async function savePassword(event) {
    event.preventDefault()
    if (passwordPendingRef.current) return
    const errors = {}
    if (!passwordValues.contrasenaActual) errors.contrasenaActual = 'Ingresá tu contraseña actual.'
    if (!passwordValues.nuevaContrasena) errors.nuevaContrasena = 'Ingresá una contraseña nueva.'
    else if (passwordValues.nuevaContrasena.length < 8) errors.nuevaContrasena = 'La contraseña nueva debe tener al menos 8 caracteres.'
    if (!passwordValues.confirmarContrasena) errors.confirmarContrasena = 'Confirmá la contraseña nueva.'
    else if (passwordValues.confirmarContrasena !== passwordValues.nuevaContrasena) errors.confirmarContrasena = 'Las contraseñas no coinciden.'
    if (passwordValues.contrasenaActual && passwordValues.nuevaContrasena && passwordValues.contrasenaActual === passwordValues.nuevaContrasena) {
      errors.nuevaContrasena = 'La contraseña nueva debe ser diferente a la actual.'
    }
    setPasswordErrors(errors)
    setPasswordError('')
    setPasswordNotice('')
    if (Object.keys(errors).length) return

    passwordPendingRef.current = true
    try {
      await passwordMutation.mutateAsync({
        contrasenaActual: passwordValues.contrasenaActual,
        nuevaContrasena: passwordValues.nuevaContrasena,
      })
      setPasswordValues(emptyPassword)
      setPasswordErrors({})
      setVisiblePasswords({})
      setPasswordNotice('Tu contraseña se cambió correctamente.')
    } catch (error) {
      setPasswordError(getClientPasswordError(error))
    } finally {
      passwordPendingRef.current = false
    }
  }

  function togglePasswordVisibility(key) {
    setVisiblePasswords((current) => ({ ...current, [key]: !current[key] }))
  }

  return <section className="client-profile-page" aria-labelledby="client-profile-title">
    <header className="client-profile-heading">
      <div><span className="client-profile-eyebrow">MI ESPACIO</span><h1 id="client-profile-title">Mi cuenta</h1><p>Administrá tus datos personales y la seguridad de tu cuenta.</p></div>
      <span className="client-profile-heading-icon" aria-hidden="true"><UserRound size={21} /></span>
    </header>

    {profileNotice && <p className="client-profile-notice" role="status"><CheckCircle2 size={17} />{profileNotice}</p>}
    {profileQuery.isPending ? <ProfileSkeleton /> : profileQuery.isError ? <div className="client-profile-query-error" role="alert"><UserRound size={22} /><strong>No pudimos cargar tu perfil</strong><p>{getClientProfileError(profileQuery.error).message}</p><button type="button" onClick={() => profileQuery.refetch()} disabled={profileQuery.isFetching}><RefreshCw size={14} /> Reintentar</button></div> : profile && <>
      <section className="client-profile-section" aria-labelledby="client-personal-title">
        <div className="client-profile-section-heading"><div><span className="client-profile-kicker">TUS DATOS</span><h2 id="client-personal-title">Datos personales</h2></div>{!editing && <button type="button" className="client-profile-button is-secondary" onClick={beginEditing}>Editar perfil</button>}</div>
        {editing ? <form className="client-profile-card" onSubmit={saveProfile} noValidate>
          <div className="client-profile-fields">{FIELDS.map(({ key, label, type, maxLength, minLength, autoComplete }) => {
            const errorId = `client-profile-${key}-error`
            return <label className="client-profile-field" htmlFor={`client-profile-${key}`} key={key}><span>{label}<b aria-hidden="true"> *</b></span><input id={`client-profile-${key}`} type={type} value={values[key]} maxLength={maxLength} minLength={minLength} required disabled={updateMutation.isPending} autoComplete={autoComplete} aria-invalid={Boolean(fieldErrors[key])} aria-describedby={fieldErrors[key] ? errorId : undefined} onChange={(event) => changeProfileField(key, event.target.value)} />{fieldErrors[key] && <small className="client-profile-field-error" id={errorId}>{fieldErrors[key]}</small>}</label>
          })}</div>
          {values.email.trim() !== profile.email.trim() && <div className="client-profile-email-confirmation"><label className="client-profile-field" htmlFor="client-profile-confirm-email"><span>Confirmar nuevo email<b aria-hidden="true"> *</b></span><input id="client-profile-confirm-email" type="email" value={emailConfirmation} maxLength={150} required disabled={updateMutation.isPending} autoComplete="email" aria-invalid={Boolean(fieldErrors.confirmarEmail)} aria-describedby={fieldErrors.confirmarEmail ? 'client-profile-confirm-email-error' : undefined} onChange={(event) => { setEmailConfirmation(event.target.value); setFieldErrors((current) => ({ ...current, confirmarEmail: undefined })); setProfileError('') }} />{fieldErrors.confirmarEmail && <small className="client-profile-field-error" id="client-profile-confirm-email-error">{fieldErrors.confirmarEmail}</small>}</label></div>}
          <p className="client-profile-email-warning" role="note">Importante: si cambiás tu email, tu sesión se cerrará automáticamente. Después tendrás que volver a iniciar sesión utilizando tu nuevo correo electrónico.</p>
          {profileError && <p className="client-profile-submit-error" role="alert">{profileError}</p>}
          <div className="client-profile-actions"><button type="button" className="client-profile-button is-secondary" onClick={cancelEditing} disabled={updateMutation.isPending}><X size={15} /> Cancelar</button><button type="submit" className="client-profile-button is-primary" disabled={updateMutation.isPending}>{updateMutation.isPending ? <><LoaderCircle size={15} className="client-profile-spinner" /> Guardando…</> : <><CheckCircle2 size={15} /> Guardar cambios</>}</button></div>
        </form> : <dl className="client-profile-card client-profile-values">{FIELDS.map(({ key, label }) => <div className="client-profile-value" key={key}><dt>{label}</dt><dd>{profile[key] || '—'}</dd></div>)}</dl>}
      </section>

      <section className="client-profile-section" aria-labelledby="client-security-title">
        <div className="client-profile-section-heading"><div><span className="client-profile-kicker">PROTECCIÓN</span><h2 id="client-security-title">Seguridad</h2></div><span className="client-profile-security-icon" aria-hidden="true"><LockKeyhole size={18} /></span></div>
        <form className="client-profile-card client-password-form" onSubmit={savePassword} noValidate>
          <p className="client-password-intro">Elegí una contraseña segura para proteger tu cuenta.</p>
          <div className="client-password-fields">{[
            ['contrasenaActual', 'Contraseña actual'], ['nuevaContrasena', 'Nueva contraseña'], ['confirmarContrasena', 'Confirmar nueva contraseña'],
          ].map(([key, label]) => {
            const errorId = `client-password-${key}-error`
            const visible = Boolean(visiblePasswords[key])
            return <label className="client-profile-field" htmlFor={`client-password-${key}`} key={key}><span>{label}<b aria-hidden="true"> *</b></span><span className="client-password-input-wrap"><input id={`client-password-${key}`} type={visible ? 'text' : 'password'} value={passwordValues[key]} autoComplete={key === 'contrasenaActual' ? 'current-password' : 'new-password'} disabled={passwordMutation.isPending} aria-invalid={Boolean(passwordErrors[key])} aria-describedby={passwordErrors[key] ? errorId : undefined} onChange={(event) => changePasswordField(key, event.target.value)} /><button type="button" className="client-password-visibility" onClick={() => togglePasswordVisibility(key)} aria-label={visible ? `Ocultar ${label.toLowerCase()}` : `Mostrar ${label.toLowerCase()}`} aria-pressed={visible} disabled={passwordMutation.isPending}>{visible ? <EyeOff size={17} /> : <Eye size={17} />}</button></span>{passwordErrors[key] && <small className="client-profile-field-error" id={errorId}>{passwordErrors[key]}</small>}</label>
          })}</div>
          {passwordError && <p className="client-profile-submit-error" role="alert">{passwordError}</p>}
          {passwordNotice && <p className="client-profile-notice" role="status"><CheckCircle2 size={17} />{passwordNotice}</p>}
          <div className="client-profile-actions"><button type="submit" className="client-profile-button is-primary" disabled={passwordMutation.isPending}>{passwordMutation.isPending ? <><LoaderCircle size={15} className="client-profile-spinner" /> Actualizando…</> : <><LockKeyhole size={15} /> Cambiar contraseña</>}</button></div>
        </form>
      </section>
    </>}
  </section>
}
