import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRef } from 'react'
import { cancelClientAppointment } from '../api/clientApi.js'
import { clientAppointmentsQueryKey } from './useClientAppointments.js'
import { clientAppointmentDetailQueryKey } from './useClientAppointmentDetail.js'

function invalidateAppointmentQueries(queryClient, turnoId) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: clientAppointmentsQueryKey, exact: true }),
    queryClient.invalidateQueries({ queryKey: clientAppointmentDetailQueryKey(turnoId), exact: true }),
  ])
}

export default function useCancelClientAppointment() {
  const queryClient = useQueryClient()
  const pendingRequest = useRef(null)

  return useMutation({
    mutationFn: async (turnoId) => {
      if (pendingRequest.current) return pendingRequest.current

      const request = cancelClientAppointment(turnoId)
      pendingRequest.current = request
      try {
        return await request
      } finally {
        pendingRequest.current = null
      }
    },
    onSuccess: (appointment, turnoId) => {
      queryClient.setQueryData(clientAppointmentDetailQueryKey(turnoId), appointment)
      return queryClient.invalidateQueries({ queryKey: clientAppointmentsQueryKey, exact: true })
    },
    onError: (error, turnoId) => {
      if (error.response?.status === 409 || !error.response) {
        return invalidateAppointmentQueries(queryClient, turnoId)
      }
      return undefined
    },
  })
}
