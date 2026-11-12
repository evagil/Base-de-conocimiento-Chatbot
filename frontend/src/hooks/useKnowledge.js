import { useCallback, useEffect, useState } from 'react'
import { knowledgeApi } from '../api/knowledgeApi.js'

export function useKnowledge() {
  const [tree, setTree] = useState([])
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadTree = useCallback(async () => {
    setLoading(true); setError('')
    try { setTree(await knowledgeApi.tree()) } catch (e) { setError(e.message) } finally { setLoading(false) }
  }, [])

  const selectProblem = useCallback(async id => {
    setLoading(true); setError('')
    try { setProblem(await knowledgeApi.problem(id)) } catch (e) { setError(e.message) } finally { setLoading(false) }
  }, [])

  useEffect(() => { loadTree() }, [loadTree])

  return { tree, problem, loading, error, setError, loadTree, selectProblem, setProblem }
}
