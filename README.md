# Base de conocimiento

Aplicación interna para organizar áreas, subáreas, problemas y sus soluciones. La implementación se realiza por etapas como un monolito modular con backend y frontend independientes.

## Documentación del proyecto

- [Uso local en Windows con base independiente](docs/USO_WINDOWS.md)

- [Adjuntos PDF y Word](docs/ADJUNTOS.md)

- [Arranque automático y copias de seguridad en Ubuntu](docs/USO_LOCAL.md)

- [Prompt y requerimientos originales](docs/PROMPT_Y_REQUERIMIENTOS.md)
- [Etapas, despliegue local y pruebas](docs/ETAPAS_DESPLIEGUE_Y_PRUEBAS.md)
- [Concurrencia, versiones y pruebas PostgreSQL](docs/CONCURRENCIA.md)
- [Pruebas y permisos para IA local](docs/PRUEBAS_IA.md)

Cada etapa debe terminar con pruebas automatizadas. Para la integración de IA,
el umbral operativo se eleva a **92 % de líneas**, superando el 91 % solicitado.
El alcance de la medición del frontend está explicado en la guía de pruebas.

## Estado actual

Actualizado el 12/11/2026. Las etapas 1 a 4 están implementadas. El esquema
incluye migraciones V1 a V10; la búsqueda de texto completo de la etapa 5 sigue
pendiente. Actualmente el árbol se filtra por área, subárea, título y etiquetas
en el navegador. Todavía no hay un chatbot implementado.

La etapa 1 de preparación para IA local corrige la visibilidad de soluciones,
pasos e imágenes para lectores y conserva el acceso de edición al historial.
Se verificó con 39 pruebas backend y 56 frontend. Consultar
[PRUEBAS_IA.md](docs/PRUEBAS_IA.md) para resultados, cobertura y comandos.

La Etapa 1 incluye:

- backend Java 21 y Spring Boot;
- PostgreSQL, JPA y migraciones Flyway;
- CRUD REST de áreas y subáreas;
- validación, paginación, baja lógica y nombres únicos normalizados;
- manejo uniforme de errores;
- documentación OpenAPI/Swagger;
- perfiles de desarrollo, producción y pruebas;
- pruebas unitarias y de integración.

La Etapa 2 agrega problemas, etiquetas, múltiples soluciones, pasos, versiones anteriores, sanitización e imágenes privadas.

La Etapa 3 agrega el frontend responsive con React y Vite: árbol jerárquico filtrable, detalle de problemas con múltiples soluciones y pasos, formularios con contenido enriquecido, carga/vista ampliada de imágenes, navegación por URL y actualización del árbol después de cada cambio.

La Etapa 4 agrega login JWT, renovación y cierre de sesión, BCrypt, roles `ADMINISTRADOR`, `EDITOR` y `LECTOR`, administración de usuarios, auditoría y acceso autenticado a imágenes.

Los cambios posteriores incluyen control de concurrencia mediante `version`,
estados `BORRADOR`, `PUBLICADO` y `ARCHIVADO`, adjuntos PDF/DOCX, inicio de sesión
por nombre de usuario, contraseñas temporales, cambio de contraseña y correo
opcional. Los lectores solo consultan problemas publicados y archivos visibles.

## Iniciar la instalación Windows preparada

Desde la raíz del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/local/start-windows.ps1
```

Abrir **http://localhost:5173** e ingresar con un usuario del respaldo y su
contraseña habitual. La API escucha en `127.0.0.1:8080` y PostgreSQL en
`127.0.0.1:55432`. Los datos y la configuración están en `.local/`.

Para detener:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/local/stop-windows.ps1
```

Estos scripts usan la instalación ya preparada: no crean una instancia nueva ni
restauran datos. Ver [uso en Windows](docs/USO_WINDOWS.md) para recompilar,
consultar logs y conectarse a la base. Para Ubuntu, ver [uso local](docs/USO_LOCAL.md).

Las instrucciones manuales siguientes corresponden a una instalación nueva con
PostgreSQL en el puerto 5432; no son necesarias para iniciar la copia Windows.

## Requisitos locales

- Java 21
- PostgreSQL
- Node.js 22 o superior y npm (para el frontend)
- No es necesario instalar Maven: el proyecto incluye Maven Wrapper.

## Preparar PostgreSQL

Crear una base y un usuario. Los nombres siguientes coinciden con los valores predeterminados de desarrollo:

```sql
CREATE USER base_conocimiento WITH PASSWORD 'base_conocimiento';
CREATE DATABASE base_conocimiento OWNER base_conocimiento;
```

