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

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
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
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
