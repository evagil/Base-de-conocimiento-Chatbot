import { useCallback, useEffect, useRef, useState } from 'react'
import { knowledgeApi } from '../api/knowledgeApi.js'

export function useKnowledge() {
  const [tree, setTree] = useState([])
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const treeRequest = useRef(0)
  const problemRequest = useRef(0)
  useEffect(() => () => { treeRequest.current++; problemRequest.current++ }, [])

  const loadTree = useCallback(async () => {
    const request = ++treeRequest.current
    setLoading(true); setError('')
    try { const result = await knowledgeApi.tree(); if (request === treeRequest.current) setTree(result) }
    catch (e) { if (request === treeRequest.current) setError(e.message) }
    finally { if (request === treeRequest.current) setLoading(false) }
  }, [])

  const selectProblem = useCallback(async id => {
    const request = ++problemRequest.current
    setLoading(true); setError('')
    try { const result = await knowledgeApi.problem(id); if (request === problemRequest.current) setProblem(result) }
    catch (e) { if (request === problemRequest.current) setError(e.message) }
    finally { if (request === problemRequest.current) setLoading(false) }
  }, [])

  useEffect(() => { loadTree() }, [loadTree])

  return { tree, problem, loading, error, setError, loadTree, selectProblem, setProblem }
}
