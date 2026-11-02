import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createOwnerAbono, getOwnerDeuda, getOwnerDeudas, getOwnerDeudasClientes } from '../api/ownerDeudasApi.js'

export const ownerDeudasPrefix = (negocioId) => [
  'ownerBusinesses', 'detail', String(negocioId), 'deudas',
]

export const ownerDeudasQueryKey = ({ negocioId, page, size, q, estado }) => [
  ...ownerDeudasPrefix(negocioId), 'page', { page, size, q: q || '', estado: estado || '' },
]

export function useOwnerDeudas(filters, options = {}) {
  return useQuery({
    queryKey: ownerDeudasQueryKey(filters),
    queryFn: () => getOwnerDeudas(filters),
    enabled: Boolean(filters.negocioId) && (options.enabled ?? true),
  })
}

export const ownerDeudasClientesQueryKey = ({ negocioId, page, size, q }) => [
  ...ownerDeudasPrefix(negocioId), 'clientes', 'page', { page, size, q: q || '' },
]

export function useOwnerDeudasClientes(filters, options = {}) {
  return useQuery({
    queryKey: ownerDeudasClientesQueryKey(filters),
    queryFn: () => getOwnerDeudasClientes(filters),
    enabled: Boolean(filters.negocioId) && (options.enabled ?? true),
  })
}

export const ownerDeudaDetailQueryKey = ({ negocioId, deudaId }) => [
  ...ownerDeudasPrefix(negocioId), 'detail', String(deudaId),
]

export function useOwnerDeuda({ negocioId, deudaId }, options = {}) {
  return useQuery({
    queryKey: ownerDeudaDetailQueryKey({ negocioId, deudaId }),
    queryFn: () => getOwnerDeuda({ negocioId, deudaId }),
    enabled: Boolean(negocioId && deudaId) && (options.enabled ?? true),
  })
}

export function useCreateOwnerAbono() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOwnerAbono,
    onSuccess: (deuda, variables) => {
      const negocioId = String(variables.negocioId)
      queryClient.setQueryData(
        ownerDeudaDetailQueryKey({ negocioId, deudaId: variables.deudaId }),
        deuda,
      )
      queryClient.invalidateQueries({ queryKey: ownerDeudasPrefix(negocioId) })
      queryClient.invalidateQueries({ queryKey: [...ownerDeudasPrefix(negocioId), 'clientes'] })
      queryClient.invalidateQueries({
        queryKey: ownerDeudaDetailQueryKey({ negocioId, deudaId: variables.deudaId }),
      })
    },
  })
}
