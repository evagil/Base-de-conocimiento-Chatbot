# Uso diario en Ubuntu

Para la instalación Windows preparada el 12/11/2026, consultar
[USO_WINDOWS.md](USO_WINDOWS.md). Sus datos están en `.local/` y PostgreSQL usa
el puerto 55432; los comandos systemd de esta guía no se aplican a Windows.

Esta configuración es para la computadora del usuario. El frontend se sirve con
Vite en 127.0.0.1:5173 y el backend en 127.0.0.1:8080. No es un despliegue para la
red municipal. Requiere Java 21, Node 22+, Python 3, PostgreSQL en funcionamiento
y `pg_dump` de versión igual o posterior a la del servidor.

## Configurar una sola vez

Cerrá con Ctrl+C las terminales que ejecutan el backend y el frontend. Desde la
raíz del proyecto ejecutá:

```bash
bash scripts/local/setup.sh
```

Solicita la contraseña PostgreSQL y la guarda con permisos 600 en
`backend/.env.local`, excluido de Git. Si ya existe, conserva esa configuración.
No modifica la contraseña del administrador existente. Genera una clave JWT
local nueva la primera vez, por lo que deberás iniciar sesión nuevamente.
Primero crea una copia de seguridad, luego compila el backend, instala las
dependencias del frontend y registra servicios systemd del usuario.

Ambos servicios arrancarán automáticamente **al iniciar sesión en Ubuntu**.
No necesitan una terminal abierta. La base PostgreSQL debe tener su propio
servicio habilitado. Abrí <http://localhost:5173>.

El backend usa un JAR en `.local/backend.jar`, que no se borra al ejecutar las
pruebas. Para aplicar futuros cambios de backend, volvé a ejecutar `setup.sh`.
Los servicios guardan la ruta actual del repositorio: si se mueve, ejecutar de
nuevo el instalador. El frontend sigue siendo el entorno local de desarrollo.

## Control y diagnóstico

```bash
systemctl --user status base-conocimiento-backend base-conocimiento-frontend
journalctl --user -u base-conocimiento-backend -n 80 --no-pager
systemctl --user restart base-conocimiento-backend base-conocimiento-frontend
```

Para volver al arranque manual:

```bash
systemctl --user disable --now base-conocimiento-backend base-conocimiento-frontend base-conocimiento-backup.timer
```

## Copias de seguridad

El temporizador crea una copia diaria mientras la sesión está activa; si la
computadora estaba apagada, recupera la ejecución pendiente al iniciar sesión.

```bash
systemctl --user list-timers base-conocimiento-backup.timer
journalctl --user -u base-conocimiento-backup -n 40 --no-pager
bash scripts/local/run.sh backup
```

Cada carpeta completa en `backups/copia-*` contiene `database.dump`, `storage/`
y un manifiesto SHA-256. Una copia que falla no se presenta como completa.
La base se copia primero; después se copian las imágenes, que actualmente son
inmutables y no se borran físicamente al desactivarlas. Si en el futuro se agrega
borrado físico, deberá coordinarse con el respaldo.

No hay eliminación automática de copias. Revisá el espacio disponible y copiá
periódicamente una carpeta completa a otro disco: las copias en el mismo disco
no protegen contra una falla del equipo. BACKUP_PATH puede cambiarse en el
archivo de configuración local. Las copias contienen información privada.

## Comprobar y recuperar una copia

Comprobar integridad sin modificar la base:

```bash
python3 scripts/local/verify-backup.py /ruta/a/backups/copia-FECHA
```

Probá una restauración primero en una **base nueva vacía**, con propietario
`base_conocimiento`, usando una copia completa y un directorio de imágenes nuevo:

```bash
sudo -u postgres createdb --owner=base_conocimiento bc_restauracion_prueba
pg_restore --host=localhost --username=base_conocimiento --dbname=bc_restauracion_prueba --no-owner --no-privileges --exit-on-error /ruta/a/copia/database.dump
mkdir -p /ruta/nueva/storage
cp -a /ruta/a/copia/storage/. /ruta/nueva/storage/
```

`pg_restore` solicita la contraseña. Al restaurar con el usuario propietario se
conserva la capacidad de Flyway para modificar las tablas. Para usar la copia,
configurá DB_URL hacia esa base y STORAGE_PATH hacia las imágenes recuperadas,
y reiniciá el backend. Conservá la base original hasta verificar la recuperación.
El manifiesto comprueba integridad de los archivos; también hay que probar el
inicio, los registros y las imágenes en la base restaurada.
