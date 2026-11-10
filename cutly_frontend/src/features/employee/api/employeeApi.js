import apiClient from '../../../services/apiClient.js'

const EMPLOYEE_PROFILE_FIELDS = ['nombre', 'apellido', 'dni', 'email']

export async function getEmployeeProfile() {
  const { data } = await apiClient.get('/api/empleado/perfil')
  return data
}

export async function getEmployeeBusiness() {
  const { data } = await apiClient.get('/api/empleado/perfil/negocio')
  return data
}

export async function getEmployeeNotifications() {
  const { data } = await apiClient.get('/api/empleado/notificaciones')
  return data
}

export async function markEmployeeNotificationAsRead(notificationId) {
  const { data } = await apiClient.patch(`/api/empleado/notificaciones/${notificationId}/leer`)
  return data
}

export async function markAllEmployeeNotificationsAsRead() {
  const { data } = await apiClient.patch('/api/empleado/notificaciones/leer-todas')
  return data
}

export function getEmployeeNotificationErrorMessage(error) {
  const status = error.response?.status
  const message = error.response?.data?.mensaje || error.response?.data?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permiso para consultar tus notificaciones.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'No pudimos completar la operación por un problema del servidor. Intentá más tarde.'
  return message || 'No pudimos actualizar las notificaciones. Intentá nuevamente.'
}

export async function updateEmployeeProfile(profile) {
  const { data } = await apiClient.patch('/api/empleado/perfil', profile)
  return data
}

export function getEmployeeBusinessErrorMessage(error) {
  const status = error.response?.status
  const message = error.response?.data?.mensaje || error.response?.data?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permiso para consultar la información del negocio.'
  if (status === 404) return message || 'No encontramos la información del negocio.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'No pudimos cargar la información del negocio. Intentá más tarde.'
  return message || 'No pudimos cargar la información del negocio.'
}

export function getEmployeeProfileUpdateError(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = body?.mensaje || body?.message
  if (status === 400) {
    const fieldErrors = {}
    if (Array.isArray(body?.errors)) {
      body.errors.forEach((item) => {
        const field = item?.field || item?.propertyPath
        const fieldMessage = item?.defaultMessage || item?.message
        if (EMPLOYEE_PROFILE_FIELDS.includes(field) && fieldMessage) fieldErrors[field] = fieldMessage
      })
    } else if (body?.errors && typeof body.errors === 'object') {
      Object.entries(body.errors).forEach(([field, fieldMessage]) => {
        if (EMPLOYEE_PROFILE_FIELDS.includes(field) && typeof fieldMessage === 'string') fieldErrors[field] = fieldMessage
      })
    }
    return { message: message || Object.values(fieldErrors)[0] || 'Revisá los datos ingresados.', fieldErrors }
  }
  if (status === 401) return { message: 'Tu sesión venció. Iniciá sesión nuevamente.', fieldErrors: {} }
  if (status === 403) return { message: 'No tenés permiso para modificar este perfil.', fieldErrors: {} }
  if (status === 404) return { message: message || 'No encontramos el perfil del empleado.', fieldErrors: {} }
  if (status === 409) {
    const field = message?.toLowerCase().includes('dni') ? 'dni' : message?.toLowerCase().includes('email') ? 'email' : null
    return { message: message || 'El email o el DNI ya está registrado.', fieldErrors: field ? { [field]: message } : {} }
  }
  if (!error.response) return { message: 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.', fieldErrors: {} }
  if (status >= 500) return { message: 'No pudimos actualizar tu perfil por un problema del servidor. Intentá más tarde.', fieldErrors: {} }
  return { message: message || 'No pudimos actualizar tu perfil. Intentá nuevamente.', fieldErrors: {} }
}

export async function getEmployeeAppointments(fecha, status) {
  const { data } = await apiClient.get('/api/empleado/turnos', { params: { fecha, status: status || undefined } })
  return data
}

export async function getEmployeeAppointmentHistory(fecha, status) {
  const { data } = await apiClient.get('/api/empleado/turnos/historial', { params: { fecha: fecha || undefined, status: status || undefined } })
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
