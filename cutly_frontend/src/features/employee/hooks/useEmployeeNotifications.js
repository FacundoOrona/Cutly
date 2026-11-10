import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getEmployeeNotifications,
  markAllEmployeeNotificationsAsRead,
  markEmployeeNotificationAsRead,
} from '../api/employeeApi.js'

export const employeeNotificationsQueryKey = ['employee', 'notifications']

export function useEmployeeNotifications() {
  return useQuery({
    queryKey: employeeNotificationsQueryKey,
    queryFn: getEmployeeNotifications,
  })
}

export function useMarkEmployeeNotificationAsRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: markEmployeeNotificationAsRead,
    onSuccess: (updatedNotification) => {
      queryClient.setQueryData(employeeNotificationsQueryKey, (notifications) => {
        if (!Array.isArray(notifications)) return notifications
        return notifications.map((notification) =>
          String(notification.notificacionId) === String(updatedNotification.notificacionId)
            ? updatedNotification
            : notification,
        )
      })
    },
  })
}

export function useMarkAllEmployeeNotificationsAsRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: markAllEmployeeNotificationsAsRead,
    onSuccess: () => {
      queryClient.setQueryData(employeeNotificationsQueryKey, (notifications) => {
        if (!Array.isArray(notifications)) return notifications
        return notifications.map((notification) => ({ ...notification, leida: true }))
      })
    },
  })
}
