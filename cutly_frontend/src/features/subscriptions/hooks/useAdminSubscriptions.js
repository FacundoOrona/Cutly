import { useQuery } from '@tanstack/react-query'
import { fetchAdminSubscriptions, fetchAdminSubscriptionStats } from '../api/adminSubscriptionsApi.js'

export const adminSubscriptionsQueryKey = (filters, page, size) => ['admin', 'subscriptions', filters, page, size]

export function useAdminSubscriptions(filters, page, size) {
  return useQuery({
    queryKey: adminSubscriptionsQueryKey(filters, page, size),
    queryFn: () => fetchAdminSubscriptions({ ...filters, page, size }),
    placeholderData: previousData => previousData,
  })
}

export function useAdminSubscriptionStats() {
  return useQuery({
    queryKey: ['admin', 'subscription-stats'],
    queryFn: fetchAdminSubscriptionStats,
  })
}
