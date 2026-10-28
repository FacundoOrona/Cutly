import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createOwnerBusinessAddress, getOwnerBusinessAddress, updateOwnerBusinessAddress } from '../api/ownerBusinessAddressApi.js'

export const ownerBusinessAddressQueryKey = (id) => ['ownerBusinesses', 'detail', String(id), 'address']

export function useOwnerBusinessAddress(id, options = {}) {
  return useQuery({
    queryKey: ownerBusinessAddressQueryKey(id),
    queryFn: () => getOwnerBusinessAddress(id),
    ...options,
  })
}

function useSaveOwnerBusinessAddress(mutationFn) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: (address) => {
      queryClient.setQueryData(ownerBusinessAddressQueryKey(address.negocioId), address)
    },
  })
}

export function useCreateOwnerBusinessAddress() {
  return useSaveOwnerBusinessAddress(createOwnerBusinessAddress)
}

export function useUpdateOwnerBusinessAddress() {
  return useSaveOwnerBusinessAddress(updateOwnerBusinessAddress)
}
