import { useEffect,useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControl from '@mui/material/FormControl'
import FormControlLabel from '@mui/material/FormControlLabel'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { authApi } from '../api/authApi.js'
const roles=['ADMINISTRADOR','EDITOR','LECTOR']
const empty={nombre:'',usuario:'',email:'',password:'',rol:'LECTOR',passwordTemporal:true}
export default function AdminDialog({open,onClose}){const [tab,setTab]=useState(0);const [users,setUsers]=useState([]);const [events,setEvents]=useState([]);const [form,setForm]=useState(empty);const [resets,setResets]=useState({});const [error,setError]=useState('');const [ok,setOk]=useState('')
 const load=()=>Promise.all([authApi.users(),authApi.audit()]).then(([u,a])=>{setUsers(u);setEvents(a)}).catch(e=>setError(e.message));useEffect(()=>{if(open)load()},[open])
 const create=async()=>{setError('');setOk('');try{await authApi.createUser({...form,email:form.email.trim()||null});setForm(empty);await load();setOk('Usuario creado correctamente.')}catch(e){setError(e.message)}}
 const update=async (user,changes)=>{setError('');setOk('');try{const body={nombre:user.nombre,usuario:user.usuario,email:user.email||null,rol:user.rol,activo:user.activo,passwordTemporal:user.passwordTemporal,...changes};if(typeof body.email==='string')body.email=body.email.trim()||null;await authApi.updateUser(user.id,body,user.version);await load()}catch(e){setError(e.message)}}
 const resetState=(id)=>resets[id]||{password:'',temporal:true}
 const setReset=(id,changes)=>setResets(current=>({...current,[id]:{...resetState(id),...changes}}))
 const resetPassword=async user=>{setError('');setOk('');const state=resetState(user.id);try{await authApi.resetUserPassword(user.id,{password:state.password,temporal:state.temporal});setResets(current=>({...current,[user.id]:{password:'',temporal:true}}));await load();setOk(`Contraseña de ${user.nombre} actualizada correctamente.`)}catch(e){setError(e.message)}}
 return <Dialog open={open} onClose={onClose} fullWidth maxWidth="md"><DialogTitle>Administración</DialogTitle><DialogContent>{error&&<Alert severity="error" sx={{mb:2}}>{error}</Alert>}{ok&&<Alert severity="success" sx={{mb:2}}>{ok}</Alert>}<Tabs value={tab} onChange={(_,v)=>setTab(v)}><Tab label="Usuarios"/><Tab label="Auditoría"/></Tabs>
 {tab===0&&<Stack sx={{gap:2,pt:2}}><Typography variant="h6">Crear usuario</Typography><TextField label="Nombre y apellido" value={form.nombre} onChange={e=>setForm({...form,nombre:e.target.value})} required/><TextField label="Usuario" value={form.usuario} onChange={e=>setForm({...form,usuario:e.target.value})} required helperText="Se usa para ingresar al sistema. No puede repetirse."/><TextField label="Correo" type="email" value={form.email} onChange={e=>setForm({...form,email:e.target.value})} helperText="Opcional."/><TextField label="Contraseña inicial" type="password" value={form.password} onChange={e=>setForm({...form,password:e.target.value})} required/><FormControl required><InputLabel>Rol</InputLabel><Select label="Rol" value={form.rol} onChange={e=>setForm({...form,rol:e.target.value})}>{roles.map(r=><MenuItem key={r} value={r}>{r}</MenuItem>)}</Select></FormControl><FormControlLabel control={<Switch checked={form.passwordTemporal} onChange={e=>setForm({...form,passwordTemporal:e.target.checked})}/>} label="Solicitar cambio de contraseña al primer ingreso"/><Button variant="contained" onClick={create}>Crear usuario</Button><Typography variant="h6">Usuarios existentes</Typography>{users.map(u=>{const reset=resetState(u.id);return <Paper variant="outlined" key={u.id} sx={{p:2}}><Stack sx={{gap:1.5}}><Stack direction="row" sx={{justifyContent:'space-between',alignItems:'flex-start',gap:2,flexWrap:'wrap'}}><Stack sx={{gap:.75,minWidth:240}}><Typography><b>{u.nombre}</b></Typography><Typography color="text.secondary">{u.usuario}{u.email?` · ${u.email}`:''}</Typography><Stack direction="row" sx={{gap:1,alignItems:'center',flexWrap:'wrap'}}><FormControl size="small" sx={{minWidth:180}}><InputLabel>Rol</InputLabel><Select label="Rol" value={u.rol} onChange={e=>update(u,{rol:e.target.value})}>{roles.map(r=><MenuItem key={r} value={r}>{r}</MenuItem>)}</Select></FormControl>{u.passwordTemporal&&<Chip size="small" label="Contraseña temporal" color="warning" variant="outlined"/>}</Stack></Stack><Stack direction="row" sx={{alignItems:'center',gap:1,flexWrap:'wrap',justifyContent:'flex-end'}}><Typography variant="caption">Activo</Typography><Switch checked={u.activo} onChange={()=>update(u,{activo:!u.activo})}/></Stack></Stack><Stack direction={{xs:'column',sm:'row'}} sx={{gap:1,alignItems:{sm:'center'}}}><TextField size="small" label="Nueva contraseña" type="password" value={reset.password} onChange={e=>setReset(u.id,{password:e.target.value})} sx={{flex:1}}/><FormControlLabel control={<Switch checked={reset.temporal} onChange={e=>setReset(u.id,{temporal:e.target.checked})}/>} label="Solicitar cambio al ingresar"/><Button variant="outlined" disabled={reset.password.length<8} onClick={()=>resetPassword(u)}>Resetear contraseña</Button></Stack></Stack></Paper>})}</Stack>}
 {tab===1&&<Stack sx={{gap:1,pt:2}}>{events.map(e=><Paper variant="outlined" key={e.id} sx={{p:1.5}}><Typography variant="body2"><b>{e.accion}</b> {e.recurso} · HTTP {e.codigoHttp}</Typography><Typography variant="caption" color="text.secondary">{e.usuario} · {new Date(e.ocurridoEn).toLocaleString('es-AR')}</Typography></Paper>)}</Stack>}
 </DialogContent><DialogActions><Button onClick={onClose}>Cerrar</Button></DialogActions></Dialog>}
