import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
vi.mock('../api/knowledgeApi.js',()=>({knowledgeApi:{tree:vi.fn(),problem:vi.fn()}}))
import { knowledgeApi } from '../api/knowledgeApi.js'
import { useKnowledge } from './useKnowledge.js'
afterEach(cleanup)

describe('useKnowledge',()=>{
  beforeEach(()=>vi.clearAllMocks())
  it('loads, selects and exposes setters',async()=>{
    knowledgeApi.tree.mockResolvedValue([{id:1}]);knowledgeApi.problem.mockResolvedValue({problema:{id:9}})
    const {result}=renderHook(()=>useKnowledge());await waitFor(()=>expect(result.current.loading).toBe(false));expect(result.current.tree).toEqual([{id:1}])
    await act(()=>result.current.selectProblem(9));expect(result.current.problem.problema.id).toBe(9)
    act(()=>{result.current.setError('manual');result.current.setProblem(null)});expect(result.current.error).toBe('manual');expect(result.current.problem).toBeNull()
  })
  it('reports loading errors for both operations',async()=>{
    knowledgeApi.tree.mockRejectedValue(new Error('tree failed'));knowledgeApi.problem.mockRejectedValue(new Error('problem failed'))
    const {result}=renderHook(()=>useKnowledge());await waitFor(()=>expect(result.current.error).toBe('tree failed'))
    await act(()=>result.current.selectProblem(2));expect(result.current.error).toBe('problem failed');expect(result.current.loading).toBe(false)
    knowledgeApi.tree.mockResolvedValue([]);await act(()=>result.current.loadTree());expect(result.current.error).toBe('')
  })
  it('keeps the latest selected problem when older responses or failures arrive late',async()=>{
    for (const failure of [false,true]) {
      knowledgeApi.tree.mockResolvedValue([])
      let finishOld,finishNew
      knowledgeApi.problem.mockImplementation(id=>new Promise((resolve,reject)=>{
        if(id===1)finishOld=()=>failure?reject(new Error('Respuesta antigua')):resolve({problema:{id:1}})
        else finishNew=()=>resolve({problema:{id:2}})
      }))
      const {result,unmount}=renderHook(useKnowledge)
      await waitFor(()=>expect(result.current.loading).toBe(false))
      let oldRequest,newRequest
      act(()=>{oldRequest=result.current.selectProblem(1);newRequest=result.current.selectProblem(2)})
      await act(async()=>{finishNew();await newRequest})
      expect(result.current.problem.problema.id).toBe(2)
      await act(async()=>{finishOld();await oldRequest})
      expect(result.current.problem.problema.id).toBe(2);expect(result.current.error).toBe('');expect(result.current.loading).toBe(false)
      unmount()
    }
  })
  it('ignores an old tree refresh and pending responses after unmount',async()=>{
    let failOld,finishNew,finishProblem
    knowledgeApi.tree.mockImplementationOnce(()=>new Promise((_,reject)=>{failOld=reject})).mockImplementationOnce(()=>new Promise(resolve=>{finishNew=resolve}))
    knowledgeApi.problem.mockImplementation(()=>new Promise(resolve=>{finishProblem=resolve}))
    const {result,unmount}=renderHook(useKnowledge)
    let refresh,selection
    act(()=>{refresh=result.current.loadTree()})
    await act(async()=>{finishNew([{id:2}]);await refresh})
    await act(async()=>{failOld(new Error('Árbol anterior'))})
    expect(result.current.tree).toEqual([{id:2}]);expect(result.current.error).toBe('')
    act(()=>{selection=result.current.selectProblem(9)})
    unmount()
    await act(async()=>{finishProblem({problema:{id:9}});await selection})
  })
})
