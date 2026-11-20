import apiClient from '../../../services/apiClient.js'

export async function getOwnerNotifications(page = 0, size = 20) {
  const { data } = await apiClient.get('/api/owner/notificaciones', { params: { page, size } })
  return data
}

export async function getOwnerUnreadNotificationCount() {
  const { data } = await apiClient.get('/api/owner/notificaciones/no-leidas/contador')
  return data
}

export async function markOwnerNotificationAsRead(notificationId) {
  const { data } = await apiClient.patch(`/api/owner/notificaciones/${notificationId}/leer`)
  return data
}

export async function markAllOwnerNotificationsAsRead() {
  const { data } = await apiClient.patch('/api/owner/notificaciones/leer-todas')
  return data
}

export function getOwnerNotificationErrorMessage(error) {
  const status = error.response?.status
  const message = error.response?.data?.mensaje || error.response?.data?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permiso para consultar tus notificaciones.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'No pudimos completar la operación por un problema del servidor. Intentá más tarde.'
  return message || 'No pudimos actualizar las notificaciones. Intentá nuevamente.'
}
