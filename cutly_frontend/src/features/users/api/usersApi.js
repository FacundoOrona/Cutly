import apiClient from '../../../services/apiClient.js'

export async function fetchUsers({ filters, page, size, sort }) {
  const params = { page, size, sort: `${sort.field},${sort.direction}` }
  for (const field of ['rol', 'status', 'nombre', 'apellido', 'email', 'dni']) {
    const value = filters[field]?.trim()
    if (value) params[field] = value
  }

  const { data } = await apiClient.get('/api/admin/usuarios', { params })
  if (!data || !Array.isArray(data.content) || typeof data.totalPages !== 'number') {
    throw new Error('El servidor devolvió una respuesta de listado que no pudimos interpretar.')
  }
  if (data.totalPages > 0 && page >= data.totalPages) {
    const lastPage = await apiClient.get('/api/admin/usuarios', { params: { ...params, page: data.totalPages - 1 } })
    if (!lastPage.data || !Array.isArray(lastPage.data.content) || typeof lastPage.data.totalPages !== 'number') {
      throw new Error('El servidor devolvió una respuesta de listado que no pudimos interpretar.')
    }
    return lastPage.data
  }
  return data
}

export function getUsersErrorMessage(error) {
  const status = error.response?.status
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'No tenés permisos para consultar este listado.'
  if (status === 400) return error.response?.data?.mensaje || 'No pudimos consultar con esos filtros. Revisá los datos e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  return 'No pudimos cargar los usuarios. Intentá nuevamente.'
}
