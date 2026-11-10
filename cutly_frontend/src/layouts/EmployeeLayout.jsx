import { Bell, CalendarDays, History, Home, LogOut, Menu, UserRound, X } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { useState } from 'react'
import BrandMark from '../components/BrandMark.jsx'
import useAuth from '../auth/useAuth.js'
import { useEmployeeNotifications } from '../features/employee/hooks/useEmployeeNotifications.js'
import './EmployeeLayout.css'

export default function EmployeeLayout() {
  const { user, signOut } = useAuth()
  const notificationsQuery = useEmployeeNotifications()
  const unreadCount = (notificationsQuery.data ?? []).filter((notification) => notification.leida === false).length
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const displayName = [user?.nombre, user?.apellido].filter(Boolean).join(' ') || user?.email

  async function handleSignOut() {
    try {
      await signOut()
    } catch {
      // signOut always clears the local session, even if server revocation fails.
    }
  }

  return (
    <main className="admin-page">
      <aside className={`admin-sidebar${isMenuOpen ? ' admin-sidebar-open' : ''}`}>
        <div className="admin-sidebar-top"><BrandMark to="/employee" /><button className="icon-button mobile-menu-close" onClick={() => setIsMenuOpen(false)} aria-label="Cerrar menú"><X size={20} /></button></div>
        <p className="sidebar-label">MI ESPACIO</p>
        <nav aria-label="Navegación del empleado">
          <NavLink end to="/employee" onClick={() => setIsMenuOpen(false)} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><Home size={17} /> Inicio</NavLink>
          <NavLink to="/employee/agenda" onClick={() => setIsMenuOpen(false)} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><CalendarDays size={17} /> Agenda</NavLink>
          <p className="sidebar-label employee-sidebar-upcoming-label">MI CUENTA</p>
          <div className="sidebar-item employee-sidebar-upcoming" aria-disabled="true"><History size={17} /><span>Historial</span><small>Próximamente</small></div>
          <NavLink to="/employee/perfil" onClick={() => setIsMenuOpen(false)} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><UserRound size={17} /> Mi perfil</NavLink>
          <NavLink to="/employee/notificaciones" onClick={() => setIsMenuOpen(false)} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}>
            <Bell size={17} /><span>Notificaciones</span>{unreadCount > 0 && <span className="employee-notification-badge" aria-label={`${unreadCount} sin leer`}>{unreadCount}</span>}
          </NavLink>
        </nav>
        <div className="sidebar-user">
          <span className="user-avatar">{(user?.nombre?.[0] || user?.email?.[0] || 'C').toUpperCase()}</span>
          <div className="user-details"><strong>{displayName}</strong><small>EMPLOYEE</small></div>
          <button className="icon-button" onClick={handleSignOut} aria-label="Cerrar sesión"><LogOut size={17} /></button>
        </div>
      </aside>
      {isMenuOpen && <button className="sidebar-backdrop" aria-label="Cerrar menú" onClick={() => setIsMenuOpen(false)} />}
      <section className="admin-main">
        <header className="admin-header"><button className="icon-button mobile-menu-open" onClick={() => setIsMenuOpen(true)} aria-label="Abrir menú"><Menu size={20} /></button><span>Espacio del empleado</span><button className="header-signout" onClick={handleSignOut}><LogOut size={16} /> Cerrar sesión</button></header>
        <div className="admin-content"><Outlet /></div>
        <footer className="dashboard-footer"><span>CUTLY</span><span>Espacio seguro para EMPLOYEE</span></footer>
      </section>
    </main>
  )
}
