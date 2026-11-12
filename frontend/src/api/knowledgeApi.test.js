import { beforeEach, describe, expect, it, vi } from 'vitest'
vi.mock('./httpClient.js',()=>({http:{get:vi.fn(),post:vi.fn(),put:vi.fn(),patch:vi.fn()}}))
import { http } from './httpClient.js'
import { knowledgeApi } from './knowledgeApi.js'

describe('knowledgeApi',()=>{
  beforeEach(()=>vi.clearAllMocks())
  it('assembles the navigation tree',async()=>{
    http.get.mockResolvedValueOnce({data:{content:[{id:1,nombre:'A'}]}}).mockResolvedValueOnce({data:[{id:2,nombre:'S'}]}).mockResolvedValueOnce({data:{content:[{id:3,titulo:'P'}]}})
    await expect(knowledgeApi.tree()).resolves.toEqual([{id:1,nombre:'A',subareas:[{id:2,nombre:'S',problems:[{id:3,titulo:'P'}]}]}])
    expect(http.get).toHaveBeenCalledTimes(3)
  })
  it('maps CRUD operations and queries',async()=>{
    http.get.mockResolvedValue({data:'get'});http.post.mockResolvedValue({data:'post'});http.put.mockResolvedValue({data:'put'});http.patch.mockResolvedValue({data:'patch'})
    await expect(knowledgeApi.problem(7)).resolves.toBe('get');await expect(knowledgeApi.tags()).resolves.toBe('get');await expect(knowledgeApi.images('problemas',7)).resolves.toBe('get');await expect(knowledgeApi.imageContent(7)).resolves.toBe('get')
    await expect(knowledgeApi.createProblem({})).resolves.toBe('post');await expect(knowledgeApi.updateProblem(7,{},2)).resolves.toBe('put');await expect(knowledgeApi.archiveProblem(7,2)).resolves.toBe('patch')
    await expect(knowledgeApi.createArea({})).resolves.toBe('post');await expect(knowledgeApi.createSubarea(1,{})).resolves.toBe('post')
    await expect(knowledgeApi.createSolution(7,{})).resolves.toBe('post');await expect(knowledgeApi.updateSolution(8,{},3)).resolves.toBe('put');expect(http.put).toHaveBeenLastCalledWith('/api/soluciones/8',{},{params:{version:3}});await expect(knowledgeApi.createStep(8,{})).resolves.toBe('post')
    await expect(knowledgeApi.updateArea(1,{nombre:'Rentas'},3)).resolves.toBe('put');expect(http.put).toHaveBeenLastCalledWith('/api/areas/1',{nombre:'Rentas'},{params:{version:3}})
    await expect(knowledgeApi.updateSubarea(2,{nombre:'Comercio'},4)).resolves.toBe('put');expect(http.put).toHaveBeenLastCalledWith('/api/subareas/2',{nombre:'Comercio'},{params:{version:4}})
    await expect(knowledgeApi.removeArea(1,3)).resolves.toBe('patch');expect(http.patch).toHaveBeenLastCalledWith('/api/areas/1/estado',{activo:false},{params:{version:3}})
    await expect(knowledgeApi.removeSubarea(2,4)).resolves.toBe('patch');expect(http.patch).toHaveBeenLastCalledWith('/api/subareas/2/estado',{activo:false},{params:{version:4}})
    await expect(knowledgeApi.updateImage(8,'Captura',3,2)).resolves.toBe('put');expect(http.put).toHaveBeenLastCalledWith('/api/archivos/8',null,{params:{version:2,textoAlternativo:'Captura',orden:3}})
    await expect(knowledgeApi.removeImage(8,3)).resolves.toBe('patch');expect(http.patch).toHaveBeenLastCalledWith('/api/archivos/8/activo',{activo:false},{params:{version:3}})
    await expect(knowledgeApi.removeProblem(7,2)).resolves.toBe('patch');expect(http.patch).toHaveBeenLastCalledWith('/api/problemas/7/activo',{activo:false},{params:{version:2}})
    await expect(knowledgeApi.removeSolution(8,3)).resolves.toBe('patch');expect(http.patch).toHaveBeenLastCalledWith('/api/soluciones/8/activo',{activo:false},{params:{version:3}})
    await expect(knowledgeApi.attachments('problemas',7)).resolves.toBe('get');expect(http.get).toHaveBeenLastCalledWith('/api/problemas/7/adjuntos')
    await expect(knowledgeApi.attachmentContent(8)).resolves.toBe('get')
    await expect(knowledgeApi.updateAttachment(8,'Manual',0,3)).resolves.toBe('put')
    await expect(knowledgeApi.removeAttachment(8,3)).resolves.toBe('patch')
    const document=new File(['pdf'],'manual.pdf',{type:'application/pdf'});await expect(knowledgeApi.uploadAttachment('soluciones',9,document,'Manual')).resolves.toBe('post');expect(http.post).toHaveBeenLastCalledWith('/api/soluciones/9/adjuntos',expect.any(FormData),{params:{nombre:'Manual'}})
    const file=new File(['x'],'x.png',{type:'image/png'});await expect(knowledgeApi.uploadImage('problemas',7,file,'captura',2)).resolves.toBe('post')
    expect(http.post.mock.calls.at(-1)[1]).toBeInstanceOf(FormData)
  })
})
