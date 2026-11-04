import apiClient from '../../../services/apiClient.js'

async function getOwnerEstadistica({ negocioId, endpoint, desde, hasta }) {
  const params = new URLSearchParams()
  if (desde) params.set('desde', desde)
  if (hasta) params.set('hasta', hasta)
  const query = params.toString()
  const { data } = await apiClient.get(
    `/api/owner/negocios/${negocioId}/estadisticas/${endpoint}${query ? `?${query}` : ''}`,
  )
  return data
}

export const getOwnerEstadisticasResumen = (filters) => getOwnerEstadistica({ ...filters, endpoint: 'resumen' })
export const getOwnerEstadisticasTurnos = (filters) => getOwnerEstadistica({ ...filters, endpoint: 'turnos' })
export const getOwnerEstadisticasEmpleados = (filters) => getOwnerEstadistica({ ...filters, endpoint: 'empleados' })
export const getOwnerEstadisticasServicios = (filters) => getOwnerEstadistica({ ...filters, endpoint: 'servicios' })

export function getOwnerEstadisticasErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a las estadísticas de este negocio.'
  if (status === 404) return 'No se encontró el negocio solicitado.'
  if (status === 400) return message || 'Revisá el rango de fechas seleccionado.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || 'No pudimos cargar las estadísticas. Intentá nuevamente.'
}
