import { Box, Button, Stack, TextField } from '@mui/material'
import { AddPhotoAlternate } from '@mui/icons-material'

export const asImages = files => Array.from(files).map((file,index) => ({ file, alt: file.name.replace(/\.[^.]+$/, '') || `Imagen pegada ${index + 1}` }))

export default function ImageFields({ images, onChange, label }) {
  const add = files => onChange([...images, ...asImages(files)])
  const updateAlt = (index, alt) => onChange(images.map((image, position) => position === index ? { ...image, alt } : image))
  const remove = index => onChange(images.filter((_, position) => position !== index))
  return <Box><Button component="label" variant="outlined" startIcon={<AddPhotoAlternate/>}>{label}<input hidden multiple type="file" accept="image/png,image/jpeg,image/webp" onChange={e => add(e.target.files || [])}/></Button>
    {images.map((image, index) => <Stack key={`${image.file.name}-${index}`} direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 1, mt: 1 }}><TextField fullWidth size="small" label={`Descripción de ${image.file.name}`} value={image.alt} onChange={e => updateAlt(index, e.target.value)} helperText="Este texto explica qué muestra la captura."/><Button color="error" onClick={() => remove(index)}>Quitar</Button></Stack>)}
  </Box>
}
