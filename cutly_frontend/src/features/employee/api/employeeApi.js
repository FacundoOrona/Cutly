import apiClient from '../../../services/apiClient.js'

export async function getEmployeeProfile() {
  const { data } = await apiClient.get('/api/empleado/perfil')
  return data
}

export async function getEmployeeBusiness() {
  const { data } = await apiClient.get('/api/empleado/perfil/negocio')
  return data
}

export async function getEmployeeAppointments(fecha) {
  const { data } = await apiClient.get('/api/empleado/turnos', { params: { fecha } })
  return data
}
