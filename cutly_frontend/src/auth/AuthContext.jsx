import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient, { SESSION_STORAGE_KEY } from '../services/apiClient.js'
import AuthContext from './authContext.js'

function readStoredSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(SESSION_STORAGE_KEY) || 'null')
    if (session?.token && session?.user) return session
  } catch {
    // Invalid stored data is cleared below.
  }
  sessionStorage.removeItem(SESSION_STORAGE_KEY)
  return null
}

export function AuthProvider({ children }) {
  const navigate = useNavigate()
  const [session, setSession] = useState(readStoredSession)
  const [isInitializing] = useState(false)

  const signIn = useCallback(async (email, contrasena) => {
    const { data } = await apiClient.post('/auth/iniciarSesion', { email, contrasena })
    if (!data?.token || !data?.rol) {
      throw new Error('La respuesta del servidor no contiene una sesión válida.')
    }
    const nextSession = {
      token: data.token,
      user: {
        userId: data.userId,
        dni: data.dni,
        nombre: data.nombre,
        apellido: data.apellido,
        email: data.email,
        status: data.status,
        rol: data.rol,
        fechaExpiracion: data.fechaExpiracion,
      },
    }
    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(nextSession))
    setSession(nextSession)
    if (data.rol === 'SUPER_ADMIN') navigate('/dashboard', { replace: true })
    else if (data.rol === 'OWNER') navigate('/owner', { replace: true })
    else navigate('/acceso-denegado', { replace: true })
  }, [navigate])

  const signOut = useCallback(async () => {
    let logoutError
    try {
      if (session?.token) await apiClient.post('/auth/cerrarSesion')
    } catch (error) {
      logoutError = error
    } finally {
      sessionStorage.removeItem(SESSION_STORAGE_KEY)
      setSession(null)
      navigate('/login', { replace: true })
    }
    if (logoutError) throw logoutError
  }, [navigate, session])

  useEffect(() => {
    const interceptorId = apiClient.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401 && sessionStorage.getItem(SESSION_STORAGE_KEY)) {
          sessionStorage.removeItem(SESSION_STORAGE_KEY)
          setSession(null)
          navigate('/login', { replace: true, state: { sessionExpired: true } })
        }
        return Promise.reject(error)
      },
    )
    return () => apiClient.interceptors.response.eject(interceptorId)
  }, [navigate])

  const value = useMemo(() => ({
    isAuthenticated: Boolean(session?.token),
    isInitializing,
    user: session?.user ?? null,
    role: session?.user?.rol ?? null,
    signIn,
    signOut,
  }), [isInitializing, session, signIn, signOut])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

