import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
vi.mock('../api/knowledgeApi.js',()=>({knowledgeApi:{tree:vi.fn(),problem:vi.fn()}}))
import { knowledgeApi } from '../api/knowledgeApi.js'
import { useKnowledge } from './useKnowledge.js'

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
})
