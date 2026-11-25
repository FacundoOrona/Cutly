import { useQuery } from '@tanstack/react-query'
import useAuth from '../../../auth/useAuth.js'
import { getOwnerSubscription } from '../api/ownerSubscriptionApi.js'

export const ownerSubscriptionQueryKey = (ownerId) => ['owner', ownerId, 'subscription']

export function useOwnerSubscription() {
  const { user } = useAuth()
  return useQuery({
    queryKey: ownerSubscriptionQueryKey(user?.userId),
    queryFn: getOwnerSubscription,
    enabled: Boolean(user?.userId),
  })
}
