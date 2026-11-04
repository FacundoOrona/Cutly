import { LogOut } from 'lucide-react'
import { Outlet } from 'react-router-dom'
import BrandMark from '../components/BrandMark.jsx'
import useAuth from '../auth/useAuth.js'

export default function EmployeeLayout() {
  const { user, signOut } = useAuth()
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
      <aside className="admin-sidebar">
        <div className="admin-sidebar-top"><BrandMark to="/employee" /></div>
        <p className="sidebar-label">MI ESPACIO</p>
        <nav aria-label="Navegación del empleado" />
        <div className="sidebar-user">
          <span className="user-avatar">{(user?.nombre?.[0] || user?.email?.[0] || 'C').toUpperCase()}</span>
          <div className="user-details"><strong>{displayName}</strong><small>EMPLOYEE</small></div>
          <button className="icon-button" onClick={handleSignOut} aria-label="Cerrar sesión"><LogOut size={17} /></button>
        </div>
      </aside>
      <section className="admin-main">
        <header className="admin-header"><span>Espacio del empleado</span><button className="header-signout" onClick={handleSignOut}><LogOut size={16} /> Cerrar sesión</button></header>
        <div className="admin-content"><Outlet /></div>
        <footer className="dashboard-footer"><span>CUTLY</span><span>Espacio seguro para EMPLOYEE</span></footer>
      </section>
    </main>
  )
}
