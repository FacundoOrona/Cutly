import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import useAuth from '../../../auth/useAuth.js'
import { createOwnerBusiness, getOwnerBusinesses, updateOwnerBusiness } from '../api/ownerBusinessesApi.js'

export const ownerBusinessesQueryKey = (ownerId) => ['ownerBusinesses', ownerId]

export function useOwnerBusinesses() {
  const { user } = useAuth()
  return useQuery({
    queryKey: ownerBusinessesQueryKey(user?.userId),
    queryFn: getOwnerBusinesses,
    enabled: Boolean(user?.userId),
  })
}

export function useCreateOwnerBusiness() {
  return useMutation({ mutationFn: createOwnerBusiness })
}

export const ownerBusinessDetailQueryKey = (id) => ['ownerBusinesses', 'detail', String(id)]

export function useUpdateOwnerBusiness() {
  const queryClient = useQueryClient()
  const { user } = useAuth()
  return useMutation({
    mutationFn: updateOwnerBusiness,
    onSuccess: (updatedBusiness) => {
      queryClient.setQueryData(ownerBusinessDetailQueryKey(updatedBusiness.negocioId), updatedBusiness)
      queryClient.setQueryData(ownerBusinessesQueryKey(user?.userId), (businesses) =>
        Array.isArray(businesses)
          ? businesses.map((business) => business.negocioId === updatedBusiness.negocioId ? updatedBusiness : business)
          : businesses,
      )
    },
  })
}
