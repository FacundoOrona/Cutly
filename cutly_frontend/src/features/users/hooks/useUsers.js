import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchUserById, fetchUsers, updateUserStatus } from '../api/usersApi.js'

export const usersQueryKey = (params) => ['users', params]

export default function useUsers(params) {
  return useQuery({
    queryKey: usersQueryKey(params),
    queryFn: () => fetchUsers(params),
    placeholderData: (previousData) => previousData,
  })
}

export const userDetailQueryKey = (id) => ['users', 'detail', id]

export function useUser(id) {
  const validId = typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))
  return useQuery({
    queryKey: userDetailQueryKey(validId ? id : null),
    queryFn: () => fetchUserById(id),
    enabled: validId,
  })
}

export function useUpdateUserStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateUserStatus,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  })
}
