import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { knowledgeApi } from '../api/knowledgeApi.js'
import ProtectedImage from './ProtectedImage.jsx'

function ImageEditor({ image, onSaved, onChanged }) {
  const [name, setName] = useState(image.textoAlternativo)
  const [busy, setBusy] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const save = async remove => {
    if (busy) return
    setError(''); setNotice('')
    if (!remove && !name.trim()) { setError('Escribí un nombre para la imagen.'); return }
    setBusy(true)
    try {
      const saved = remove
        ? await knowledgeApi.removeImage(image.id, image.version)
        : await knowledgeApi.updateImage(image.id, name.trim(), image.orden, image.version)
      onSaved(saved)
      if (!remove) { setName(saved.textoAlternativo); setNotice('Nombre guardado.') }
      onChanged?.()
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 2 }}>
    <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 2, alignItems: 'center' }}>
      <Box sx={{ width: 160, flexShrink: 0 }}><ProtectedImage image={image}/></Box>
      <TextField fullWidth label="Nombre de la imagen" value={name} disabled={busy} onChange={e => { setName(e.target.value); setNotice('') }} inputProps={{ maxLength: 300 }} helperText="También se utiliza como descripción accesible."/>
      <Button disabled={busy || name.trim() === image.textoAlternativo} onClick={() => save(false)}>Guardar nombre</Button>
      <Button disabled={busy} onClick={() => setConfirming(true)}>Eliminar imagen</Button>
    </Stack>
    {confirming && <Alert severity="warning" sx={{ mt: 1 }}>¿Eliminar esta imagen del problema?
      <Button disabled={busy} onClick={() => save(true)}>Confirmar eliminación</Button>
      <Button disabled={busy} onClick={() => setConfirming(false)}>Conservar</Button>
    </Alert>}
    {error && <Alert severity="error" sx={{ mt: 1 }}>{error}</Alert>}
    {notice && <Alert severity="success" sx={{ mt: 1 }}>{notice}</Alert>}
  </Box>
}

export default function ExistingImagesEditor({ owner, id, onChanged }) {
  const [images, setImages] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [attempt, setAttempt] = useState(0)
  useEffect(() => {
    let active = true
    setLoading(true); setError(''); setImages([])
    knowledgeApi.images(owner, id).then(data => { if (active) setImages(data) })
      .catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [owner, id, attempt])
  return <Stack sx={{ gap: 1 }}>
    <Typography variant="subtitle2">Imágenes cargadas</Typography>
    <Typography variant="caption">Guardar nombre y eliminar imagen se aplican al momento, independientemente de los cambios del problema.</Typography>
    {loading && <Typography>Cargando imágenes…</Typography>}
    {error && <Alert severity="error">{error}<Button onClick={() => setAttempt(value => value + 1)}>Reintentar</Button></Alert>}
    {!loading && !error && !images.some(image => image.activo) && <Typography variant="body2">No hay imágenes cargadas.</Typography>}
    {images.filter(image => image.activo).map(image => <ImageEditor key={image.id} image={image} onChanged={onChanged} onSaved={saved => setImages(current => current.map(item => item.id === saved.id ? saved : item))}/>)}
  </Stack>
}
