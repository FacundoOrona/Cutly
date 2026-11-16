const dateFormatter = new Intl.DateTimeFormat('es-AR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
const timeFormatter = new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })

export function parseClientLocalDateTime(value) {
  if (typeof value !== 'string') return null
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/.exec(value)
  if (!match) return null

  const [, year, month, day, hour, minute, second = '0', fraction = '0'] = match
  const date = new Date(
    Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute),
    Number(second), Number(fraction.slice(0, 3).padEnd(3, '0')),
  )
  if (date.getFullYear() !== Number(year) || date.getMonth() !== Number(month) - 1
    || date.getDate() !== Number(day) || date.getHours() !== Number(hour)
    || date.getMinutes() !== Number(minute)) return null
  return date
}

export function formatClientAppointmentDate(value) {
  const date = parseClientLocalDateTime(value)
  return date ? dateFormatter.format(date) : 'Fecha no disponible'
}

export function formatClientAppointmentTime(value) {
  const date = parseClientLocalDateTime(value)
  return date ? timeFormatter.format(date) : '—'
}
