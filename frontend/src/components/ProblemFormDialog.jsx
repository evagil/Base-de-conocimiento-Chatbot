import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, Divider, FormControl, InputLabel, MenuItem, Paper, Select, Stack, TextField, Typography } from '@mui/material'
import ExistingImagesEditor from './ExistingImagesEditor.jsx'
import SolutionTitleEditor from './SolutionTitleEditor.jsx'
import RichTextEditor from './RichTextEditor.jsx'
import ImageFields, { asImages } from './ImageFields.jsx'
import AttachmentFields, { invalidAttachment } from './AttachmentFields.jsx'
import { knowledgeApi } from '../api/knowledgeApi.js'

const initial = { subareaId: '', titulo: '', descripcionHtml: '<p></p>', estado: 'PUBLICADO', etiquetas: [] }
const initialSolution = { titulo: '', descripcionHtml: '<p></p>' }
const hasText = html => html.replace(/<[^>]*>/g, '').trim().length > 0
export default function ProblemFormDialog({ open, onClose, tree, detail, onSaved, onImagesChanged }) {
  const editing = detail?.problema
  const [form, setForm] = useState(initial); const [solution, setSolution] = useState(initialSolution)
  const [problemImages, setProblemImages] = useState([]); const [solutionImages, setSolutionImages] = useState([])
  const [problemAttachments, setProblemAttachments] = useState([]); const [solutionAttachments, setSolutionAttachments] = useState([])
  const [tagText, setTagText] = useState(''); const [error, setError] = useState(''); const [saving, setSaving] = useState(false)
  useEffect(() => { if (open) { setForm(editing ? { subareaId: editing.subareaId, titulo: editing.titulo, descripcionHtml: editing.descripcionHtml, estado: editing.estado, etiquetas: editing.etiquetas } : initial); setSolution(initialSolution); setProblemImages([]); setSolutionImages([]); setProblemAttachments([]); setSolutionAttachments([]); setTagText(''); setError('') } }, [open, editing])
  const subareas = useMemo(() => tree.flatMap(area => area.subareas.map(sub => ({ ...sub, areaName: area.nombre }))), [tree])
  const set = (key, value) => setForm(current => ({ ...current, [key]: value }))
  const addTag = () => { const tag = tagText.trim(); if (tag && !form.etiquetas.includes(tag)) set('etiquetas', [...form.etiquetas, tag]); setTagText('') }
  const uploadAll = async (owner, id, images) => { for (const [order, image] of images.entries()) await knowledgeApi.uploadImage(owner, id, image.file, image.alt.trim(), order) }
  const uploadAttachments = async (owner, id, attachments) => { for (const attachment of attachments) await knowledgeApi.uploadAttachment(owner, id, attachment.file, attachment.name.trim()) }
  const pasteProblemImages = files => setProblemImages(current => [...current, ...asImages(files)])
  const pasteSolutionImages = files => setSolutionImages(current => [...current, ...asImages(files)])
  const submit = async () => {
    setError(''); if (!form.subareaId || !form.titulo.trim() || !hasText(form.descripcionHtml)) { setError('Completá la subárea, el título y la descripción del problema.'); return }
    if ([...problemImages, ...solutionImages].some(image => !image.alt.trim())) { setError('Todas las imágenes necesitan una descripción.'); return }
    const attachmentError = [...problemAttachments, ...solutionAttachments].map(invalidAttachment).find(Boolean); if (attachmentError) { setError(attachmentError); return }
    if (!editing && solution.titulo.length > 200) { setError('Acortá el título de la solución: admite hasta 200 caracteres incluyendo el formato.'); return }
    setSaving(true)
    try {
      const body = { ...form, subareaId: Number(form.subareaId), motivoCambio: editing ? 'Edición desde la interfaz' : null }
      const saved = editing ? await knowledgeApi.updateProblem(editing.id, body, editing.version) : await knowledgeApi.createProblem(body)
      await uploadAll('problemas', saved.id, problemImages)
      await uploadAttachments('problemas', saved.id, problemAttachments)
      const wantsSolution = !editing && (solution.titulo.trim() || hasText(solution.descripcionHtml) || solutionImages.length || solutionAttachments.length)
      if (wantsSolution) { if (!hasText(solution.descripcionHtml)) throw new Error('Describí la solución antes de guardar sus imágenes.'); const created = await knowledgeApi.createSolution(saved.id, { titulo: solution.titulo.trim() || null, descripcionHtml: solution.descripcionHtml, principal: true, orden: 0, motivoCambio: null }); await uploadAll('soluciones', created.id, solutionImages); await uploadAttachments('soluciones', created.id, solutionAttachments) }
      await onSaved(saved.id); onClose()
    } catch (e) { setError(e.message) } finally { setSaving(false) }
  }
  return <Dialog open={open} onClose={onClose} fullWidth maxWidth="lg"><DialogTitle>{editing ? 'Editar problema' : 'Nuevo problema'}</DialogTitle><DialogContent><Stack sx={{ gap: 2, pt: 1 }}>{error && <Alert severity="error">{error}</Alert>}
    <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 } }}><Typography variant="h6">1. Descripción del problema</Typography><Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>Explicá el error y adjuntá las capturas que ayudan a reconocerlo.</Typography><Stack sx={{ gap: 2 }}>
      <FormControl fullWidth><InputLabel>Subárea</InputLabel><Select value={form.subareaId} label="Subárea" onChange={e => set('subareaId', e.target.value)}>{subareas.map(s => <MenuItem key={s.id} value={s.id}>{s.areaName} / {s.nombre}</MenuItem>)}</Select></FormControl>
      <TextField label="Título del problema" value={form.titulo} onChange={e => set('titulo', e.target.value)} inputProps={{ maxLength: 200 }} required/><FormControl fullWidth><InputLabel>Estado</InputLabel><Select value={form.estado} label="Estado" onChange={e => set('estado', e.target.value)}>{['BORRADOR', 'PUBLICADO', 'ARCHIVADO'].map(v => <MenuItem key={v} value={v}>{v.replace('_', ' ')}</MenuItem>)}</Select></FormControl>
      <RichTextEditor label="Descripción detallada del problema" value={form.descripcionHtml} onChange={v => set('descripcionHtml', v)} onPasteImages={pasteProblemImages}/><Typography variant="caption" color="text.secondary">También podés copiar una imagen y pegarla aquí con Ctrl + V.</Typography>{open && editing && <ExistingImagesEditor owner="problemas" id={editing.id} onChanged={onImagesChanged}/>}<ImageFields images={problemImages} onChange={setProblemImages} label="Agregar imágenes del problema"/><AttachmentFields attachments={problemAttachments} onChange={setProblemAttachments} label="Agregar PDF o Word del problema"/>
      <Divider/><Typography variant="subtitle2">Etiquetas (opcional)</Typography><Typography variant="caption" color="text.secondary">Palabras clave para filtrar, por ejemplo: AFIP, red, impresión o error 500.</Typography><Stack direction="row" sx={{ gap: 1 }}><TextField fullWidth label="Nueva etiqueta" value={tagText} onChange={e => setTagText(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addTag() } }}/><Button onClick={addTag}>Agregar</Button></Stack><Stack direction="row" sx={{ gap: 1, flexWrap: 'wrap' }}>{form.etiquetas.map(t => <Chip key={t} label={t} onDelete={() => set('etiquetas', form.etiquetas.filter(x => x !== t))}/>)}</Stack>
    </Stack></Paper>
    {!editing && <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 }, bgcolor: 'success.50' }}><Typography variant="h6">2. Solución inicial (opcional)</Typography><Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>Podés documentarla ahora o guardar el problema y agregar varias soluciones después.</Typography><Stack sx={{ gap: 2 }}><SolutionTitleEditor label="Título de la solución" value={solution.titulo} onChange={titulo => setSolution(current => ({ ...current, titulo }))}/><RichTextEditor label="Descripción de la solución" value={solution.descripcionHtml} onChange={descripcionHtml => setSolution(current => ({ ...current, descripcionHtml }))} onPasteImages={pasteSolutionImages}/><Typography variant="caption" color="text.secondary">Las imágenes pegadas con Ctrl + V se adjuntan a esta solución.</Typography><ImageFields images={solutionImages} onChange={setSolutionImages} label="Agregar imágenes de la solución"/><AttachmentFields attachments={solutionAttachments} onChange={setSolutionAttachments} label="Agregar PDF o Word de la solución"/></Stack></Paper>}
  </Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancelar</Button><Button variant="contained" onClick={submit} disabled={saving}>{saving ? 'Guardando problema, solución e imágenes…' : 'Guardar'}</Button></DialogActions></Dialog>
}
