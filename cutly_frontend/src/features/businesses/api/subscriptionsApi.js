import apiClient from '../../../services/apiClient.js'

export async function fetchSubscription(id) {
  const { data } = await apiClient.get(`/api/admin/suscripciones/${id}`)
  return data
}

export async function fetchSubscriptionHistory(id) {
  const { data } = await apiClient.get(`/api/admin/suscripciones/${id}/historial`)
  return data
}

export async function fetchSubscriptionPlans() {
  const { data } = await apiClient.get('/api/admin/planes-suscripcion')
  return data
}

export async function assignSubscription({ businessId, ...body }) {
  const { data } = await apiClient.post(`/api/admin/negocios/${businessId}/suscripcion`, body)
  return data
}

export async function changeSubscriptionStatus({ subscriptionId, action, observacion }) {
  const path = { activate: 'activar', suspend: 'suspender', cancel: 'cancelar' }[action]
  if (!path) throw new Error('La acción solicitada no es válida.')
  const { data } = await apiClient.patch(`/api/admin/suscripciones/${subscriptionId}/${path}`, { observacion })
  return data
}

export async function regularizeSubscription({ subscriptionId, planId, fechaInicio, fechaVencimiento, status, precioContratado, versionEsperada, observacion }) {
  const body = { planId, fechaInicio, fechaVencimiento, status, precioContratado, versionEsperada, observacion }
  Object.keys(body).forEach((key) => body[key] === undefined && delete body[key])
  const { data } = await apiClient.put(`/api/admin/suscripciones/${subscriptionId}/regularizar`, body)
  return data
}

export function getSubscriptionErrorMessage(error) {
  const status = error.response?.status
  const message = error.response?.data?.mensaje
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para gestionar suscripciones.'
  if (message) return message
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá nuevamente.'
  return 'No se pudo completar la operación de suscripción.'
}
