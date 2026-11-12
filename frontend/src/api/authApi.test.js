import { expect, it, vi } from 'vitest'
vi.mock('./httpClient.js', () => ({ http: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }))
import { authApi } from './authApi.js'
import { http } from './httpClient.js'

it('sends the observed user version and maps authentication endpoints', async () => {
  http.get.mockResolvedValue({ data: 'get' })
  http.post.mockResolvedValue({ data: 'post' })
  http.put.mockResolvedValue({ data: 'put' })
  await expect(authApi.login({})).resolves.toBe('post')
  await expect(authApi.logout('token')).resolves.toMatchObject({ data: 'post' })
  await expect(authApi.me()).resolves.toBe('get')
  await expect(authApi.users()).resolves.toBe('get')
  await expect(authApi.audit()).resolves.toBe('get')
  await expect(authApi.changePassword({ passwordActual: 'a', passwordNueva: 'b' })).resolves.toMatchObject({ data: 'post' })
  await expect(authApi.createUser({})).resolves.toBe('post')
  await expect(authApi.updateUser(1, { activo: false }, 3)).resolves.toBe('put')
  expect(http.put).toHaveBeenLastCalledWith('/api/usuarios/1', { activo: false }, { params: { version: 3 } })
})
