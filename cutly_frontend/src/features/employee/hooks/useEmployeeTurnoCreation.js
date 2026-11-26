import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createEmployeeAppointment,
  getEmployeeAvailability,
  getEmployeeServices,
  searchEmployeeLocalClients,
} from '../api/employeeApi.js'

export const employeeServicesQueryKey = ['employee', 'services']
export const employeeAvailabilityQueryKey = (fecha) => ['employee', 'availability', { fecha }]
export const employeeLocalClientsQueryKey = (query) => ['employee', 'local-clients', { query }]

export function useEmployeeServices(enabled = true) {
  return useQuery({
    queryKey: employeeServicesQueryKey,
    queryFn: getEmployeeServices,
    enabled,
  })
}

export function useEmployeeAvailability(fecha, enabled = true) {
  return useQuery({
    queryKey: employeeAvailabilityQueryKey(fecha),
    queryFn: () => getEmployeeAvailability(fecha),
    enabled: enabled && Boolean(fecha),
  })
}

export function useEmployeeLocalClients(query, enabled = true) {
  const normalizedQuery = query.trim()
  return useQuery({
    queryKey: employeeLocalClientsQueryKey(normalizedQuery),
    queryFn: () => searchEmployeeLocalClients(normalizedQuery),
    enabled: enabled && normalizedQuery.replace(/\s/g, '').length >= 2,
  })
}

export function useCreateEmployeeAppointment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createEmployeeAppointment,
    onSuccess: (_created, request) => {
      const fecha = request.fechaHoraInicio.slice(0, 10)
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ['employee', 'appointments'] }),
        queryClient.invalidateQueries({ queryKey: ['employee', 'appointment-history'] }),
        queryClient.invalidateQueries({ queryKey: employeeAvailabilityQueryKey(fecha) }),
      ])
    },
    onError: (error, request) => {
      const message = error.response?.data?.mensaje || error.response?.data?.message || ''
      if (error.response?.status === 409 && /horario|disponib/i.test(message)) {
        const fecha = request.fechaHoraInicio.slice(0, 10)
        return Promise.all([
          queryClient.invalidateQueries({ queryKey: employeeAvailabilityQueryKey(fecha) }),
          queryClient.invalidateQueries({ queryKey: ['employee', 'appointments'] }),
        ])
      }
      return undefined
    },
  })
}
