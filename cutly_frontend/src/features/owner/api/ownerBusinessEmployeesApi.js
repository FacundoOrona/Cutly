import apiClient from '../../../services/apiClient.js'

const employeesEndpoint = (id) => `/api/owner/negocios/${id}/empleados`

export async function getOwnerBusinessEmployees(id) {
  const { data } = await apiClient.get(employeesEndpoint(id))
  return data
}

export async function createOwnerBusinessEmployee({ id, employee }) {
  const { data } = await apiClient.post(employeesEndpoint(id), employee)
  return data
}

export async function updateOwnerBusinessEmployee({ id, empleadoId, changes }) {
  const { data } = await apiClient.patch(`${employeesEndpoint(id)}/${empleadoId}`, changes)
  return data
}

export async function updateOwnerBusinessEmployeeStatus({ id, empleadoId, activo }) {
  const action = activo ? 'activar' : 'desactivar'
  const { data } = await apiClient.patch(`${employeesEndpoint(id)}/${empleadoId}/${action}`)
  return data
}

export function getOwnerBusinessEmployeesErrorMessage(error, action = 'consultar') {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message

  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a este negocio.'
  if (status === 404) return 'No se encontró el negocio o el empleado solicitado.'
  if (status === 409) return message || 'El email o el DNI ya pertenece a otro usuario.'
  if (status === 400) {
    const validationMessages = Array.isArray(body?.errors)
      ? body.errors.map((item) => item?.defaultMessage || item?.message).filter((item) => typeof item === 'string')
      : []
    return message || validationMessages.join(' ') || 'Revisá los datos ingresados e intentá nuevamente.'
  }
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || `No pudimos ${action} el empleado. Intentá nuevamente.`
}
