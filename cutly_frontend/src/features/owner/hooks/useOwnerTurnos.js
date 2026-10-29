import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createOwnerClienteLocal, createOwnerTurno, getOwnerTurno, getOwnerTurnos, searchOwnerClientesLocales } from '../api/ownerTurnosApi.js'

export const ownerTurnosQueryKey = ({ negocioId, fecha, empleadoId, status }) => [
  'ownerBusinesses', 'detail', String(negocioId), 'turnos',
  { fecha: fecha || '', empleadoId: empleadoId || '', status: status || '' },
]

export function useOwnerTurnos(filters, options = {}) {
  return useQuery({
    queryKey: ownerTurnosQueryKey(filters),
    queryFn: () => getOwnerTurnos(filters),
    enabled: Boolean(filters.negocioId) && (options.enabled ?? true),
  })
}

export const ownerTurnoDetailQueryKey = ({ negocioId, turnoId }) => [
  'ownerBusinesses', 'detail', String(negocioId), 'turnos', 'detail', String(turnoId),
]

export function useOwnerTurno({ negocioId, turnoId }, options = {}) {
  return useQuery({
    queryKey: ownerTurnoDetailQueryKey({ negocioId, turnoId }),
    queryFn: () => getOwnerTurno({ negocioId, turnoId }),
    enabled: Boolean(negocioId && turnoId) && (options.enabled ?? true),
  })
}

export const ownerClientesLocalesQueryKey = ({ negocioId, query }) => [
  'ownerBusinesses', 'detail', String(negocioId), 'clientes-locales', { query: query || '' },
]

export const ownerClientesLocalesPrefix = (negocioId) => [
  'ownerBusinesses', 'detail', String(negocioId), 'clientes-locales',
]

export function useOwnerClientesLocales(negocioId, query, options = {}) {
  return useQuery({
    queryKey: ownerClientesLocalesQueryKey({ negocioId, query }),
    queryFn: () => searchOwnerClientesLocales({ negocioId, query }),
    enabled: Boolean(negocioId && query.trim().length >= 2) && (options.enabled ?? true),
  })
}

export function useCreateOwnerTurno() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOwnerTurno,
    onSuccess: (_turno, variables) => {
      const negocioId = String(variables.negocioId)
      queryClient.invalidateQueries({ queryKey: ['ownerBusinesses', 'detail', negocioId, 'turnos'] })
      queryClient.invalidateQueries({ queryKey: ownerClientesLocalesPrefix(negocioId) })
    },
  })
}

export function useCreateOwnerClienteLocal() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOwnerClienteLocal,
    onSuccess: (_cliente, variables) => {
      const negocioId = String(variables.negocioId)
      queryClient.invalidateQueries({ queryKey: ownerClientesLocalesPrefix(negocioId) })
    },
  })
}
