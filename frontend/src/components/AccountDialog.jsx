import { useEffect,useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Divider from '@mui/material/Divider'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useAuth } from '../auth/AuthContext.jsx'

export default function AccountDialog({open,onClose,required=false}){
  const auth=useAuth()
  const [nombre,setNombre]=useState('')
  const [email,setEmail]=useState('')
  const [passwordActual,setPasswordActual]=useState('')
  const [passwordNueva,setPasswordNueva]=useState('')
  const [confirmacion,setConfirmacion]=useState('')
  const [error,setError]=useState('')
  const [ok,setOk]=useState('')
  const [saving,setSaving]=useState(false)

  useEffect(()=>{
    if(open){
      setNombre(auth.user?.nombre||'')
      setEmail(auth.user?.email||'')
      setPasswordActual('')
      setPasswordNueva('')
      setConfirmacion('')
      setError('')
      setOk('')
    }
  },[open,auth.user?.nombre,auth.user?.email])

  const close=()=>{if(!required)onClose()}

  const saveProfile=async()=>{
    setError('')
    setOk('')
    setSaving(true)
    try{
      await auth.updateProfile({nombre,email})
      setOk('Datos de la cuenta actualizados.')
    }catch(err){setError(err.message)}finally{setSaving(false)}
  }

  const savePassword=async e=>{
    e.preventDefault()
    setError('')
    setOk('')
    if(passwordNueva!==confirmacion){setError('Las contraseñas nuevas no coinciden.');return}
    setSaving(true)
    try{
      await auth.changePassword({passwordActual,passwordNueva})
      setPasswordActual('')
      setPasswordNueva('')
      setConfirmacion('')
      setOk('Contraseña actualizada correctamente.')
      if(required)onClose()
    }catch(err){setError(err.message)}finally{setSaving(false)}
  }

  return <Dialog open={open} onClose={close} fullWidth maxWidth="xs" disableEscapeKeyDown={required}>
    <DialogTitle>{required?'Cambiar contraseña':'Mi cuenta'}</DialogTitle>
    <DialogContent>
      <Stack sx={{gap:2,pt:1}}>
        {required&&<Alert severity="warning">Ingresaste con una contraseña temporal. Para continuar, cambiala por una contraseña definitiva.</Alert>}
        {error&&<Alert severity="error">{error}</Alert>}
        {ok&&<Alert severity="success">{ok}</Alert>}
        {!required&&<>
          <TextField label="Nombre y apellido" value={nombre} onChange={e=>setNombre(e.target.value)} required/>
          <TextField label="Correo" type="email" value={email} onChange={e=>setEmail(e.target.value)} required/>
          <Button variant="outlined" onClick={saveProfile} disabled={saving}>Guardar datos</Button>
          <Divider/>
          <Typography variant="subtitle2">Cambiar contraseña</Typography>
        </>}
        <Stack component="form" id="account-password-form" onSubmit={savePassword} sx={{gap:2}}>
          <TextField label="Contraseña actual" type="password" value={passwordActual} onChange={e=>setPasswordActual(e.target.value)} required autoComplete="current-password" helperText={required?'Es la contraseña que te dio el administrador.':''}/>
          <TextField label="Nueva contraseña" type="password" value={passwordNueva} onChange={e=>setPasswordNueva(e.target.value)} required autoComplete="new-password"/>
          <TextField label="Repetir nueva contraseña" type="password" value={confirmacion} onChange={e=>setConfirmacion(e.target.value)} required autoComplete="new-password"/>
        </Stack>
      </Stack>
    </DialogContent>
    <DialogActions>
      {!required&&<Button onClick={close}>Cerrar</Button>}
      <Button type="submit" form="account-password-form" variant="contained" disabled={saving}>{saving?'Guardando…':'Cambiar contraseña'}</Button>
    </DialogActions>
  </Dialog>
}
