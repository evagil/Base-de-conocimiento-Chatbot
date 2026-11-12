import { useEffect, useMemo, useState } from 'react'
import { Route, Routes, useNavigate, useParams } from 'react-router-dom'
import { Add, Archive, Clear, DeleteOutlined, Edit, Image, Menu as MenuIcon, PushPinOutlined, Search } from '@mui/icons-material'
import { Alert, AppBar, Box, Button, CircularProgress, Drawer, MenuItem, IconButton, InputAdornment, Stack, TextField, Toolbar, Tooltip, Typography, useMediaQuery } from '@mui/material'
import NavigationTree from '../components/NavigationTree.jsx'
import ProblemDetail from '../components/ProblemDetail.jsx'
import ProblemFormDialog from '../components/ProblemFormDialog.jsx'
import SolutionFormDialog from '../components/SolutionFormDialog.jsx'
import ImageUploadDialog from '../components/ImageUploadDialog.jsx'
import HierarchyFormDialog from '../components/HierarchyFormDialog.jsx'
import { useKnowledge } from '../hooks/useKnowledge.js'
import { countProblems, filterByStatus, filterTree } from '../utils/tree.js'
import { knowledgeApi } from '../api/knowledgeApi.js'
import { useAuth } from '../auth/AuthContext.jsx'
import LoginPage from '../components/LoginPage.jsx'
import AdminDialog from '../components/AdminDialog.jsx'
import AccountDialog from '../components/AccountDialog.jsx'

