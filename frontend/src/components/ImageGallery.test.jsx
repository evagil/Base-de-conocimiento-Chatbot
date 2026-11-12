import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import ImageGallery from './ImageGallery.jsx'
import { knowledgeApi } from '../api/knowledgeApi.js'

vi.mock('../api/knowledgeApi.js', () => ({ knowledgeApi: { images: vi.fn() } }))
vi.mock('./ProtectedImage.jsx', () => ({ default: ({ image, large }) =>
  <img alt={image.textoAlternativo} data-testid={large ? 'large-image' : undefined}/> }))

afterEach(() => { cleanup(); vi.resetAllMocks() })

const images = ['Primera', 'Segunda', 'Tercera'].map((textoAlternativo, index) => ({
  id: index + 1, textoAlternativo, activo: true,
}))

it('navigates gallery images with buttons, keyboard and horizontal swipes', async () => {
  knowledgeApi.images.mockResolvedValue(images)
  render(<ImageGallery owner="soluciones" id={7}/> )
  fireEvent.click(await screen.findByRole('button', { name: 'Ampliar imagen: Primera' }))
  const dialog = screen.getByRole('dialog')
  const enlarged = () => within(dialog).getByTestId('large-image')

  expect(enlarged()).toHaveAttribute('alt', 'Primera')
  expect(within(dialog).getByRole('button', { name: 'Imagen anterior' })).toBeDisabled()
  fireEvent.click(within(dialog).getByRole('button', { name: 'Imagen siguiente' }))
  expect(enlarged()).toHaveAttribute('alt', 'Segunda')
  expect(within(dialog).getByText('2 de 3')).toBeInTheDocument()

  fireEvent.keyDown(document, { key: 'ArrowRight' })
  expect(enlarged()).toHaveAttribute('alt', 'Tercera')
  expect(within(dialog).getByRole('button', { name: 'Imagen siguiente' })).toBeDisabled()
  fireEvent.keyDown(document, { key: 'ArrowLeft' })
  expect(enlarged()).toHaveAttribute('alt', 'Segunda')

  fireEvent.touchStart(enlarged(), { touches: [{ clientX: 100, clientY: 20 }] })
  fireEvent.touchEnd(enlarged(), { changedTouches: [{ clientX: 180, clientY: 22 }] })
  expect(enlarged()).toHaveAttribute('alt', 'Primera')
  fireEvent.click(within(dialog).getByRole('button', { name: 'Cerrar imagen' }))
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
})

it('does not show navigation controls for a single image', async () => {
  knowledgeApi.images.mockResolvedValue([images[0]])
  render(<ImageGallery owner="problemas" id={3}/> )
  fireEvent.click(await screen.findByRole('button', { name: 'Ampliar imagen: Primera' }))
  const dialog = screen.getByRole('dialog')
  expect(within(dialog).queryByRole('button', { name: 'Imagen siguiente' })).not.toBeInTheDocument()
  expect(within(dialog).queryByRole('button', { name: 'Imagen anterior' })).not.toBeInTheDocument()
})
