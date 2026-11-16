import { useQuery } from '@tanstack/react-query'
import { fetchClientAvailability, fetchClientEmployeeAvailability } from '../api/clientApi.js'

const validId = (id) => {
  if (typeof id !== 'string' && typeof id !== 'number') return false
  const value = String(id)
  return /^[1-9]\d*$/.test(value) && Number.isSafeInteger(Number(value))
}

const validDate = (fecha) => {
  if (typeof fecha !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(fecha)) return false
  const parsed = new Date(`${fecha}T00:00:00.000Z`)
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === fecha
}

const useAvailabilityQuery = ({ queryKey, queryFn, enabled, options }) => useQuery({
  queryKey,
  queryFn,
  enabled: enabled && (options.enabled ?? true),
  ...(options.retry !== undefined ? { retry: options.retry } : {}),
})

export const clientAvailabilityQueryKey = ({ negocioId, fecha, servicioId, empleadoId }) => [
  'client', 'availability', String(negocioId), String(servicioId), fecha, empleadoId == null ? null : String(empleadoId),
]

export function useClientAvailability(filters = {}, options = {}) {
  const { negocioId, fecha, servicioId, empleadoId } = filters
  const enabled = validId(negocioId) && validId(servicioId) && validDate(fecha)
    && (empleadoId === undefined || empleadoId === null || validId(empleadoId))

  return useAvailabilityQuery({
    queryKey: clientAvailabilityQueryKey(filters),
    queryFn: () => fetchClientAvailability(filters),
    enabled,
    options,
  })
}

export const clientEmployeeAvailabilityQueryKey = ({ negocioId, empleadoId, fecha, servicioId }) => [
  'client', 'availability', 'employee', String(negocioId), String(empleadoId), String(servicioId), fecha,
]

export function useClientEmployeeAvailability(filters = {}, options = {}) {
  const { negocioId, empleadoId, fecha, servicioId } = filters
  const enabled = validId(negocioId) && validId(empleadoId) && validId(servicioId) && validDate(fecha)

  return useAvailabilityQuery({
    queryKey: clientEmployeeAvailabilityQueryKey(filters),
    queryFn: () => fetchClientEmployeeAvailability(filters),
    enabled,
    options,
  })
}
