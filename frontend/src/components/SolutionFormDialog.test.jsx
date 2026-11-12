import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import SolutionFormDialog from './SolutionFormDialog.jsx'
import { knowledgeApi } from '../api/knowledgeApi.js'
vi.mock('../api/knowledgeApi.js',()=>({knowledgeApi:{updateSolution:vi.fn(),createSolution:vi.fn(),createStep:vi.fn(),uploadImage:vi.fn(),images:vi.fn().mockResolvedValue([])}}))
vi.mock('./RichTextEditor.jsx',()=>({default:({label,value,onChange,onPasteImages})=><textarea aria-label={label} value={value} onChange={e=>onChange(e.target.value)} onPaste={e=>onPasteImages?.(Array.from(e.clipboardData?.files||[]))}/>}))
afterEach(()=>{cleanup();vi.clearAllMocks()})
const solution={id:8,titulo:'Original',descripcionHtml:'<p>Anterior</p>',principal:true,orden:4,version:7}
it('loads and updates the selected solution while preserving its order',async()=>{
  knowledgeApi.updateSolution.mockResolvedValue({id:8})
  const onSaved=vi.fn(),onClose=vi.fn()
  render(<SolutionFormDialog open solution={solution} problemId={3} onSaved={onSaved} onClose={onClose}/>)
  expect(screen.getByLabelText('Título opcional')).toHaveValue('Original')
  expect(screen.queryByLabelText('Primer paso opcional')).not.toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Título opcional'),{target:{value:'<strong>Original</strong>'}})
  fireEvent.change(screen.getByLabelText('Descripción de la solución'),{target:{value:'<p>Corregida</p>'}})
  fireEvent.click(screen.getByRole('button',{name:'Guardar cambios'}))
  await waitFor(()=>expect(onClose).toHaveBeenCalled())
  expect(knowledgeApi.updateSolution).toHaveBeenCalledWith(8,{titulo:'<strong>Original</strong>',descripcionHtml:'<p>Corregida</p>',principal:true,orden:4,motivoCambio:'Edición desde la interfaz'},7)
  expect(onSaved).toHaveBeenCalledWith(3)
  expect(knowledgeApi.createSolution).not.toHaveBeenCalled()
  expect(knowledgeApi.createStep).not.toHaveBeenCalled()
})

it('uploads images when saving an existing solution',async()=>{
  knowledgeApi.updateSolution.mockResolvedValue({id:8})
  knowledgeApi.uploadImage.mockResolvedValue({id:99})
  const onSaved=vi.fn(),onClose=vi.fn()
  render(<SolutionFormDialog open solution={solution} problemId={3} onSaved={onSaved} onClose={onClose}/>)
  const file=new File(['img'],'captura.png',{type:'image/png'})
  const input=screen.getByLabelText('Agregar imágenes de la solución')
  fireEvent.change(input,{target:{files:[file]}})
  expect(screen.getByLabelText('Descripción de captura.png')).toHaveValue('captura')
  fireEvent.click(screen.getByRole('button',{name:'Guardar cambios'}))
  await waitFor(()=>expect(onClose).toHaveBeenCalled())
  expect(knowledgeApi.uploadImage).toHaveBeenCalledWith('soluciones',8,file,'captura',0)
})

it('keeps the form open when saving fails and resets for a new solution',async()=>{
  knowledgeApi.updateSolution.mockRejectedValue(Object.assign(new Error('Otra persona modificó el registro'), {code:'VERSION_DESACTUALIZADA',status:409}))
  const onClose=vi.fn()
  const {rerender}=render(<SolutionFormDialog open solution={solution} problemId={3} onSaved={vi.fn()} onClose={onClose}/>)
  fireEvent.change(screen.getByLabelText('Descripción de la solución'),{target:{value:'<p>Mi texto pendiente</p>'}})
  fireEvent.click(screen.getByRole('button',{name:'Guardar cambios'}))
  expect(await screen.findByRole('alert')).toHaveTextContent('Otra persona modificó el registro')
  expect(screen.getByLabelText('Descripción de la solución')).toHaveValue('<p>Mi texto pendiente</p>')
  expect(knowledgeApi.updateSolution).toHaveBeenLastCalledWith(solution.id,expect.objectContaining({descripcionHtml:'<p>Mi texto pendiente</p>'}),solution.version)
  expect(onClose).not.toHaveBeenCalled()
  rerender(<SolutionFormDialog open problemId={3} onSaved={vi.fn()} onClose={onClose}/>)
  expect(screen.getByLabelText('Título opcional')).toHaveValue('')
  expect(screen.getByLabelText('Primer paso opcional')).toBeInTheDocument()
  expect(screen.getByRole('switch')).not.toBeChecked()
})
