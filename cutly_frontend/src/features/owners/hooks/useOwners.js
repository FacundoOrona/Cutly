import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createOwner, fetchOwners } from '../api/ownersApi.js'

export const ownersQueryKey = (filters) => ['owners', filters]

export default function useOwners(filters) {
  return useQuery({
    queryKey: ownersQueryKey(filters),
    queryFn: () => fetchOwners(filters),
    placeholderData: (previousData) => previousData,
  })
}

export function useCreateOwner() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOwner,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['owners'] }),
  })
}
