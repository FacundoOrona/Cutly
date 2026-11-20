import apiClient from '../../../services/apiClient.js'

const plansPath = '/api/admin/planes-suscripcion'

export async function fetchAdminPlans() {
  const { data } = await apiClient.get(plansPath)
  if (!Array.isArray(data)) throw new Error('El servidor devolvió una lista de planes que no pudimos interpretar.')
  return data
}

export async function fetchAdminPlan(id) {
  const { data } = await apiClient.get(`${plansPath}/${id}`)
  if (!data || typeof data !== 'object' || data.planId === undefined) {
    throw new Error('El servidor devolvió un plan que no pudimos interpretar.')
  }
  return data
}

export async function createAdminPlan(body) {
  const { data } = await apiClient.post(plansPath, body)
  return data
}

export async function updateAdminPlan({ id, ...body }) {
  const { data } = await apiClient.put(`${plansPath}/${id}`, body)
  return data
}

export async function changeAdminPlanStatus({ id, active, versionEsperada }) {
  const operation = active ? 'activar' : 'desactivar'
  const { data } = await apiClient.patch(`${plansPath}/${id}/${operation}`, { versionEsperada })
  return data
}

export function getAdminPlanErrorMessage(error) {
  const status = error.response?.status
  const message = error.response?.data?.mensaje
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para administrar planes.'
  if (message) return message
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá nuevamente.'
  return 'No se pudo completar la operación de planes.'
}
