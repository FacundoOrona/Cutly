import apiClient from '../../../services/apiClient.js'

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
