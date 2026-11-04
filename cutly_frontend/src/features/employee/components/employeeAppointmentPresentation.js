export const EMPLOYEE_APPOINTMENT_ACTIONS = {
  PENDING: ['confirmar', 'cancelar'],
  CONFIRMED: ['cancelar', 'completar', 'no-show'],
  IN_PROGRESS: ['cancelar', 'completar'],
}

export function employeeClientName(turn) {
  return [turn.clienteNombre, turn.clienteApellido].filter(Boolean).join(' ') || 'Cliente'
}

export function employeeStatusLabel(status) {
  return ({ PENDING: 'Pendiente', CONFIRMED: 'Confirmado', IN_PROGRESS: 'En curso', COMPLETED: 'Completado', CANCELLED: 'Cancelado', NO_SHOW: 'Ausencia', REJECTED: 'Rechazado' })[status] || status || 'Estado no disponible'
}
