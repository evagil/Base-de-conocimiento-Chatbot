export const filterTree = (areas, query) => {
  const term = query.trim().toLocaleLowerCase('es')
  if (!term) return areas
  return areas.map(area => {
    const areaMatches = area.nombre.toLocaleLowerCase('es').includes(term)
    const subareas = area.subareas.map(subarea => {
      const subMatches = subarea.nombre.toLocaleLowerCase('es').includes(term)
      const problems = areaMatches || subMatches ? subarea.problems : subarea.problems.filter(p => `${p.titulo} ${p.etiquetas?.join(' ') || ''}`.toLocaleLowerCase('es').includes(term))
      return { ...subarea, problems }
    }).filter(subarea => areaMatches || subarea.nombre.toLocaleLowerCase('es').includes(term) || subarea.problems.length)
    return { ...area, subareas }
  }).filter(area => area.nombre.toLocaleLowerCase('es').includes(term) || area.subareas.length)
}

export const countProblems = areas => areas.reduce((sum, area) => sum + area.subareas.reduce((subtotal, subarea) => subtotal + subarea.problems.length, 0), 0)

export const filterByStatus = (areas, status = 'SIN_ARCHIVADOS') => areas.map(area => ({
  ...area,
  subareas: area.subareas.map(subarea => ({
    ...subarea,
    problems: subarea.problems.filter(problem => status === 'TODOS' || (status === 'SIN_ARCHIVADOS' ? problem.estado !== 'ARCHIVADO' : problem.estado === status)),
  })),
}))
