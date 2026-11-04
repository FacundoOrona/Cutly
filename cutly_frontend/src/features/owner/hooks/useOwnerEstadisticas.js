import { useQuery } from '@tanstack/react-query'
import {
  getOwnerEstadisticasEmpleados,
  getOwnerEstadisticasResumen,
  getOwnerEstadisticasServicios,
  getOwnerEstadisticasTurnos,
} from '../api/ownerEstadisticasApi.js'

const queryKey = (tipo, filters) => [
  'ownerBusinesses', 'detail', String(filters.negocioId), 'estadisticas', tipo,
  { desde: filters.desde || '', hasta: filters.hasta || '' },
]

function useOwnerEstadistica(tipo, filters, queryFn, options = {}) {
  return useQuery({
    queryKey: queryKey(tipo, filters),
    queryFn: () => queryFn(filters),
    enabled: Boolean(filters.negocioId) && (options.enabled ?? true),
  })
}

export const useOwnerEstadisticasResumen = (filters, options) => useOwnerEstadistica('resumen', filters, getOwnerEstadisticasResumen, options)
export const useOwnerEstadisticasTurnos = (filters, options) => useOwnerEstadistica('turnos', filters, getOwnerEstadisticasTurnos, options)
export const useOwnerEstadisticasEmpleados = (filters, options) => useOwnerEstadistica('empleados', filters, getOwnerEstadisticasEmpleados, options)
export const useOwnerEstadisticasServicios = (filters, options) => useOwnerEstadistica('servicios', filters, getOwnerEstadisticasServicios, options)
