import apiClient from '../../../services/apiClient.js'

const servicesEndpoint = (id) => `/api/owner/negocios/${id}/servicios`

export async function getOwnerBusinessServices(id) {
  const { data } = await apiClient.get(servicesEndpoint(id))
  return data
}

export async function createOwnerBusinessService({ id, service }) {
  const { data } = await apiClient.post(servicesEndpoint(id), service)
  return data
}

export async function updateOwnerBusinessService({ id, servicioId, changes }) {
  const { data } = await apiClient.patch(`${servicesEndpoint(id)}/${servicioId}`, changes)
  return data
}

export async function updateOwnerBusinessServiceStatus({ id, servicioId, activo }) {
  const action = activo ? 'activar' : 'desactivar'
  const { data } = await apiClient.patch(`${servicesEndpoint(id)}/${servicioId}/${action}`)
  return data
}

export function getOwnerBusinessServicesErrorMessage(error, action = 'consultar') {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a este negocio.'
  if (status === 404) return 'No se encontró el negocio o el servicio solicitado.'
  if (status === 400) return message || 'Revisá los datos ingresados e intentá nuevamente.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return message || 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || `No pudimos ${action} el servicio. Intentá nuevamente.`
}
