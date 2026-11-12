import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, InputLabel, MenuItem, Select, Stack, TextField } from '@mui/material'
import { DeleteOutlined } from '@mui/icons-material'
import { knowledgeApi } from '../api/knowledgeApi.js'

const empty = { areaId: '', nombre: '', descripcion: '', orden: 0 }

export default function HierarchyFormDialog({ open, mode, tree, entity = null, onClose, onSaved }) {
  const [form, setForm] = useState(empty)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const isSubarea = mode === 'subarea'
  const defaultAreaId = useMemo(() => tree[0]?.id || '', [tree])

  useEffect(() => {
    if (open) { setForm(entity ? { areaId: entity.areaId ?? '', nombre: entity.nombre, descripcion: entity.descripcion ?? '', orden: entity.orden } : { ...empty, areaId: isSubarea ? defaultAreaId : '' }); setError('') }
  }, [open, isSubarea, defaultAreaId, entity])

  const set = (key, value) => setForm(current => ({ ...current, [key]: value }))
  const submit = async () => {
    setError('')
    if (!form.nombre.trim() || (isSubarea && !form.areaId)) { setError(isSubarea ? 'Seleccioná el área y escribí el nombre.' : 'Escribí el nombre del área.'); return }
    setSaving(true)
    try {
      const body = { nombre: form.nombre.trim(), descripcion: form.descripcion.trim() || null, orden: Number(form.orden) || 0 }
      if (entity) await (isSubarea ? knowledgeApi.updateSubarea : knowledgeApi.updateArea)(entity.id, body, entity.version)
      else if (isSubarea) await knowledgeApi.createSubarea(Number(form.areaId), body)
      else await knowledgeApi.createArea(body)
      await onSaved(); onClose()
    } catch (e) { setError(e.message) } finally { setSaving(false) }
  }

  const remove = async () => {
    setError('')
    const label = isSubarea ? 'subárea' : 'área'
    if (!window.confirm(`¿Eliminar la ${label} «${entity.nombre}»? Se ocultará del árbol y se conservará el historial.`)) return
    setSaving(true)
    try {
      await (isSubarea ? knowledgeApi.removeSubarea : knowledgeApi.removeArea)(entity.id, entity.version)
      await onSaved(); onClose()
    } catch (e) { setError(e.message) } finally { setSaving(false) }
  }

  return <Dialog open={open} onClose={saving ? undefined : onClose} fullWidth maxWidth="sm">
    <DialogTitle>{entity ? (isSubarea ? 'Editar subárea' : 'Editar área') : (isSubarea ? 'Nueva subárea' : 'Nueva área')}</DialogTitle>
    <DialogContent><Stack sx={{ gap: 2, pt: 1 }}>
      {error && <Alert severity="error">{error}</Alert>}
      {isSubarea && <FormControl fullWidth><InputLabel>Área</InputLabel><Select disabled={Boolean(entity)} value={form.areaId} label="Área" onChange={e => set('areaId', e.target.value)}>{tree.map(area => <MenuItem key={area.id} value={area.id}>{area.nombre}</MenuItem>)}</Select></FormControl>}
      <TextField label="Nombre" value={form.nombre} onChange={e => set('nombre', e.target.value)} required inputProps={{ maxLength: 100 }}/>
      <TextField label="Descripción opcional" value={form.descripcion} onChange={e => set('descripcion', e.target.value)} multiline minRows={2} inputProps={{ maxLength: 500 }}/>
      <TextField label="Orden" type="number" value={form.orden} onChange={e => set('orden', e.target.value)} inputProps={{ min: 0 }}/>
    </Stack></DialogContent>
    <DialogActions sx={{ justifyContent: entity ? 'space-between' : 'flex-end' }}>
      {entity && <Button color="error" disabled={saving} startIcon={<DeleteOutlined/>} onClick={remove}>Eliminar</Button>}
      <span><Button disabled={saving} onClick={onClose}>Cancelar</Button><Button variant="contained" disabled={saving} onClick={submit}>{saving ? 'Guardando…' : 'Guardar'}</Button></span>
    </DialogActions>
  </Dialog>
}
