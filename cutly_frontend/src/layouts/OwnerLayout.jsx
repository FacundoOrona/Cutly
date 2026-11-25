import { useState } from 'react'
import { BarChart3, Bell, Building2, CalendarDays, CreditCard, LoaderCircle, LogOut, Menu, ReceiptText, RefreshCw, Shield, UsersRound, X } from 'lucide-react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import BrandMark from '../components/BrandMark.jsx'
import useAuth from '../auth/useAuth.js'
import { OwnerBusinessProvider, useActiveOwnerBusiness } from '../features/owner/context/OwnerBusinessContext.jsx'
import { useOwnerUnreadNotificationCount } from '../features/owner/hooks/useOwnerNotifications.js'
import OwnerOnboardingPage from '../features/owner/pages/OwnerOnboardingPage.jsx'
import './OwnerLayout.css'

export default function OwnerLayout() {
  return <OwnerBusinessProvider><OwnerLayoutContent /></OwnerBusinessProvider>
}

function OwnerLayoutContent() {
  const { user, signOut } = useAuth()
  const { accessReady, accessError, isOnboarding, trialActivationPending, retryAccessLoad, subscriptionQuery, businesses, businessesQuery, activeBusinessId, setActiveBusinessId } = useActiveOwnerBusiness()
  const planName = subscriptionQuery.data?.status === 'TRIAL'
    ? 'Prueba gratuita'
    : subscriptionQuery.data?.nombrePlan
  const canLoadNotifications = accessReady && !isOnboarding && !trialActivationPending
  const unreadCountQuery = useOwnerUnreadNotificationCount({ enabled: canLoadNotifications })
  const unreadCount = unreadCountQuery.data?.cantidad
  const location = useLocation()
  const isOwnerHome = location.pathname.replace(/\/$/, '') === '/owner'
  const navigate = useNavigate()
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

  function selectBusiness(negocioId) {
    setActiveBusinessId(negocioId)
    if (/^\/owner\/negocios\/[^/]+$/.test(location.pathname)) {
      navigate(`/owner/negocios/${negocioId}`, { replace: true })
    }
  }

  return (
    <main className="admin-page">
      <aside className={`admin-sidebar${isMenuOpen ? ' admin-sidebar-open' : ''}`}>
        <div className="admin-sidebar-top"><BrandMark /><button className="icon-button mobile-menu-close" onClick={closeMenu} aria-label="Cerrar menú"><X size={20} /></button></div>
        {businessesQuery.isSuccess && businesses.length > 1 && <label className="owner-global-business-picker"><span>Negocio activo</span><select aria-label="Negocio activo" value={activeBusinessId} onChange={(event) => selectBusiness(event.target.value)}>{businesses.map((business) => <option key={business.negocioId} value={String(business.negocioId)}>{business.nombre}</option>)}</select></label>}
        <p className="sidebar-label">MI ESPACIO</p>
        <nav aria-label="Navegación principal"><NavLink end to="/owner" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><Building2 size={17} /> Mis negocios</NavLink>{canLoadNotifications && <><NavLink to="/owner/turnos" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><CalendarDays size={17} /> Turnos</NavLink><NavLink to="/owner/clientes" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><UsersRound size={17} /> Clientes</NavLink><NavLink to="/owner/cuentas-corrientes" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><CreditCard size={17} /> Cuentas corrientes</NavLink><NavLink to="/owner/transacciones" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><ReceiptText size={17} /> Transacciones</NavLink><NavLink to="/owner/estadisticas" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><BarChart3 size={17} /> Estadísticas</NavLink><NavLink to="/owner/notificaciones" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><Bell size={17} /><span>Notificaciones</span>{unreadCount > 0 && <span className="owner-notification-badge" aria-label={`${unreadCount} sin leer`}>{unreadCount > 99 ? '99+' : unreadCount}</span>}</NavLink></>}<NavLink to="/owner/seguridad" onClick={closeMenu} className={({ isActive }) => `sidebar-item${isActive ? ' sidebar-item-active' : ''}`}><Shield size={17} /> Seguridad</NavLink></nav>
        <div className="sidebar-user"><span className="user-avatar">{(user?.nombre?.[0] || user?.email?.[0] || 'C').toUpperCase()}</span><div className="user-details"><strong>{displayName}</strong><small>OWNER</small></div><button className="icon-button" onClick={handleSignOut} aria-label="Cerrar sesión"><LogOut size={17} /></button></div>
      </aside>
      {isMenuOpen && <button className="sidebar-backdrop" aria-label="Cerrar menú" onClick={closeMenu} />}
      <section className="admin-main owner-layout-main">
        <header className="admin-header"><button className="icon-button mobile-menu-open" onClick={() => setIsMenuOpen(true)} aria-label="Abrir menú"><Menu size={20} /></button><div className="owner-header-title"><span>Espacio del propietario</span>{planName && <span className="owner-plan-indicator" aria-label={`Plan actual: ${planName}`}>{planName}</span>}</div><button className="header-signout" onClick={handleSignOut}><LogOut size={16} /> Cerrar sesión</button></header>
        <div className="admin-content">{logoutError && <p className="form-error" role="alert">{logoutError}</p>}{!accessReady ? accessError ? <section className="owner-access-state" role="alert"><h1>No pudimos verificar tu espacio</h1><p>Necesitamos confirmar tus negocios y tu suscripción antes de cargar el panel.</p><button type="button" className="owner-create-button" onClick={retryAccessLoad} disabled={businessesQuery.isFetching || subscriptionQuery.isFetching}><RefreshCw size={15} /> Reintentar</button></section> : <div className="owner-access-state" role="status"><LoaderCircle size={20} className="spin" /> Verificando tu espacio…</div> : trialActivationPending && !isOwnerHome ? <OwnerOnboardingPage /> : isOnboarding && !isOwnerHome ? <OwnerOnboardingPage /> : <Outlet />}</div>
        <footer className="dashboard-footer"><span><Building2 size={14} /> CUTLY</span><span>Espacio seguro para OWNER</span></footer>
      </section>
    </main>
  )
}


