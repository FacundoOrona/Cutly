import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getEmployeeNotifications,
  getEmployeeUnreadNotificationCount,
  markAllEmployeeNotificationsAsRead,
  markEmployeeNotificationAsRead,
} from '../api/employeeApi.js'

export const employeeNotificationsQueryKey = ['employee', 'notifications']
export const employeeUnreadNotificationCountQueryKey = ['employee', 'notificationUnreadCount']

export function useEmployeeNotifications(page) {
  return useQuery({
    queryKey: [...employeeNotificationsQueryKey, page],
    queryFn: () => getEmployeeNotifications(page, 20),
  })
}

export function useEmployeeUnreadNotificationCount() {
  return useQuery({
    queryKey: employeeUnreadNotificationCountQueryKey,
    queryFn: getEmployeeUnreadNotificationCount,
  })
}

export function useMarkEmployeeNotificationAsRead(page) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: markEmployeeNotificationAsRead,
    onSuccess: (updatedNotification) => {
      queryClient.setQueryData([...employeeNotificationsQueryKey, page], (result) => {
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
      queryClient.invalidateQueries({ queryKey: employeeUnreadNotificationCountQueryKey })
    },
  })
}

export function useMarkAllEmployeeNotificationsAsRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: markAllEmployeeNotificationsAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: employeeNotificationsQueryKey })
      queryClient.invalidateQueries({ queryKey: employeeUnreadNotificationCountQueryKey })
    },
  })
}
