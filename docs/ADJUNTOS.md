# Documentos adjuntos

Cada problema guardado y cada solución tienen una sección **Archivos adjuntos**.
Administradores y editores pueden subir PDF o Word (.docx), cambiar el nombre
visible y eliminarlos. Los lectores pueden descargarlos cuando el problema está
publicado y tanto el problema como la solución y el archivo están activos.

Para adjuntar a un problema o solución nuevos, primero guardalos. En su detalle,
usá **Adjuntar PDF o Word**, seleccioná el documento y presioná **Subir archivo**.
El límite predeterminado es 10 MB. El formato Word antiguo `.doc` no se admite.
Los adjuntos se descargan; no se incrustan como imágenes en la descripción.

La descarga usa autenticación. El servidor identifica PDF por cabecera y cierre;
para DOCX valida la estructura ZIP y XML requerida, con límites de expansión y
sin admitir macros. Esta validación de formato no es un análisis antivirus ni
una validación completa de todas las estructuras internas de PDF/Office.

Se reutilizan almacenamiento, vínculos y versiones de archivos existentes.
Los documentos no aparecen en las galerías de imágenes. La eliminación es lógica,
conservando el archivo físico, y los cambios de nombre y estado requieren la
versión vigente. Las copias que incluyen PostgreSQL y `storage` incluyen adjuntos.

La migración V7 amplía `archivos.tipo_mime` para almacenar el MIME de DOCX.
No cambia los datos existentes. El usuario de Flyway debe ser propietario de
`archivos`; en instalaciones restauradas con propietario `postgres`, un
administrador de PostgreSQL debe corregirlo antes de reiniciar:

```bash
sudo -u postgres psql -X -v ON_ERROR_STOP=1 -d base_conocimiento -c "ALTER TABLE public.archivos OWNER TO base_conocimiento;"
```

Endpoints: `GET/POST /api/problemas/{id}/adjuntos` y
`GET/POST /api/soluciones/{id}/adjuntos`. Los cambios de nombre, baja lógica y
contenido utilizan los endpoints existentes `/api/archivos/{id}`,
`/api/archivos/{id}/activo` y `/api/archivos/{id}/contenido`.
