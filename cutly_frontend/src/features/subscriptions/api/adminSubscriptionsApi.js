import apiClient from '../../../services/apiClient.js'

export async function fetchAdminSubscriptions({ status, planId, ownerId, page, size }) {
  const params = { page, size, sort: 'fechaInicio,DESC' }
  if (status) params.status = status
  if (planId) params.planId = planId
  if (ownerId) params.ownerId = ownerId

  const { data } = await apiClient.get('/api/admin/suscripciones', { params })
  if (!data || !Array.isArray(data.content) || typeof data.totalPages !== 'number') {
    throw new Error('El servidor devolvió una lista de suscripciones que no pudimos interpretar.')
  }
  if (data.totalPages > 0 && page >= data.totalPages) {
    const { data: lastPage } = await apiClient.get('/api/admin/suscripciones', {
      params: { ...params, page: data.totalPages - 1 },
    })
    if (!lastPage || !Array.isArray(lastPage.content) || typeof lastPage.totalPages !== 'number') {
      throw new Error('El servidor devolvió una lista de suscripciones que no pudimos interpretar.')
    }
    return lastPage
  }
  return data
}

export async function fetchAdminSubscriptionStats() {
  const { data } = await apiClient.get('/api/admin/estadisticas/suscripciones')
  return data
}

export function getAdminSubscriptionsErrorMessage(error) {
  const status = error.response?.status
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar las suscripciones.'
  if (status === 400) return error.response?.data?.mensaje || 'Revisá los filtros e intentá nuevamente.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Podés reintentar.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión.'
  return error.response?.data?.mensaje || 'No pudimos cargar las suscripciones.'
}
