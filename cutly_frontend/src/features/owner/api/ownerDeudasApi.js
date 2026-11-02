import apiClient from '../../../services/apiClient.js'

export async function getOwnerDeudas({ negocioId, page = 0, size = 10, q, estado }) {
  const params = { page, size }
  if (q) params.q = q
  if (estado) params.estado = estado

  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/deudas`, { params })
  return data
}

export async function getOwnerDeudasClientes({ negocioId, page = 0, size = 20, q }) {
  const params = { page, size }
  if (q) params.q = q

  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/deudas/clientes`, { params })
  return data
}

export async function getOwnerDeuda({ negocioId, deudaId }) {
  const { data } = await apiClient.get(`/api/owner/negocios/${negocioId}/deudas/${deudaId}`)
  return data
}

export async function createOwnerAbono({ negocioId, deudaId, body, idempotencyKey }) {
  const { data } = await apiClient.post(
    `/api/owner/negocios/${negocioId}/deudas/${deudaId}/abonos`,
    body,
    { headers: { 'Idempotency-Key': idempotencyKey } },
  )
  return data
}
