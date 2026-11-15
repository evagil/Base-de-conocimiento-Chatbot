import DOMPurify from 'dompurify'
import { useEffect } from 'react'
import Accordion from '@mui/material/Accordion'
import AccordionDetails from '@mui/material/AccordionDetails'
import AccordionSummary from '@mui/material/AccordionSummary'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import DeleteOutlined from '@mui/icons-material/DeleteOutlined'
import Edit from '@mui/icons-material/Edit'
import ExpandMore from '@mui/icons-material/ExpandMore'
import Star from '@mui/icons-material/Star'
import AttachmentPanel from './AttachmentPanel.jsx'
import ImageGallery from './ImageGallery.jsx'

const RichHtml=({html})=><>
  <Box className="rich-content" dangerouslySetInnerHTML={{__html:DOMPurify.sanitize(html||'',{
    ALLOWED_TAGS:['p','br','strong','b','em','i','s','strike','u','code','pre','h1','h2','h3','ul','ol','li','blockquote','a','span','div','table','thead','tbody','tr','th','td','hr'],
    ALLOWED_ATTR:['href','title','target','rel','colspan','rowspan','class']
  })}}/>
  {/<\s*(?:img|picture|svg|video|object|embed|iframe)\b/i.test(html||'')&&<Alert severity="info">Imagen incrustada oculta. Para verla protegida, adjuntala a la galería de imágenes.</Alert>}
</>
export default function ProblemDetail({ detail, onEditSolution, onRemoveSolution, removing = false, imageRevision, targetSolutionId, targetRevision }) {
  const problemId=detail?.problema?.id
  const solutionIds=detail?.soluciones?.map(s=>s.id).join(',')
  useEffect(()=>{
    if(targetSolutionId)document.getElementById(`solucion-${targetSolutionId}`)?.scrollIntoView?.({block:'start'})
  },[targetSolutionId,targetRevision,problemId,solutionIds])
  if(!detail)return <Paper sx={{p:{xs:3,md:6},textAlign:'center'}}><Typography variant="h5">Seleccioná un problema</Typography><Typography color="text.secondary" sx={{mt:1}}>Explorá el árbol para consultar el error y todas sus soluciones.</Typography></Paper>
  const {problema}=detail
  const soluciones=detail.soluciones.filter(solution=>solution.activo !== false)
  return <Paper sx={{p:{xs:2,md:4}}}><Stack direction={{xs:'column',sm:'row'}} sx={{justifyContent:'space-between',gap:2,flexWrap:'wrap'}}><Box sx={{minWidth:0,flex:1,overflowWrap:"anywhere"}}><Typography variant="overline" color="primary">{problema.area} / {problema.subarea}</Typography><Typography variant="h4">{problema.titulo}</Typography></Box><Chip label={problema.estado.replace('_',' ')} color={problema.estado==='PUBLICADO'?'success':'default'} sx={{alignSelf:'flex-start'}}/></Stack>
    <Stack direction="row" sx={{gap:1,flexWrap:'wrap',my:2}}>{problema.etiquetas.map(tag=><Chip key={tag} label={tag} size="small" variant="outlined"/>)}</Stack><Divider sx={{mb:2}}/><RichHtml html={problema.descripcionHtml}/><ImageGallery revision={imageRevision} owner="problemas" id={problema.id} title="Capturas del error"/><AttachmentPanel key={`problem-${problema.id}`} revision={imageRevision} owner="problemas" id={problema.id} canEdit={Boolean(onEditSolution)}/>
    <Typography variant="h5" sx={{mt:4,mb:2}}>Soluciones</Typography>{!soluciones.length&&<Alert severity="info">Todavía no hay soluciones documentadas.</Alert>}{soluciones.map((solution,index)=><Accordion id={`solucion-${solution.id}`} key={`${solution.id}-${targetSolutionId||0}`} sx={{scrollMarginTop:"200px"}} defaultExpanded={solution.id===targetSolutionId||solution.principal||index===0}><AccordionSummary expandIcon={<ExpandMore/>}><Stack direction="row" sx={{alignItems:'center',gap:1,flexWrap:'wrap',minWidth:0,overflowWrap:'anywhere'}}>{solution.principal&&<Star color="secondary"/>}<Typography component="span" sx={{fontWeight:400}} dangerouslySetInnerHTML={{__html:DOMPurify.sanitize(solution.titulo||`Solución ${index+1}`,{ALLOWED_TAGS:['strong','b','em','i','s','strike','code'],ALLOWED_ATTR:[]})}}/>{solution.principal&&<Chip size="small" label="Recomendada" color="secondary"/>}</Stack></AccordionSummary><AccordionDetails>{onEditSolution&&<Box sx={{display:'flex',justifyContent:'flex-end',mb:1,flexWrap:'wrap',gap:1}}><Button startIcon={<Edit/>} onClick={()=>onEditSolution(solution)}>Editar solución</Button>{onRemoveSolution&&<Button disabled={removing} startIcon={<DeleteOutlined/>} onClick={()=>onRemoveSolution(solution)}>Eliminar solución</Button>}</Box>}<RichHtml html={solution.descripcionHtml}/><ImageGallery revision={imageRevision} owner="soluciones" id={solution.id}/><AttachmentPanel key={`solution-${solution.id}`} revision={imageRevision} owner="soluciones" id={solution.id} canEdit={Boolean(onEditSolution)}/>{solution.pasos.map(step=><Box key={step.id} sx={{display:'grid',gridTemplateColumns:'36px 1fr',gap:1.5,mt:2}}><Box sx={{width:32,height:32,borderRadius:'50%',bgcolor:'primary.main',color:'white',display:'grid',placeItems:'center',fontWeight:700}}>{step.orden}</Box><Box><Typography fontWeight={700}>{step.titulo||`Paso ${step.orden}`}</Typography><RichHtml html={step.contenidoHtml}/><ImageGallery revision={imageRevision} owner="pasos" id={step.id}/></Box></Box>)}</AccordionDetails></Accordion>)}
    <Typography variant="caption" color="text.secondary" sx={{display:'block',mt:3}}>Actualizado: {new Date(problema.modificadoEn).toLocaleString('es-AR')}</Typography></Paper>
}
