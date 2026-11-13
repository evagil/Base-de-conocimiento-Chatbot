import { useEffect, useRef, useState } from 'react'
import Box from '@mui/material/Box'
import Drawer from '@mui/material/Drawer'

const minimum = 260
const defaultWidth = 350
const storageKey = 'bc_sidebar_width'
const maximum = () => Math.max(minimum, Math.min(720, window.innerWidth - 640))
const clamp = width => Math.min(maximum(), Math.max(minimum, width))

export default function ResizableSidebar({ children }) {
  const [width, setWidth] = useState(() => {
    try {
      const saved = Number(localStorage.getItem(storageKey))
      return clamp(saved > 0 && Number.isFinite(saved) ? saved : defaultWidth)
    } catch { return clamp(defaultWidth) }
  })
  const drag = useRef(null)
  useEffect(() => {
    const resize = () => setWidth(current => clamp(current))
    window.addEventListener('resize', resize)
    return () => window.removeEventListener('resize', resize)
  }, [])
  useEffect(() => {
    try { localStorage.setItem(storageKey, String(width)) } catch { /* Storage may be unavailable. */ }
  }, [width])
  const start = event => {
    if (event.button !== 0) return
    event.preventDefault()
    event.currentTarget.focus()
    drag.current = { x: event.clientX, width }
    event.currentTarget.setPointerCapture(event.pointerId)
  }
  const move = event => {
    if (drag.current) setWidth(clamp(drag.current.width + event.clientX - drag.current.x))
  }
  const end = () => { drag.current = null }
  const keyDown = event => {
    const values = { ArrowLeft: width - 20, ArrowRight: width + 20, Home: minimum, End: maximum() }
    if (event.key in values) {
      event.preventDefault()
      setWidth(clamp(values[event.key]))
    }
  }
  return <Drawer variant="permanent" sx={{
     width, flexShrink: 0,
    '& .MuiDrawer-paper': { width, pt: 8, overflow: 'hidden', borderRight: '1px solid', borderColor: 'divider' },
  }}>
    {children}
    <Box role="separator" tabIndex={0} aria-label="Ancho de la barra lateral"
      aria-orientation="vertical" aria-valuemin={minimum} aria-valuemax={maximum()} aria-valuenow={width}
      title="Arrastrá para ajustar el ancho. Doble clic para restablecer."
      onPointerDown={start} onPointerMove={move} onPointerUp={end}
      onPointerCancel={end} onLostPointerCapture={end} onKeyDown={keyDown}
      onDoubleClick={() => setWidth(clamp(defaultWidth))}
      sx={{
        position: 'absolute', right: 0, top: 64, bottom: 0, width: 7,
        cursor: 'col-resize', touchAction: 'none', userSelect: 'none',
        '&::after': { content: '""', position: 'absolute', right: 2, top: '45%', height: 40, width: 3, borderRadius: 2, bgcolor: 'divider' },
        '&:hover, &:focus-visible': { bgcolor: 'action.selected', outline: 'none', '&::after': { bgcolor: 'primary.main' } },
      }}/>
  </Drawer>
}
