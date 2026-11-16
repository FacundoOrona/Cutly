import apiClient from '../../../services/apiClient.js'

export async function fetchClientAppointments() {
  const { data } = await apiClient.get('/api/cliente/turnos')
  if (!Array.isArray(data)) throw new Error('El servidor devolvió una respuesta de turnos que no pudimos interpretar.')
  return data
}

export async function fetchClientAppointmentById(turnoId) {
  const { data } = await apiClient.get(`/api/cliente/turnos/${turnoId}`)
  if (!data || typeof data !== 'object' || data.turnoId === undefined) {
    throw new Error('El servidor devolvió una respuesta de turno que no pudimos interpretar.')
  }
  return data
}

export async function cancelClientAppointment(turnoId) {
  const { data } = await apiClient.patch(`/api/cliente/turnos/${turnoId}/cancelar`)
  if (!data || typeof data !== 'object' || data.turnoId === undefined) {
    throw new Error('El servidor devolvió una respuesta de cancelación que no pudimos interpretar.')
  }
  return data
}

export async function fetchClientBusinesses({ nombre, page = 0, size = 20, sort = 'nombre,asc' }) {
  const params = { page, size, sort }
  if (nombre?.trim()) params.nombre = nombre.trim()

  const { data } = await apiClient.get('/api/cliente/negocios', { params })
  if (!data || !Array.isArray(data.content) || typeof data.totalPages !== 'number') {
    throw new Error('El servidor devolvió una respuesta de negocios que no pudimos interpretar.')
  }
  return data
}

export async function fetchClientBusiness(id) {
  const { data } = await apiClient.get(`/api/cliente/negocios/${id}`)
  if (!data || typeof data !== 'object' || data.negocioId === undefined) {
    throw new Error('El servidor devolvió una respuesta de negocio que no pudimos interpretar.')
  }
  return data
}

export async function fetchClientBusinessServices(id) {
  const { data } = await apiClient.get(`/api/cliente/negocios/${id}/servicios`)
  if (!Array.isArray(data)) throw new Error('El servidor devolvió una respuesta de servicios que no pudimos interpretar.')
  return data
}

export async function fetchClientBusinessEmployees(id) {
  const { data } = await apiClient.get(`/api/cliente/negocios/${id}/empleados`)
  if (!Array.isArray(data)) throw new Error('El servidor devolvió una respuesta de empleados que no pudimos interpretar.')
  return data
}

export async function fetchClientAvailability({ negocioId, fecha, servicioId, empleadoId }) {
  const params = { fecha, servicioId }
  if (empleadoId !== undefined && empleadoId !== null) params.empleadoId = empleadoId

  const { data } = await apiClient.get(`/api/cliente/negocios/${negocioId}/disponibilidad`, { params })
  if (!Array.isArray(data)) throw new Error('El servidor devolvió una respuesta de disponibilidad que no pudimos interpretar.')
  return data
}

export async function fetchClientEmployeeAvailability({ negocioId, empleadoId, fecha, servicioId }) {
  const { data } = await apiClient.get(
    `/api/cliente/negocios/${negocioId}/empleados/${empleadoId}/disponibilidad`,
    { params: { fecha, servicioId } },
  )
  if (!Array.isArray(data)) throw new Error('El servidor devolvió una respuesta de disponibilidad que no pudimos interpretar.')
  return data
}

export async function createClientAppointment(appointment) {
  const { negocioId, servicioId, empleadoId, fechaHoraInicio, observaciones } = appointment
  const { data } = await apiClient.post('/api/cliente/turnos', {
    negocioId,
    servicioId,
    empleadoId,
    fechaHoraInicio,
    observaciones,
  })
  return data
}

export const isClientAvailabilityConflict = (error) => error.response?.status === 409

export function getClientAppointmentErrorMessage(error) {
  const status = error.response?.status
  const backendMessage = error.response?.data?.mensaje
  if (status === 409 && backendMessage) return backendMessage
  if (status === 400) return backendMessage || 'La solicitud de reserva no es válida.'
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para realizar esta reserva.'
  if (status === 404) return backendMessage || 'El negocio, servicio o empleado ya no está disponible.'
  if (status === 409) return 'Ese horario ya no está disponible. Consultá los horarios nuevamente.'
  if (status >= 500) return 'No pudimos completar la reserva en este momento.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return backendMessage || error.message || 'No pudimos completar la reserva.'
}

export function getClientBusinessErrorMessage(error) {
  if (error.response?.status === 404) return 'Este negocio no existe o ya no está disponible.'
  return getClientBusinessesErrorMessage(error)
}

export function getClientBusinessServicesErrorMessage(error) {
  const status = error.response?.status
  if (status === 404) return 'El negocio ya no está disponible.'
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar los servicios.'
  if (status >= 500) return 'No pudimos cargar los servicios en este momento.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return error.message || 'No pudimos cargar los servicios. Intentá nuevamente.'
}

export function getClientBusinessEmployeesErrorMessage(error) {
  const status = error.response?.status
  if (status === 404) return 'El negocio ya no está disponible.'
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar el equipo.'
  if (status >= 500) return 'No pudimos cargar el equipo en este momento.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return error.message || 'No pudimos cargar el equipo. Intentá nuevamente.'
}

export function getClientBusinessesErrorMessage(error) {
  const status = error.response?.status
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar los negocios.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return error.message || 'No pudimos cargar los negocios. Intentá nuevamente.'
}

export function getClientAppointmentsErrorMessage(error) {
  const status = error.response?.status
  const backendMessage = error.response?.data?.mensaje
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar tus turnos.'
  if (status >= 500) return 'No pudimos cargar tus turnos en este momento.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return backendMessage || error.message || 'No pudimos cargar tus turnos. Intentá nuevamente.'
}

export function getClientAppointmentDetailErrorMessage(error) {
  const status = error.response?.status
  const backendMessage = error.response?.data?.mensaje
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar este turno.'
  if (status === 404) return 'No encontramos este turno o no está disponible para tu cuenta.'
  if (status >= 500) return 'No pudimos cargar el detalle de este turno en este momento.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return backendMessage || error.message || 'No pudimos cargar el detalle de este turno. Intentá nuevamente.'
}

export function getClientAppointmentCancellationErrorMessage(error) {
  const status = error.response?.status
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para cancelar este turno.'
  if (status === 404) return 'El turno no está disponible para esta cuenta.'
  if (status === 409) return 'El turno cambió de estado y ya no puede cancelarse. Actualizamos su información.'
  if (!error.response) return 'No pudimos confirmar el resultado. Actualizamos la información del turno; revisá su estado antes de volver a intentarlo.'
  return 'No se pudo completar la cancelación del turno. Intentá nuevamente.'
}
