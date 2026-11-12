// Updates must use the version the user saw, never a newly fetched version.
export function versionParams(version) {
  if (!Number.isSafeInteger(version) || version < 0) {
    throw new Error('Falta la versión del registro. Volvé a abrirlo antes de editar.');
  }
  return { params: { version } }
}
