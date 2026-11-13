# Etapas, despliegue local y pruebas

Este documento define cómo implementar, desplegar localmente y verificar cada etapa. La primera versión no utiliza Docker: PostgreSQL, Java y Node.js se ejecutan directamente en el equipo.

## Estado al 12/11/2026

Las etapas 1 a 4 están implementadas y el esquema llega a V10. La etapa 5
continúa pendiente: el filtro actual del árbol no es búsqueda de texto completo.
Las instrucciones por etapa describen la ejecución manual; para la instalación
Windows ya restaurada usar [USO_WINDOWS.md](USO_WINDOWS.md), con PostgreSQL en
el puerto 55432 y archivos en `.local/storage`.

Durante la instalación Windows se verificaron el empaquetado del backend con
Java 21, la compilación de producción del frontend, la validación de las diez
migraciones y las respuestas HTTP del frontend (200) y de `/api/auth/me` sin
sesión (401). No se ejecutaron las suites completas ni se midió nuevamente
la cobertura en esa instalación. Los resultados históricos de este documento
y de `CONCURRENCIA.md` no sustituyen una ejecución actual.

## Regla de calidad obligatoria

Para la integración de IA local se elevó el umbral operativo a 92 % de líneas.
Ver [PRUEBAS_IA.md](PRUEBAS_IA.md) para el alcance, los comandos Windows y las
pruebas de visibilidad. Las referencias al 91 % siguientes describen el mínimo
original de las etapas; la configuración vigente es la que exige 92 %.

Cada etapa debe finalizar con **más del 90 % de cobertura automatizada**. El mínimo operativo será 91 % para evitar interpretar 90 % exacto como suficiente.

La puerta automática se aplica a la cobertura de líneas. JaCoCo también reporta ramas para identificar decisiones que necesitan más escenarios. Se controlan:

- líneas, con mínimo obligatorio de 91 %;
- ramas de decisión, reportadas y cubiertas especialmente en reglas críticas;
- servicios y reglas de negocio;
- validaciones, autorización y manejo de errores;
- controladores mediante pruebas de integración;
- componentes y flujos críticos del frontend.

No se considerará terminada una etapa si las pruebas fallan o si la cobertura total queda en 90 % o menos. No se deben excluir clases con lógica de negocio para aumentar artificialmente el porcentaje. DTO puramente declarativos, clases de arranque y código generado podrán excluirse de manera explícita y justificada.

Comandos de control previstos:

```powershell
# Backend: tests, integración, reporte y umbral JaCoCo
cd backend
.\mvnw.cmd clean verify

# El reporte HTML quedará en:
# backend/target/site/jacoco/index.html

# Frontend, a partir de la Etapa 3
cd ..\frontend
npm ci
npm run test:coverage
```

JaCoCo ya está incorporado al backend y `mvn verify` falla automáticamente si la cobertura de líneas es menor a 91 %. Vitest y su proveedor V8 están incorporados al frontend con umbrales automáticos de 91 % para líneas, sentencias y funciones, y 80 % para ramas. La cobertura cuantitativa se aplica a API, estado y utilidades; los componentes críticos se verifican además mediante pruebas de integración visual.

## Preparación común del entorno

Requisitos:

- Java 21;
- PostgreSQL instalado y en ejecución;
- Node.js 22 o superior y npm desde la Etapa 3;
- PowerShell;
- puertos locales 8080, 5173 y 5432 disponibles.

Crear la base inicial desde `psql` con un usuario administrador de PostgreSQL:

```sql
CREATE USER base_conocimiento WITH PASSWORD 'base_conocimiento';
CREATE DATABASE base_conocimiento OWNER base_conocimiento;
```

Configurar la terminal que ejecutará el backend:

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/base_conocimiento'
$env:DB_USERNAME='base_conocimiento'
$env:DB_PASSWORD='base_conocimiento'
$env:CORS_ALLOWED_ORIGINS='http://localhost:5173'
```

Estas credenciales son exclusivamente locales. Producción deberá usar secretos externos al repositorio.

## Etapa 1 — Base backend, áreas y subáreas

Incluye configuración de Spring Boot, PostgreSQL, Flyway, perfiles, manejo de errores y CRUD de áreas y subáreas.

### Desplegar localmente

```powershell
cd backend
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

