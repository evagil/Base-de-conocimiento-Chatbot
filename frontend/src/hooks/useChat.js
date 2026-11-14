import { useEffect, useRef, useState } from 'react'
import { chatApi } from '../api/chatApi.js'
import { extractedText, MAX_QUESTION, validateChatFile } from '../utils/chat.js'

export function useChat(agent = false) {
  const [question, setQuestion] = useState('')
  const [history, setHistory] = useState([])
  const [document, setDocument] = useState(null)
  const [extracted, setExtracted] = useState('')
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const pending = useRef(null)
  useEffect(() => () => { pending.current?.abort(); pending.current = null }, [])
  const run = async (kind, operation, success) => {
    if (pending.current) return
    const controller = new AbortController()
    pending.current = controller
    setBusy(kind); setError('')
    try {
      const result = await operation(controller.signal)
      if (pending.current === controller && !controller.signal.aborted) success(result)
    } catch (e) {
      if (pending.current === controller && !controller.signal.aborted) setError(e.message || 'No se pudo completar la consulta.')
    } finally {
      if (pending.current === controller) { pending.current = null; setBusy('') }
    }
  }
  const cancel = () => {
    pending.current?.abort(); pending.current = null; setBusy('')
  }
  const clear = () => {
    cancel(); setQuestion(''); setHistory([]); setDocument(null); setExtracted(''); setError('')
  }
  const ask = () => {
    const text = question.trim()
    if (!text || question.length > MAX_QUESTION) { setError(`Escribí una consulta de hasta ${MAX_QUESTION} caracteres.`); return }
    return run('answer', signal => (agent ? chatApi.agent : chatApi.answer)(text, signal), answer => {
      // Cada turno es una consulta independiente; no se envía el historial como evidencia.
      setHistory(current => [...current.slice(-9), { question: text, answer }]); setQuestion('')
    })
  }
  const extract = file => {
    const invalid = validateChatFile(file)
    if (invalid) { setError(invalid); return }
    return run('extract', signal => chatApi.extract(file, signal), result => {
      setDocument(result); setExtracted(extractedText(result))
    })
  }
  const applyExtracted = () => {
    setQuestion(current => [current.trim(), extracted.trim()].filter(Boolean).join('\n\n'))
    setDocument(null); setExtracted(''); setError('')
  }
  return { question, setQuestion, history, document, extracted, setExtracted, busy, error, setError,
    ask, extract, applyExtracted, cancel, clear, run }
}
