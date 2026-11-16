import { useMutation } from '@tanstack/react-query'
import { useRef } from 'react'
import { createClientAppointment } from '../api/clientApi.js'

export function useCreateClientAppointment() {
  const pendingRequest = useRef(null)

  return useMutation({
    mutationFn: async (appointment) => {
      if (pendingRequest.current) return pendingRequest.current

      const request = createClientAppointment(appointment)
      pendingRequest.current = request
      try {
        return await request
      } finally {
        pendingRequest.current = null
      }
    },
  })
}
