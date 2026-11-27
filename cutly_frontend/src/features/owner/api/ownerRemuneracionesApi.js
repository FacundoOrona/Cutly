import apiClient from '../../../services/apiClient.js'

export async function getOwnerRemuneracionesResumen(negocioId) {
  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/empleados/resumen-remuneraciones`)
  return data
}

export async function getOwnerRemuneraciones({ negocioId, empleadoId, estado, desde, hasta, page = 0, size = 20 }) {
  const params = { page, size }
  if (empleadoId) params.empleadoId = empleadoId
  if (estado) params.estado = estado
  if (desde) params.desde = desde
  if (hasta) params.hasta = hasta
  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/remuneraciones`, { params })
  return data
}

export async function getOwnerRemuneracion({ negocioId, remuneracionId }) {
  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/remuneraciones/${remuneracionId}`)
  return data
}

export function getOwnerRemuneracionesErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a las remuneraciones de este negocio.'
  if (status === 404) return 'No se encontró el negocio o la remuneración solicitada.'
  if (status === 400) return message || 'Revisá los filtros seleccionados.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || 'No pudimos cargar las remuneraciones. Intentá nuevamente.'
}
