import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { businessDetailQueryKey, businessesQueryKey } from './useBusinesses.js'
import {
  assignSubscription,
  changeSubscriptionStatus,
  fetchSubscription,
  fetchSubscriptionHistory,
  fetchSubscriptionPlans,
  regularizeSubscription,
} from '../api/subscriptionsApi.js'

export const subscriptionsQueryKey = ['admin', 'subscriptions']
export const subscriptionDetailQueryKey = (id) => [...subscriptionsQueryKey, 'detail', String(id)]
export const subscriptionHistoryQueryKey = (id) => [...subscriptionsQueryKey, 'history', String(id)]
export const subscriptionPlansQueryKey = [...subscriptionsQueryKey, 'plans']

export function useSubscription(id, enabled = true) {
  return useQuery({ queryKey: subscriptionDetailQueryKey(id), queryFn: () => fetchSubscription(id), enabled })
}

export function useSubscriptionHistory(id, enabled = true) {
  return useQuery({ queryKey: subscriptionHistoryQueryKey(id), queryFn: () => fetchSubscriptionHistory(id), enabled })
}

export function useSubscriptionPlans(enabled = true) {
  return useQuery({ queryKey: subscriptionPlansQueryKey, queryFn: fetchSubscriptionPlans, enabled })
}

function useSubscriptionMutation(mutationFn) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: (_data, variables) => Promise.all([
      queryClient.invalidateQueries({ queryKey: businessesQueryKey }),
      queryClient.invalidateQueries({ queryKey: businessDetailQueryKey(String(variables.businessId ?? variables.negocioId)) }),
      variables.subscriptionId
        ? queryClient.invalidateQueries({ queryKey: subscriptionDetailQueryKey(variables.subscriptionId) })
        : Promise.resolve(),
      variables.subscriptionId
        ? queryClient.invalidateQueries({ queryKey: subscriptionHistoryQueryKey(variables.subscriptionId) })
        : Promise.resolve(),
      queryClient.invalidateQueries({ queryKey: subscriptionsQueryKey }),
    ]),
  })
}

export function useAssignSubscription() {
  return useSubscriptionMutation(assignSubscription)
}

export function useChangeSubscriptionStatus() {
  return useSubscriptionMutation(changeSubscriptionStatus)
}

export function useRegularizeSubscription() {
  return useSubscriptionMutation(regularizeSubscription)
}
