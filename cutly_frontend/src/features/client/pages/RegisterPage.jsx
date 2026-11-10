import { useRef, useState } from 'react'
import { ArrowLeft, ArrowRight, LoaderCircle, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { getRoleDestination } from '../../../auth/roleNavigation.js'
import useAuth from '../../../auth/useAuth.js'
import BrandMark from '../../../components/BrandMark.jsx'
import './RegisterPage.css'

const initialValues = { nombre: '', apellido: '', email: '', dni: '', contrasena: '', confirmarContrasena: '' }

function validate(values) {
  const errors = {}
  const nombre = values.nombre.trim()
  const apellido = values.apellido.trim()
  const email = values.email.trim()
  const dni = values.dni.trim()

  if (nombre.length < 2 || nombre.length > 20) errors.nombre = 'El nombre debe tener entre 2 y 20 caracteres.'
  if (apellido.length < 2 || apellido.length > 20) errors.apellido = 'El apellido debe tener entre 2 y 20 caracteres.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Ingresá un email válido.'
  if (!/^\d{7,8}$/.test(dni)) errors.dni = 'El DNI debe tener 7 u 8 dígitos.'
  if (values.contrasena.length < 8) errors.contrasena = 'La contraseña debe tener al menos 8 caracteres.'
  if (values.confirmarContrasena !== values.contrasena) errors.confirmarContrasena = 'Las contraseñas no coinciden.'
  return errors
}

function getRegisterError(error) {
  const status = error.response?.status
  const body = error.response?.data
  if (!error.response) {
    if (error.message?.includes('sesión válida')) return 'El servidor no devolvió una sesión de cliente válida. Intentá nuevamente.'
    return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  }
  if (status >= 500) return 'No pudimos crear tu cuenta en este momento. Intentá más tarde.'

  const message = typeof body === 'string' ? body.trim() : ''
  if (/^el email ya se encuentra registrado\.?$/i.test(message)) return 'Ese correo electrónico ya está registrado.'
  if (status === 409) return 'El email o el DNI ya están registrados.'
  if (status === 400) return 'Revisá los datos ingresados. El email o el DNI podrían estar registrados.'
  return 'No pudimos crear tu cuenta. Revisá los datos e intentá nuevamente.'
}

export default function RegisterPage() {
  const { registerClient, isAuthenticated, role } = useAuth()
  const [values, setValues] = useState(initialValues)
  const [errors, setErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const pendingRef = useRef(false)
  const navigate = useNavigate()

  if (isAuthenticated) return <Navigate to={getRoleDestination(role)} replace />

  function updateField(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
    setSubmitError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (pendingRef.current) return
    const nextErrors = validate(values)
    setErrors(nextErrors)
    setSubmitError('')
    if (Object.keys(nextErrors).length) return

    pendingRef.current = true
    setIsSubmitting(true)
    try {
      await registerClient({
        nombre: values.nombre.trim(),
        apellido: values.apellido.trim(),
        email: values.email.trim(),
        dni: values.dni.trim(),
        contrasena: values.contrasena,
      })
      navigate('/client', { replace: true })
    } catch (error) {
      setSubmitError(getRegisterError(error))
    } finally {
      pendingRef.current = false
      setIsSubmitting(false)
    }
  }

  return (
    <main className="client-register-page">
      <header className="client-register-header"><BrandMark to="/login" /><Link to="/login" className="client-register-back"><ArrowLeft size={15} aria-hidden="true" /> Volver al login</Link></header>
      <section className="client-register-card">
        <span className="client-register-eyebrow"><UserRound size={14} aria-hidden="true" /> CUENTA CLIENTE</span>
        <h1>Creá tu cuenta</h1>
        <p className="client-register-intro">Completá tus datos para empezar a usar Cutly.</p>

        <form className="client-register-form" onSubmit={handleSubmit} noValidate>
          <label className="client-register-field" htmlFor="register-nombre">
            <span>Nombre</span>
            <div className="client-register-input-wrap"><UserRound size={16} aria-hidden="true" /><input id="register-nombre" name="nombre" autoComplete="given-name" maxLength={20} value={values.nombre} onChange={updateField} aria-invalid={Boolean(errors.nombre)} aria-describedby={errors.nombre ? 'register-nombre-error' : undefined} required /></div>
            {errors.nombre && <small id="register-nombre-error" className="client-register-field-error">{errors.nombre}</small>}
          </label>

          <label className="client-register-field" htmlFor="register-apellido">
            <span>Apellido</span>
            <div className="client-register-input-wrap"><UserRound size={16} aria-hidden="true" /><input id="register-apellido" name="apellido" autoComplete="family-name" maxLength={20} value={values.apellido} onChange={updateField} aria-invalid={Boolean(errors.apellido)} aria-describedby={errors.apellido ? 'register-apellido-error' : undefined} required /></div>
            {errors.apellido && <small id="register-apellido-error" className="client-register-field-error">{errors.apellido}</small>}
          </label>

          <label className="client-register-field client-register-field-wide" htmlFor="register-email">
            <span>Correo electrónico</span>
            <div className="client-register-input-wrap"><Mail size={16} aria-hidden="true" /><input id="register-email" name="email" type="email" autoComplete="email" value={values.email} onChange={updateField} aria-invalid={Boolean(errors.email)} aria-describedby={errors.email ? 'register-email-error' : undefined} required /></div>
            {errors.email && <small id="register-email-error" className="client-register-field-error">{errors.email}</small>}
          </label>

          <label className="client-register-field client-register-field-wide" htmlFor="register-dni">
            <span>DNI</span>
            <div className="client-register-input-wrap"><input id="register-dni" name="dni" type="text" inputMode="numeric" autoComplete="off" maxLength={8} value={values.dni} onChange={updateField} aria-invalid={Boolean(errors.dni)} aria-describedby={errors.dni ? 'register-dni-error' : 'register-dni-hint'} required /></div>
            {errors.dni ? <small id="register-dni-error" className="client-register-field-error">{errors.dni}</small> : <small id="register-dni-hint" className="client-register-field-hint">Ingresá 7 u 8 dígitos, sin puntos.</small>}
          </label>

          <label className="client-register-field" htmlFor="register-password">
            <span>Contraseña</span>
            <div className="client-register-input-wrap"><LockKeyhole size={16} aria-hidden="true" /><input id="register-password" name="contrasena" type="password" autoComplete="new-password" value={values.contrasena} onChange={updateField} aria-invalid={Boolean(errors.contrasena)} aria-describedby={errors.contrasena ? 'register-password-error' : 'register-password-hint'} required /></div>
            {errors.contrasena ? <small id="register-password-error" className="client-register-field-error">{errors.contrasena}</small> : <small id="register-password-hint" className="client-register-field-hint">Mínimo 8 caracteres.</small>}
          </label>

          <label className="client-register-field" htmlFor="register-confirm-password">
            <span>Confirmar contraseña</span>
            <div className="client-register-input-wrap"><LockKeyhole size={16} aria-hidden="true" /><input id="register-confirm-password" name="confirmarContrasena" type="password" autoComplete="new-password" value={values.confirmarContrasena} onChange={updateField} aria-invalid={Boolean(errors.confirmarContrasena)} aria-describedby={errors.confirmarContrasena ? 'register-confirm-error' : undefined} required /></div>
            {errors.confirmarContrasena && <small id="register-confirm-error" className="client-register-field-error">{errors.confirmarContrasena}</small>}
          </label>

          {submitError && <p className="client-register-submit-error" role="alert">{submitError}</p>}
          <button className="client-register-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? <><LoaderCircle size={17} className="spin" aria-hidden="true" /> Creando cuenta…</> : <>Crear cuenta <ArrowRight size={17} aria-hidden="true" /></>}
          </button>
        </form>

        <p className="client-register-login">¿Ya tenés una cuenta? <Link to="/login">Iniciá sesión</Link></p>
      </section>
      <footer className="client-register-footer">© 2026 Cutly</footer>
    </main>
  )
}
