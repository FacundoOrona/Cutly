import { useState } from 'react'
import { LogOut, Menu, Scissors, ShieldCheck, X } from 'lucide-react'
import BrandMark from '../components/BrandMark.jsx'
import useAuth from '../auth/useAuth.js'

export default function DashboardPage() {
  const { user, signOut } = useAuth()
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [logoutError, setLogoutError] = useState('')

  async function handleSignOut() {
    setLogoutError('')
    try {
      await signOut()
    } catch {
      setLogoutError('La sesión local se cerró, pero no pudimos confirmar el cierre en el servidor.')
    }
  }

  const displayName = [user?.nombre, user?.apellido].filter(Boolean).join(' ') || user?.email

  return (
    <main className="admin-page">
      <aside className={`admin-sidebar${isMenuOpen ? ' admin-sidebar-open' : ''}`}>
        <div className="admin-sidebar-top"><BrandMark /><button className="icon-button mobile-menu-close" onClick={() => setIsMenuOpen(false)} aria-label="Cerrar menú"><X size={20} /></button></div>
        <p className="sidebar-label">ADMINISTRACIÓN</p>
        <nav aria-label="Navegación principal"><a className="sidebar-item sidebar-item-active" href="/dashboard" aria-current="page"><ShieldCheck size={17} /> Panel principal</a></nav>
        <div className="sidebar-user"><span className="user-avatar">{(user?.nombre?.[0] || user?.email?.[0] || 'C').toUpperCase()}</span><div className="user-details"><strong>{displayName}</strong><small>{user?.rol}</small></div><button className="icon-button" onClick={handleSignOut} aria-label="Cerrar sesión"><LogOut size={17} /></button></div>
      </aside>
      {isMenuOpen && <button className="sidebar-backdrop" aria-label="Cerrar menú" onClick={() => setIsMenuOpen(false)} />}
      <section className="admin-main">
        <header className="admin-header"><button className="icon-button mobile-menu-open" onClick={() => setIsMenuOpen(true)} aria-label="Abrir menú"><Menu size={20} /></button><span>Panel principal</span><button className="header-signout" onClick={handleSignOut}><LogOut size={16} /> Cerrar sesión</button></header>
        <div className="admin-content">
          {logoutError && <p className="form-error" role="alert">{logoutError}</p>}
          <span className="eyebrow"><span className="eyebrow-dot" /> ESPACIO ADMINISTRATIVO</span>
          <h1>Hola, <span>{user?.nombre || 'bienvenido'}.</span></h1>
          <p className="dashboard-lead">Sesión iniciada como {user?.email}. Desde aquí vas a poder administrar Cutly.</p>
          <section className="admin-welcome"><span className="admin-welcome-icon"><Scissors size={20} /></span><div><strong>Tu panel está listo</strong><p>Los módulos administrativos estarán disponibles a medida que se conecten sus servicios.</p></div></section>
        </div>
        <footer className="dashboard-footer"><span><Scissors size={14} /> CUTLY</span><span>Acceso seguro para SUPER_ADMIN</span></footer>
      </section>
    </main>
  )
}
