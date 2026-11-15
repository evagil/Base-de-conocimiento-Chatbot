import { act, render, screen, waitFor, cleanup } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import ProtectedImage from './ProtectedImage.jsx'
import { knowledgeApi } from '../api/knowledgeApi.js'
vi.mock('../api/knowledgeApi.js', () => ({ knowledgeApi: { imageContent: vi.fn() } }))
afterEach(() => { cleanup(); vi.restoreAllMocks() })
it('muestra solo el blob protegido y revoca la URL al salir', async () => {
  vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:protegida'); const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
  knowledgeApi.imageContent.mockResolvedValue(new Blob(['protegida'], { type: 'image/png' }))
  const { unmount } = render(<ProtectedImage image={{ id: 1, version: 0, textoAlternativo: 'Captura' }}/>)
  expect(await screen.findByRole('img')).toHaveAttribute('src', 'blob:protegida'); unmount(); expect(revoke).toHaveBeenCalledWith('blob:protegida')
})
it('oculta la imagen si falla la protección sin cargar un original alternativo', async () => {
  knowledgeApi.imageContent.mockRejectedValue(new Error('OCR no disponible'))
  render(<ProtectedImage image={{ id: 2, textoAlternativo: 'Captura' }}/>)
  expect(await screen.findByText('Imagen oculta: no se pudo preparar la vista protegida.')).toBeVisible(); expect(screen.queryByRole('img')).not.toBeInTheDocument()
})
it('serializa las preparaciones para no saturar el OCR local', async () => {
  vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:protegida'); vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
  let terminar; knowledgeApi.imageContent.mockImplementationOnce(() => new Promise(resolve => { terminar = resolve })).mockResolvedValue(new Blob(['ok']))
  render(<><ProtectedImage image={{ id: 3, textoAlternativo: 'Una' }}/><ProtectedImage image={{ id: 4, textoAlternativo: 'Otra' }}/></>)
  await waitFor(() => expect(terminar).toBeTypeOf('function')); expect(knowledgeApi.imageContent).not.toHaveBeenCalledWith(4)
  await act(async () => terminar(new Blob(['ok']))); await screen.findByAltText('Otra')
})
