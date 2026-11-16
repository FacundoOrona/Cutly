import { useQuery } from '@tanstack/react-query'
import { fetchClientProfile } from '../api/clientApi.js'

export const clientProfileQueryKey = ['client', 'profile']

export default function useClientProfile() {
  return useQuery({
    queryKey: clientProfileQueryKey,
    queryFn: fetchClientProfile,
  })
}
