import { Scissors } from 'lucide-react'
import useAuth from '../auth/useAuth.js'

export default function DashboardPage() {
  const { user } = useAuth()

  return (
    <>
          <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO ADMINISTRATIVO</span>
          <h1>Hola, <span>{user?.nombre || 'bienvenido'}.</span></h1>
          <p className="dashboard-lead">Sesión iniciada como {user?.email}. Desde aquí vas a poder administrar Cutly.</p>
          <section className="admin-welcome"><span className="admin-welcome-icon"><Scissors size={20} /></span><div><strong>Tu panel está listo</strong><p>Los módulos administrativos estarán disponibles a medida que se conecten sus servicios.</p></div></section>
    </>
  )
}
