import { ArrowRight, CalendarDays, Compass, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import useAuth from '../../../auth/useAuth.js'
import './ClientHomePage.css'

export default function ClientHomePage() {
  const { user } = useAuth()
  const firstName = user?.nombre?.trim()

  return (
    <div className="client-home-page">
      <section className="client-home-welcome">
        <span className="client-home-eyebrow"><Sparkles size={14} aria-hidden="true" /> TU ESPACIO CUTLY</span>
        <h1>{firstName ? `Hola, ${firstName}.` : 'Hola, bienvenido.'}</h1>
        <p>Próximamente vas a poder encontrar servicios y organizar tus turnos desde un solo lugar.</p>
      </section>

      <section className="client-home-actions" aria-label="Accesos rápidos">
        <Link className="client-home-card client-home-card-primary" to="/client/explorar">
          <span className="client-home-card-icon"><Compass size={20} aria-hidden="true" /></span>
          <span className="client-home-card-copy"><strong>Explorar negocios</strong><small>La exploración de servicios estará disponible próximamente.</small></span>
          <ArrowRight size={18} aria-hidden="true" />
        </Link>
        <Link className="client-home-card" to="/client/turnos">
          <span className="client-home-card-icon"><CalendarDays size={20} aria-hidden="true" /></span>
          <span className="client-home-card-copy"><strong>Mis turnos</strong><small>Próximamente vas a poder consultar tus turnos.</small></span>
          <ArrowRight size={18} aria-hidden="true" />
        </Link>
      </section>

      <p className="client-home-note">Estamos preparando nuevas herramientas para que puedas gestionar tu experiencia en Cutly.</p>
    </div>
  )
}
