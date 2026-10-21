import { Navigate, Outlet, useLocation } from 'react-router-dom'
import useAuth from './useAuth.js'

function LoadingSession() {
  return <main className="session-loading" role="status">Verificando sesión…</main>
}

export function RequireAuth() {
  const { isAuthenticated, isInitializing } = useAuth()
  const location = useLocation()
  if (isInitializing) return <LoadingSession />
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />
}

export function RequireRole({ role }) {
  const { role: currentRole, isInitializing } = useAuth()
  if (isInitializing) return <LoadingSession />
  return currentRole === role ? <Outlet /> : <Navigate to="/acceso-denegado" replace />
}
