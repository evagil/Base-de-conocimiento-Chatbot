import { useEffect, useState } from 'react'
import Box from '@mui/material/Box'
import { knowledgeApi } from '../api/knowledgeApi.js'

export default function ProtectedImage({ image, large = false }) {
  const [src, setSrc] = useState('')
  useEffect(() => {
    let url, active = true
    knowledgeApi.imageContent(image.id).then(blob => {
      if (active) { url = URL.createObjectURL(blob); setSrc(url) }
    }).catch(() => {})
    return () => { active = false; if (url) URL.revokeObjectURL(url) }
  }, [image.id])
  if (!src) return <Box sx={{ height: large ? 300 : 130, bgcolor: 'grey.100' }}/>
  return <Box component="img" src={src} alt={image.textoAlternativo} loading="lazy" sx={large
    ? { display: 'block', maxWidth: '100%', maxHeight: '75vh', width: 'auto', height: 'auto', objectFit: 'contain', mx: 'auto' }
    : { display: 'block', width: '100%', height: 130, objectFit: 'contain' }}/>
}