En un entorno real no se debe utilizar esa contraseña. Configurar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` como variables de entorno. El archivo `backend/.env.example` documenta los nombres, pero Spring Boot no lee automáticamente archivos `.env`.

## Ejecutar el backend

Desde PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd backend
$env:DB_URL='jdbc:postgresql://localhost:5432/base_conocimiento'
$env:DB_USERNAME='base_conocimiento'
$env:DB_PASSWORD='base_conocimiento'
.\mvnw.cmd spring-boot:run
```

Flyway crea las tablas al iniciar. La API queda disponible en `http://localhost:8080` y Swagger en `http://localhost:8080/swagger-ui.html`.

## Ejecutar las pruebas

Las pruebas usan bases H2 temporales en modo compatible con PostgreSQL. Una prueba específica ejecuta la migración Flyway real desde una base vacía:

```powershell
cd backend
.\mvnw.cmd clean verify
```

El build exige al menos 92 % de cobertura de líneas. El reporte se genera en `backend/target/site/jacoco/index.html`.

## Ejecutar el frontend (Etapa 3)

Con el backend iniciado, abrir otra terminal PowerShell:

```powershell
cd frontend
npm ci
npm run dev
```

Abrir `http://localhost:5173`. La URL del backend se puede cambiar copiando `.env.example` a `.env` y modificando `VITE_API_URL`.

Para verificar esta etapa:

```powershell
cd frontend
npm run lint
npm run test:coverage
npm run build
```

Vitest exige 92 % de líneas, sentencias y funciones y 80 % de ramas en los
módulos API, hooks y utilidades incluidos en `frontend/vite.config.js`. Genera
el reporte en `frontend/coverage/index.html`. También hay pruebas de componentes;
el porcentaje medido no representa la cobertura de toda la interfaz. Ejecutar
la suite para obtener los resultados actuales.

## Endpoints de la Etapa 2

- `/api/problemas`
- `/api/problemas/{id}`
- `/api/problemas/{id}/soluciones`
- `/api/soluciones/{id}`
- `/api/soluciones/{id}/pasos`
- `/api/pasos/{id}`
- `/api/problemas/{id}/imagenes`
- `/api/soluciones/{id}/imagenes`
- `/api/pasos/{id}/imagenes`
- `/api/archivos/{id}/contenido`
- `/api/problemas/{id}/adjuntos`
- `/api/soluciones/{id}/adjuntos`

Un problema puede tener cero, una o varias soluciones. Cuando se marca una nueva solución principal, la anterior deja automáticamente de serlo.

## Endpoints de la Etapa 1

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/areas` | Lista paginada; filtro opcional `activo` |
| POST | `/api/areas` | Crear área |
| GET | `/api/areas/{id}` | Consultar área |
| PUT | `/api/areas/{id}` | Modificar área |
| PATCH | `/api/areas/{id}/estado` | Activar o desactivar área |
| GET | `/api/areas/{id}/subareas` | Listar subáreas; filtro opcional `activo` |
| POST | `/api/areas/{id}/subareas` | Crear subárea |
| GET | `/api/subareas/{id}` | Consultar subárea |
| PUT | `/api/subareas/{id}` | Modificar subárea |
| PATCH | `/api/subareas/{id}/estado` | Activar o desactivar subárea |

Ejemplo para crear un área:

```json
{
  "nombre": "Rentas",
  "descripcion": "Gestión de ingresos municipales",
  "orden": 1
}
```

La seguridad por roles está implementada: las escrituras de áreas y subáreas
requieren `ADMINISTRADOR`. Las modificaciones de registros existentes exigen la
versión vigente; ver [contrato de concurrencia](docs/CONCURRENCIA.md).

## Roles de PostgreSQL

La aplicación usa roles de PostgreSQL para separar responsabilidades y reducir riesgos operativos.

| Rol | Uso | Permisos esperados |
|---|---|---|
| `base_conocimiento` | Usuario técnico usado por el backend. | Lectura y escritura sobre las tablas de la aplicación, uso de secuencias y migraciones Flyway. |
| `consultas_readonly` | Usuario para consultas desde DBeaver u otra herramienta. | Solo lectura (`SELECT`). No debe modificar datos. |
| `postgres` | Superusuario de administración. | Crear bases, crear roles, restaurar backups, cambiar dueños y permisos. No debe usarse para la aplicación. |

Flujo recomendado:

```text
Aplicación web -> base_conocimiento -> lectura/escritura controlada
DBeaver/consultas -> consultas_readonly -> solo lectura
Administración técnica -> postgres -> mantenimiento
```

Crear o actualizar roles en un servidor:

```bash
read -rsp 'Contraseña para usuario base_conocimiento: ' BC_PASS; echo

sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'base_conocimiento') THEN
    CREATE ROLE base_conocimiento WITH LOGIN PASSWORD '$BC_PASS';
  ELSE
    ALTER ROLE base_conocimiento WITH LOGIN PASSWORD '$BC_PASS';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'consultas_readonly') THEN
    CREATE ROLE consultas_readonly WITH LOGIN PASSWORD '$BC_PASS';
  ELSE
    ALTER ROLE consultas_readonly WITH LOGIN PASSWORD '$BC_PASS';
  END IF;
END
\$\$;
SQL

unset BC_PASS
```

En producción conviene usar contraseñas distintas para `base_conocimiento` y `consultas_readonly`. El ejemplo usa la misma solo para simplificar una primera instalación.

## Exportar base y archivos desde una PC local

La base de datos guarda los registros y las rutas relativas de imágenes/adjuntos. Los binarios se guardan en `STORAGE_PATH`, que en local suele ser `storage`. Para mover el sistema a otro servidor se deben exportar ambas cosas.

Exportar la base local:

```bash
cd /home/egil/Base_Conocimientos/backend
set -a
source .env.local
set +a

PGPASSWORD="$DB_PASSWORD" pg_dump \
  -h localhost \
  -p 5432 \
  -U "$DB_USERNAME" \
  -d base_conocimiento \
  -Fc \
  -f /tmp/base_conocimiento.dump
```

Exportar imágenes y adjuntos:

```bash
cd /home/egil/Base_Conocimientos
tar -czf /tmp/base_conocimiento_storage.tar.gz storage
```

Generar artefactos:

```bash
cd /home/egil/Base_Conocimientos/backend
bash mvnw -q -DskipTests package

cd /home/egil/Base_Conocimientos
VITE_API_URL=http://10.1.1.12/base-conocimiento VITE_BASE_PATH=/base-conocimiento/ npm --prefix frontend run build
```

Copiar al servidor:

```bash
scp /tmp/base_conocimiento.dump egil@10.1.1.12:/tmp/
scp /tmp/base_conocimiento_storage.tar.gz egil@10.1.1.12:/tmp/
scp /home/egil/Base_Conocimientos/backend/target/base_conocimiento-0.0.1-SNAPSHOT.jar egil@10.1.1.12:/tmp/base_conocimiento.jar
scp -r /home/egil/Base_Conocimientos/frontend/dist egil@10.1.1.12:/tmp/base_conocimiento_frontend
```

`VITE_API_URL` debe apuntar a la URL visible desde las PCs usuarias. Si se compila con `localhost`, el navegador intentará conectarse a la propia PC del usuario y mostrará `Network Error`.

## Importar en el servidor 10.1.1.12

Verificar que la base esté vacía:

```bash
sudo -u postgres psql -d base_conocimiento -c "SELECT count(*) AS tablas FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';"
```

Si los roles ya existen, actualizar contraseña con el bloque de la sección de roles. Luego restaurar:

```bash
sudo -u postgres pg_restore \
  -d base_conocimiento \
  --clean \
  --if-exists \
  /tmp/base_conocimiento.dump
```

Verificar datos y migraciones:

```bash
sudo -u postgres psql -d base_conocimiento -c "SELECT count(*) FROM usuarios;"
sudo -u postgres psql -d base_conocimiento -c "SELECT count(*) FROM problemas;"
sudo -u postgres psql -d base_conocimiento -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Importar storage:

```bash
sudo mkdir -p /var/lib/base_conocimiento
sudo tar -xzf /tmp/base_conocimiento_storage.tar.gz -C /var/lib/base_conocimiento
sudo chown -R egil:egil /var/lib/base_conocimiento
```

No renombrar carpetas internas del storage. La base guarda rutas relativas y deben coincidir con los nombres restaurados.

## Instalar backend como servicio systemd

Copiar artefactos:

```bash
sudo mkdir -p /opt/base_conocimiento/backend
sudo cp /tmp/base_conocimiento.jar /opt/base_conocimiento/backend/backend.jar
sudo chown -R egil:egil /opt/base_conocimiento
```

Crear `/opt/base_conocimiento/backend/.env`:

