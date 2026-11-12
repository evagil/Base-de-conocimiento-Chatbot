import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
vi.mock('../api/knowledgeApi.js',()=>({knowledgeApi:{attachments:vi.fn().mockResolvedValue([]),images:vi.fn(),imageContent:vi.fn()}}))
import { knowledgeApi } from '../api/knowledgeApi.js'
import NavigationTree from './NavigationTree.jsx'
import ProblemDetail from './ProblemDetail.jsx'
import ImageGallery from './ImageGallery.jsx'

describe('main visual components',()=>{
  beforeEach(()=>{vi.clearAllMocks();knowledgeApi.images.mockResolvedValue([]);knowledgeApi.imageContent.mockResolvedValue(new Blob(['image']))})
  it('navigates and collapses the knowledge tree', async ()=>{
    const select=vi.fn();const areas=[{id:1,nombre:'Área',subareas:[{id:2,nombre:'Subárea',problems:[{id:3,titulo:'Problema',estado:'PUBLICADO'}]}]}]
    render(<NavigationTree areas={areas} selectedId={3} onSelect={select}/>)
    expect(screen.queryByText('Problema')).not.toBeInTheDocument()
    fireEvent.click(screen.getByText('Área'))
    fireEvent.click(screen.getByText('Subárea'))
    fireEvent.click(screen.getByText('Problema'))
    expect(select).toHaveBeenCalledWith(3)
    fireEvent.click(screen.getByText('Subárea'))
    await waitFor(()=>expect(screen.queryByText('Problema')).not.toBeInTheDocument())
  })
  it('shows empty navigation and problem states',()=>{
    const {rerender}=render(<NavigationTree areas={[]} onSelect={()=>{}}/>);expect(screen.getByText('No hay resultados.')).toBeInTheDocument()
    rerender(<ProblemDetail detail={null}/>);expect(screen.getByText('Seleccioná un problema')).toBeInTheDocument()
  })
  it('renders all solutions and sanitizes unsafe markup',()=>{
    const detail={problema:{id:1,area:'A',subarea:'S',titulo:'Error',estado:'PUBLICADO',etiquetas:['red'],descripcionHtml:'<p>Detalle</p><script>alert(1)</script>',modificadoEn:'2026-01-01'},soluciones:[{id:2,titulo:'Reiniciar',principal:true,descripcionHtml:'<p>Solución</p>',pasos:[{id:3,orden:1,titulo:'Abrir',contenidoHtml:'<p>Panel</p>'}]}]}
    render(<ProblemDetail detail={detail}/>);expect(screen.getByText('Reiniciar')).toBeInTheDocument();expect(screen.getByText('Panel')).toBeInTheDocument();expect(document.querySelector('script')).toBeNull()
  })
  it('loads and opens an image gallery, and tolerates failures',async()=>{
    knowledgeApi.images.mockResolvedValueOnce([{id:1,activo:true,urlContenido:'/x.png',textoAlternativo:'captura'}])
    const {rerender}=render(<ImageGallery owner="problemas" id={1}/>);await screen.findByAltText('captura');fireEvent.click(screen.getByAltText('captura'));await waitFor(()=>expect(screen.getAllByAltText('captura')).toHaveLength(2))
    knowledgeApi.images.mockRejectedValueOnce(new Error('fail'));rerender(<ImageGallery owner="problemas" id={2}/>);await waitFor(()=>expect(screen.queryByAltText('captura')).not.toBeInTheDocument())
  })
})

it('hides removed solutions and exposes removal only to editors', () => {
  const solution={id:9,titulo:'Vigente',activo:true,descripcionHtml:'<p>Solución</p>',pasos:[],version:2}
  const detail={problema:{id:1,area:'A',subarea:'S',titulo:'Error',estado:'PUBLICADO',etiquetas:[],descripcionHtml:'<p>Detalle</p>'},soluciones:[solution,{...solution,id:10,titulo:'Eliminada',activo:false}]}
  const remove=vi.fn()
  const {rerender}=render(<ProblemDetail detail={detail} onEditSolution={vi.fn()} onRemoveSolution={remove}/> )
  expect(screen.queryByText('Eliminada')).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('button',{name:'Eliminar solución'}))
  expect(remove).toHaveBeenCalledWith(solution)
  rerender(<ProblemDetail detail={detail}/> )
  expect(screen.queryByRole('button',{name:'Eliminar solución'})).not.toBeInTheDocument()
})
