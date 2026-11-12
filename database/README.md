# Respaldo de PostgreSQL

## Respaldo utilizado en Windows el 12/11/2026

La instalación actual se restauró desde
`E:\respaldo_base_conocimiento_20261112_165549\base_conocimiento.dump`, un
respaldo PostgreSQL CUSTOM generado con PostgreSQL 17.11. Se lee con
`pg_restore`, no con `psql`. Los archivos físicos originales están en
`E:\respaldo_base_conocimiento_20261112_165549\storage\storage`.

Se restauró en una instancia independiente en `127.0.0.1:55432`, base y usuario
`base_conocimiento`, con `--no-owner --no-privileges --exit-on-error`. Todas las
tablas de la aplicación quedaron a nombre de `base_conocimiento`, permitiendo
que Flyway gestione migraciones. Los archivos se copiaron a `.local/storage`.

Se comprobaron los hashes de `base_conocimiento.dump` y `storage.tar.gz` contra
`SHA256SUMS`, y la existencia y el hash de los 249 archivos registrados. El
respaldo contiene 9 áreas, 40 subáreas, 263 problemas, 213 soluciones, 4 usuarios
y diez migraciones exitosas. Son cantidades del respaldo, no contadores en vivo.

Consultar [uso en Windows](../docs/USO_WINDOWS.md) para operar esta instancia.
Los scripts de inicio no restauran ni eliminan datos.

## Exportación SQL alternativa

Los respaldos SQL y CUSTOM se excluyen de Git porque pueden contener datos
privados. `base_conocimiento.sql` debe generarse o transferirse por separado;
no está incluido al clonar este repositorio.

Las instrucciones siguientes corresponden al formato SQL plano y a una
instalación manual en el puerto 5432. No son el procedimiento usado para el
respaldo CUSTOM anterior.

`base_conocimiento.sql` contiene la creación de la base, las tablas, relaciones,
índices, secuencias y los datos existentes al momento de la exportación.

## Restaurar en otra computadora

La restauración elimina y vuelve a crear una base llamada `base_conocimiento`.
Cerrar el backend antes de ejecutarla y usar un usuario de PostgreSQL autorizado
para crear bases, por ejemplo `postgres`:

```powershell
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" `
  --host=localhost `
  --port=5432 `
  --username=postgres `
  --dbname=postgres `
  --file=".\database\base_conocimiento.sql"
```

PostgreSQL solicitará la contraseña del usuario. Luego se puede iniciar el backend;
Flyway validará que las migraciones incluidas coincidan con el esquema restaurado.

## Imágenes

El SQL conserva los registros y rutas de las imágenes, pero no sus archivos físicos.
Para trasladarlas también se debe copiar el contenido de `storage` a la misma ruta
relativa del proyecto nuevo.

## Actualizar el respaldo

Desde la raíz del proyecto:

```powershell
& "C:\Program Files\PostgreSQL\17\bin\pg_dump.exe" `
  --host=localhost `
  --port=5432 `
  --username=base_conocimiento `
  --dbname=base_conocimiento `
  --format=plain `
  --create `
  --clean `
  --if-exists `
  --no-owner `
  --no-privileges `
  --encoding=UTF8 `
  --file=".\database\base_conocimiento.sql"
```

El comando solicitará la contraseña de `base_conocimiento`.
