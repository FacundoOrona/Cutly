import { useQuery } from '@tanstack/react-query'
import { fetchClientTopBusinesses } from '../api/clientApi.js'

export default function useClientTopBusinesses() {
  return useQuery({
    queryKey: ['client', 'businesses', 'top'],
    queryFn: fetchClientTopBusinesses,
  })
}
