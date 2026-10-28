import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createOwnerBusinessHour, deleteOwnerBusinessHour, getOwnerBusinessHours, updateOwnerBusinessHour } from '../api/ownerBusinessHoursApi.js'

export const ownerBusinessHoursQueryKey = (id) => ['ownerBusinesses', 'detail', String(id), 'hours']

export function useOwnerBusinessHours(id) {
  return useQuery({ queryKey: ownerBusinessHoursQueryKey(id), queryFn: () => getOwnerBusinessHours(id), enabled: Boolean(id) })
}

function useOwnerBusinessHourMutation(mutationFn, updateCache) {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn, onSuccess: (hour, variables) => {
    queryClient.setQueryData(ownerBusinessHoursQueryKey(variables.id), (hours) => updateCache(hours, hour))
  } })
}

export function useCreateOwnerBusinessHour() {
  return useOwnerBusinessHourMutation(createOwnerBusinessHour, (hours, created) => [...(hours || []), created])
}

export function useUpdateOwnerBusinessHour() {
  return useOwnerBusinessHourMutation(updateOwnerBusinessHour, (hours, updated) => (hours || []).map((hour) => hour.horarioId === updated.horarioId ? updated : hour))
}

export function useDeleteOwnerBusinessHour() {
  return useOwnerBusinessHourMutation(deleteOwnerBusinessHour, (hours, _deleted, variables) => (hours || []).filter((hour) => hour.horarioId !== variables.horarioId))
}