Alternativa usando el artefacto empaquetado:

```powershell
cd backend
.\mvnw.cmd clean package
java -jar target\base_conocimiento-0.0.1-SNAPSHOT.jar
```

### Probar y verificar

```powershell
cd backend
.\mvnw.cmd test
```

Verificaciones manuales:

En la versión actual estas operaciones requieren un token de administrador.
Definir `$headers = @{ Authorization = "Bearer $accessToken" }` con un token
obtenido al iniciar sesión y agregar `-Headers $headers` a cada solicitud.

```powershell
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/areas' `
  -ContentType 'application/json' `
  -Body '{"nombre":"Rentas","descripcion":"Gestión tributaria","orden":1}'

Invoke-RestMethod -Uri 'http://localhost:8080/api/areas?activo=true'

Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/areas/1/subareas' `
  -ContentType 'application/json' `
  -Body '{"nombre":"Catastro","descripcion":"Información catastral","orden":1}'
```

Swagger: `http://localhost:8080/swagger-ui.html`.

La etapa se acepta cuando Flyway crea el esquema, las operaciones persisten en PostgreSQL, los duplicados se rechazan y la cobertura supera el 90 %.

## Etapa 2 — Problemas, múltiples soluciones, pasos e imágenes

Incluye problemas, etiquetas, cero o varias soluciones por problema, solución principal opcional, pasos ordenados, sanitización, historial y almacenamiento protegido de imágenes.

### Desplegar localmente

```powershell
$env:STORAGE_PATH=(Resolve-Path '..\storage').Path
$env:MAX_IMAGE_SIZE='10485760'

cd backend
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Flyway aplicará las nuevas migraciones sin recrear la base. Antes de probar archivos debe existir la carpeta `storage`, pero nunca será expuesta como directorio público.

### Probar y verificar

```powershell
cd backend
.\mvnw.cmd clean verify
Start-Process '.\target\site\jacoco\index.html'
```

Se probarán especialmente:

- un problema con ninguna, una y varias soluciones;
- imposibilidad de tener dos soluciones principales activas;
- reordenamiento de pasos e imágenes;
- rechazo de HTML peligroso;
- rechazo de ejecutables, MIME falso y archivos demasiado grandes;
- baja lógica e historial recuperable;
- descarga de imágenes solamente mediante el endpoint protegido.

## Etapa 3 — Frontend React (realizada)

Incluye Vite, React Router, Axios, Material UI, TipTap, árbol jerárquico, formularios, detalle del problema y vista previa/ampliación de imágenes.

### Instalar y desplegar en desarrollo

En una terminal, ejecutar el backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

En otra terminal, ejecutar React:

```powershell
cd frontend
npm ci
npm run dev
```

Aplicación: `http://localhost:5173`.

### Probar y generar el build

```powershell
cd frontend
npm run lint
npm run test:coverage
npm run build
npm run preview
```

El build estático quedará en `frontend/dist`. En esta primera versión se seguirá ejecutando el frontend como proyecto independiente.

Se verificarán árbol responsive, selección resaltada, actualización tras modificaciones, formularios, errores comprensibles y visualización de múltiples soluciones.

La etapa implementada incluye:

- navegación responsive y ruta directa `/problemas/{id}`;
- filtro por área, subárea, título o etiqueta;
- problema con cero, una o varias soluciones y sus pasos;
- editor enriquecido TipTap y sanitización DOMPurify;
- alta/edición de problemas, alta de soluciones y primer paso opcional;
- carga de PNG, JPEG o WebP con texto alternativo y vista ampliada;
- actualización del árbol y del detalle después de guardar;
- mensajes de carga, éxito y error.

Resultado histórico de referencia de la etapa 3; no corresponde al total actual de pruebas:

```text
Test Files  4 passed (4)
Tests       10 passed (10)
Lines       100 % (43/43)
Statements  100 % (67/67)
Functions   100 % (34/34)
Branches    100 % (14/14)
```

