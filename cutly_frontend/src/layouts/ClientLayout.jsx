import { CalendarDays, Compass, Home, LogOut, UserRound, Bell } from 'lucide-react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import BrandMark from '../components/BrandMark.jsx'
import useAuth from '../auth/useAuth.js'
import { useClientUnreadNotificationCount } from '../features/client/hooks/useClientNotifications.js'
import './ClientLayout.css'

const navigation = [
  { to: '/client', label: 'Inicio', icon: Home, end: true },
  { to: '/client/explorar', label: 'Explorar', icon: Compass },
  { to: '/client/turnos', label: 'Mis turnos', icon: CalendarDays },
  { to: '/client/cuenta', label: 'Cuenta', icon: UserRound },
]

function ClientNavigation({ onNavigate }) {
  return (
    <nav className="client-navigation" aria-label="Navegación de cliente">
      {navigation.map(({ to, label, icon: Icon, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          onClick={onNavigate}
          className={({ isActive }) => `client-navigation-link${isActive ? ' client-navigation-link-active' : ''}`}
        >
          <Icon size={19} aria-hidden="true" />
          <span>{label}</span>
        </NavLink>
      ))}
    </nav>
  )
}

export default function ClientLayout() {
  const { user, signOut } = useAuth()
  const unreadQuery = useClientUnreadNotificationCount()
  const unreadCount = unreadQuery.data?.cantidad
  const displayName = [user?.nombre, user?.apellido].filter(Boolean).join(' ') || user?.email || 'Cliente'

  async function handleSignOut() {
    try {
      await signOut()
    } catch {
      // signOut clears the local session even when server revocation fails.
    }
  }

  return (
    <div className="client-shell">
      <aside className="client-sidebar">
        <BrandMark to="/client" />
        <p className="client-sidebar-label">MI ESPACIO</p>
        <ClientNavigation />
        <div className="client-sidebar-account">
          <span className="client-avatar" aria-hidden="true">{displayName[0]?.toUpperCase() || 'C'}</span>
          <span className="client-display-name" title={displayName}>{displayName}</span>
          <button type="button" className="client-logout-button" onClick={handleSignOut} aria-label="Cerrar sesión" title="Cerrar sesión">
            <LogOut size={17} />
          </button>
        </div>
      </aside>

      <section className="client-main">
        <header className="client-topbar">
          <div className="client-mobile-brand"><BrandMark to="/client" /></div>
          <span className="client-topbar-greeting">Hola, {user?.nombre || 'bienvenido'}.</span>
          <Link className="client-topbar-notifications" to="/client/notificaciones" aria-label={unreadCount > 0 ? `Notificaciones, ${unreadCount} sin leer` : 'Notificaciones'} title="Notificaciones">
            <Bell size={17} aria-hidden="true" />{unreadCount > 0 && <span>{unreadCount > 99 ? '99+' : unreadCount}</span>}
          </Link>
          <button type="button" className="client-topbar-logout" onClick={handleSignOut}>
            <LogOut size={16} aria-hidden="true" /> Cerrar sesión
          </button>
        </header>
        <main className="client-content">
          <Outlet />
        </main>
      </section>

      <div className="client-bottom-navigation"><ClientNavigation /></div>
    </div>
  )
}
