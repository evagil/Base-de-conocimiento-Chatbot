import { expect, it, vi } from 'vitest'
import { http } from './httpClient.js'
import { chatApi } from './chatApi.js'
vi.mock('./httpClient.js', () => ({ http: { post: vi.fn(), get: vi.fn() } }))

it('uses authenticated transport, explicit inference timeouts and abort signals', async () => {
  const signal = new AbortController().signal
  http.post.mockResolvedValue({ data: { estado: 'CON_FUENTES' } })
  expect(await chatApi.answer('E17', signal)).toEqual({ estado: 'CON_FUENTES' })
  expect(http.post).toHaveBeenLastCalledWith('/api/rag/responder', { pregunta: 'E17' }, { signal, timeout: 300000 })
  const file = new File(['contenido'], 'manual.pdf')
  await chatApi.extract(file, signal)
  const [route, form, options] = http.post.mock.lastCall
  expect(route).toBe('/api/documentos/extraer'); expect(form.get('archivo')).toBe(file)
  expect(options).toEqual({ signal, timeout: 180000 })
  http.get.mockResolvedValue({ data: new Blob(['privado']) })
  expect(await chatApi.download(7, signal)).toBeInstanceOf(Blob)
  expect(http.get).toHaveBeenCalledWith('/api/archivos/7/contenido', { signal, responseType: 'blob' })
})
