import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchBusinessById, fetchBusinesses, updateBusinessStatus } from '../api/businessesApi.js'

export const businessesQueryKey = ['businesses']
export const businessDetailQueryKey = (id) => ['businesses', 'detail', id]

export default function useBusinesses() {
  return useQuery({
    queryKey: businessesQueryKey,
    queryFn: fetchBusinesses,
  })
}

export function useBusiness(id, enabled = true) {
  return useQuery({
    queryKey: businessDetailQueryKey(id),
    queryFn: () => fetchBusinessById(id),
    enabled,
  })
}

export function useUpdateBusinessStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateBusinessStatus,
    onSuccess: (_data, variables) => Promise.all([
      queryClient.invalidateQueries({ queryKey: businessesQueryKey }),
      queryClient.invalidateQueries({ queryKey: businessDetailQueryKey(String(variables.id)) }),
    ]),
  })
}
