import { ArrowUpRight, CalendarDays, Clock3, Scissors, Sparkles, UsersRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import BrandMark from '../components/BrandMark.jsx'

const highlights = [
  { icon: CalendarDays, title: 'Agenda del día', text: 'Tu jornada, organizada de un vistazo.', tag: 'PRÓXIMAMENTE' },
  { icon: UsersRound, title: 'Tu equipo', text: 'Todo listo para trabajar en equipo.', tag: 'PRÓXIMAMENTE' },
  { icon: Clock3, title: 'Más tiempo', text: 'Menos tareas manuales, más salón.', tag: 'PRÓXIMAMENTE' },
]

export default function DashboardPage() {
  return (
    <main className="dashboard-page">
      <header className="dashboard-nav"><BrandMark /><Link className="nav-link" to="/login">Vista de acceso <ArrowUpRight size={15} /></Link></header>
      <section className="dashboard-content">
        <span className="eyebrow"><span className="eyebrow-dot" /> TU SALÓN, MÁS SIMPLE</span>
        <h1>Un buen día<br />empieza <span>por acá.</span></h1>
        <p className="dashboard-lead">Estamos preparando un espacio más simple para organizar tu peluquería. Pronto, todo lo que necesitás va a estar en un mismo lugar.</p>
        <div className="dashboard-note"><span className="note-icon"><Sparkles size={17} /></span><div><strong>Tu espacio está tomando forma</strong><p>Las funciones de gestión se habilitarán al conectar los servicios reales del backend.</p></div></div>
        <div className="feature-grid">{highlights.map(({ icon: Icon, title, text, tag }) => <article className="feature-card" key={title}><span className="feature-icon"><Icon size={19} /></span><span className="feature-tag">{tag}</span><h2>{title}</h2><p>{text}</p></article>)}</div>
        <Link className="login-cta" to="/login">Volver a la pantalla de acceso <ArrowUpRight size={16} /></Link>
      </section>
      <footer className="dashboard-footer"><span><Scissors size={14} /> CUTLY</span><span>Simple para vos. Mejor para tu salón.</span></footer>
    </main>
  )
}
