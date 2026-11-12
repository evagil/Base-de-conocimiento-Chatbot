import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import App from './App.jsx'

vi.mock('../api/knowledgeApi.js', () => ({ knowledgeApi: { images: vi.fn().mockResolvedValue([]) } }))
vi.mock('../auth/AuthContext.jsx', () => ({
  useAuth: () => ({ user: { nombre: 'Editor', usuario: 'editor', rol: 'EDITOR' }, canEdit: true, isAdmin: false, changePassword: vi.fn(), logout: vi.fn() }),
}))
vi.mock('../hooks/useKnowledge.js', () => ({
  useKnowledge: () => ({
    tree: [],
    problem: { problema: { id: 3, subareaId: '', titulo: 'Problema anterior', descripcionHtml: '<p>Anterior</p>', estado: 'PUBLICADO', etiquetas: [] } },
    selectProblem: vi.fn(), loadTree: vi.fn(),
  }),
}))
vi.mock('../components/ProblemDetail.jsx', () => ({ default: () => null }))
vi.mock('../components/AdminDialog.jsx', () => ({ default: () => null }))
vi.mock('../components/RichTextEditor.jsx', () => ({
  default: ({ label, value, onChange }) => <textarea aria-label={label} value={value} onChange={e => onChange(e.target.value)}/>,
}))

afterEach(() => { cleanup(); localStorage.removeItem('bc_sidebar_pinned'); vi.restoreAllMocks() })

it('opens an empty creation form even when a problem is selected, and preserves editing', async () => {
  render(<MemoryRouter initialEntries={['/problemas/3']}><App/></MemoryRouter>)
  fireEvent.click(screen.getByRole('button', { name: 'Editar', exact: true }))
  let dialog = screen.getByRole('dialog')
  expect(within(dialog).getByText('Editar problema')).toBeInTheDocument()
  expect(within(dialog).getByLabelText(/Título del problema/)).toHaveValue('Problema anterior')
  fireEvent.click(within(dialog).getByRole('button', { name: 'Cancelar' }))
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

  fireEvent.click(screen.getByRole('button', { name: 'Nuevo problema' }))
  dialog = screen.getByRole('dialog')
  expect(within(dialog).getByText('Nuevo problema')).toBeInTheDocument()
  expect(within(dialog).getByLabelText(/Título del problema/)).toHaveValue('')
  expect(within(dialog).getByText('2. Solución inicial (opcional)')).toBeInTheDocument()
  fireEvent.change(within(dialog).getByLabelText(/Título del problema/), { target: { value: 'Descartar' } })
  fireEvent.click(within(dialog).getByRole('button', { name: 'Cancelar' }))
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  fireEvent.click(screen.getByRole('button', { name: 'Nuevo problema' }))
  expect(within(screen.getByRole('dialog')).getByLabelText(/Título del problema/)).toHaveValue('')
}, 15000)


it('can unpin the desktop sidebar and remembers the preference', async () => {
  vi.spyOn(window, 'matchMedia').mockImplementation(query => ({ matches: query.includes('min-width:1200px'), media: query, addEventListener() {}, removeEventListener() {} }))
  const { unmount } = render(<MemoryRouter><App/></MemoryRouter>)
  fireEvent.click(screen.getByRole('button', { name: 'Desfijar barra lateral' }))
  expect(localStorage.getItem('bc_sidebar_pinned')).toBe('false')
  fireEvent.click(screen.getByRole('button', { name: 'Abrir barra lateral' }))
  expect(await screen.findByRole('button', { name: 'Fijar barra lateral' })).toBeInTheDocument()
  unmount()
  render(<MemoryRouter><App/></MemoryRouter>)
  expect(screen.getByRole('button', { name: 'Abrir barra lateral' })).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Abrir barra lateral' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Fijar barra lateral' }))
  expect(localStorage.getItem('bc_sidebar_pinned')).toBe('true')
  await waitFor(() => expect(screen.getAllByRole('button', { name: 'Desfijar barra lateral' })).toHaveLength(1))
})

it('uses an overlay on narrow screens even when pinning is preferred', async () => {
  localStorage.setItem('bc_sidebar_pinned', 'true')
  render(<MemoryRouter><App/></MemoryRouter>)
  expect(screen.queryByRole('separator', { name: 'Ancho de la barra lateral' })).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Abrir barra lateral' }))
  expect(await screen.findByRole('button', { name: 'Desfijar barra lateral' })).toBeInTheDocument()
})
