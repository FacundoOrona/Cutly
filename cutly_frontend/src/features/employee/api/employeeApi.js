import apiClient from '../../../services/apiClient.js'

export async function getEmployeeProfile() {
  const { data } = await apiClient.get('/api/empleado/perfil')
  return data
}

export async function getEmployeeBusiness() {
  const { data } = await apiClient.get('/api/empleado/perfil/negocio')
  return data
}

export async function getEmployeeAppointments(fecha, status) {
  const { data } = await apiClient.get('/api/empleado/turnos', { params: { fecha, status: status || undefined } })
  return data
}

export async function confirmEmployeeAppointment(turnoId) {
  const { data } = await apiClient.patch(`/api/empleado/turnos/${turnoId}/confirmar`)
  return data
}

export async function cancelEmployeeAppointment(turnoId) {
  const { data } = await apiClient.patch(`/api/empleado/turnos/${turnoId}/cancelar`)
  return data
}

export async function completeEmployeeAppointment(turnoId) {
  const { data } = await apiClient.patch(`/api/empleado/turnos/${turnoId}/completar`)
  return data
}

export async function markEmployeeAppointmentNoShow(turnoId) {
  const { data } = await apiClient.patch(`/api/empleado/turnos/${turnoId}/no-show`)
  return data
}

export async function updateEmployeeAppointmentStatus({ turnoId, action }) {
  const actions = {
    confirmar: confirmEmployeeAppointment,
    cancelar: cancelEmployeeAppointment,
    completar: completeEmployeeAppointment,
    'no-show': markEmployeeAppointmentNoShow,
  }
  const update = actions[action]
  if (!update) throw new Error('Acción de turno no válida.')
  return update(turnoId)
}

export function getEmployeeAppointmentActionErrorMessage(error) {
  const status = error.response?.status
  const message = error.response?.data?.mensaje

  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 409) return 'El turno pudo haber cambiado desde otra sesión o esta acción ya no está permitida. Actualizamos la agenda.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'No pudimos actualizar el turno por un problema del servidor. Intentá más tarde.'
  return message || 'No pudimos actualizar el turno. Intentá nuevamente.'
}
