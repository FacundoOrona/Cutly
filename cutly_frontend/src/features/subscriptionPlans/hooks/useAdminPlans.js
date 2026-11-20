import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  changeAdminPlanStatus,
  createAdminPlan,
  fetchAdminPlan,
  fetchAdminPlans,
  updateAdminPlan,
} from '../api/adminPlansApi.js'

export const adminPlansQueryKey = ['admin', 'subscription-plans']
export const adminPlanDetailQueryKey = (id) => [...adminPlansQueryKey, 'detail', String(id)]

export function useAdminPlans() {
  return useQuery({ queryKey: adminPlansQueryKey, queryFn: fetchAdminPlans })
}

export function useAdminPlan(id, enabled = true) {
  return useQuery({ queryKey: adminPlanDetailQueryKey(id), queryFn: () => fetchAdminPlan(id), enabled })
}

function useAdminPlanMutation(mutationFn) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin'] }),
  })
}

export function useCreateAdminPlan() {
  return useAdminPlanMutation(createAdminPlan)
}

export function useUpdateAdminPlan() {
  return useAdminPlanMutation(updateAdminPlan)
}

export function useChangeAdminPlanStatus() {
  return useAdminPlanMutation(changeAdminPlanStatus)
}
