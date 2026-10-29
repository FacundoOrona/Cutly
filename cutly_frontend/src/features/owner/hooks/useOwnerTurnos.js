import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createOwnerTurno, getOwnerTurnos, searchOwnerClientesLocales } from '../api/ownerTurnosApi.js'

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

export const ownerClientesLocalesQueryKey = ({ negocioId, query }) => [
  'ownerBusinesses', 'detail', String(negocioId), 'clientes-locales', { query: query || '' },
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
      queryClient.invalidateQueries({ queryKey: ['ownerBusinesses', 'detail', negocioId, 'clientes-locales'] })
    },
  })
}
