import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createOwnerBusiness, getOwnerBusinesses, updateOwnerBusiness } from '../api/ownerBusinessesApi.js'

export const ownerBusinessesQueryKey = ['ownerBusinesses']

export function useOwnerBusinesses() {
  return useQuery({ queryKey: ownerBusinessesQueryKey, queryFn: getOwnerBusinesses })
}

export function useCreateOwnerBusiness() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOwnerBusiness,
    onSuccess: (createdBusiness) => {
      queryClient.setQueryData(ownerBusinessesQueryKey, (businesses) =>
        Array.isArray(businesses) ? [createdBusiness, ...businesses] : [createdBusiness],
      )
    },
  })
}

export const ownerBusinessDetailQueryKey = (id) => ['ownerBusinesses', 'detail', String(id)]

export function useUpdateOwnerBusiness() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateOwnerBusiness,
    onSuccess: (updatedBusiness) => {
      queryClient.setQueryData(ownerBusinessDetailQueryKey(updatedBusiness.negocioId), updatedBusiness)
      queryClient.setQueryData(ownerBusinessesQueryKey, (businesses) =>
        Array.isArray(businesses)
          ? businesses.map((business) => business.negocioId === updatedBusiness.negocioId ? updatedBusiness : business)
          : businesses,
      )
    },
  })
}