El reporte navegable se abre con:

```powershell
Start-Process '.\coverage\index.html'
```

## Etapa 4 — Seguridad y auditoría (realizada)

Incluye Spring Security, BCrypt, JWT de acceso, refresh token rotativo y revocable, roles, permisos backend, CORS, auditoría y protección por propietario/visibilidad de archivos.

### Desplegar localmente

```powershell
$env:JWT_SECRET='reemplazar-por-un-secreto-local-largo-y-aleatorio'
$env:JWT_ACCESS_EXPIRATION='900'
$env:JWT_REFRESH_EXPIRATION='604800'
$env:ADMIN_INITIAL_PASSWORD='cambiar-inmediatamente'

cd backend
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

No se deben guardar esos valores en Git ni copiarlos a archivos versionados.

### Probar y verificar

```powershell
cd backend
.\mvnw.cmd clean verify
```

Las pruebas deberán cubrir como mínimo:

- login correcto e incorrecto;
- token vencido, alterado y ausente;
- renovación y revocación de sesión;
- matriz completa `ADMINISTRADOR`, `EDITOR` y `LECTOR`;
- prohibición backend aunque el cliente intente llamar directamente al endpoint;
- lector limitado a información publicada;
- auditoría de operaciones relevantes;
- acceso no autorizado a imágenes.

En una base nueva se inicializa el administrador configurado mediante
`ADMIN_INITIAL_USER`, `ADMIN_INITIAL_EMAIL` y `ADMIN_INITIAL_PASSWORD`. El login
actual utiliza nombre de usuario. Los valores de desarrollo son `admin`,
`admin@municipio.local` y `Admin123!`; deben reemplazarse para producción. El
perfil `prod` exige `JWT_SECRET` y `ADMIN_INITIAL_PASSWORD`. Una restauración
conserva los usuarios y contraseñas existentes; estas variables no son un
mecanismo para restablecer sus contraseñas.

Las ampliaciones posteriores incluyen adjuntos PDF/DOCX (V7), nombres de usuario
(V8), contraseñas temporales (V9) y correo opcional (V10). La cuenta permite
editar el perfil y cambiar la contraseña; el administrador puede restablecer
contraseñas de usuarios.

Matriz implementada:

- `ADMINISTRADOR`: usuarios, auditoría, áreas, subáreas y todo el conocimiento;
- `EDITOR`: crea y modifica problemas, soluciones, pasos e imágenes;
- `LECTOR`: sólo consulta problemas publicados y sus archivos visibles.

Endpoints agregados: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/me`, `/api/usuarios` y `/api/auditoria`.

## Etapa 5 — Búsqueda, endurecimiento y cierre

Incluirá búsqueda de texto completo de PostgreSQL, índices, fragmentos relevantes, paginación final, revisión de seguridad, datos de ejemplo y documentación definitiva.

### Despliegue de validación completo

```powershell
cd backend
.\mvnw.cmd clean verify
java -jar target\base_conocimiento-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

En otra terminal:

```powershell
cd frontend
npm ci
npm run lint
npm run test:coverage
npm run build
npm run preview
```

### Lista de cierre

- todas las migraciones se aplican sobre una base vacía;
- también se prueba una actualización desde la versión anterior;
- backend y frontend superan 90 % de cobertura;
- no existen secretos versionados;
- Swagger coincide con el comportamiento real;
- búsqueda respeta estado y permisos;
- los criterios de aceptación del prompt están cubiertos por pruebas o una verificación manual documentada;
- la instalación puede repetirse siguiendo únicamente el README.

## Limpieza del entorno local de pruebas

Detener backend y frontend con `Ctrl+C`. La base PostgreSQL se conserva entre ejecuciones. Si se necesita una base totalmente nueva, hacerlo de manera explícita con un usuario administrador:

```sql
DROP DATABASE base_conocimiento;
CREATE DATABASE base_conocimiento OWNER base_conocimiento;
```

Esta operación elimina todos los datos y solo debe utilizarse en un entorno local descartable.
