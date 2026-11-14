import { expect, it } from 'vitest'
import { downloadName, extractedText, problemPath, solutionPath, solutionTarget, validateChatFile } from './chat.js'

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
it('links directly to a solution and rejects invalid or arbitrary hash targets', () => {
  expect(solutionPath({ problemaId: 7, solucionId: 8, enlace: 'https://externo.invalid' })).toBe('/problemas/7#solucion-8')
  expect(solutionPath({ problemaId: 7, solucionId: '8' })).toBeNull()
  expect(solutionPath({ problemaId: 0, solucionId: 8 })).toBeNull()
  expect(solutionTarget('#solucion-8')).toBe(8)
  for (const hash of [undefined, '', '#solucion-0', '#solucion--1', '#solucion-1.2', '#solucion-99999999999999999', '#externo', '#solucion-01']) expect(solutionTarget(hash)).toBeNull()
})
