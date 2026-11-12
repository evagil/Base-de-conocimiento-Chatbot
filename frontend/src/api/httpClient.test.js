import axios from 'axios'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { http } from './httpClient.js'
import { session } from '../auth/session.js'
import { versionParams } from './version.js'

const response = config => ({ data: 'ok', status: 200, statusText: 'OK', headers: {}, config })
const initial = { accessToken: 'old', refreshToken: 'refresh-old', usuario: { nombre: 'Usuario' } }
const renewed = { ...initial, accessToken: 'new', refreshToken: 'refresh-new' }
const originalAdapter = http.defaults.adapter
beforeEach(() => { localStorage.clear(); session.save(initial) })
afterEach(() => { vi.restoreAllMocks(); http.defaults.adapter = originalAdapter; localStorage.clear() })

it('shares a single refresh between simultaneous unauthorized requests', async () => {
  let finish
  const refresh = vi.spyOn(axios, 'post').mockImplementation(() => new Promise(resolve => { finish = resolve }))
  http.defaults.adapter = config => config.headers.Authorization === 'Bearer old'
    ? Promise.reject(new axios.AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, { status: 401 }))
    : Promise.resolve(response(config))
  const requests = Promise.all([http.get('/api/areas'), http.get('/api/problemas')])
  await vi.waitFor(() => expect(refresh).toHaveBeenCalledTimes(1))
  finish({ data: renewed })
  await expect(requests).resolves.toHaveLength(2)
  expect(refresh).toHaveBeenCalledTimes(1)
  expect(session.refresh()).toBe('refresh-new')
})

it('does not resurrect a session logged out during refresh', async () => {
  let finish
  const refresh = vi.spyOn(axios, 'post').mockImplementation(() => new Promise(resolve => { finish = resolve }))
  http.defaults.adapter = config => Promise.reject(new axios.AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, { status: 401 }))
  const request = http.get('/api/areas')
  const rejected = expect(request).rejects.toThrow('La sesión cambió')
  await vi.waitFor(() => expect(refresh).toHaveBeenCalledTimes(1))
  session.clear()
  finish({ data: renewed })
  await rejected
  expect(session.access()).toBeNull()
})

it('preserves conflict information and never automatically retries a stale edit', async () => {
  const refresh = vi.spyOn(axios, 'post')
  const adapter = vi.fn(config => Promise.reject(new axios.AxiosError('Conflict', 'ERR_BAD_REQUEST', config, null,
    { status: 409, data: { codigo: 'VERSION_DESACTUALIZADA', mensaje: 'Otra persona modificó el registro.' } })))
  http.defaults.adapter = adapter
  await expect(http.put('/api/problemas/1', {}, versionParams(2))).rejects.toMatchObject({
    status: 409, code: 'VERSION_DESACTUALIZADA', message: 'Otra persona modificó el registro.',
  })
  expect(adapter).toHaveBeenCalledTimes(1)
  expect(refresh).not.toHaveBeenCalled()
})

it('explains a server upload size rejection', async () => {
  http.defaults.adapter = config => Promise.reject(new axios.AxiosError('Request failed', 'ERR_BAD_REQUEST', config, null,
    { status: 413, data: '<html>Request Entity Too Large</html>' }))
  await expect(http.post('/api/soluciones/1/adjuntos', new FormData())).rejects.toMatchObject({
    status: 413, message: expect.stringContaining('servidor rechazó el archivo por tamaño'),
  })
})

it('requires an explicit nonnegative version for modifications', () => {
  expect(versionParams(0)).toEqual({ params: { version: 0 } })
  for (const value of [undefined, null, -1, 1.2, '2']) expect(() => versionParams(value)).toThrow('Falta la versión')
})

it('uses the token already renewed by another request without refreshing again', async () => {
  const refresh = vi.spyOn(axios, 'post')
  http.defaults.adapter = config => {
    if (config.headers.Authorization === 'Bearer old') {
      session.save(renewed)
      return Promise.reject(new axios.AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, { status: 401 }))
    }
    return Promise.resolve(response(config))
  }
  await expect(http.get('/api/areas')).resolves.toMatchObject({ data: 'ok' })
  expect(refresh).not.toHaveBeenCalled()
})

it('coordinates refresh through Web Locks when available', async () => {
  const locks = { request: vi.fn((_name, operation) => operation()) }
  vi.stubGlobal('navigator', { locks })
  try {
    vi.spyOn(axios, 'post').mockResolvedValue({ data: renewed })
    http.defaults.adapter = config => config.headers.Authorization === 'Bearer old'
      ? Promise.reject(new axios.AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, { status: 401 }))
      : Promise.resolve(response(config))
    await http.get('/api/areas')
    expect(locks.request).toHaveBeenCalledTimes(1)
  } finally { vi.unstubAllGlobals() }
})

it('clears an expired refresh token and reports its error', async () => {
  vi.spyOn(axios, 'post').mockRejectedValue(new axios.AxiosError('Expired', 'ERR_BAD_REQUEST', {}, null,
    { status: 401, data: { detail: 'La sesión venció.' } }))
  http.defaults.adapter = config => Promise.reject(new axios.AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, { status: 401 }))
  await expect(http.get('/api/areas')).rejects.toThrow('La sesión venció.')
  expect(session.access()).toBeNull()
})

it('does not renew failed logins or requests after logout', async () => {
  const refresh = vi.spyOn(axios, 'post')
  http.defaults.adapter = config => Promise.reject(new axios.AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, { status: 401 }))
  await expect(http.post('/api/auth/login', {})).rejects.toThrow()
  session.clear()
  await expect(http.get('/api/areas')).rejects.toThrow()
  expect(refresh).not.toHaveBeenCalled()
})
