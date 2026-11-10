import { useQuery } from '@tanstack/react-query'
import { fetchClientBusinesses } from '../api/clientApi.js'

export const clientBusinessesQueryKey = ({ nombre, page, size, sort }) => [
  'client', 'businesses', { nombre: nombre || '', page, size, sort },
]

export default function useClientBusinesses(params) {
  return useQuery({
    queryKey: clientBusinessesQueryKey(params),
    queryFn: () => fetchClientBusinesses(params),
    placeholderData: (previousData) => previousData,
  })
}
