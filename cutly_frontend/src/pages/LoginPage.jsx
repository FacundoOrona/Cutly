import { useState } from 'react'
import { ArrowRight, LoaderCircle, LockKeyhole, Mail, Sparkles } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import BrandMark from '../components/BrandMark.jsx'
import useAuth from '../auth/useAuth.js'

function getLoginError(error) {
  const status = error.response?.status
  if (status === 401 || status === 403) return 'El correo o la contraseña no son correctos.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return 'No pudimos iniciar sesión. Revisá tus datos e intentá de nuevo.'
}

export default function LoginPage() {
  const { signIn, isAuthenticated, isInitializing, role } = useAuth()
  const [email, setEmail] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()

  if (isInitializing) return <main className="session-loading" role="status">Verificando sesión…</main>
  if (isAuthenticated) {
    navigate(role === 'SUPER_ADMIN' ? '/dashboard' : '/acceso-denegado', { replace: true })
    return null
  }

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
      await signIn(normalizedEmail, contrasena)
      const destination = location.state?.from?.pathname
      if (destination && destination !== '/login') navigate(destination, { replace: true })
    } catch (error) {
      setErrorMessage(error.response ? getLoginError(error) : error.message?.includes('sesión válida') ? error.message : getLoginError(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-story" aria-label="Presentación de Cutly">
        <div className="story-content">
          <BrandMark light />
          <div className="story-copy">
            <span className="eyebrow eyebrow-light"><Sparkles size={14} /> TU SALÓN, EN SINTONÍA</span>
            <h1>Más tiempo para<br />hacer lo que <span>amás.</span></h1>
            <p>Organizá tu peluquería en un solo lugar y hacé que cada día fluya mejor.</p>
          </div>
          <div className="preview-card"><div className="preview-heading">ACCESO ADMINISTRATIVO</div><p className="access-copy">Ingresá con tus credenciales para continuar de forma segura.</p></div>
          <span className="story-footnote">UNA MEJOR EXPERIENCIA, PARA TODOS.</span>
        </div>
        <div className="story-decoration story-decoration-one" />
        <div className="story-decoration story-decoration-two" />
      </section>
      <section className="login-panel">
        <div className="login-panel-inner">
          <div className="mobile-brand"><BrandMark /></div>
          <span className="eyebrow">BIENVENIDO A CUTLY</span>
          <h2>Tu espacio<br />te está esperando.</h2>
          <p className="login-intro">Iniciá sesión para continuar con la gestión de tu salón.</p>
          <form className="login-form" onSubmit={handleSubmit} noValidate>
            <label htmlFor="email">Correo electrónico</label>
            <div className="input-wrap"><Mail size={17} aria-hidden="true" /><input id="email" name="email" type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} aria-invalid={Boolean(errorMessage && !email)} required /></div>
            <label htmlFor="password">Contraseña</label>
            <div className="input-wrap"><LockKeyhole size={17} aria-hidden="true" /><input id="password" name="password" type="password" autoComplete="current-password" value={contrasena} onChange={(event) => setContrasena(event.target.value)} required /></div>
            {location.state?.sessionExpired && <p className="form-error" role="status">Tu sesión venció. Iniciá sesión nuevamente.</p>}
            {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
            <button className="login-submit" type="submit" disabled={isSubmitting}>
              {isSubmitting ? <><LoaderCircle size={17} className="spin" /> Ingresando…</> : <>Iniciar sesión <ArrowRight size={17} /> </>}
            </button>
          </form>
          <p className="login-disclaimer">El panel está disponible únicamente para cuentas con rol SUPER_ADMIN.</p>
        </div>
        <footer className="panel-footer"><span>© 2026 Cutly</span><span>Hecho para hacer crecer tu salón.</span></footer>
      </section>
    </main>
  )
}
