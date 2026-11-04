import { useQuery } from '@tanstack/react-query'
import { getEmployeeAppointments, getEmployeeBusiness, getEmployeeProfile } from '../api/employeeApi.js'

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
