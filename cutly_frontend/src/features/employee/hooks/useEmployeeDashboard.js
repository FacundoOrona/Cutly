import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getEmployeeAppointments, getEmployeeBusiness, getEmployeeProfile, updateEmployeeAppointmentStatus } from '../api/employeeApi.js'

export const employeeProfileQueryKey = ['employee', 'profile']
export const employeeBusinessQueryKey = ['employee', 'business']
export const employeeAppointmentsQueryKey = (fecha) => ['employee', 'appointments', { fecha }]

export function useEmployeeProfile() {
  return useQuery({
    queryKey: employeeProfileQueryKey,
    queryFn: getEmployeeProfile,
  })
}

export function useEmployeeBusiness() {
  return useQuery({
    queryKey: employeeBusinessQueryKey,
    queryFn: getEmployeeBusiness,
  })
}

export function useEmployeeAppointments(fecha) {
  return useQuery({
    queryKey: employeeAppointmentsQueryKey(fecha),
    queryFn: () => getEmployeeAppointments(fecha),
  })
}

export function useEmployeeAppointmentAction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateEmployeeAppointmentStatus,
    onSuccess: (updatedTurno) => {
      queryClient.setQueriesData({ queryKey: ['employee', 'appointments'] }, (cached) => {
        if (!Array.isArray(cached)) return cached
        return cached.map((turno) => String(turno.turnoId) === String(updatedTurno.turnoId) ? updatedTurno : turno)
      })
      return queryClient.invalidateQueries({ queryKey: ['employee', 'appointments'] })
    },
    onError: (error) => {
      if (error.response?.status === 409) {
        return queryClient.invalidateQueries({ queryKey: ['employee', 'appointments'] })
      }
      return undefined
    },
  })
}
