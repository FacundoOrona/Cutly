import apiClient from '../../../services/apiClient.js'

export async function getOwnerBusinesses() {
  const { data } = await apiClient.get('/api/owner/negocios')
  return data
}

export async function createOwnerBusiness(business) {
  const { data } = await apiClient.post('/api/owner/negocios', business)
  return data
}

export async function getOwnerBusiness(id) {
  const { data } = await apiClient.get(`/api/owner/negocios/${id}`)
  return data
}

export async function updateOwnerBusiness({ id, changes }) {
  const { data } = await apiClient.patch(`/api/owner/negocios/${id}`, changes)
  return data
}

export function getOwnerBusinessErrorMessage(error) {
  const status = error.response?.status
  if (status === 404) return 'No se encontró el negocio solicitado.'
  if (status === 403) return 'Tu cuenta no tiene acceso a este negocio.'
  if (status === 400) return 'El identificador del negocio no es válido.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return 'No pudimos cargar la información del negocio.'
}

export function getCreateOwnerBusinessErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene permiso para crear negocios.'
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
  return 'No pudimos crear el negocio. Intentá nuevamente.'
}

export function getUpdateOwnerBusinessErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene permiso para editar este negocio.'
  if (status === 404) return 'No se encontró el negocio solicitado.'
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
  return 'No pudimos actualizar el negocio. Intentá nuevamente.'
}
