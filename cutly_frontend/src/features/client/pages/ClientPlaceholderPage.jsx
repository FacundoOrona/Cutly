import { ArrowLeft, Clock3 } from 'lucide-react'
import { Link } from 'react-router-dom'
import './ClientPlaceholderPage.css'

export default function ClientPlaceholderPage({ title, description }) {
  return (
    <section className="client-placeholder-page">
      <Link className="client-placeholder-back" to="/client"><ArrowLeft size={15} aria-hidden="true" /> Volver al inicio</Link>
      <div className="client-placeholder-card">
        <span className="client-placeholder-icon"><Clock3 size={22} aria-hidden="true" /></span>
        <span className="client-placeholder-eyebrow">PRÓXIMAMENTE</span>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
    </section>
  )
}
