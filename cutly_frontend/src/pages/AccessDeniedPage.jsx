import { ShieldX } from 'lucide-react'
import { Link } from 'react-router-dom'
import BrandMark from '../components/BrandMark.jsx'
import useAuth from '../auth/useAuth.js'

export default function AccessDeniedPage() {
  const { signOut } = useAuth()
  return (
    <main className="denied-page">
      <BrandMark />
      <section className="denied-card">
        <span className="denied-icon"><ShieldX size={25} /></span>
        <span className="eyebrow">ACCESO RESTRINGIDO</span>
        <h1>Esta área no está disponible para tu cuenta.</h1>
        <p>El panel administrativo requiere el rol SUPER_ADMIN.</p>
        <div className="denied-actions"><Link className="login-submit denied-link" to="/login">Volver al inicio</Link><button className="text-button" onClick={() => signOut()}>Cerrar sesión</button></div>
      </section>
      <footer className="denied-footer">© 2026 Cutly</footer>
    </main>
  )
}
