import { useQuery } from '@tanstack/react-query'
import { fetchClientAppointments } from '../api/clientApi.js'

export const clientAppointmentsQueryKey = ['client', 'appointments']

export default function useClientAppointments() {
  return useQuery({
    queryKey: clientAppointmentsQueryKey,
    queryFn: fetchClientAppointments,
  })
}
