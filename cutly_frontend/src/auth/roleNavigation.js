export function getRoleDestination(role) {
  if (role === 'SUPER_ADMIN') return '/dashboard'
  if (role === 'OWNER') return '/owner'
  if (role === 'EMPLOYEE') return '/employee'
  if (role === 'CLIENT') return '/client'
  return '/acceso-denegado'
}

export function getLoginDestination(pathname) {
  return pathname === '/client' || pathname?.startsWith('/client/') ? '/client/login' : '/login'
}

export function canRoleAccessPath(role, pathname) {
  if (!pathname) return false
  if (role === 'SUPER_ADMIN') return pathname === '/dashboard' || pathname.startsWith('/admin/')
  if (role === 'OWNER') return pathname === '/owner' || pathname.startsWith('/owner/')
  if (role === 'EMPLOYEE') return pathname === '/employee' || pathname.startsWith('/employee/')
  if (role === 'CLIENT') return pathname === '/client' || pathname.startsWith('/client/')
  return false
}
