import { useQuery } from '@tanstack/react-query'
import { fetchClientBusiness, fetchClientBusinessEmployees, fetchClientBusinessServices } from '../api/clientApi.js'

const validBusinessId = (id) => typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))

export const clientBusinessQueryKey = (id) => ['client', 'business', 'detail', id]
export const clientBusinessServicesQueryKey = (id) => ['client', 'business', id, 'services']
export const clientBusinessEmployeesQueryKey = (id) => ['client', 'business', id, 'employees']

export function useClientBusiness(id) {
  const validId = validBusinessId(id)
  return useQuery({
    queryKey: clientBusinessQueryKey(validId ? id : null),
    queryFn: () => fetchClientBusiness(id),
    enabled: validId,
  })
}

export function useClientBusinessServices(id, enabled = true) {
  const validId = validBusinessId(id)
  return useQuery({
    queryKey: clientBusinessServicesQueryKey(validId ? id : null),
    queryFn: () => fetchClientBusinessServices(id),
    enabled: validId && enabled,
  })
}

export function useClientBusinessEmployees(id, enabled = true) {
  const validId = validBusinessId(id)
  return useQuery({
    queryKey: clientBusinessEmployeesQueryKey(validId ? id : null),
    queryFn: () => fetchClientBusinessEmployees(id),
    enabled: validId && enabled,
  })
}
