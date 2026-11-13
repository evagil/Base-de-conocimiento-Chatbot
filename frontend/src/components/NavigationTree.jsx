import { useState } from 'react'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import Tooltip from '@mui/material/Tooltip'
import Chip from '@mui/material/Chip'
import Collapse from '@mui/material/Collapse'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import Typography from '@mui/material/Typography'
import EditOutlined from '@mui/icons-material/EditOutlined'
import ExpandLess from '@mui/icons-material/ExpandLess'
import ExpandMore from '@mui/icons-material/ExpandMore'
import Folder from '@mui/icons-material/Folder'
import FolderOpen from '@mui/icons-material/FolderOpen'
import ArticleOutlined from '@mui/icons-material/ArticleOutlined'

export default function NavigationTree({ areas, selectedId, onSelect, onEditArea, onEditSubarea }) {
  const [open, setOpen] = useState({})
  const toggle = key => setOpen(current => ({ ...current, [key]: !current[key] }))
  if (!areas.length) return <Typography color="text.secondary" sx={{ p: 2 }}>No hay resultados.</Typography>
  return <List dense disablePadding aria-label="Árbol de conocimiento">{areas.map(area => {
    const areaKey=`a-${area.id}`, areaOpen=Boolean(open[areaKey])
    return <Box key={areaKey}><Box sx={{ display: 'flex', alignItems: 'center' }}><ListItemButton onClick={() => toggle(areaKey)} aria-expanded={areaOpen} sx={{ borderRadius: 1 }}><ListItemIcon sx={{ minWidth: 34 }}>{areaOpen?<FolderOpen color="primary"/>:<Folder/>}</ListItemIcon><ListItemText primary={area.nombre} title={area.nombre} sx={{ minWidth: 0 }} primaryTypographyProps={{ fontWeight: 700, noWrap: true }}/>{areaOpen?<ExpandLess/>:<ExpandMore/>}</ListItemButton>{onEditArea && <Tooltip title="Editar área"><IconButton size="small" aria-label={`Editar área ${area.nombre}`} onClick={() => onEditArea(area)}><EditOutlined fontSize="small"/></IconButton></Tooltip>}</Box>
      <Collapse in={areaOpen} unmountOnExit><List disablePadding>{area.subareas.map(sub => {const subKey=`s-${sub.id}`,subOpen=Boolean(open[subKey]);return <Box key={subKey}><Box sx={{ display: 'flex', alignItems: 'center' }}><ListItemButton onClick={()=>toggle(subKey)} aria-expanded={subOpen} sx={{ pl: 3,borderRadius:1 }}><ListItemIcon sx={{minWidth:32}}>{subOpen?<ExpandLess fontSize="small"/>:<ExpandMore fontSize="small"/>}</ListItemIcon><ListItemText primary={sub.nombre} title={sub.nombre} sx={{ minWidth: 0 }} primaryTypographyProps={{ noWrap: true }}/><Chip size="small" label={sub.problems.length}/></ListItemButton>{onEditSubarea && <Tooltip title="Editar subárea"><IconButton size="small" aria-label={`Editar subárea ${sub.nombre}`} onClick={() => onEditSubarea(sub)}><EditOutlined fontSize="small"/></IconButton></Tooltip>}</Box><Collapse in={subOpen} unmountOnExit><List disablePadding>{sub.problems.map(problem=><ListItemButton key={`p-${problem.id}`} selected={selectedId===problem.id} onClick={()=>onSelect(problem.id)} sx={{pl:6,borderRadius:1,my:.25}}><ListItemIcon sx={{minWidth:30}}><ArticleOutlined fontSize="small"/></ListItemIcon><ListItemText primary={problem.titulo} secondary={problem.estado} title={problem.titulo} sx={{ minWidth: 0 }} primaryTypographyProps={{ noWrap: true }}/></ListItemButton>)}</List></Collapse></Box>})}</List></Collapse>
    </Box>})}</List>
}
