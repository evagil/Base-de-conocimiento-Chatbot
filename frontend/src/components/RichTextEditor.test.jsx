import { fireEvent, render, screen, cleanup } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import RichTextEditor from './RichTextEditor.jsx'

afterEach(cleanup)

it.each([
  ['texto', '', 'Contenido único'],
  ['HTML', '<p><strong>Contenido único</strong></p>', 'Contenido único'],
])('inserts pasted %s only once', async (_kind, html, text) => {
  render(<RichTextEditor onChange={vi.fn()}/>)
  const editor = screen.getByLabelText('Contenido', { selector: '[contenteditable]' })
  fireEvent.paste(editor, { clipboardData: { items: [], getData: type => type === 'text/html' ? html : text } })
  expect(editor.textContent).toBe('Contenido único')
  if (html) expect(editor.querySelector('strong')).toHaveTextContent('Contenido único')
})

it('attaches a pasted image once and inserts its accompanying text once', async () => {
  const onPasteImages = vi.fn()
  render(<RichTextEditor onChange={vi.fn()} onPasteImages={onPasteImages}/>)
  const editor = screen.getByLabelText('Contenido', { selector: '[contenteditable]' })
  const file = new File(['image'], 'captura.png', { type: 'image/png' })
  fireEvent.paste(editor, { clipboardData: {
    items: [{ kind: 'file', type: file.type, getAsFile: () => file }],
    getData: type => type === 'text/plain' ? 'Captura única' : '',
  } })
  expect(onPasteImages).toHaveBeenCalledExactlyOnceWith([file])
  expect(editor.textContent).toBe('Captura única')
})

it('removes empty clipboard edges while preserving formatting and interior blank paragraphs', () => {
  render(<RichTextEditor onChange={vi.fn()}/> )
  const editor = screen.getByLabelText('Contenido', { selector: '[contenteditable]' })
  fireEvent.paste(editor, { clipboardData: { items: [], getData: type => type === 'text/html'
    ? '<div><p><br></p><p><span>&nbsp;</span></p><p><strong>Primero</strong></p><p></p><p>Segundo</p><p><br></p></div>' : 'Primero\n\nSegundo' } })
  expect(editor.firstElementChild).toHaveTextContent('Primero')
  expect(editor.lastElementChild).toHaveTextContent('Segundo')
  expect(editor.querySelector('strong')).toHaveTextContent('Primero')
  expect(editor.querySelectorAll('p')).toHaveLength(3)
})

it('removes blank lines surrounding plain text without duplicating it', () => {
  render(<RichTextEditor onChange={vi.fn()}/> )
  const editor = screen.getByLabelText('Contenido', { selector: '[contenteditable]' })
  fireEvent.paste(editor, { clipboardData: { items: [], getData: type => type === 'text/plain' ? '\n\nContenido\n\n' : '' } })
  expect(editor.textContent).toBe('Contenido')
})

it('replaces the initial empty paragraph when pasting into a new form', () => {
  render(<RichTextEditor value="<p></p>" onChange={vi.fn()}/> )
  const editor = screen.getByLabelText('Contenido', { selector: '[contenteditable]' })
  fireEvent.paste(editor, { clipboardData: { items: [], getData: type => type === 'text/html' ? '<p>Texto pegado</p>' : 'Texto pegado' } })
  expect(editor.firstElementChild).toHaveTextContent('Texto pegado')
  expect(editor.querySelectorAll('p')).toHaveLength(1)
})
