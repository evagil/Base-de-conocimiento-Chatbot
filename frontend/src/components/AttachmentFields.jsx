import { Box, Button, Stack, TextField, Typography } from '@mui/material'
import { AttachFile } from '@mui/icons-material'

const MAX_SIZE = 10 * 1024 * 1024
export const asAttachments = files => Array.from(files).map(file => ({ file, name: file.name.replace(/\.[^.]+$/, '') || file.name }))
export const invalidAttachment = attachment => {
  if (!/\.(pdf|docx)$/i.test(attachment.file.name)) return 'Solo se permiten PDF o Word (.docx).'
  if (attachment.file.size > MAX_SIZE) return 'Hay un archivo que supera los 10 MB.'
  if (!attachment.name.trim()) return 'Todos los adjuntos necesitan un nombre.'
  return ''
}

export default function AttachmentFields({ attachments, onChange, label }) {
  const add = files => onChange([...attachments, ...asAttachments(files).filter(item => /\.(pdf|docx)$/i.test(item.file.name) && item.file.size <= MAX_SIZE)])
  const updateName = (index, name) => onChange(attachments.map((attachment, position) => position === index ? { ...attachment, name } : attachment))
  const remove = index => onChange(attachments.filter((_, position) => position !== index))
  return <Box>
    <Button component="label" variant="outlined" startIcon={<AttachFile/>}>{label}<input hidden multiple type="file" accept=".pdf,.docx" onChange={event => add(event.target.files || [])}/></Button>
    <Typography variant="caption" sx={{ display: 'block', mt: .5 }}>PDF o DOCX, hasta 10 MB. Se adjuntan al guardar.</Typography>
    {attachments.map((attachment, index) => <Stack key={`${attachment.file.name}-${index}`} direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 1, mt: 1 }}>
      <TextField fullWidth size="small" label={`Nombre de ${attachment.file.name}`} value={attachment.name} onChange={event => updateName(index, event.target.value)} helperText="Este nombre se mostrará para descargar el archivo."/>
      <Button color="error" onClick={() => remove(index)}>Quitar</Button>
    </Stack>)}
  </Box>
}
