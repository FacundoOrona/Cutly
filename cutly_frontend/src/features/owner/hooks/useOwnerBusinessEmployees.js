import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createOwnerBusinessEmployee, getOwnerBusinessEmployees, updateOwnerBusinessEmployee, updateOwnerBusinessEmployeeStatus } from '../api/ownerBusinessEmployeesApi.js'

export const ownerBusinessEmployeesQueryKey = (id) => ['ownerBusinesses', 'detail', String(id), 'employees']

export function useOwnerBusinessEmployees(id) {
  return useQuery({ queryKey: ownerBusinessEmployeesQueryKey(id), queryFn: () => getOwnerBusinessEmployees(id), enabled: Boolean(id) })
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
