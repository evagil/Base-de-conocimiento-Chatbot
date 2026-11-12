import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import HierarchyFormDialog from './HierarchyFormDialog.jsx'
import NavigationTree from './NavigationTree.jsx'
import { knowledgeApi } from '../api/knowledgeApi.js'
vi.mock('../api/knowledgeApi.js', () => ({ knowledgeApi: { updateArea: vi.fn(), updateSubarea: vi.fn(), createArea: vi.fn(), createSubarea: vi.fn(), removeArea: vi.fn(), removeSubarea: vi.fn() } }))
afterEach(() => { cleanup(); vi.resetAllMocks() })
const sub = { id: 2, areaId: 1, nombre: 'Comercio', descripcion: null, orden: 2, version: 4, problems: [] }
const area = { id: 1, nombre: 'Rentas', descripcion: 'Municipal', orden: 1, version: 3, subareas: [sub] }
it.each([['area', area, 'updateArea'], ['subarea', sub, 'updateSubarea']])('edits %s using its loaded version', async (mode, entity, method) => {
  const onSaved = vi.fn(), onClose = vi.fn()
  render(<HierarchyFormDialog open mode={mode} entity={entity} tree={[area]} onSaved={onSaved} onClose={onClose}/> )
  expect(screen.getByLabelText(/Nombre/)).toHaveValue(entity.nombre)
  fireEvent.change(screen.getByLabelText(/Nombre/), { target: { value: 'Nuevo nombre' } })
  fireEvent.click(screen.getByRole('button', { name: 'Guardar' }))
  await waitFor(() => expect(onClose).toHaveBeenCalled())
  expect(knowledgeApi[method]).toHaveBeenCalledWith(entity.id, { nombre: 'Nuevo nombre', descripcion: entity.descripcion, orden: entity.orden }, entity.version)
  expect(onSaved).toHaveBeenCalled()
})

it.each([['area', area, 'removeArea'], ['subarea', sub, 'removeSubarea']])('removes %s using its loaded version', async (mode, entity, method) => {
  vi.spyOn(window, 'confirm').mockReturnValue(true)
  const onSaved = vi.fn(), onClose = vi.fn()
  render(<HierarchyFormDialog open mode={mode} entity={entity} tree={[area]} onSaved={onSaved} onClose={onClose}/> )
  fireEvent.click(screen.getByRole('button', { name: 'Eliminar' }))
  await waitFor(() => expect(onClose).toHaveBeenCalled())
  expect(knowledgeApi[method]).toHaveBeenCalledWith(entity.id, entity.version)
  expect(onSaved).toHaveBeenCalled()
  window.confirm.mockRestore()
})

it('preserves edits on a concurrency conflict', async () => {
  knowledgeApi.updateArea.mockRejectedValue(new Error('Otra persona modificó el registro'))
  const onClose = vi.fn()
  render(<HierarchyFormDialog open mode="area" entity={area} tree={[area]} onSaved={vi.fn()} onClose={onClose}/> )
  fireEvent.change(screen.getByLabelText(/Nombre/), { target: { value: 'Mi cambio' } })
  fireEvent.click(screen.getByRole('button', { name: 'Guardar' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Otra persona modificó')
  expect(screen.getByLabelText(/Nombre/)).toHaveValue('Mi cambio')
  expect(onClose).not.toHaveBeenCalled()
})
it('exposes editing only when callbacks are provided and opens folders with one click', () => {
  const onEditArea = vi.fn(), onEditSubarea = vi.fn()
  const { rerender } = render(<NavigationTree areas={[area]} onSelect={vi.fn()}/> )
  expect(screen.queryByRole('button', { name: /Editar/ })).not.toBeInTheDocument()
  rerender(<NavigationTree areas={[area]} onEditArea={onEditArea} onEditSubarea={onEditSubarea}/> )
  expect(screen.queryByRole('button', { name: 'Editar subárea Comercio' })).not.toBeInTheDocument()
  fireEvent.click(screen.getByText('Rentas'))
  fireEvent.click(screen.getByRole('button', { name: 'Editar área Rentas' }))
  fireEvent.click(screen.getByRole('button', { name: 'Editar subárea Comercio' }))
  expect(onEditArea).toHaveBeenCalledWith(area)
  expect(onEditSubarea).toHaveBeenCalledWith(sub)
})
