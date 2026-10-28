import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createOwnerBusinessService, getOwnerBusinessServices, updateOwnerBusinessService, updateOwnerBusinessServiceStatus } from '../api/ownerBusinessServicesApi.js'

export const ownerBusinessServicesQueryKey = (id) => ['ownerBusinesses', 'detail', String(id), 'services']

export function useOwnerBusinessServices(id, options = {}) {
  return useQuery({ queryKey: ownerBusinessServicesQueryKey(id), queryFn: () => getOwnerBusinessServices(id), enabled: Boolean(id) && (options.enabled ?? true) })
}

function useOwnerBusinessServiceMutation(mutationFn, updateCache) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: (service, variables) => {
      queryClient.setQueryData(ownerBusinessServicesQueryKey(variables.id), (services) => updateCache(services, service))
    },
  })
}

export function useCreateOwnerBusinessService() {
  return useOwnerBusinessServiceMutation(createOwnerBusinessService, (services, created) => [...(services || []), created])
}

export function useUpdateOwnerBusinessService() {
  return useOwnerBusinessServiceMutation(updateOwnerBusinessService, (services, updated) => (services || []).map((service) => service.servicioId === updated.servicioId ? updated : service))
}

export function useUpdateOwnerBusinessServiceStatus() {
  return useOwnerBusinessServiceMutation(updateOwnerBusinessServiceStatus, (services, updated) => (services || []).map((service) => service.servicioId === updated.servicioId ? updated : service))
}
