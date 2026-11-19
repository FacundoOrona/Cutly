import { AlertCircle, ArrowRight, CalendarDays, Compass, LoaderCircle, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import useAuth from '../../../auth/useAuth.js'
import ClientBusinessCard from '../components/ClientBusinessCard.jsx'
import { getClientBusinessesErrorMessage } from '../api/clientApi.js'
import useClientTopBusinesses from '../hooks/useClientTopBusinesses.js'
import './ClientHomePage.css'

export default function ClientHomePage() {
  const { user } = useAuth()
  const firstName = user?.nombre?.trim()
  const rankingQuery = useClientTopBusinesses()

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

      <section className="client-home-ranking" aria-labelledby="client-home-ranking-title">
        <header className="client-home-ranking-heading">
          <div><span className="client-home-eyebrow">DESCUBRÍ</span><h2 id="client-home-ranking-title">Negocios más elegidos</h2><p>Según los turnos recientes registrados en Cutly.</p></div>
          <Link to="/client/explorar">Ver todos <ArrowRight size={15} aria-hidden="true" /></Link>
        </header>
        {rankingQuery.isPending ? <div className="client-home-ranking-state" role="status"><LoaderCircle size={17} /> Cargando negocios destacados…</div>
          : rankingQuery.isError ? <div className="client-home-ranking-state is-error" role="alert"><AlertCircle size={17} /><span>{getClientBusinessesErrorMessage(rankingQuery.error)}</span><button type="button" onClick={() => rankingQuery.refetch()} disabled={rankingQuery.isFetching}>Reintentar</button></div>
            : rankingQuery.data.length === 0 ? <p className="client-home-ranking-empty">Todavía no hay negocios destacados para mostrar.</p>
              : <div className="client-home-ranking-grid">{rankingQuery.data.map((business) => <ClientBusinessCard business={business} key={business.negocioId} />)}</div>}
      </section>

      <p className="client-home-note">Elegí un negocio para conocer sus servicios y opciones de reserva.</p>
    </div>
  )
}
