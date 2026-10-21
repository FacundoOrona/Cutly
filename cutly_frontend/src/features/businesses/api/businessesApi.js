import apiClient from '../../../services/apiClient.js'

export async function fetchBusinesses() {
  const { data } = await apiClient.get('/api/admin/negocios')
  if (!Array.isArray(data)) {
    throw new Error('El servidor devolvió una respuesta de negocios que no pudimos interpretar.')
  }
  return data
}

export async function fetchBusinessById(id) {
  const { data } = await apiClient.get(`/api/admin/negocios/${id}`)
  if (!data || typeof data !== 'object' || data.negocioId === undefined) {
    throw new Error('El servidor devolvió una respuesta de negocio que no pudimos interpretar.')
  }
  return data
}

export async function updateBusinessStatus({ id, action }) {
  const operations = { suspend: 'suspender', reactivate: 'reactivar', deactivate: 'desactivar' }
  const operation = operations[action]
  if (!operation) throw new Error('La acción solicitada no es válida.')
  const { data } = await apiClient.patch(`/api/admin/negocios/${id}/${operation}`)
  return data
}

export function getBusinessesErrorMessage(error) {
  const status = error.response?.status
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar este listado.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return 'No pudimos cargar los negocios. Intentá nuevamente.'
}

export function getBusinessErrorMessage(error) {
  const status = error.response?.status
  if (status === 404) return 'Este negocio no existe o ya no está disponible.'
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar este negocio.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Podés reintentar.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá nuevamente.'
  return 'No pudimos cargar los datos del negocio.'
}

export function getUpdateBusinessStatusErrorMessage(error) {
  const status = error.response?.status
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para cambiar el estado de este negocio.'
  if (status === 404) return 'El negocio ya no existe o no está disponible. Actualizá el listado.'
  if (status === 409) return 'La transición de estado no es válida o el negocio cambió. Actualizá el listado.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Podés reintentar.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá nuevamente.'
  return 'No pudimos actualizar el estado del negocio. Intentá nuevamente.'
}
