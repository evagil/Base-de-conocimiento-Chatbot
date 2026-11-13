import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import ChatbotDialog from './ChatbotDialog.jsx'
import { chatApi } from '../api/chatApi.js'
vi.mock('../api/chatApi.js', () => ({ chatApi: { answer: vi.fn(), extract: vi.fn(), download: vi.fn() } }))
beforeEach(() => vi.resetAllMocks())
afterEach(() => { cleanup(); vi.restoreAllMocks() })
const source = { problemaId: 7, solucionId: 8, pasoId: 9, archivoId: 10, tipo: 'ADJUNTO', titulo: 'Error E17', pagina: 2, extracto: 'Reiniciar la aplicación.', enlace: 'https://externo.invalid', enlaceArchivo: 'https://externo.invalid/archivo' }
function show() {
 const close = vi.fn()
 const view = render(<MemoryRouter><Routes><Route path="/" element={<ChatbotDialog open onClose={close}/>}/><Route path="/problemas/7" element={<p>Detalle del problema abierto</p>}/></Routes></MemoryRouter>)
 return { ...view, close }
}
it('renders evidence as text, warns about bounded search and navigates by verified identifiers', async () => {
 chatApi.answer.mockResolvedValue({ estado: 'CON_FUENTES', texto: '<script>Texto literal</script>', fuentes: [source], busquedaAcotada: true })
 show()
 fireEvent.change(screen.getByLabelText('Tu consulta al conocimiento'), { target: { value: 'E17' } })
 fireEvent.click(screen.getByRole('button', { name: 'Consultar', exact: true }))
 expect(await screen.findByText('<script>Texto literal</script>')).toBeInTheDocument()
 expect(document.querySelector('script')).toBeNull()
 expect(screen.getByText(/límite de candidatos/)).toBeInTheDocument()
 expect(screen.getByText(/Página 2/)).toBeInTheDocument()
 expect(document.querySelector('a[href^="https://externo"]')).toBeNull()
 fireEvent.click(screen.getByRole('button', { name: 'Abrir problema' }))
 expect(await screen.findByText('Detalle del problema abierto')).toBeInTheDocument()
})
it('extracts and reviews a document before querying, with explicit length validation', async () => {
 chatApi.extract.mockResolvedValue({ nombre: 'captura.jpg', paginas: 1, ocrUsado: true, secciones: [{ texto: 'x'.repeat(1200) }] })
 chatApi.answer.mockResolvedValue({ estado: 'INFORMACION_INSUFICIENTE', texto: 'Información insuficiente para responder.', fuentes: [] })
 show()
 fireEvent.change(screen.getByLabelText('Seleccionar archivo para el chatbot'), { target: { files: [new File(['x'], 'captura.jpg')] } })
 const extracted = await screen.findByLabelText('Texto extraído (editable)')
 expect(extracted).toHaveValue('x'.repeat(1200)); expect(screen.getByText(/revisá posibles errores/)).toBeInTheDocument()
 expect(chatApi.answer).not.toHaveBeenCalled()
 fireEvent.click(screen.getByRole('button', { name: 'Usar texto en consulta' }))
 expect(screen.getByRole('button', { name: 'Consultar', exact: true })).toBeDisabled()
 expect(screen.getByText(/Acortá el texto/)).toBeInTheDocument()
 fireEvent.change(screen.getByLabelText('Tu consulta al conocimiento'), { target: { value: 'Error E17' } })
 fireEvent.click(screen.getByRole('button', { name: 'Consultar', exact: true }))
 expect(await screen.findByText('Información insuficiente para responder.')).toBeInTheDocument()
 expect(screen.queryByRole('button', { name: 'Abrir problema' })).not.toBeInTheDocument()
 fireEvent.click(screen.getByRole('button', { name: 'Limpiar conversación' }))
 expect(screen.queryByText('Información insuficiente para responder.')).not.toBeInTheDocument()
})
it('shows busy and failure states and cancels on close without accepting a late answer', async () => {
 let resolve
 chatApi.answer.mockImplementation(() => new Promise(r => { resolve = r }))
 const { close } = show()
 fireEvent.change(screen.getByLabelText('Tu consulta al conocimiento'), { target: { value: 'E17' } })
 fireEvent.click(screen.getByRole('button', { name: 'Consultar', exact: true }))
 expect(screen.getByRole('status')).toHaveTextContent('Buscando evidencia')
 expect(screen.getByRole('button', { name: 'Consultar', exact: true })).toBeDisabled()
 const signal = chatApi.answer.mock.lastCall[1]
 fireEvent.click(screen.getByRole('button', { name: 'Cerrar chatbot' }))
 expect(close).toHaveBeenCalled(); expect(signal.aborted).toBe(true)
 await act(async () => resolve({ texto: 'Tardío', fuentes: [] }))
 expect(screen.queryByText('Tardío')).not.toBeInTheDocument()
 chatApi.answer.mockRejectedValue(new Error('Abrí Ollama.'))
 fireEvent.click(screen.getByRole('button', { name: 'Consultar', exact: true }))
 expect(await screen.findByRole('alert')).toHaveTextContent('Abrí Ollama.')
 expect(screen.getByLabelText('Tu consulta al conocimiento')).toHaveValue('E17')
})
it('downloads a source through the authenticated API as a blob', async () => {
 const create = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:privado')
 const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
 chatApi.answer.mockResolvedValue({ estado: 'CON_FUENTES', texto: 'Reiniciar.', fuentes: [source] })
 chatApi.download.mockResolvedValue(new Blob(['pdf']))
 show()
 fireEvent.change(screen.getByLabelText('Tu consulta al conocimiento'), { target: { value: 'E17' } })
 fireEvent.click(screen.getByRole('button', { name: 'Consultar', exact: true }))
 fireEvent.click(await screen.findByRole('button', { name: 'Descargar fuente' }))
 await waitFor(() => expect(click).toHaveBeenCalled())
 expect(chatApi.download).toHaveBeenCalledWith(10, expect.any(AbortSignal)); expect(create).toHaveBeenCalled()
})
