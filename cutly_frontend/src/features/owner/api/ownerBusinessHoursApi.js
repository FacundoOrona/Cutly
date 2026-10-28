import apiClient from '../../../services/apiClient.js'

const hoursEndpoint = (id) => `/api/owner/negocios/${id}/horarios`

export async function getOwnerBusinessHours(id) {
  const { data } = await apiClient.get(hoursEndpoint(id))
  return data
}

export async function createOwnerBusinessHour({ id, hour }) {
  const { data } = await apiClient.post(hoursEndpoint(id), hour)
  return data
}

export async function updateOwnerBusinessHour({ id, horarioId, changes }) {
  const { data } = await apiClient.patch(`${hoursEndpoint(id)}/${horarioId}`, changes)
  return data
}

export async function deleteOwnerBusinessHour({ id, horarioId }) {
  await apiClient.delete(`${hoursEndpoint(id)}/${horarioId}`)
}

export function getOwnerBusinessHoursErrorMessage(error, action = 'consultar') {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a este negocio.'
  if (status === 404) return 'No se encontró el negocio o el horario solicitado.'
  if (status === 409) return message || 'El tramo se superpone con otro horario o entra en conflicto con un día cerrado.'
  if (status === 400) return message || 'Revisá el día y las horas ingresadas.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return message || 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || `No pudimos ${action} el horario. Intentá nuevamente.`
}
