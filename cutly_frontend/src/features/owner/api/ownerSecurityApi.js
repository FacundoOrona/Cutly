import apiClient from '../../../services/apiClient.js'

export async function changeOwnerPassword(payload) {
  const { data } = await apiClient.patch('/api/owner/seguridad/cambiar-contrasena', payload)
  return data
}

export function getOwnerPasswordError(error) {
  const status = error.response?.status
  const message = error.response?.data?.mensaje
  if (status === 400) return message || 'Revisá la contraseña actual y la nueva contraseña.'
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para cambiar la contraseña.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'No pudimos cambiar la contraseña por un problema del servidor. Intentá más tarde.'
  return message || 'No pudimos cambiar la contraseña. Intentá nuevamente.'
}
