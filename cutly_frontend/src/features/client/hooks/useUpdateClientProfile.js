import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateClientProfile } from '../api/clientApi.js'
import { clientProfileQueryKey } from './useClientProfile.js'

export default function useUpdateClientProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateClientProfile,
    onSuccess: (profile) => queryClient.setQueryData(clientProfileQueryKey, profile),
  })
}
