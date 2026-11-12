import axios from 'axios'
import { session } from '../auth/session.js'

export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '')
export const http = axios.create({ baseURL: API_URL, timeout: 15000, headers: { Accept: 'application/json' } })

http.interceptors.request.use(config => {
  const token = session.access()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let refreshInFlight
async function refreshSession(failedAuthorization) {
  // Another request or browser tab may already have rotated the token.
  if (session.access() && `Bearer ${session.access()}` !== failedAuthorization) return
  const refreshToken = session.refresh()
  if (!refreshToken) throw new Error('La sesión terminó. Volvé a ingresar.')
  try {
    const { data } = await axios.post(`${API_URL}/api/auth/refresh`, { refreshToken }, { timeout: 15000 })
    // Do not restore a session that was logged out or replaced while waiting.
    if (session.refresh() !== refreshToken) throw new Error('La sesión cambió. Volvé a ingresar.')
    session.save(data)
  } catch (error) {
    if (session.refresh() === refreshToken) session.clear()
    throw error
  }
}

function renew(authorization) {
  if (!refreshInFlight) {
    // Web Locks coordinate same-origin tabs; the promise also covers browsers without Web Locks.
    const operation = () => refreshSession(authorization)
    refreshInFlight = Promise.resolve().then(() =>
      globalThis.navigator?.locks
        ? navigator.locks.request('bc-session-refresh', operation)
        : operation()
    ).finally(() => { refreshInFlight = undefined })
  }
  return refreshInFlight
}

http.interceptors.response.use(
  response => response,
  async error => {
    const original = error.config
    if (original && error.response?.status === 401 && !original._retried &&
        session.refresh() && !original.url?.includes('/api/auth/')) {
      original._retried = true
      try {
        await renew(original.headers?.Authorization)
        original.headers.Authorization = `Bearer ${session.access()}`
        return http(original)
      } catch (refreshError) {
        error = refreshError
      }
    }
    const result = new Error(error.response?.status === 413
      ? 'El servidor rechazó el archivo por tamaño. Avisá a administración para revisar el límite de carga.'
      : error.response?.data?.mensaje || error.response?.data?.detail ||
        error.message || 'No se pudo completar la operación.')
    result.status = error.response?.status
    result.code = error.response?.data?.codigo
    throw result
  }
)

export const imageUrl = path => path?.startsWith('http') ? path : `${API_URL}${path}`
