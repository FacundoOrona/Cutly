import apiClient from '../../../services/apiClient.js'

export async function getOwnerTransacciones({ negocioId, desde, hasta, metodoPago = [], tipo, q, page = 0, size = 20, sort }) {
  const params = new URLSearchParams()
  if (desde) params.append('desde', desde)
  if (hasta) params.append('hasta', hasta)
  for (const method of metodoPago) params.append('metodoPago', method)
  if (tipo) params.append('tipo', tipo)
  if (q) params.append('q', q)
  params.append('page', String(page))
  params.append('size', String(size))
  if (sort) params.append('sort', sort)

  const { data } = await apiClient.get(
    `/api/owner/negocios/${negocioId}/transacciones?${params.toString()}`,
  )
  return data
}

export function getOwnerTransaccionesErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a las transacciones de este negocio.'
  if (status === 404) return 'No se encontró el negocio solicitado.'
  if (status === 400) return message || 'Revisá las fechas y los filtros seleccionados.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || 'No pudimos cargar las transacciones. Intentá nuevamente.'
}
