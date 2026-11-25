import apiClient from '../../../services/apiClient.js'

export async function getOwnerSubscription() {
  const { data } = await apiClient.get('/api/owner/suscripcion')
  return data
}
