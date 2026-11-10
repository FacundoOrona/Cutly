import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from '../layouts/AppLayout.jsx'
import DashboardPage from '../pages/DashboardPage.jsx'
import LoginPage from '../pages/LoginPage.jsx'
import NotFoundPage from '../pages/NotFoundPage.jsx'
import { RequireAuth, RequireRole } from '../auth/RouteGuards.jsx'
import AccessDeniedPage from '../pages/AccessDeniedPage.jsx'
import { AuthProvider } from '../auth/AuthContext.jsx'
import AdminLayout from '../layouts/AdminLayout.jsx'
import OwnersPage from '../features/owners/pages/OwnersPage.jsx'
import UsersPage from '../features/users/pages/UsersPage.jsx'
import UserDetailPage from '../features/users/pages/UserDetailPage.jsx'
import BusinessesPage from '../features/businesses/pages/BusinessesPage.jsx'
import BusinessDetailPage from '../features/businesses/pages/BusinessDetailPage.jsx'
import OwnerLayout from '../layouts/OwnerLayout.jsx'
import OwnerBusinessesPage from '../features/owner/pages/OwnerBusinessesPage.jsx'
import OwnerBusinessPage from '../features/owner/pages/OwnerBusinessPage.jsx'
import OwnerTurnosPage, { LegacyOwnerTurnosRedirect } from '../features/owner/pages/OwnerTurnosPage.jsx'
import OwnerClientesPage from '../features/owner/pages/OwnerClientesPage.jsx'
import OwnerDeudasPage from '../features/owner/pages/OwnerDeudasPage.jsx'
import OwnerTransaccionesPage from '../features/owner/pages/OwnerTransaccionesPage.jsx'
import OwnerEstadisticasPage from '../features/owner/pages/OwnerEstadisticasPage.jsx'
import EmployeeLayout from '../layouts/EmployeeLayout.jsx'
import EmployeeHomePage from '../features/employee/pages/EmployeeHomePage.jsx'
import EmployeeAgendaPage from '../features/employee/pages/EmployeeAgendaPage.jsx'
import EmployeeProfilePage from '../features/employee/pages/EmployeeProfilePage.jsx'
import EmployeeNotificationsPage from '../features/employee/pages/EmployeeNotificationsPage.jsx'
import { getRoleDestination } from '../auth/roleNavigation.js'
import useAuth from '../auth/useAuth.js'

function HomeRedirect() {
  const { isAuthenticated, role } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <Navigate to={getRoleDestination(role)} replace />
}

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomeRedirect />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/acceso-denegado" element={<AccessDeniedPage />} />
          <Route element={<RequireAuth />}>
            <Route element={<RequireRole role="SUPER_ADMIN" />}>
              <Route element={<AdminLayout />}>
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/admin/owners" element={<OwnersPage />} />
                <Route path="/admin/businesses" element={<BusinessesPage />} />
                <Route path="/admin/businesses/:id" element={<BusinessDetailPage />} />
                <Route path="/admin/users" element={<UsersPage />} />
                <Route path="/admin/users/:id" element={<UserDetailPage />} />
              </Route>
            </Route>
            <Route element={<RequireRole role="OWNER" />}>
              <Route element={<OwnerLayout />}>
                <Route path="/owner" element={<OwnerBusinessesPage />} />
                <Route path="/owner/negocios/:negocioId" element={<OwnerBusinessPage />} />
                <Route path="/owner/negocios/:negocioId/turnos" element={<LegacyOwnerTurnosRedirect />} />
                <Route path="/owner/turnos" element={<OwnerTurnosPage />} />
                <Route path="/owner/clientes" element={<OwnerClientesPage />} />
                <Route path="/owner/cuentas-corrientes" element={<OwnerDeudasPage />} />
                <Route path="/owner/transacciones" element={<OwnerTransaccionesPage />} />
                <Route path="/owner/estadisticas" element={<OwnerEstadisticasPage />} />
              </Route>
            </Route>
            <Route element={<RequireRole role="EMPLOYEE" />}>
              <Route element={<EmployeeLayout />}>
                <Route path="/employee" element={<EmployeeHomePage />} />
                <Route path="/employee/agenda" element={<EmployeeAgendaPage />} />
                <Route path="/employee/perfil" element={<EmployeeProfilePage />} />
                <Route path="/employee/notificaciones" element={<EmployeeNotificationsPage />} />
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
