import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getOwnerTransacciones } from '../api/ownerTransaccionesApi.js'

export const ownerTransaccionesPrefix = (negocioId) => [
  'ownerBusinesses', 'detail', String(negocioId), 'transacciones',
]

export const ownerTransaccionesQueryKey = ({ negocioId, desde, hasta, metodoPago, tipo, q, page, size, sort }) => [
  ...ownerTransaccionesPrefix(negocioId),
  { desde: desde || '', hasta: hasta || '', metodoPago: [...(metodoPago || [])].sort(), tipo: tipo || '', q: q || '', page, size, sort: sort || '' },
]

export function useOwnerTransacciones(filters, options = {}) {
  return useQuery({
    queryKey: ownerTransaccionesQueryKey(filters),
    queryFn: () => getOwnerTransacciones(filters),
    enabled: Boolean(filters.negocioId) && (options.enabled ?? true),
    placeholderData: keepPreviousData,
  })
}
