import apiClient from '../../../services/apiClient.js'

export async function getOwnerTurnos({ negocioId, fecha, empleadoId, status }) {
  const params = {}
  if (fecha) params.fecha = fecha
  if (empleadoId) params.empleadoId = empleadoId
  if (status) params.status = status

  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/turnos`, { params })
  return data
}

export async function createOwnerTurno({ negocioId, turno }) {
  const { data } = await apiClient.post(`/api/owner/negocios/${negocioId}/turnos`, turno)
  return data
}

export function getOwnerClientesLocalesErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a los clientes de este negocio.'
  if (status === 404) return 'No se encontró el negocio solicitado.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return message || 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || 'No pudimos buscar los clientes. Intentá nuevamente.'
}

export function getCreateOwnerTurnoErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 400) return message || 'Revisá los datos ingresados, el servicio y el horario elegido.'
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return message || 'Tu cuenta no tiene permiso para crear turnos en este negocio.'
  if (status === 404) return message || 'No se encontró el negocio, servicio, empleado o cliente seleccionado.'
  if (status === 409) return message || 'Ese horario ya no está disponible. Elegí otro horario.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return message || 'No pudimos crear el turno por un problema del servidor. Intentá más tarde.'
  return message || 'No pudimos crear el turno. Intentá nuevamente.'
}

export async function searchOwnerClientesLocales({ negocioId, query }) {
  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/clientes-locales`, { params: { q: query } })
  return data
}

export async function updateOwnerTurnoStatus({ negocioId, turnoId, action }) {
  const allowedActions = ['confirmar', 'rechazar', 'cancelar', 'completar', 'no-show']
  if (!allowedActions.includes(action)) throw new Error('Acción de turno no válida.')
  const { data } = await apiClient.patch(`/api/owner/negocios/${negocioId}/turnos/${turnoId}/${action}`)
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

export function getUpdateOwnerTurnoErrorMessage(error, actionLabel) {
  const status = error.response?.status
  const body = error.response?.data
  const serverMessage = typeof body === 'string' ? body : body?.mensaje || body?.message

  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permiso para actualizar este turno.'
  if (status === 404) return 'El turno ya no está disponible para este negocio. Actualizá el listado.'
  if (status === 409) return serverMessage || 'El estado del turno cambió o no permite esta acción. Actualizamos el listado.'
  if (!error.response) return `No pudimos ${actionLabel} el turno por un problema de conexión.`
  if (status >= 500) return `No pudimos ${actionLabel} el turno por un problema del servidor.`
  return serverMessage || `No pudimos ${actionLabel} el turno. Intentá nuevamente.`
}
