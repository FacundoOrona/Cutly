import { useState } from 'react'
import { LogOut, Menu, ShieldCheck, Store, Users, X } from 'lucide-react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import BrandMark from '../components/BrandMark.jsx'
import useAuth from '../auth/useAuth.js'

export default function AdminLayout() {
  const { user, signOut } = useAuth()
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  const location = useLocation()
  const pageTitle = location.pathname === '/admin/owners' ? 'Owners' : location.pathname === '/admin/businesses' ? 'Negocios' : location.pathname.startsWith('/admin/users/') ? 'Detalle de usuario' : location.pathname === '/admin/users' ? 'Usuarios' : 'Panel principal'
  const displayName = [user?.nombre, user?.apellido].filter(Boolean).join(' ') || user?.email

  async function handleSignOut() {
    setLogoutError('')
    try {
      await signOut()
    } catch {
      setLogoutError('La sesión local se cerró, pero no pudimos confirmar el cierre en el servidor.')
    }
  }

  function closeMenu() {
    setIsMenuOpen(false)
  }

  return (
    <main className="admin-page">
      <aside className={`admin-sidebar${isMenuOpen ? ' admin-sidebar-open' : ''}`}>
        <div className="admin-sidebar-top"><BrandMark /><button className="icon-button mobile-menu-close" onClick={closeMenu} aria-label="Cerrar menú"><X size={20} /></button></div>
        <p className="sidebar-label">ADMINISTRACIÓN</p>
        <nav aria-label="Navegación principal">
          <NavLink end to="/dashboard" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><ShieldCheck size={17} /> Panel principal</NavLink>
          <NavLink to="/admin/owners" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><Users size={17} /> Owners</NavLink>
          <NavLink to="/admin/businesses" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><Store size={17} /> Negocios</NavLink>
          <NavLink to="/admin/users" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><Users size={17} /> Usuarios</NavLink>
        </nav>
        <div className="sidebar-user"><span className="user-avatar">{(user?.nombre?.[0] || user?.email?.[0] || 'C').toUpperCase()}</span><div className="user-details"><strong>{displayName}</strong><small>{user?.rol}</small></div><button className="icon-button" onClick={handleSignOut} aria-label="Cerrar sesión"><LogOut size={17} /></button></div>
      </aside>
      {isMenuOpen && <button className="sidebar-backdrop" aria-label="Cerrar menú" onClick={closeMenu} />}
      <section className="admin-main">
        <header className="admin-header"><button className="icon-button mobile-menu-open" onClick={() => setIsMenuOpen(true)} aria-label="Abrir menú"><Menu size={20} /></button><span>{pageTitle}</span><button className="header-signout" onClick={handleSignOut}><LogOut size={16} /> Cerrar sesión</button></header>
        <div className="admin-content">
          {logoutError && <p className="form-error" role="alert">{logoutError}</p>}
          <Outlet />
        </div>
        <footer className="dashboard-footer"><span><ShieldCheck size={14} /> CUTLY</span><span>Acceso seguro para SUPER_ADMIN</span></footer>
      </section>
    </main>
  )
}
