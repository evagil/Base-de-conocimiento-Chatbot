import { http } from './httpClient.js'

export const chatApi = {
  answer: (pregunta, signal) => http.post('/api/rag/responder', { pregunta }, { signal, timeout: 300000 }).then(r => r.data),
  extract: (archivo, signal) => {
    const form = new FormData()
    form.append('archivo', archivo)
    return http.post('/api/documentos/extraer', form, { signal, timeout: 180000 }).then(r => r.data)
  },
  download: (id, signal) => http.get(`/api/archivos/${id}/contenido`, { signal, responseType: 'blob' }).then(r => r.data),
}
