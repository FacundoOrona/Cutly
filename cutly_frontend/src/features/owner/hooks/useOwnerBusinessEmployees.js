import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { assignOwnerBusinessEmployeeService, createOwnerBusinessEmployee, getOwnerBusinessEmployeeServices, getOwnerBusinessEmployees, removeOwnerBusinessEmployeeService, updateOwnerBusinessEmployee, updateOwnerBusinessEmployeeStatus } from '../api/ownerBusinessEmployeesApi.js'

export const ownerBusinessEmployeesQueryKey = (id) => ['ownerBusinesses', 'detail', String(id), 'employees']
export const ownerBusinessEmployeeServicesQueryKey = (id, empleadoId) => ['ownerBusinesses', 'detail', String(id), 'employees', String(empleadoId), 'services']

export function useOwnerBusinessEmployees(id) {
  return useQuery({ queryKey: ownerBusinessEmployeesQueryKey(id), queryFn: () => getOwnerBusinessEmployees(id), enabled: Boolean(id) })
}

export function useOwnerBusinessEmployeeServices(id, empleadoId) {
  return useQuery({
    queryKey: ownerBusinessEmployeeServicesQueryKey(id, empleadoId),
    queryFn: () => getOwnerBusinessEmployeeServices({ id, empleadoId }),
    enabled: Boolean(id && empleadoId),
  })
}

function useOwnerBusinessEmployeeServiceMutation(mutationFn, updateCache) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: (service, variables) => {
      queryClient.setQueryData(ownerBusinessEmployeeServicesQueryKey(variables.id, variables.empleadoId), (services) => updateCache(services, service, variables))
    },
  })
}

export function useAssignOwnerBusinessEmployeeService() {
  return useOwnerBusinessEmployeeServiceMutation(assignOwnerBusinessEmployeeService, (services, assigned) => {
    const current = services || []
    return current.some((service) => service.servicioId === assigned.servicioId) ? current : [...current, assigned]
  })
}

export function useRemoveOwnerBusinessEmployeeService() {
  return useOwnerBusinessEmployeeServiceMutation(removeOwnerBusinessEmployeeService, (services, _removed, variables) => (services || []).filter((service) => service.servicioId !== variables.servicioId))
}

function useOwnerBusinessEmployeeMutation(mutationFn, updateCache) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: (employee, variables) => {
      queryClient.setQueryData(ownerBusinessEmployeesQueryKey(variables.id), (employees) => updateCache(employees, employee))
    },
  })
}

export function useCreateOwnerBusinessEmployee() {
  return useOwnerBusinessEmployeeMutation(createOwnerBusinessEmployee, (employees, created) => [...(employees || []), created])
}

export function useUpdateOwnerBusinessEmployee() {
  return useOwnerBusinessEmployeeMutation(updateOwnerBusinessEmployee, (employees, updated) => (employees || []).map((employee) => employee.empleadoId === updated.empleadoId ? updated : employee))
}

export function useUpdateOwnerBusinessEmployeeStatus() {
  return useOwnerBusinessEmployeeMutation(updateOwnerBusinessEmployeeStatus, (employees, updated) => (employees || []).map((employee) => employee.empleadoId === updated.empleadoId ? updated : employee))
}
