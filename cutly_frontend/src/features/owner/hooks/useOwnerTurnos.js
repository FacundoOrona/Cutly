import { useQuery } from '@tanstack/react-query'
import { getOwnerTurnos } from '../api/ownerTurnosApi.js'

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
