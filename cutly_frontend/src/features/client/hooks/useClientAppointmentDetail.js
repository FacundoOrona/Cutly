import { useQuery } from '@tanstack/react-query'
import { fetchClientAppointmentById } from '../api/clientApi.js'

const validAppointmentId = (id) => typeof id === 'string'
  && /^[1-9]\d*$/.test(id)
  && Number.isSafeInteger(Number(id))

export const clientAppointmentDetailQueryKey = (turnoId) => [
  'client', 'appointments', 'detail', String(turnoId),
]

export default function useClientAppointmentDetail(turnoId) {
  const validId = validAppointmentId(turnoId)
  return useQuery({
    queryKey: clientAppointmentDetailQueryKey(turnoId),
    queryFn: () => fetchClientAppointmentById(turnoId),
    enabled: validId,
  })
}
