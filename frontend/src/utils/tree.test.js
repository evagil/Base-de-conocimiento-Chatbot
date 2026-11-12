import { describe, expect, it } from 'vitest'
import { countProblems, filterByStatus, filterTree } from './tree.js'

const areas=[{id:1,nombre:'Sistemas',subareas:[{id:2,nombre:'Correo',problems:[{id:3,titulo:'No envía',etiquetas:['SMTP']},{id:4,titulo:'Clave',etiquetas:[]}]}]},{id:5,nombre:'Redes',subareas:[{id:6,nombre:'WiFi',problems:[]}]}]
describe('tree utils',()=>{
  it('returns the same tree without a search and counts problems',()=>{expect(filterTree(areas,'  ')).toBe(areas);expect(countProblems(areas)).toBe(2)})
  it('filters by area, subarea, title and tag',()=>{
    expect(filterTree(areas,'sistemas')[0].subareas[0].problems).toHaveLength(2)
    expect(filterTree(areas,'correo')[0].subareas[0].problems).toHaveLength(2)
    expect(filterTree(areas,'envía')[0].subareas[0].problems[0].id).toBe(3)
    expect(filterTree(areas,'smtp')[0].subareas[0].problems[0].id).toBe(3)
    expect(filterTree(areas,'inexistente')).toEqual([])
  })
})


describe('archived problems', () => {
  const tree = [{ id: 1, nombre: 'Rentas', subareas: [
    { id: 2, nombre: 'Configuraciones', problems: [
      { id: 3, titulo: 'Publicado', estado: 'PUBLICADO', etiquetas: ['AFI'] },
      { id: 4, titulo: 'Anterior', estado: 'ARCHIVADO', etiquetas: ['AFI'] },
      { id: 5, titulo: 'Pendiente', estado: 'BORRADOR', etiquetas: [] },
    ] },
    { id: 6, nombre: 'Vacía', problems: [] },
  ] }]
  it('hides archived problems by default without changing the original tree or empty subareas', () => {
    const visible = filterByStatus(tree)
    expect(visible[0].subareas[0].problems.map(p => p.id)).toEqual([3, 5])
    expect(visible[0].subareas).toHaveLength(2)
    expect(tree[0].subareas[0].problems).toHaveLength(3)
    expect(countProblems(visible)).toBe(2)
  })
  it('includes archived problems when enabled and combines with tag searches', () => {
    expect(countProblems(filterByStatus(tree, 'TODOS'))).toBe(3)
    expect(countProblems(filterTree(filterByStatus(tree), 'afi'))).toBe(1)
    expect(countProblems(filterTree(filterByStatus(tree, 'TODOS'), 'afi'))).toBe(2)
    expect(countProblems(filterByStatus(tree, 'SIN_ARCHIVADOS'))).toBe(2)
  })
  it.each(['BORRADOR', 'PUBLICADO', 'ARCHIVADO'])('selects only %s without changing the original tree', status => {
    const result = filterByStatus(tree, status)
    expect(result[0].subareas[0].problems).toHaveLength(1)
    expect(result[0].subareas[0].problems[0].estado).toBe(status)
    expect(countProblems(result)).toBe(1)
    expect(tree[0].subareas[0].problems).toHaveLength(3)
    expect(countProblems(filterTree(result, 'afi'))).toBe(status === 'BORRADOR' ? 0 : 1)
  })

})
