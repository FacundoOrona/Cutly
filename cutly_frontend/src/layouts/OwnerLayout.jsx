import { useState } from 'react'
import { Building2, CalendarDays, LogOut, Menu, X } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import BrandMark from '../components/BrandMark.jsx'
import useAuth from '../auth/useAuth.js'

export default function OwnerLayout() {
  const { user, signOut } = useAuth()
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  const displayName = [user?.nombre, user?.apellido].filter(Boolean).join(' ') || user?.email

  async function handleSignOut() {
    setLogoutError('')
    try {
      await signOut()
    } catch {
      setLogoutError('La sesión local se cerró, pero no pudimos confirmar el cierre en el servidor.')
    }
  }

  function closeMenu() { setIsMenuOpen(false) }

  return (
    <main className="admin-page">
      <aside className={`admin-sidebar${isMenuOpen ? ' admin-sidebar-open' : ''}`}>
        <div className="admin-sidebar-top"><BrandMark /><button className="icon-button mobile-menu-close" onClick={closeMenu} aria-label="Cerrar menú"><X size={20} /></button></div>
        <p className="sidebar-label">MI ESPACIO</p>
        <nav aria-label="Navegación principal"><NavLink end to="/owner" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><Building2 size={17} /> Mis negocios</NavLink><NavLink to="/owner/turnos" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><CalendarDays size={17} /> Turnos</NavLink></nav>
        <div className="sidebar-user"><span className="user-avatar">{(user?.nombre?.[0] || user?.email?.[0] || 'C').toUpperCase()}</span><div className="user-details"><strong>{displayName}</strong><small>OWNER</small></div><button className="icon-button" onClick={handleSignOut} aria-label="Cerrar sesión"><LogOut size={17} /></button></div>
      </aside>
      {isMenuOpen && <button className="sidebar-backdrop" aria-label="Cerrar menú" onClick={closeMenu} />}
      <section className="admin-main">
        <header className="admin-header"><button className="icon-button mobile-menu-open" onClick={() => setIsMenuOpen(true)} aria-label="Abrir menú"><Menu size={20} /></button><span>Espacio del propietario</span><button className="header-signout" onClick={handleSignOut}><LogOut size={16} /> Cerrar sesión</button></header>
        <div className="admin-content">{logoutError && <p className="form-error" role="alert">{logoutError}</p>}<Outlet /></div>
        <footer className="dashboard-footer"><span><Building2 size={14} /> CUTLY</span><span>Espacio seguro para OWNER</span></footer>
      </section>
    </main>
  )
}
