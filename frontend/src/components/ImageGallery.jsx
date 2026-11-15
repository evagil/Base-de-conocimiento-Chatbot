import { useEffect, useRef, useState } from 'react'
import Box from '@mui/material/Box'
import ButtonBase from '@mui/material/ButtonBase'
import Dialog from '@mui/material/Dialog'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import IconButton from '@mui/material/IconButton'
import Typography from '@mui/material/Typography'
import ChevronLeft from '@mui/icons-material/ChevronLeft'
import ChevronRight from '@mui/icons-material/ChevronRight'
import Close from '@mui/icons-material/Close'
import ProtectedImage from './ProtectedImage.jsx'
import { knowledgeApi } from '../api/knowledgeApi.js'


export default function ImageGallery({ owner, id, title = 'Imágenes', revision = 0 }) {
  const [images, setImages] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const touchStart = useRef(null)
  useEffect(() => {
    let active = true
    if (id) knowledgeApi.images(owner, id).then(data => active && setImages(data)).catch(() => active && setImages([]))
    return () => { active = false }
  }, [owner, id, revision])
  const visible = images.filter(image => image.activo)
  const selectedIndex = visible.findIndex(image => image.id === selectedId)
  const selected = visible[selectedIndex]
  const move = direction => {
    const next = visible[selectedIndex + direction]
    if (next) setSelectedId(next.id)
  }
  useEffect(() => {
    if (selectedId === null) return
    const onKeyDown = event => {
      if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
      const active = images.filter(image => image.activo)
      const index = active.findIndex(image => image.id === selectedId)
      const next = active[index + (event.key === 'ArrowRight' ? 1 : -1)]
      if (next) { event.preventDefault(); setSelectedId(next.id) }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [images, selectedId])
  const finishSwipe = event => {
    const start = touchStart.current
    touchStart.current = null
    const end = event.changedTouches[0]
    if (!start || !end) return
    const dx = end.clientX - start.x
    const dy = end.clientY - start.y
    if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy) * 1.3) move(dx < 0 ? 1 : -1)
  }
  if (!visible.length) return null
  return <Box sx={{ mt: 2 }}>
    <Typography variant="subtitle2" sx={{ mb: 1 }}>{title}</Typography>
    <Typography variant="caption" color="text.secondary">Vista con ocultación automática de datos sensibles. Revisá la imagen antes de compartirla.</Typography>
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
      {visible.map(img => <ButtonBase key={img.id} onClick={() => setSelectedId(img.id)}
        aria-label={`Ampliar imagen: ${img.textoAlternativo}`}
        sx={{ width: 200, maxWidth: '100%', height: 140, p: .5, cursor: 'zoom-in', border: '1px solid', borderColor: 'divider', borderRadius: 2, overflow: 'hidden', bgcolor: 'grey.50',
          '&:hover': { borderColor: 'primary.main' }, '&.Mui-focusVisible': { outline: '2px solid', outlineColor: 'primary.main' } }}>
        <ProtectedImage image={img}/>
      </ButtonBase>)}
    </Box>
    <Dialog open={Boolean(selected)} onClose={() => setSelectedId(null)} maxWidth="lg" aria-label="Imagen ampliada"
      slotProps={{ paper: { sx: { maxWidth: 'calc(100vw - 32px)', m: 2 } } }}>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 1 }}>
        <Typography component="span" sx={{ flex: 1, overflowWrap: 'anywhere' }}>{selected?.textoAlternativo}</Typography>
        {visible.length > 1 && <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>{selectedIndex + 1} de {visible.length}</Typography>}
        <IconButton aria-label="Cerrar imagen" onClick={() => setSelectedId(null)}><Close/></IconButton>
      </DialogTitle>
      <DialogContent sx={{ p: 2, minWidth: 0 }}
        onTouchStart={event => { const touch = event.touches[0]; touchStart.current = touch && { x: touch.clientX, y: touch.clientY } }}
        onTouchEnd={finishSwipe}>
        {selected && <Box sx={{ display: 'flex', alignItems: 'center', gap: { xs: 0, sm: 1 } }}>
          {visible.length > 1 && <IconButton aria-label="Imagen anterior" disabled={selectedIndex === 0} onClick={() => move(-1)}><ChevronLeft/></IconButton>}
          <Box sx={{ flex: 1, minWidth: 0 }}><ProtectedImage key={selected.id} image={selected} large/></Box>
          {visible.length > 1 && <IconButton aria-label="Imagen siguiente" disabled={selectedIndex === visible.length - 1} onClick={() => move(1)}><ChevronRight/></IconButton>}
        </Box>}
      </DialogContent>
    </Dialog>
  </Box>
}
