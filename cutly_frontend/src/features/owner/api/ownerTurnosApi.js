import apiClient from '../../../services/apiClient.js'

export async function getOwnerTurnos({ negocioId, fecha, empleadoId, status }) {
  const params = {}
  if (fecha) params.fecha = fecha
  if (empleadoId) params.empleadoId = empleadoId
  if (status) params.status = status

  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/turnos`, { params })
  return data
}

export function getOwnerTurnosErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message

  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a esta sección.'
  if (status === 404) return 'No se encontró el negocio solicitado.'
  if (status === 400) return message || 'La fecha o alguno de los filtros no tiene un formato válido.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || 'No pudimos cargar los turnos. Intentá nuevamente.'
}
