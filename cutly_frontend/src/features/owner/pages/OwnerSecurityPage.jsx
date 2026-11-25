import { useRef, useState } from 'react'
import { CheckCircle2, Eye, EyeOff, LockKeyhole, Shield } from 'lucide-react'
import useChangeOwnerPassword from '../hooks/useChangeOwnerPassword.js'
import { getOwnerPasswordError } from '../api/ownerSecurityApi.js'
import './OwnerSecurityPage.css'

const emptyValues = { contrasenaActual: '', nuevaContrasena: '', confirmarContrasena: '' }
const fields = [
  ['contrasenaActual', 'Contraseña actual'],
  ['nuevaContrasena', 'Nueva contraseña'],
  ['confirmarContrasena', 'Confirmar nueva contraseña'],
]

export default function OwnerSecurityPage() {
  const mutation = useChangeOwnerPassword()
  const pendingRef = useRef(false)
  const [values, setValues] = useState(emptyValues)
  const [visible, setVisible] = useState({})
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function submit(event) {
    event.preventDefault()
    if (pendingRef.current) return
    const nextErrors = {}
    if (!values.contrasenaActual) nextErrors.contrasenaActual = 'Ingresá tu contraseña actual.'
    if (!values.nuevaContrasena) nextErrors.nuevaContrasena = 'Ingresá una contraseña nueva.'
    else if (values.nuevaContrasena.length < 8) nextErrors.nuevaContrasena = 'La contraseña nueva debe tener al menos 8 caracteres.'
    if (!values.confirmarContrasena) nextErrors.confirmarContrasena = 'Confirmá la contraseña nueva.'
    else if (values.confirmarContrasena !== values.nuevaContrasena) nextErrors.confirmarContrasena = 'Las contraseñas no coinciden.'
    if (values.contrasenaActual && values.nuevaContrasena && values.contrasenaActual === values.nuevaContrasena) nextErrors.nuevaContrasena = 'La contraseña nueva debe ser diferente a la actual.'
    setErrors(nextErrors)
    setError('')
    setNotice('')
    if (Object.keys(nextErrors).length) return
    pendingRef.current = true
    try {
      await mutation.mutateAsync({ contrasenaActual: values.contrasenaActual, nuevaContrasena: values.nuevaContrasena })
      setValues(emptyValues)
      setVisible({})
      setErrors({})
      setNotice('Tu contraseña se cambió correctamente.')
    } catch (requestError) {
      setError(getOwnerPasswordError(requestError))
    } finally {
      pendingRef.current = false
    }
  }

  return <section className="owner-security-page" aria-labelledby="owner-security-title">
    <header className="owner-security-heading"><div><span>CUENTA</span><h1 id="owner-security-title">Seguridad</h1><p>Administrá la contraseña de tu cuenta.</p></div><span className="owner-security-heading-icon"><Shield size={20} /></span></header>
    <section className="owner-security-section" aria-labelledby="owner-password-title">
      <div className="owner-security-section-heading"><div><span>PROTECCIÓN</span><h2 id="owner-password-title">Cambiar contraseña</h2></div><LockKeyhole size={19} /></div>
      <form className="owner-security-card" onSubmit={submit} noValidate>
        <p>Elegí una contraseña segura para proteger tu cuenta.</p>
        <div className="owner-security-fields">{fields.map(([key, label]) => {
          const errorId = `owner-password-${key}-error`
          return <label className="owner-security-field" htmlFor={`owner-password-${key}`} key={key}><span>{label}<b aria-hidden="true"> *</b></span><span className="owner-security-input-wrap"><input id={`owner-password-${key}`} type={visible[key] ? 'text' : 'password'} value={values[key]} autoComplete={key === 'contrasenaActual' ? 'current-password' : 'new-password'} required disabled={mutation.isPending} aria-invalid={Boolean(errors[key])} aria-describedby={errors[key] ? errorId : undefined} onChange={(event) => { setValues((current) => ({ ...current, [key]: event.target.value })); setErrors((current) => ({ ...current, [key]: undefined })); setError(''); setNotice('') }} /><button type="button" onClick={() => setVisible((current) => ({ ...current, [key]: !current[key] }))} aria-label={`${visible[key] ? 'Ocultar' : 'Mostrar'} ${label.toLowerCase()}`} aria-pressed={Boolean(visible[key])} disabled={mutation.isPending}>{visible[key] ? <EyeOff size={17} /> : <Eye size={17} />}</button></span>{errors[key] && <small id={errorId}>{errors[key]}</small>}</label>
        })}</div>
        {error && <p className="owner-security-error" role="alert">{error}</p>}
        {notice && <p className="owner-security-notice" role="status"><CheckCircle2 size={17} />{notice}</p>}
        <button className="owner-security-submit" type="submit" disabled={mutation.isPending}>{mutation.isPending ? 'Actualizando…' : 'Cambiar contraseña'}</button>
      </form>
    </section>
  </section>
}

