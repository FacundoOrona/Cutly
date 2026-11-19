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
        <p>Encontrá negocios, consultá sus servicios y gestioná tus turnos desde Cutly.</p>
      </section>

      <section className="client-home-actions" aria-label="Accesos rápidos">
        <Link className="client-home-card client-home-card-primary" to="/client/explorar">
          <span className="client-home-card-icon"><Compass size={20} aria-hidden="true" /></span>
          <span className="client-home-card-copy"><strong>Explorar negocios</strong><small>Buscá negocios y consultá los servicios disponibles.</small></span>
          <ArrowRight size={18} aria-hidden="true" />
        </Link>
        <Link className="client-home-card" to="/client/turnos">
          <span className="client-home-card-icon"><CalendarDays size={20} aria-hidden="true" /></span>
          <span className="client-home-card-copy"><strong>Mis turnos</strong><small>Consultá tus turnos y revisá el detalle de cada reserva.</small></span>
          <ArrowRight size={18} aria-hidden="true" />
        </Link>
      </section>

      <p className="client-home-note">Elegí un negocio para conocer sus servicios y opciones de reserva.</p>
    </div>
  )
}
