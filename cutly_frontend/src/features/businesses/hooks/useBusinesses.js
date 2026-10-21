import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchBusinesses, updateBusinessStatus } from '../api/businessesApi.js'

export const businessesQueryKey = ['businesses']

export default function useBusinesses() {
  return useQuery({
    queryKey: businessesQueryKey,
    queryFn: fetchBusinesses,
  })
}

export function useUpdateBusinessStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateBusinessStatus,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: businessesQueryKey }),
  })
}
