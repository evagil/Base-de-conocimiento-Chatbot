import { act, cleanup, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { chatApi } from '../api/chatApi.js'
import { useChat } from './useChat.js'
vi.mock('../api/chatApi.js', () => ({ chatApi: { answer: vi.fn(), agent: vi.fn(), extract: vi.fn() } }))
beforeEach(() => { vi.resetAllMocks() })
afterEach(cleanup)
const answer = { estado: 'INFORMACION_INSUFICIENTE', texto: 'Información insuficiente.', fuentes: [] }

it('uses only the textual agent endpoint in agent mode', async () => {
  chatApi.agent.mockResolvedValue(answer)
  const { result } = renderHook(() => useChat(true))
  act(() => result.current.setQuestion('Error E17'))
  await act(() => result.current.ask())
  expect(chatApi.agent).toHaveBeenCalledWith('Error E17', expect.any(AbortSignal))
  expect(chatApi.answer).not.toHaveBeenCalled(); expect(chatApi.extract).not.toHaveBeenCalled()
})

it('submits independent questions and retains at most ten turns without storage', async () => {
  const save = vi.spyOn(Storage.prototype, 'setItem')
  chatApi.answer.mockResolvedValue(answer)
  const { result } = renderHook(useChat)
  for (let i = 0; i < 12; i++) {
    act(() => result.current.setQuestion(`  Consulta ${i}  `))
    await act(() => result.current.ask())
  }
  expect(result.current.history).toHaveLength(10)
  expect(result.current.history[0].question).toBe('Consulta 2')
  expect(result.current.question).toBe('')
  expect(chatApi.answer.mock.lastCall[0]).toBe('Consulta 11')
  expect(save).not.toHaveBeenCalled(); save.mockRestore()
  act(() => result.current.clear()); expect(result.current.history).toEqual([])
})
it('rejects empty and oversized questions without sending or silently truncating', async () => {
  const { result } = renderHook(useChat)
  act(() => result.current.ask()); expect(result.current.error).toContain('1000')
  act(() => result.current.setQuestion('x'.repeat(1001)))
  act(() => result.current.ask()); expect(result.current.question).toHaveLength(1001)
  expect(chatApi.answer).not.toHaveBeenCalled()
})
it('reviews extraction before adding it to the question and preserves all text', async () => {
  const { result } = renderHook(useChat)
  act(() => result.current.extract(new File(['x'], 'a.exe')))
  expect(chatApi.extract).not.toHaveBeenCalled()
  const document = { nombre: 'a.pdf', secciones: [{ texto: 'x'.repeat(1200) }] }
  chatApi.extract.mockResolvedValue(document)
  await act(() => result.current.extract(new File(['x'], 'a.pdf')))
  expect(result.current.document).toEqual(document); expect(result.current.question).toBe('')
  act(() => result.current.setQuestion('Mi pregunta'))
  act(() => result.current.applyExtracted())
  expect(result.current.question).toBe(`Mi pregunta\n\n${'x'.repeat(1200)}`)
  expect(result.current.document).toBeNull()
  act(() => result.current.setExtracted(' ')); act(() => result.current.applyExtracted())
  act(() => result.current.clear()); expect(result.current.extracted).toBe('')
})
it('cancels a pending turn, rejects duplicates and ignores its late completion', async () => {
  let resolve
  chatApi.answer.mockImplementation(() => new Promise(r => { resolve = r }))
  const { result } = renderHook(useChat)
  act(() => result.current.setQuestion('E17'))
  let operation
  act(() => { operation = result.current.ask() })
  act(() => { result.current.ask() }); expect(chatApi.answer).toHaveBeenCalledTimes(1)
  const signal = chatApi.answer.mock.lastCall[1]
  act(() => result.current.cancel()); expect(signal.aborted).toBe(true)
  await act(async () => { resolve(answer); await operation })
  expect(result.current.history).toEqual([]); expect(result.current.question).toBe('E17')
})
it('keeps the draft on errors and ignores failures after unmount', async () => {
  const { result, unmount } = renderHook(useChat)
  act(() => result.current.setQuestion('E17'))
  chatApi.answer.mockRejectedValue(new Error('Abrí Ollama.'))
  await act(() => result.current.ask()); expect(result.current.error).toBe('Abrí Ollama.'); expect(result.current.question).toBe('E17')
  chatApi.answer.mockRejectedValue({})
  await act(() => result.current.ask()); expect(result.current.error).toContain('No se pudo')
  let reject
  chatApi.answer.mockImplementation(() => new Promise((_, r) => { reject = r }))
  let operation
  act(() => { operation = result.current.ask() })
  const signal = chatApi.answer.mock.lastCall[1]
  unmount(); expect(signal.aborted).toBe(true)
  await act(async () => { reject(new Error('Tardío')); await operation })
})