import ResizableSidebar from '../components/ResizableSidebar.jsx'
function Workspace(){
  const auth=useAuth()
  const wideScreen=useMediaQuery('(min-width:1200px)')
  const [sidebarPinned,setSidebarPinned]=useState(()=>{try{return localStorage.getItem('bc_sidebar_pinned') !== 'false'}catch{return true}})
  const sidebarDocked=wideScreen&&sidebarPinned
  useEffect(()=>{try{localStorage.setItem('bc_sidebar_pinned',String(sidebarPinned))}catch{/* Storage may be unavailable. */}},[sidebarPinned])
  const [imageRevision,setImageRevision]=useState(0)
  const {id}=useParams();const navigate=useNavigate();const state=useKnowledge();const [query,setQuery]=useState('');const [statusFilter,setStatusFilter]=useState('SIN_ARCHIVADOS');const [mobile,setMobile]=useState(false);const [problemForm,setProblemForm]=useState(null);const [editingSolution,setEditingSolution]=useState(null);const [solutionForm,setSolutionForm]=useState(false);const [imageForm,setImageForm]=useState(false);const [hierarchyForm,setHierarchyForm]=useState('');const [adminOpen,setAdminOpen]=useState(false);const [accountOpen,setAccountOpen]=useState(()=>Boolean(auth.user?.passwordTemporal));const [notice,setNotice]=useState('')
  useEffect(()=>{if(auth.user?.passwordTemporal)setAccountOpen(true)},[auth.user?.passwordTemporal])
  useEffect(()=>{if(id&&Number(id)!==state.problem?.problema?.id)state.selectProblem(Number(id))},[id,state.selectProblem,state.problem?.problema?.id])
  const filtered=useMemo(()=>filterTree(filterByStatus(state.tree,auth.canEdit?statusFilter:'PUBLICADO'),query),[state.tree,query,auth.canEdit,statusFilter]);const selected=state.problem?.problema
  const hasActiveFilters=query.trim()!==''||(auth.canEdit&&statusFilter!=='SIN_ARCHIVADOS')
  const clearFilters=()=>{setQuery('');setStatusFilter('SIN_ARCHIVADOS')}
  const [removing,setRemoving]=useState(false)
  const hasSolutions=state.problem?.soluciones?.some(solution=>solution.activo !== false)
  const removeProblem=async()=>{
    if(!selected||removing)return
    if(hasSolutions){state.setError("Primero eliminá todas las soluciones del problema.");return}
    if(!window.confirm(`¿Eliminar el problema «${selected.titulo}»? Dejará de aparecer en el árbol. Se conserva el historial.`))return
    setRemoving(true)
    try{await knowledgeApi.removeProblem(selected.id,selected.version);navigate("/");state.setProblem(null);await state.loadTree();setNotice("Problema eliminado correctamente.")}catch(e){state.setError(e.message)}finally{setRemoving(false)}
  }
  const removeSolution=async solution=>{
    if(removing||!window.confirm("¿Eliminar esta solución? Dejará de mostrarse y se conservará su historial."))return
    setRemoving(true)
    try{await knowledgeApi.removeSolution(solution.id,solution.version);await refresh(selected.id)}catch(e){state.setError(e.message)}finally{setRemoving(false)}
  }
  const choose=id=>{navigate(`/problemas/${id}`);setMobile(false)}
  const refresh=async id=>{navigate(`/problemas/${id}`);await state.loadTree();await state.selectProblem(id);setImageRevision(value=>value+1);setNotice('Cambios guardados correctamente.')}
  const refreshHierarchy=async()=>{await state.loadTree();if(selected?.id)await state.selectProblem(selected.id);setNotice('Área o subárea guardada correctamente.')}
  const archive=async()=>{if(selected&&window.confirm('¿Archivar este problema?')){try{await knowledgeApi.archiveProblem(selected.id,selected.version);await refresh(selected.id)}catch(e){state.setError(e.message)}}}
  const sidebar=<Box sx={{p:2,height:'100%',overflow:'auto'}}><Stack direction="row" sx={{alignItems:'center',justifyContent:'space-between'}}><Typography variant="subtitle1" fontWeight={750}>Conocimiento</Typography><Tooltip title={sidebarPinned?'Desfijar barra lateral':'Fijar barra lateral'}><IconButton aria-label={sidebarPinned?'Desfijar barra lateral':'Fijar barra lateral'} aria-pressed={sidebarPinned} color={sidebarPinned?'primary':'default'} onClick={()=>{setSidebarPinned(value=>!value);if(wideScreen)setMobile(false)}}><PushPinOutlined/></IconButton></Tooltip></Stack><Typography variant="caption" color="text.secondary">{countProblems(filtered)} problemas visibles</Typography>{auth.isAdmin&&<Stack direction="row" sx={{gap:1,my:2}}><Button size="small" variant="outlined" onClick={()=>setHierarchyForm({mode:'area'})}>Nueva área</Button><Button size="small" variant="outlined" disabled={!state.tree.length} onClick={()=>setHierarchyForm({mode:'subarea'})}>Nueva subárea</Button></Stack>}<TextField size="small" fullWidth placeholder="Filtrar árbol…" value={query} onChange={e=>setQuery(e.target.value)} sx={{my:2}} InputProps={{startAdornment:<InputAdornment position="start"><Search fontSize="small"/></InputAdornment>}}/>{auth.canEdit&&<TextField select fullWidth size="small" label="Filtrar por estado" value={statusFilter} onChange={event=>setStatusFilter(event.target.value)} sx={{mb:1}}><MenuItem value="SIN_ARCHIVADOS">Sin archivados</MenuItem><MenuItem value="TODOS">Todos los estados</MenuItem><MenuItem value="BORRADOR">Borrador</MenuItem><MenuItem value="PUBLICADO">Publicado</MenuItem><MenuItem value="ARCHIVADO">Archivado</MenuItem></TextField>}{hasActiveFilters&&<Button size="small" variant="text" startIcon={<Clear/>} onClick={clearFilters} sx={{alignSelf:'flex-start',mb:2}}>Limpiar filtros</Button>}<NavigationTree areas={filtered} selectedId={selected?.id} onSelect={choose} onEditArea={auth.isAdmin?entity=>setHierarchyForm({mode:'area',entity}):undefined} onEditSubarea={auth.isAdmin?entity=>setHierarchyForm({mode:'subarea',entity}):undefined}/></Box>
  return <Box sx={{display:'flex',minHeight:'100vh'}}><AppBar position="fixed" sx={{zIndex:theme=>theme.zIndex.drawer+1,background:theme=>`linear-gradient(110deg,${theme.palette.primary.main},${theme.palette.primary.light})`,color:'primary.contrastText'}}><Toolbar sx={{flexWrap:{xs:"wrap",sm:"nowrap"},minHeight:{xs:104,sm:64}}}><IconButton aria-label="Abrir barra lateral" color="inherit" onClick={()=>setMobile(true)} sx={{display:sidebarDocked?"none":"inline-flex",mr:1}}><MenuIcon/></IconButton><Box sx={{flexGrow:1,minWidth:0}}><Typography variant="h6" sx={{fontSize:{xs:"1rem",sm:"1.25rem"}}}>Base de conocimiento</Typography><Typography variant="caption" sx={{opacity:.78}}>Gestión interna de soluciones</Typography></Box><Stack direction="row" sx={{alignItems:"center",gap:1,width:{xs:"100%",sm:"auto"},justifyContent:"flex-end"}}><Button color="inherit" onClick={()=>setAccountOpen(true)} sx={{textTransform:"none",fontWeight:700}}>{auth.user.nombre}</Button>{auth.isAdmin&&<Button color="inherit" onClick={()=>setAdminOpen(true)}>Administrar</Button>}<Button color="inherit" onClick={auth.logout}>Salir</Button></Stack></Toolbar></AppBar>
    {sidebarDocked&&<ResizableSidebar>{sidebar}</ResizableSidebar>}<Drawer open={!sidebarDocked&&mobile} onClose={()=>setMobile(false)} sx={{'& .MuiDrawer-paper':{width:'86%',maxWidth:360,pt:{xs:13,sm:8}}}}>{sidebar}</Drawer>
    <Box component="main" sx={{flexGrow:1,px:{xs:2,md:4},pb:{xs:2,md:4},pt:{xs:16,sm:12},minWidth:0}}>{auth.canEdit&&<Stack direction="row" justifyContent="flex-end" sx={{mb:2,gap:1,flexWrap:"wrap",'& .MuiButton-root':{flexShrink:0,whiteSpace:"nowrap"}}}><Button startIcon={<Add/>} variant="contained" onClick={()=>setProblemForm('create')}>Nuevo problema</Button>{selected&&<><Button startIcon={<Edit/>} onClick={()=>setProblemForm('edit')}>Editar</Button><Button startIcon={<Add/>} onClick={()=>{setEditingSolution(null);setSolutionForm(true)}}>Solución</Button><Button startIcon={<Image/>} onClick={()=>setImageForm(true)}>Imagen</Button><Tooltip title="Conserva el historial"><Button color="primary" startIcon={<Archive/>} onClick={archive}>Archivar</Button></Tooltip><Tooltip title={hasSolutions?"Primero eliminá todas las soluciones":"Eliminar problema"}><span><Button startIcon={<DeleteOutlined/>} disabled={removing||hasSolutions} onClick={removeProblem}>Eliminar problema</Button></span></Tooltip></>}</Stack>}{notice&&<Alert severity="success" onClose={()=>setNotice('')} sx={{mb:2}}>{notice}</Alert>}{state.error&&<Alert severity="error" onClose={()=>state.setError('')} sx={{mb:2}}>{state.error}</Alert>}{state.loading&&<Box sx={{display:'flex',justifyContent:'center',py:2}}><CircularProgress size={28}/></Box>}<ProblemDetail removing={removing} onRemoveSolution={auth.canEdit?removeSolution:undefined} imageRevision={imageRevision} detail={state.problem} onEditSolution={auth.canEdit?solution=>{setEditingSolution(solution);setSolutionForm(true)}:undefined}/></Box>
    <AdminDialog open={adminOpen} onClose={()=>setAdminOpen(false)}/><AccountDialog open={accountOpen} required={Boolean(auth.user?.passwordTemporal)} onClose={()=>setAccountOpen(false)}/><HierarchyFormDialog open={Boolean(hierarchyForm)} mode={hierarchyForm.mode} entity={hierarchyForm.entity} tree={state.tree} onClose={()=>setHierarchyForm('')} onSaved={refreshHierarchy}/><ProblemFormDialog open={Boolean(problemForm)} onClose={()=>setProblemForm(null)} tree={state.tree} detail={problemForm==='edit'?state.problem:null} onImagesChanged={()=>setImageRevision(value=>value+1)} onSaved={refresh}/><SolutionFormDialog solution={editingSolution} open={solutionForm} onClose={()=>setSolutionForm(false)} problemId={selected?.id} onSaved={refresh}/><ImageUploadDialog open={imageForm} onClose={()=>setImageForm(false)} owner="problemas" id={selected?.id} onSaved={()=>refresh(selected.id)}/>
  </Box>
}
export default function App(){const auth=useAuth();if(auth.checking)return <Box sx={{minHeight:'100vh',display:'grid',placeItems:'center'}}><CircularProgress/></Box>;if(!auth.user)return <LoginPage/>;return <Routes><Route path="*" element={<Workspace/>}/><Route path="/problemas/:id" element={<Workspace/>}/></Routes>}
