import apiClient from '../../../services/apiClient.js'

const addressEndpoint = (id) => `/api/owner/negocios/${id}/direccion`

export async function getOwnerBusinessAddress(id) {
  const { data } = await apiClient.get(addressEndpoint(id))
  return data
}

export async function createOwnerBusinessAddress({ id, address }) {
  const { data } = await apiClient.post(addressEndpoint(id), address)
  return data
}

export async function updateOwnerBusinessAddress({ id, changes }) {
  const { data } = await apiClient.patch(addressEndpoint(id), changes)
  return data
}

const noAddressMessage = 'El negocio todavía no tiene una dirección registrada'

export function isOwnerBusinessAddressMissing(error) {
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  return error.response?.status === 404 && message === noAddressMessage
}

export function getOwnerBusinessAddressErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a este negocio.'
  if (status === 404) return 'No se encontró el negocio solicitado.'
  if (status === 409) return 'El negocio ya tiene una dirección registrada. Actualizá la información.'
  if (status === 400) {
    if (typeof body === 'string' && body.trim()) return body.trim()
    if (typeof body?.mensaje === 'string' && body.mensaje.trim()) return body.mensaje.trim()
    if (typeof body?.message === 'string' && body.message.trim()) return body.message.trim()
    if (Array.isArray(body?.errors)) {
      const messages = body.errors.map((item) => item?.defaultMessage || item?.message).filter((item) => typeof item === 'string')
      if (messages.length) return messages.join(' ')
    }
    return 'Revisá los datos ingresados e intentá nuevamente.'
  }
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return 'No pudimos procesar la dirección. Intentá nuevamente.'
}
