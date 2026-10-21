import { useQuery } from '@tanstack/react-query'
import { fetchUsers } from '../api/usersApi.js'

export const usersQueryKey = (params) => ['users', params]

export default function useUsers(params) {
  return useQuery({
    queryKey: usersQueryKey(params),
    queryFn: () => fetchUsers(params),
    placeholderData: (previousData) => previousData,
  })
}
