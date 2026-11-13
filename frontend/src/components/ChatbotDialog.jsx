import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import useMediaQuery from '@mui/material/useMediaQuery'
import { chatApi } from '../api/chatApi.js'
import { useChat } from '../hooks/useChat.js'
import { downloadName, MAX_QUESTION, problemPath } from '../utils/chat.js'

export default function ChatbotDialog({ open, onClose }) {
  const chat = useChat()
  const navigate = useNavigate()
  const small = useMediaQuery('(max-width:600px)')
  const end = useRef(null)
  useEffect(() => { end.current?.scrollIntoView?.({ block: 'nearest' }) }, [chat.history.length, chat.busy])
  const close = () => { chat.cancel(); onClose() }
  const download = source => chat.run('download', signal => chatApi.download(source.archivoId, signal), blob => {
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url; link.download = downloadName(source); link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  })
  return <Dialog open={open} onClose={close} fullScreen={small} fullWidth maxWidth="md" aria-labelledby="chatbot-title">
    <DialogTitle id="chatbot-title">Chatbot del conocimiento</DialogTitle>
    <DialogContent dividers>
      <Stack sx={{ gap: 2 }}>
        <Typography variant="body2" color="text.secondary">Consultá problemas, soluciones y pasos publicados. La IA busca evidencia local y puede indicar que falta información. Revisá siempre la fuente.</Typography>
        <Typography variant="caption">Cada consulta es independiente. Hasta 10 intercambios en memoria; se borran al limpiar, salir de la sesión o recargar.</Typography>
        <Box role="log" aria-label="Conversación del chatbot" aria-live="polite" sx={{ maxHeight: { xs: '35vh', sm: '40vh' }, overflowY: 'auto' }}>
          {!chat.history.length && <Typography color="text.secondary">Describí el error o pegá el texto de un documento para empezar.</Typography>}
          {chat.history.map((turn, index) => <Stack key={index} sx={{ gap: 1, mb: 3 }}>
            <Box sx={{ bgcolor: 'action.hover', p: 2, borderRadius: 2 }}><Typography variant="subtitle2">Tu consulta</Typography><Typography sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{turn.question}</Typography></Box>
            <Typography variant="subtitle2">Respuesta</Typography>
            <Typography sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{turn.answer.texto}</Typography>
            {turn.answer.busquedaAcotada && <Alert severity="info">La búsqueda alcanzó su límite de candidatos. Podría haber otras fuentes; precisá el código o el problema.</Alert>}
            {turn.answer.fuentes?.map((source, number) => <Box key={number} sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
              <Typography variant="subtitle2">Fuente {number + 1}: {source.titulo}</Typography>
              <Typography variant="caption">{source.tipo} · Problema {source.problemaId}{source.solucionId != null && ` · Solución ${source.solucionId}`}{source.pasoId != null && ` · Paso ${source.pasoId}`}{source.pagina != null && ` · Página ${source.pagina}`}</Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{source.extracto}</Typography>
              <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
                {problemPath(source) && <Button onClick={() => { close(); navigate(problemPath(source)) }}>Abrir problema</Button>}
                {Number.isSafeInteger(source.archivoId) && source.archivoId > 0 && <Button disabled={Boolean(chat.busy)} onClick={() => download(source)}>Descargar fuente</Button>}
              </Stack>
            </Box>)}
          </Stack>)}
          <span ref={end}/>
        </Box>
        {chat.error && <Alert severity="error" onClose={() => chat.setError('')}>{chat.error}</Alert>}
        {chat.document && <Stack sx={{ gap: 1 }}>
          <Typography variant="subtitle2">Documento: {chat.document.nombre}</Typography>
          <Typography variant="caption">{chat.document.paginas == null ? 'Sin paginación estable' : `${chat.document.paginas} página(s)`} · {chat.document.ocrUsado ? 'Texto reconocido con OCR: revisá posibles errores.' : 'Texto extraído del documento.'}</Typography>
          <TextField label="Texto extraído (editable)" multiline minRows={3} maxRows={8} value={chat.extracted} disabled={Boolean(chat.busy)} onChange={e => chat.setExtracted(e.target.value)}/>
          <Typography variant="caption">Conservá el fragmento relevante. Se agregará a tu consulta sin recortes automáticos.</Typography>
          <Button disabled={Boolean(chat.busy) || !chat.extracted.trim()} onClick={chat.applyExtracted}>Usar texto en consulta</Button>
        </Stack>}
        <TextField label="Tu consulta al conocimiento" multiline minRows={3} maxRows={8} value={chat.question} disabled={Boolean(chat.busy)} onChange={e => chat.setQuestion(e.target.value)} error={chat.question.length > MAX_QUESTION} helperText={`${chat.question.length}/${MAX_QUESTION} caracteres${chat.question.length > MAX_QUESTION ? '. Acortá el texto antes de consultar.' : ''}`}/>
        <Stack direction="row" sx={{ gap: 1, flexWrap: 'wrap', alignItems: 'center' }}>
          <Button variant="contained" disabled={Boolean(chat.busy) || !chat.question.trim() || chat.question.length > MAX_QUESTION} onClick={chat.ask}>Consultar</Button>
          <Button component="label" disabled={Boolean(chat.busy)}>Extraer texto de archivo<input aria-label="Seleccionar archivo para el chatbot" type="file" hidden accept=".pdf,.docx,.jpg,.jpeg" disabled={Boolean(chat.busy)} onChange={e => { const file = e.target.files?.[0]; e.target.value = ''; if (file) chat.extract(file) }}/></Button>
          {chat.busy && <><CircularProgress size={20}/><Typography role="status">{chat.busy === 'extract' ? 'Extrayendo texto…' : chat.busy === 'download' ? 'Descargando fuente…' : 'Buscando evidencia y preparando respuesta…'}</Typography><Button onClick={chat.cancel}>Cancelar operación</Button></>}
        </Stack>
        <Typography variant="caption" color="text.secondary">PDF, DOCX o JPG/JPEG hasta 10 MB. Las imágenes se leen mediante OCR. Los archivos temporales no se publican como conocimiento. Cancelar deja de esperar en esta pantalla; el servidor puede terminar la operación iniciada.</Typography>
      </Stack>
    </DialogContent>
    <DialogActions><Button onClick={chat.clear}>Limpiar conversación</Button><Button onClick={close}>Cerrar chatbot</Button></DialogActions>
  </Dialog>
}
