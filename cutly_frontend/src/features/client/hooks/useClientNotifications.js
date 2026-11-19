import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchClientNotifications,
  fetchClientUnreadNotificationCount,
  markAllClientNotificationsAsRead,
  markClientNotificationAsRead,
} from '../api/clientApi.js'

export const clientNotificationsQueryKey = ['client', 'notifications']
export const clientUnreadNotificationCountQueryKey = ['client', 'notificationUnreadCount']

export function useClientNotifications(page) {
  return useQuery({
    queryKey: [...clientNotificationsQueryKey, page],
    queryFn: () => fetchClientNotifications(page, 20),
  })
}

export function useClientUnreadNotificationCount() {
  return useQuery({
    queryKey: clientUnreadNotificationCountQueryKey,
    queryFn: fetchClientUnreadNotificationCount,
  })
}

export function useMarkClientNotificationAsRead(page) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: markClientNotificationAsRead,
    onSuccess: (updatedNotification) => {
      queryClient.setQueryData([...clientNotificationsQueryKey, page], (result) => {
        if (!result || !Array.isArray(result.content)) return result
        return {
          ...result,
          content: result.content.map((notification) =>
            String(notification.notificacionId) === String(updatedNotification.notificacionId)
              ? updatedNotification
              : notification,
          ),
        }
      })
      queryClient.invalidateQueries({ queryKey: clientUnreadNotificationCountQueryKey })
    },
  })
}

export function useMarkAllClientNotificationsAsRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: markAllClientNotificationsAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: clientNotificationsQueryKey })
      queryClient.invalidateQueries({ queryKey: clientUnreadNotificationCountQueryKey })
    },
  })
}
