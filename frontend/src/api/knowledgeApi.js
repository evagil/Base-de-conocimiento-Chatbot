import { versionParams } from './version.js'
import { http } from './httpClient.js'

const updateFile = (id, text, order, version) => http.put(`/api/archivos/${id}`, null, { params: { ...versionParams(version).params, textoAlternativo: text, orden: order } }).then(r => r.data)
const deactivateFile = (id, version) => http.patch(`/api/archivos/${id}/activo`, { activo: false }, versionParams(version)).then(r => r.data)
const fileContent = id => http.get(`/api/archivos/${id}/contenido`, { responseType: 'blob' }).then(r => r.data)

export const knowledgeApi = {
  updateArea: (id, body, version) => http.put(`/api/areas/${id}`, body, versionParams(version)).then(r => r.data),
  updateSubarea: (id, body, version) => http.put(`/api/subareas/${id}`, body, versionParams(version)).then(r => r.data),
  removeArea: (id, version) => http.patch(`/api/areas/${id}/estado`, { activo: false }, versionParams(version)).then(r => r.data),
  removeSubarea: (id, version) => http.patch(`/api/subareas/${id}/estado`, { activo: false }, versionParams(version)).then(r => r.data),
  createArea: body => http.post('/api/areas', body).then(r => r.data),
  createSubarea: (areaId, body) => http.post(`/api/areas/${areaId}/subareas`, body).then(r => r.data),
  async tree() {
    const { data: areaPage } = await http.get('/api/areas', { params: { activo: true, size: 200, sort: 'orden,asc' } })
    return Promise.all(areaPage.content.map(async area => {
      const { data: subareas } = await http.get(`/api/areas/${area.id}/subareas`, { params: { activo: true } })
      const children = await Promise.all(subareas.map(async subarea => {
        const { data: page } = await http.get('/api/problemas', { params: { subareaId: subarea.id, activo: true, size: 200, sort: 'titulo,asc' } })
        return { ...subarea, problems: page.content }
      }))
      return { ...area, subareas: children }
    }))
  },
  problem: id => http.get(`/api/problemas/${id}`).then(r => r.data),
  createProblem: body => http.post('/api/problemas', body).then(r => r.data),
  updateProblem: (id, body, version) => http.put(`/api/problemas/${id}`, body, versionParams(version)).then(r => r.data),
  removeProblem: (id, version) => http.patch(`/api/problemas/${id}/activo`, { activo: false }, versionParams(version)).then(r => r.data),
  removeSolution: (id, version) => http.patch(`/api/soluciones/${id}/activo`, { activo: false }, versionParams(version)).then(r => r.data),
  archiveProblem: (id, version) => http.patch(`/api/problemas/${id}/estado`, null, { params: { ...versionParams(version).params, estado: 'ARCHIVADO' } }).then(r => r.data),
  createSolution: (problemId, body) => http.post(`/api/problemas/${problemId}/soluciones`, body).then(r => r.data),
  updateSolution: (id, body, version) => http.put(`/api/soluciones/${id}`, body, versionParams(version)).then(r => r.data),
  createStep: (solutionId, body) => http.post(`/api/soluciones/${solutionId}/pasos`, body).then(r => r.data),
  images: (owner, id) => http.get(`/api/${owner}/${id}/imagenes`).then(r => r.data),
  updateImage: updateFile,
  removeImage: deactivateFile,
  imageContent: fileContent,
  updateAttachment: updateFile,
  removeAttachment: deactivateFile,
  attachmentContent: fileContent,
  attachments: (owner, id) => http.get(`/api/${owner}/${id}/adjuntos`).then(r => r.data),
  uploadAttachment: (owner, id, file, name) => {
    const form = new FormData(); form.append('archivo', file)
    return http.post(`/api/${owner}/${id}/adjuntos`, form, { params: { nombre: name } }).then(r => r.data)
  },
  uploadImage: (owner, id, file, alt, order = 0) => {
    const form = new FormData(); form.append('archivo', file)
    return http.post(`/api/${owner}/${id}/imagenes`, form, { params: { textoAlternativo: alt, orden: order } }).then(r => r.data)
  },
  tags: () => http.get('/api/etiquetas').then(r => r.data)
}
