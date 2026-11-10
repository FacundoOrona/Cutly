import apiClient from '../../../services/apiClient.js'

export async function fetchClientBusinesses({ nombre, page = 0, size = 20, sort = 'nombre,asc' }) {
  const params = { page, size, sort }
  if (nombre?.trim()) params.nombre = nombre.trim()

  const { data } = await apiClient.get('/api/cliente/negocios', { params })
  if (!data || !Array.isArray(data.content) || typeof data.totalPages !== 'number') {
    throw new Error('El servidor devolvió una respuesta de negocios que no pudimos interpretar.')
  }
  return data
}

export function getClientBusinessesErrorMessage(error) {
  const status = error.response?.status
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar los negocios.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return error.message || 'No pudimos cargar los negocios. Intentá nuevamente.'
}
