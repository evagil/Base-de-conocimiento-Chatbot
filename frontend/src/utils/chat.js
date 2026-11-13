export const MAX_QUESTION = 1000
export function validateChatFile(file) {
  if (!/\.(pdf|docx|jpe?g)$/i.test(file.name)) return 'Seleccioná un PDF, DOCX o JPG/JPEG.'
  if (!file.size) return 'El archivo está vacío.'
  if (file.size > 10 * 1024 * 1024) return 'El archivo supera los 10 MB.'
  return ''
}
export function extractedText(document) {
  return (document.secciones || []).map(section => section.texto).filter(Boolean).join('\n\n')
}
export function problemPath(source) {
  return Number.isSafeInteger(source.problemaId) && source.problemaId > 0 ? `/problemas/${source.problemaId}` : null
}
export function downloadName(source) {
  const title = String(source.titulo || 'Fuente').replace(/[\\/:*?"<>|\p{Cc}]/gu, '_').slice(0, 100)
  return `${title}-archivo-${source.archivoId}`
}
