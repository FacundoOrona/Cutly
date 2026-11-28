import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { annulOwnerLiquidacion, createOwnerLiquidacion, getOwnerLiquidaciones, getOwnerLiquidacion, getOwnerRemuneraciones, getOwnerRemuneracionesResumen, getOwnerRemuneracion } from '../api/ownerRemuneracionesApi.js'

export const ownerRemuneracionesPrefix = (negocioId) => [
  'ownerBusinesses', 'detail', String(negocioId), 'remuneraciones',
]

export const ownerRemuneracionesResumenQueryKey = (negocioId) => [
  ...ownerRemuneracionesPrefix(negocioId), 'resumen',
]

export function useOwnerRemuneracionesResumen(negocioId, options = {}) {
  return useQuery({
    queryKey: ownerRemuneracionesResumenQueryKey(negocioId),
    queryFn: () => getOwnerRemuneracionesResumen(negocioId),
    enabled: Boolean(negocioId) && (options.enabled ?? true),
  })
}

export const ownerRemuneracionesQueryKey = (filters) => [
  ...ownerRemuneracionesPrefix(filters.negocioId), 'page', {
    empleadoId: filters.empleadoId || '', estado: filters.estado || '',
    desde: filters.desde || '', hasta: filters.hasta || '', page: filters.page, size: filters.size,
  },
]

export function useOwnerRemuneraciones(filters, options = {}) {
  return useQuery({
    queryKey: ownerRemuneracionesQueryKey(filters),
    queryFn: () => getOwnerRemuneraciones(filters),
    enabled: Boolean(filters.negocioId) && (options.enabled ?? true),
  })
}

export const ownerRemuneracionDetailQueryKey = ({ negocioId, remuneracionId }) => [
  ...ownerRemuneracionesPrefix(negocioId), 'detail', String(remuneracionId),
]

export const ownerLiquidacionesPrefix = (negocioId) => [
  'ownerBusinesses', 'detail', String(negocioId), 'liquidaciones',
]

export const ownerLiquidacionesQueryKey = ({ negocioId, empleadoId, page, size }) => [
  ...ownerLiquidacionesPrefix(negocioId), 'page', { empleadoId: empleadoId || '', page, size },
]

export function useOwnerLiquidaciones(filters, options = {}) {
  return useQuery({
    queryKey: ownerLiquidacionesQueryKey(filters),
    queryFn: () => getOwnerLiquidaciones(filters),
    enabled: Boolean(filters.negocioId) && (options.enabled ?? true),
  })
}

export const ownerLiquidacionDetailQueryKey = ({ negocioId, liquidacionId }) => [
  ...ownerLiquidacionesPrefix(negocioId), 'detail', String(liquidacionId),
]

export function useOwnerLiquidacion({ negocioId, liquidacionId }, options = {}) {
  return useQuery({
    queryKey: ownerLiquidacionDetailQueryKey({ negocioId, liquidacionId }),
    queryFn: () => getOwnerLiquidacion({ negocioId, liquidacionId }),
    enabled: Boolean(negocioId && liquidacionId) && (options.enabled ?? true),
  })
}

export function useOwnerRemuneracion({ negocioId, remuneracionId }, options = {}) {
  return useQuery({
    queryKey: ownerRemuneracionDetailQueryKey({ negocioId, remuneracionId }),
    queryFn: () => getOwnerRemuneracion({ negocioId, remuneracionId }),
    enabled: Boolean(negocioId && remuneracionId) && (options.enabled ?? true),
  })
}

export function useCreateOwnerLiquidacion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOwnerLiquidacion,
    onSuccess: (_liquidacion, variables) => {
      queryClient.invalidateQueries({ queryKey: ownerRemuneracionesPrefix(variables.negocioId) })
    },
    onError: (error, variables) => {
      if (error.response?.status === 409) {
        queryClient.invalidateQueries({ queryKey: ownerRemuneracionesPrefix(variables.negocioId) })
      }
    },
  })
}

export function useAnularOwnerLiquidacion() {
  const queryClient = useQueryClient()
  function invalidateAffected(negocioId, liquidacionId) {
    queryClient.invalidateQueries({ queryKey: ownerLiquidacionesPrefix(negocioId) })
    queryClient.invalidateQueries({ queryKey: ownerLiquidacionDetailQueryKey({ negocioId, liquidacionId }) })
    queryClient.invalidateQueries({ queryKey: ownerRemuneracionesPrefix(negocioId) })
  }
  return useMutation({
    mutationFn: annulOwnerLiquidacion,
    onSuccess: (liquidacion, variables) => {
      queryClient.setQueryData(ownerLiquidacionDetailQueryKey(variables), liquidacion)
      invalidateAffected(variables.negocioId, variables.liquidacionId)
    },
    onError: (error, variables) => {
      if (error.response?.status === 409) invalidateAffected(variables.negocioId, variables.liquidacionId)
    },
  })
}
