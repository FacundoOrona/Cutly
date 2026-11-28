import apiClient from '../../../services/apiClient.js'

export async function getOwnerRemuneracionesResumen(negocioId) {
  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/empleados/resumen-remuneraciones`)
  return data
}

export async function getOwnerRemuneraciones({ negocioId, empleadoId, estado, desde, hasta, page = 0, size = 20 }) {
  const params = { page, size }
  if (empleadoId) params.empleadoId = empleadoId
  if (estado) params.estado = estado
  if (desde) params.desde = desde
  if (hasta) params.hasta = hasta
  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/remuneraciones`, { params })
  return data
}

export async function getOwnerRemuneracion({ negocioId, remuneracionId }) {
  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/remuneraciones/${remuneracionId}`)
  return data
}

export async function createOwnerLiquidacion({ negocioId, body, idempotencyKey }) {
  const { data } = await apiClient.post(
    `/api/owner/negocios/${negocioId}/liquidaciones`,
    body,
    { headers: { 'Idempotency-Key': idempotencyKey } },
  )
  return data
}

export async function getOwnerLiquidaciones({ negocioId, empleadoId, page = 0, size = 20 }) {
  const params = { page, size }
  if (empleadoId) params.empleadoId = empleadoId
  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/liquidaciones`, { params })
  return data
}

export async function getOwnerLiquidacion({ negocioId, liquidacionId }) {
  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/liquidaciones/${liquidacionId}`)
  return data
}

export async function annulOwnerLiquidacion({ negocioId, liquidacionId, motivo }) {
  const { data } = await apiClient.post(
    `/api/owner/negocios/${negocioId}/liquidaciones/${liquidacionId}/anulacion`,
    { motivo },
  )
  return data
}

export function getOwnerRemuneracionesErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a las remuneraciones de este negocio.'
  if (status === 404) return 'No se encontró el negocio o la remuneración solicitada.'
  if (status === 400) return message || 'Revisá los filtros seleccionados.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || 'No pudimos cargar las remuneraciones. Intentá nuevamente.'
}

export function getOwnerLiquidacionErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a las liquidaciones de este negocio.'
  if (status === 404) return message || 'No se encontró el negocio o alguna remuneración seleccionada.'
  if (status === 400 || status === 409) return message || 'Revisá los datos ingresados y el saldo disponible de las remuneraciones.'
  if (!error.response) return 'No pudimos confirmar el registro. Podés reintentar la misma operación sin cambiar los datos.'
  if (status >= 500) return 'El servicio no está disponible. Podés reintentar la misma operación sin cambiar los datos.'
  return message || 'No pudimos registrar la liquidación. Intentá nuevamente.'
}

export function getOwnerLiquidacionesErrorMessage(error, action = 'consultar') {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a las liquidaciones de este negocio.'
  if (status === 404) return message || 'No se encontró el negocio o la liquidación solicitada.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || `No pudimos ${action} las liquidaciones. Intentá nuevamente.`
}

export function getAnularOwnerLiquidacionErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a esta liquidación.'
  if (status === 404) return message || 'No se encontró la liquidación solicitada.'
  const validationMessages = Array.isArray(body?.errors)
    ? body.errors.map((item) => item?.defaultMessage || item?.message).filter((item) => typeof item === 'string').join(' ')
    : ''
  if (status === 400 || status === 409) return message || validationMessages || 'Revisá el motivo y el estado actual de la liquidación.'
  if (!error.response) return 'No pudimos confirmar la anulación. Revisá la conexión e intentá nuevamente.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || 'No pudimos anular la liquidación. Intentá nuevamente.'
}
