import apiClient from '../../../services/apiClient.js'

export async function fetchOwners({ nombre, email, status, page, size }) {
  const params = { rol: 'OWNER', page, size, sort: 'usuarioId,DESC' }
  if (nombre.trim()) params.nombre = nombre.trim()
  if (email.trim()) params.email = email.trim()
  if (status) params.status = status

  const { data } = await apiClient.get('/api/admin/usuarios', { params })
  if (!data || !Array.isArray(data.content) || typeof data.totalPages !== 'number') {
    throw new Error('El servidor devolvió una respuesta de listado que no pudimos interpretar.')
  }
  return data
}

export async function createOwner(owner) {
  const { data } = await apiClient.post('/api/admin/owners', owner)
  // Never return or persist the JWT included in AuthResponse.
  return {
    userId: data?.userId,
    nombre: data?.nombre,
    apellido: data?.apellido,
    email: data?.email,
    status: data?.status,
  }
}

export function getCreateOwnerErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para crear owners.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  if (status === 400) {
    if (typeof body === 'string' && body.trim()) return body.trim()
    if (typeof body?.mensaje === 'string' && body.mensaje.trim()) return body.mensaje.trim()
    if (typeof body?.message === 'string' && body.message.trim()) return body.message.trim()
    if (Array.isArray(body?.errors)) {
      const messages = body.errors.map((item) => item?.defaultMessage || item?.message).filter((item) => typeof item === 'string')
      if (messages.length) return messages.join(' ')
    }
    return 'Revisá los datos ingresados e intentá nuevamente.'
  }
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return 'No pudimos crear el owner. Intentá nuevamente.'
}

export function getOwnersErrorMessage(error) {
  const status = error.response?.status
  const message = error.response?.data?.mensaje
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar este listado.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  if (status === 400) return typeof message === 'string' ? message : 'No pudimos consultar con esos filtros. Revisá los datos e intentá de nuevo.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return 'No pudimos cargar los owners. Intentá nuevamente.'
}
