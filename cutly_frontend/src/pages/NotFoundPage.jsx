import { ArrowLeft } from 'lucide-react'
import { Link } from 'react-router-dom'
import BrandMark from '../components/BrandMark.jsx'

export default function NotFoundPage() {
  return (
    <main className="not-found-page">
      <BrandMark />
      <div className="not-found-content"><span className="eyebrow"><span className="eyebrow-dot" /> PÁGINA NO ENCONTRADA</span><p className="error-code">404</p><h1>Por acá no era.</h1><p className="not-found-copy">Parece que esta página se tomó el día libre. Volvamos al salón.</p><Link className="back-link" to="/dashboard"><ArrowLeft size={16} /> Volver al inicio</Link></div>
      <footer className="dashboard-footer"><span>© 2026 Cutly</span><span>Simple para vos. Mejor para tu salón.</span></footer>
    </main>
  )
}
