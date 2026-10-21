import apiClient from '../../../services/apiClient.js'

export async function getOwnerBusinesses() {
  const { data } = await apiClient.get('/api/owner/negocios')
  return data
}

export async function getOwnerBusiness(id) {
  const { data } = await apiClient.get(`/api/owner/negocios/${id}`)
  return data
}

export function getOwnerBusinessErrorMessage(error) {
  const status = error.response?.status
  if (status === 404) return 'No se encontró el negocio solicitado.'
  if (status === 403) return 'Tu cuenta no tiene acceso a este negocio.'
  if (status === 400) return 'El identificador del negocio no es válido.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return 'No pudimos cargar la información del negocio.'
}
