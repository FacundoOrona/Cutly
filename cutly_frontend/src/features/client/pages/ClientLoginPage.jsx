import { useState } from 'react'
import { ArrowRight, LoaderCircle, LockKeyhole, Mail, Sparkles } from 'lucide-react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import BrandMark from '../../../components/BrandMark.jsx'
import useAuth from '../../../auth/useAuth.js'
import { canRoleAccessPath, getRoleDestination } from '../../../auth/roleNavigation.js'
import './ClientLoginPage.css'

function getLoginError(error) {
  const status = error.response?.status
  if (status === 401 || status === 403) return 'El correo o la contraseña no son correctos.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return 'No pudimos iniciar sesión. Revisá tus datos e intentá de nuevo.'
}

export default function ClientLoginPage() {
  const { signIn, isAuthenticated, isInitializing, role } = useAuth()
  const [email, setEmail] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()

  if (isInitializing) return <main className="session-loading" role="status">Verificando sesión…</main>
  if (isAuthenticated) return <Navigate to={getRoleDestination(role)} replace />

  async function handleSubmit(event) {
    event.preventDefault()
    setErrorMessage('')
    const normalizedEmail = email.trim()
    if (!normalizedEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setErrorMessage('Ingresá un correo electrónico válido.')
      return
    }
    if (!contrasena) {
      setErrorMessage('Ingresá tu contraseña.')
      return
    }

    setIsSubmitting(true)
    try {
      const authenticatedUser = await signIn(normalizedEmail, contrasena, ['CLIENT'])
      const requestedPath = location.state?.from?.pathname
      const destination = canRoleAccessPath(authenticatedUser.rol, requestedPath)
        ? requestedPath
        : getRoleDestination(authenticatedUser.rol)
      navigate(destination, { replace: true })
    } catch (error) {
      setErrorMessage(error.response
        ? getLoginError(error)
        : error.message?.includes('sesión válida') || error.message?.includes('debe ingresar')
          ? error.message
          : getLoginError(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="client-login-page">
      <header className="client-login-header"><BrandMark to="/client/login" /><span>Tu próximo plan empieza acá</span></header>
      <section className="client-login-card">
        <div className="client-login-mark"><Sparkles size={19} aria-hidden="true" /></div>
        <span className="client-login-eyebrow">CUTLY PARA VOS</span>
        <h1>Encontrá tu próximo turno.</h1>
        <p className="client-login-intro">Ingresá para descubrir y organizar tus próximos turnos en un solo lugar.</p>
        <form className="client-login-form" onSubmit={handleSubmit} noValidate>
          <label htmlFor="client-login-email">Correo electrónico</label>
          <div className="client-login-input"><Mail size={17} aria-hidden="true" /><input id="client-login-email" name="email" type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} required /></div>
          <label htmlFor="client-login-password">Contraseña</label>
          <div className="client-login-input"><LockKeyhole size={17} aria-hidden="true" /><input id="client-login-password" name="password" type="password" autoComplete="current-password" value={contrasena} onChange={(event) => setContrasena(event.target.value)} required /></div>
          {location.state?.sessionExpired && <p className="client-login-error" role="status">Tu sesión venció. Iniciá sesión nuevamente.</p>}
          {errorMessage && <p className="client-login-error" role="alert">{errorMessage}</p>}
          <button className="client-login-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? <><LoaderCircle size={17} className="spin" aria-hidden="true" /> Ingresando…</> : <>Ingresar <ArrowRight size={17} aria-hidden="true" /></>}
          </button>
        </form>
        <p className="client-login-register">¿Todavía no tenés cuenta? <Link to="/client/registro">Crear una cuenta</Link></p>
      </section>
      <footer className="client-login-footer"><span>© 2026 Cutly</span><Link to="/login">¿Tenés un negocio? Ingresá acá.</Link></footer>
    </main>
  )
}
