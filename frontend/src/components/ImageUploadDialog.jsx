import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { knowledgeApi } from '../api/knowledgeApi.js'

export default function ImageUploadDialog({open,onClose,owner,id,onSaved}){
  const [file,setFile]=useState(null);const [alt,setAlt]=useState('');const [error,setError]=useState('');const [preview,setPreview]=useState('')
  useEffect(()=>{if(!file){setPreview('');return}const url=URL.createObjectURL(file);setPreview(url);return()=>URL.revokeObjectURL(url)},[file])
  const submit=async()=>{if(!file||!alt.trim()){setError('Seleccioná una imagen y escribí su descripción.');return}try{await knowledgeApi.uploadImage(owner,id,file,alt);onSaved?.();onClose();setFile(null);setAlt('')}catch(e){setError(e.message)}}
  return <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm"><DialogTitle>Agregar imagen</DialogTitle><DialogContent><Stack gap={2} sx={{pt:1}}>{error&&<Alert severity="error">{error}</Alert>}<Button component="label" variant="outlined">Seleccionar PNG, JPEG o WebP<input hidden type="file" accept="image/png,image/jpeg,image/webp" onChange={e=>setFile(e.target.files?.[0]||null)}/></Button>{file&&<Typography variant="body2">{file.name} · {(file.size/1024).toFixed(1)} KB</Typography>}{preview&&<Box component="img" src={preview} alt="Vista previa" sx={{maxHeight:260,maxWidth:'100%',objectFit:'contain',borderRadius:2}}/>}<TextField label="Texto alternativo" value={alt} onChange={e=>setAlt(e.target.value)} helperText="Describí qué se ve en la captura." required/></Stack></DialogContent><DialogActions><Button onClick={onClose}>Cancelar</Button><Button variant="contained" onClick={submit}>Subir</Button></DialogActions></Dialog>
}
