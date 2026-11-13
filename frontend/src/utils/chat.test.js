import { expect, it } from 'vitest'
import { downloadName, extractedText, problemPath, validateChatFile } from './chat.js'

it('bounds uploads and rejects unsupported extensions and empty files', () => {
  for (const name of ['a.PDF', 'a.docx', 'a.jpg', 'a.JPEG']) expect(validateChatFile({ name, size: 1 })).toBe('')
  expect(validateChatFile({ name: 'a.exe', size: 1 })).toContain('PDF')
  expect(validateChatFile({ name: 'a.pdf', size: 0 })).toContain('vacío')
  expect(validateChatFile({ name: 'a.pdf', size: 10485761 })).toContain('10 MB')
})
it('preserves extracted text and builds navigation from valid identifiers only', () => {
  expect(extractedText({})).toBe('')
  expect(extractedText({ secciones: [{ texto: 'Primera' }, { texto: '' }, { texto: 'Segunda' }] })).toBe('Primera\n\nSegunda')
  expect(problemPath({ problemaId: 7, enlace: 'https://malicioso.local' })).toBe('/problemas/7')
  for (const problemaId of [0, -1, '7', NaN, 1.5, Infinity]) expect(problemPath({ problemaId })).toBeNull()
  expect(downloadName({ titulo: '../Archivo\n', archivoId: 3 })).toBe('.._Archivo_-archivo-3')
  expect(downloadName({ archivoId: 3 })).toBe('Fuente-archivo-3')
})
