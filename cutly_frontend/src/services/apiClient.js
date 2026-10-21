import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL?.trim()
export const SESSION_STORAGE_KEY = 'cutly.auth.session'

const apiClient = axios.create({
  ...(baseURL ? { baseURL } : {}),
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  try {
    const session = JSON.parse(sessionStorage.getItem(SESSION_STORAGE_KEY) || 'null')
    if (session?.token) {
      config.headers.Authorization = `Bearer ${session.token}`
    }
  } catch {
    sessionStorage.removeItem(SESSION_STORAGE_KEY)
  }

  return config
})

export default apiClient
