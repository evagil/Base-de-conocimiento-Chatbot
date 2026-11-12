import { useEffect, useState } from 'react'
import { Alert, Box, Button, Stack, TextField, Typography } from '@mui/material'
import { knowledgeApi } from '../api/knowledgeApi.js'

function AttachmentRow({ attachment, canEdit, onSaved }) {
  const [name, setName] = useState(attachment.textoAlternativo)
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const act = async action => {
    setBusy(true); setError('')
    try { await action() } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  const download = () => act(async () => {
    const blob = await knowledgeApi.attachmentContent(attachment.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    const extension = attachment.tipoMime === 'application/pdf' ? '.pdf' : '.docx'
    const filename = Array.from(attachment.textoAlternativo, char => char.charCodeAt(0) < 32 ? '_' : char).join('').replace(/[\\/:*?"<>|]/g, '_')
    link.href = url; link.download = filename.toLowerCase().endsWith(extension) ? filename : filename + extension
    document.body.appendChild(link); link.click(); link.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  })
  const save = () => act(async () => {
    if (!name.trim()) throw new Error('Escribí un nombre para el adjunto.')
    const updated = await knowledgeApi.updateAttachment(attachment.id, name.trim(), attachment.orden, attachment.version)
    onSaved(updated); setEditing(false)
  })
  const remove = () => {
    if (window.confirm(`¿Eliminar el adjunto «${attachment.textoAlternativo}»?`)) {
      act(async () => onSaved(await knowledgeApi.removeAttachment(attachment.id, attachment.version)))
    }
  }
  return <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
    <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 1, alignItems: 'center' }}>
      <Box sx={{ flex: 1, minWidth: 0, overflowWrap: 'anywhere' }}>
        <Typography>{attachment.textoAlternativo}</Typography>
        <Typography variant="caption">{attachment.tipoMime === 'application/pdf' ? 'PDF' : 'Word'} · {(attachment.tamanioBytes / 1024).toFixed(1)} KB</Typography>
      </Box>
      <Button disabled={busy} onClick={download}>Descargar</Button>
      {canEdit && <><Button disabled={busy} onClick={() => { setName(attachment.textoAlternativo); setEditing(true) }}>Cambiar nombre</Button><Button disabled={busy} onClick={remove}>Eliminar adjunto</Button></>}
    </Stack>
    {editing && <Stack direction="row" sx={{ gap: 1, mt: 1 }}>
      <TextField label="Nombre del adjunto" fullWidth size="small" value={name} disabled={busy} onChange={e => setName(e.target.value)} slotProps={{ htmlInput: { maxLength: 300 } }}/>
      <Button disabled={busy} onClick={save}>Guardar nombre</Button><Button disabled={busy} onClick={() => setEditing(false)}>Cancelar</Button>
    </Stack>}
    {error && <Alert severity="error">{error}</Alert>}
  </Box>
}

export default function AttachmentPanel({ owner, id, canEdit = false, revision = 0 }) {
  const [attachments, setAttachments] = useState([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState('')
  const [actionError, setActionError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [file, setFile] = useState(null)
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => {
    let active = true
    setLoading(true); setListError('')
    knowledgeApi.attachments(owner, id).then(data => { if (active) setAttachments(data) })
      .catch(e => { if (active) setListError(e.message) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [owner, id, attempt, revision])
  const choose = event => {
    const selected = event.target.files?.[0]
    event.target.value = ''
    if (!selected) return
    setActionError('')
    if (!/\.(pdf|docx)$/i.test(selected.name)) { setActionError('Seleccioná un PDF o Word (.docx).'); return }
    if (selected.size > 10 * 1024 * 1024) { setActionError('El archivo supera los 10 MB.'); return }
    setFile(selected); setName(selected.name)
  }
  const upload = async () => {
    if (!file || busy) return
    if (!name.trim()) { setActionError('Escribí un nombre para el adjunto.'); return }
    setBusy(true); setActionError('')
    try {
      const saved = await knowledgeApi.uploadAttachment(owner, id, file, name.trim())
      setAttachments(current => [...current, saved]); setFile(null); setName('')
    } catch (e) { setActionError(e.message) } finally { setBusy(false) }
  }
  return <Stack sx={{ gap: 1, mt: 2 }} aria-label="Archivos adjuntos">
    {loading && <Typography variant="body2">Cargando adjuntos…</Typography>}
    {listError && <Alert severity="warning">No se pudo actualizar la lista de adjuntos: {listError}{attachments.length > 0 && ' Los archivos que ves corresponden a la última carga correcta.'}<Button onClick={() => setAttempt(value => value + 1)}>Reintentar carga de lista</Button></Alert>}
    {actionError && <Alert severity="error">{actionError}</Alert>}
    {attachments.filter(a => a.activo).map(attachment => <AttachmentRow key={attachment.id} attachment={attachment} canEdit={canEdit} onSaved={saved => setAttachments(current => current.map(item => item.id === saved.id ? saved : item))}/>)}
    {canEdit && <Box>
      <Button component="label" disabled={busy || loading}>Adjuntar PDF o Word<input aria-label="Seleccionar documento" hidden type="file" accept=".pdf,.docx" onChange={choose}/></Button>
      <Typography variant="caption" sx={{ display: 'block' }}>PDF o DOCX, hasta 10 MB. Se adjunta al subirlo.</Typography>
      {file && <Stack sx={{ gap: 1, mt: 1 }}><Typography variant="caption">{file.name}</Typography><TextField label="Nombre del archivo" value={name} disabled={busy} onChange={e => setName(e.target.value)} slotProps={{ htmlInput: { maxLength: 300 } }}/><Stack direction="row"><Button disabled={busy} onClick={upload}>{busy ? 'Subiendo…' : 'Subir archivo'}</Button><Button disabled={busy} onClick={() => setFile(null)}>Cancelar</Button></Stack></Stack>}
    </Box>}
  </Stack>
}
