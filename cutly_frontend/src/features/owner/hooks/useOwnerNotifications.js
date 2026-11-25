import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getOwnerNotifications,
  getOwnerUnreadNotificationCount,
  markAllOwnerNotificationsAsRead,
  markOwnerNotificationAsRead,
} from '../api/ownerNotificationsApi.js'

export const ownerNotificationsQueryKey = ['owner', 'notifications']
export const ownerUnreadNotificationCountQueryKey = ['owner', 'notificationUnreadCount']

export function useOwnerNotifications(page) {
  return useQuery({ queryKey: [...ownerNotificationsQueryKey, page], queryFn: () => getOwnerNotifications(page, 20) })
}

export function useOwnerUnreadNotificationCount(options = {}) {
  return useQuery({ queryKey: ownerUnreadNotificationCountQueryKey, queryFn: getOwnerUnreadNotificationCount, enabled: options.enabled ?? true })
}

export function useMarkOwnerNotificationAsRead(page) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: markOwnerNotificationAsRead,
    onSuccess: (updatedNotification) => {
      queryClient.setQueryData([...ownerNotificationsQueryKey, page], (result) => {
        if (!result || !Array.isArray(result.content)) return result
        return { ...result, content: result.content.map((notification) =>
          String(notification.notificacionId) === String(updatedNotification.notificacionId) ? updatedNotification : notification,
        ) }
      })
      queryClient.invalidateQueries({ queryKey: ownerUnreadNotificationCountQueryKey })
    },
  })
}

export function useMarkAllOwnerNotificationsAsRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: markAllOwnerNotificationsAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ownerNotificationsQueryKey })
      queryClient.invalidateQueries({ queryKey: ownerUnreadNotificationCountQueryKey })
    },
  })
}
