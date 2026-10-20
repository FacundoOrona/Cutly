import { ArrowRight, CalendarDays, Check, Clock3, Scissors, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import BrandMark from '../components/BrandMark.jsx'

export default function LoginPage() {
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
          <div className="preview-card">
            <div className="preview-heading"><span>HOY EN TU SALÓN</span><span className="live-dot" /> <span className="open-label">En movimiento</span></div>
            <div className="appointment"><span className="appointment-icon"><Scissors size={16} /></span><div><strong>Un día bien organizado</strong><small>Tus turnos, a primera vista</small></div><span className="appointment-time"><Clock3 size={13} /> 09—18</span></div>
            <div className="preview-footer"><span><CalendarDays size={14} /> Agenda clara, mente tranquila</span><Check size={17} /></div>
          </div>
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
          <div className="setup-note"><span className="setup-icon"><Sparkles size={17} /></span><p>La integración de acceso estará disponible cuando conectemos el contrato de autenticación del backend.</p></div>
          <Link className="preview-link" to="/dashboard">Explorar vista previa <ArrowRight size={16} /></Link>
          <p className="login-disclaimer">Esta pantalla es una vista preliminar. No solicita ni valida credenciales.</p>
        </div>
        <footer className="panel-footer"><span>© 2026 Cutly</span><span>Hecho para hacer crecer tu salón.</span></footer>
      </section>
    </main>
  )
}
