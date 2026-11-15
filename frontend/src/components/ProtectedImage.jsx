import { useEffect, useState } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import { knowledgeApi } from '../api/knowledgeApi.js'

let turnoImagen = Promise.resolve()
const cargar = (id, vigente) => {
  const resultado = turnoImagen.catch(() => {}).then(() => vigente() ? knowledgeApi.imageContent(id) : null)
  turnoImagen = resultado.then(() => undefined, () => undefined)
  return resultado
}

export default function ProtectedImage({ image, large = false }) {
  const [src, setSrc] = useState('')
  const [error, setError] = useState(false)
  const [intento, setIntento] = useState(0)
  useEffect(() => {
    let url, active = true
    setSrc(''); setError(false)
    cargar(image.id, () => active).then(blob => {
      if (active && blob) { url = URL.createObjectURL(blob); setSrc(url) }
    }).catch(() => { if (active) setError(true) })
    return () => { active = false; if (url) URL.revokeObjectURL(url) }
  }, [image.id, image.version, intento])
  if (!src) return <Box role="status" sx={{ height: large ? 300 : 130, bgcolor: 'grey.100', display: 'grid', placeItems: 'center', p: 1, textAlign: 'center' }}>
    {error ? 'Imagen oculta: no se pudo preparar la vista protegida.' : 'Preparando vista protegida…'}
    {error && large && <Button onClick={() => setIntento(value => value + 1)}>Reintentar imagen protegida</Button>}
  </Box>
  return <Box component="img" src={src} alt={image.textoAlternativo} loading="lazy" sx={large
    ? { display: 'block', maxWidth: '100%', maxHeight: '75vh', width: 'auto', height: 'auto', objectFit: 'contain', mx: 'auto' }
    : { display: 'block', width: '100%', height: 130, objectFit: 'contain' }}/>
}

