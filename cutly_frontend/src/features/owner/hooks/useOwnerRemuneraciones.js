import { useQuery } from '@tanstack/react-query'
import { getOwnerRemuneraciones, getOwnerRemuneracionesResumen, getOwnerRemuneracion } from '../api/ownerRemuneracionesApi.js'

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

export function useOwnerRemuneracion({ negocioId, remuneracionId }, options = {}) {
  return useQuery({
    queryKey: ownerRemuneracionDetailQueryKey({ negocioId, remuneracionId }),
    queryFn: () => getOwnerRemuneracion({ negocioId, remuneracionId }),
    enabled: Boolean(negocioId && remuneracionId) && (options.enabled ?? true),
  })
}