```env
DB_URL=jdbc:postgresql://localhost:5432/base_conocimiento
DB_USERNAME=base_conocimiento
DB_PASSWORD=CONTRASEÑA_REAL

SERVER_PORT=8081
SERVER_ADDRESS=127.0.0.1

STORAGE_PATH=/var/lib/base_conocimiento/storage

CORS_ALLOWED_ORIGINS=http://10.1.1.12

JWT_SECRET=SECRETO_LARGO_ALEATORIO
JWT_ACCESS_EXPIRATION=900
JWT_REFRESH_EXPIRATION=604800

SPRING_JPA_SHOW_SQL=false
LOGGING_LEVEL_ORG_HIBERNATE_SQL=INFO
```

Generar `JWT_SECRET` en la terminal del servidor:

```bash
openssl rand -hex 48
```

Pegar el resultado en `JWT_SECRET`. Este valor firma los tokens de sesión. Si cambia, los usuarios deberán iniciar sesión de nuevo.

Proteger el archivo:

```bash
sudo chown egil:egil /opt/base_conocimiento/backend/.env
sudo chmod 600 /opt/base_conocimiento/backend/.env
```

Probar manualmente:

```bash
cd /opt/base_conocimiento/backend
set -a
source .env
set +a
java -jar backend.jar
```

Debe verse `Tomcat started on port 8081`. Cortar con `Ctrl+C`.

Crear `/etc/systemd/system/base-conocimiento-backend.service`:

```ini
[Unit]
Description=Base de conocimiento - backend
After=network.target postgresql.service

[Service]
WorkingDirectory=/opt/base_conocimiento/backend
EnvironmentFile=/opt/base_conocimiento/backend/.env
ExecStart=/usr/bin/java -jar /opt/base_conocimiento/backend/backend.jar
Restart=on-failure
RestartSec=10
User=egil

[Install]
WantedBy=multi-user.target
```

Activar:

```bash
sudo systemctl daemon-reload
sudo systemctl enable base-conocimiento-backend
sudo systemctl start base-conocimiento-backend
sudo systemctl status base-conocimiento-backend --no-pager
curl -i http://localhost:8081/api/auth/me
```

El `curl` debe devolver `401`, porque el endpoint requiere autenticación.

## Publicar frontend con Nginx

Instalar/copiar frontend:

```bash
sudo mkdir -p /opt/base_conocimiento/frontend
sudo rm -rf /opt/base_conocimiento/frontend/*
sudo cp -r /tmp/base_conocimiento_frontend/* /opt/base_conocimiento/frontend/
sudo chown -R egil:egil /opt/base_conocimiento/frontend
```

Crear `/etc/nginx/sites-available/base-conocimiento`:

```nginx
server {
    listen 80;
    server_name 10.1.1.12;
    client_max_body_size 12m;

    index index.html;

    location = /base-conocimiento {
        return 301 /base-conocimiento/;
    }

    location /base-conocimiento/api/ {
        proxy_pass http://127.0.0.1:8081/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /base-conocimiento/.well-known/ {
        proxy_pass http://127.0.0.1:8081/.well-known/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /base-conocimiento/ {
        alias /opt/base_conocimiento/frontend/;
        try_files $uri $uri/ /base-conocimiento/index.html;
    }
}
```

Activar:

```bash
sudo mkdir -p /etc/nginx/sites-available /etc/nginx/sites-enabled
sudo ln -s /etc/nginx/sites-available/base-conocimiento /etc/nginx/sites-enabled/base-conocimiento
sudo nginx -t
sudo systemctl reload nginx
```

Verificar desde el servidor:

```bash
curl -I http://10.1.1.12/base-conocimiento/
curl -i http://10.1.1.12/base-conocimiento/api/auth/me
```

El primer comando debe devolver `200 OK`. El segundo debe devolver `401`.

Abrir desde una PC de la red:

```text
http://10.1.1.12/base-conocimiento/
```

Si el login muestra `Network Error`, revisar que el frontend haya sido compilado con:

```bash
VITE_API_URL=http://10.1.1.12/base-conocimiento VITE_BASE_PATH=/base-conocimiento/ npm --prefix frontend run build
```

### Si falla la carga de PDF o Word

El backend acepta archivos de hasta 10 MB y solicitudes multipart de hasta 11 MB. Nginx limita las solicitudes a 1 MB si no se configura `client_max_body_size`; en ese caso responde `413` antes de que la carga llegue al backend. En el bloque `server` que atiende `10.1.1.12`, añadir `client_max_body_size 12m;` como en el ejemplo anterior. Luego, en el servidor:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

No hace falta reiniciar Spring Boot ni volver a importar la base de datos. Si el navegador sigue mostrando el error, revisar en sus herramientas de desarrollador el código HTTP de la solicitud `POST .../adjuntos`.
