# Uso local en Windows

Configuración preparada el 12/11/2026 para esta computadora. Esta guía describe
el entorno existente; los scripts no son un instalador para otra computadora.

Esta instalación utiliza Java 21, Node.js 22 y PostgreSQL 17. La base del
proyecto funciona en una instancia independiente, limitada a `127.0.0.1:55432`.
No utiliza el servicio PostgreSQL existente en el puerto 5432.

Desde la raíz del proyecto, iniciar con:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/local/start-windows.ps1
```

Abrir http://localhost:5173. La API escucha en http://127.0.0.1:8080.
Los procesos se ejecutan sin ventanas adicionales. El inicio del backend puede
tardar; consultar `.local/backend.log` y `.local/backend-error.log`.

El mensaje del script indica las direcciones, pero no espera a que el backend
termine de arrancar. En esta instalación el primer arranque tardó cerca de
80 segundos. Esperar la línea `Started BaseConocimientoApplication` en el log.
La terminal usa Java 8 por defecto; el script selecciona explícitamente Java 21
según `javaHome` en la configuración local.

Detener los procesos del proyecto con:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/local/stop-windows.ps1
```

Los datos persistentes y la configuración local están en `.local/`:

- `postgres/`: clúster PostgreSQL del proyecto.
- `backend.jar`: copia ejecutable que no se borra al ejecutar `mvn clean`.
- `storage/`: copia de imágenes y documentos del respaldo.
- `windows-config.json`: contraseña técnica y secreto JWT; no compartir.
- `ia-config.json`: configuración opcional de Ollama, descrita en [IA_LOCAL.md](IA_LOCAL.md).
- `indice-config.json`: habilitación y límites del índice de conocimiento, descritos en [ARQUITECTURA_RAG.md](ARQUITECTURA_RAG.md).
- `documentos-config.json`: habilitación, rutas y límites de extracción documental.
- `tools/tesseract/`: OCR portable con español e inglés, preparado para esta computadora.
- `documentos-temporales/`: trabajos de extracción privados, eliminados al finalizar.
- `backend.log`, `frontend.log` y sus archivos de errores: diagnóstico.
- `postgres.log`: arranque y errores de la base.
- `backend.pid` y `frontend.pid`: procesos iniciados por el script.

`.local/` está excluida de Git. Su contenido persiste al detener el proyecto;
conserva allí tanto la base como los archivos cargados después de la restauración.

La base `base_conocimiento` fue preparada desde el respaldo externo
`E:\respaldo_base_conocimiento_20261112_165549`, conservando los usuarios y sus
contraseñas originales. La contraseña técnica de PostgreSQL no es la contraseña
de acceso a la aplicación. El respaldo externo permanece intacto.

## Ingreso y conexión a la base

El login utiliza **nombre de usuario**, no correo electrónico. Usar las
credenciales existentes del respaldo. No se cambiaron las contraseñas de esos
usuarios al instalar.

Para DBeaver u otra herramienta PostgreSQL:

| Campo | Valor |
|---|---|
| Host | `127.0.0.1` |
| Puerto | `55432` |
| Base | `base_conocimiento` |
| Usuario | `base_conocimiento` |
| Contraseña | Campo `dbPassword` de `.local/windows-config.json` |

Esta cuenta técnica tiene permisos de escritura. La instancia local no incluye
el usuario `consultas_readonly` descrito en el procedimiento de servidor.
La autenticación usa SCRAM y PostgreSQL escucha solo en la interfaz local.

## Diagnóstico

Desde la raíz:

```powershell
Get-Content .local/backend.log -Tail 40
Get-Content .local/backend-error.log -Tail 40
Get-Content .local/frontend-error.log -Tail 40
Get-Content .local/postgres.log -Tail 40
Test-NetConnection 127.0.0.1 -Port 55432
```

`http://localhost:5173` debe responder y mostrar el login. Una solicitud sin
token a `http://127.0.0.1:8080/api/auth/me` debe devolver **401**; eso confirma
que el endpoint está protegido. Swagger está disponible en desarrollo en
`http://127.0.0.1:8080/swagger-ui.html`.

Si falla el inicio, revisar que existan el JAR del backend, `frontend/node_modules`
y `.local/windows-config.json`. Si otro programa ocupa 8080 o 5173, el script
omite iniciar ese componente; comprobar el proceso antes de detenerlo. No
ejecutar simultáneamente los servidores manuales y los scripts de esta guía.

## Actualizar y recompilar

Para recompilar cambios del backend en una terminal PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd backend
.\mvnw.cmd -DskipTests package
```

Luego detener e iniciar nuevamente. Los scripts de inicio no recompilan.
La instancia local se inicia con el script; no se instaló como servicio de Windows.

## Extracción de documentos para IA

La etapa 3 agrega una API autenticada para PDF, DOCX y JPG/JPEG; todavía no agrega
la pantalla de chat. El OCR utiliza Tesseract local, independientemente de Ollama.
Ver [CHATBOT_MULTIMODAL.md](CHATBOT_MULTIMODAL.md) para el contrato HTTP, los límites
y las instrucciones de instalación en otra computadora.

El lanzador lee `.local/documentos-config.json` y admite únicamente las variables
documentales enumeradas en `backend/.env.example`. En esta instalación las rutas
se definen de forma absoluta para el ejecutable portable, los idiomas y la carpeta
temporal. Reiniciar el backend después de cambiar esa configuración; ejecutar el
lanzador sobre un backend ya iniciado no modifica su entorno.

Para verificar el OCR preparado, desde la raíz:

```powershell
& .local/tools/tesseract/tesseract.exe --version
& .local/tools/tesseract/tesseract.exe --list-langs
```

La lista debe incluir `spa` y `eng`. Para desactivar esta API, cambiar
`DOCUMENTS_ENABLED` a `false` en el JSON local y reiniciar. No es necesario
modificar ni restaurar la base de conocimientos.

## Índice de conocimiento para RAG

La etapa 4 incorpora las migraciones V11/V12 y un barrido en segundo plano de
fuentes publicadas y activas. La instalación local dispone de `embeddinggemma:300m`
y `.local/indice-config.json` habilita índice y embeddings. Ollama debe estar
abierto. La interfaz de chat y la búsqueda híbrida corresponden a etapas posteriores.

El índice inicial puede tardar según el catálogo, los adjuntos y la carga del
equipo. No bloquea el guardado normal. Un administrador puede consultar
`GET /api/ia/indice` con su token y solicitar reconstrucción o reintento; la guía
[ARQUITECTURA_RAG.md](ARQUITECTURA_RAG.md) explica contadores, límites y errores.
Antes de aplicar las nuevas migraciones se guardó una copia privada en
`.local/pre-indice-20261113_162031.dump`; el respaldo externo sigue intacto.

Para desactivar el barrido, cambiar `RAG_INDEX_ENABLED` a `false` en el JSON
local y reiniciar el backend. No borrar ni editar migraciones ya aplicadas.

Al iniciar el backend se copia el JAR compilado de `backend/target` a
`.local/backend.jar`. Si se ejecuta `clean`, el backend conserva esa copia.
Si no hay un JAR nuevo en `target`, el script utiliza la copia local existente.
Para aplicar cambios hay que compilar y luego reiniciar.

El frontend se ejecuta con Vite en modo desarrollo y toma los cambios del código.
Si cambian sus dependencias, detener el proyecto y ejecutar desde la raíz:

```powershell
npm --prefix frontend ci
npm --prefix frontend run build
```

Después iniciar nuevamente. `npm ci` puede tardar varios minutos en extraer el
paquete de iconos. El script sirve el código de desarrollo, no `frontend/dist`.

## Verificación de la instalación

El 12/11/2026 se completaron:

- restauración del dump y copia de los archivos;
- comprobación de hashes de los 249 archivos registrados;
- validación Flyway de V1 a V10 y propiedad de tablas;
- empaquetado del backend con `-DskipTests package`;
- instalación de dependencias con `npm ci` y compilación del frontend con Vite;
- HTTP 200 del frontend y HTTP 401 de `/api/auth/me` sin token.

Se corrigió una llamada antigua a `UsuarioRequest` en la prueba de concurrencia
que impedía compilar los tests. Las suites completas y la cobertura no se
ejecutaron durante esta instalación. Los comandos de control están en
[etapas y pruebas](ETAPAS_DESPLIEGUE_Y_PRUEBAS.md).

## Conservación de datos

El respaldo externo es una copia del 12/11/2026 y no incorpora cambios posteriores.
Para respaldar el entorno actual deben copiarse tanto la base mediante `pg_dump`
en el puerto 55432 como los archivos de `.local/storage`. No copiar el directorio
de datos PostgreSQL mientras está funcionando como reemplazo de un dump.
La instalación Windows no tiene copias automáticas configuradas; las tareas
systemd de [uso en Ubuntu](USO_LOCAL.md) solo corresponden a Linux.

## Habilitar las consultas RAG de la etapa 5

Con Ollama abierto, los modelos `granite4.2:3b` y `embeddinggemma:300m`
instalados y el índice de la etapa 4 preparado, crear o editar el archivo privado
`.local/rag-config.json`:

```json
{
  "RAG_ENABLED": "true",
  "RAG_SEMANTIC_ENABLED": "true"
}
```

Recompilar y reiniciar el backend con los scripts habituales para aplicar cambios.
El lanzador valida los nombres admitidos y el JSON tiene prioridad sobre sus
variables de entorno. `RAG_ENABLED=false` desactiva las consultas conservando
el CRUD y los datos. Configurar `RAG_SEMANTIC_ENABLED=false` permite buscar solo
por texto; generar una respuesta todavía requiere Ollama. El barrido y la
consulta usan flags independientes: pausar el barrido permite consultar el índice
existente, sujeto a las verificaciones de vigencia.

La API recibe un JWT de cualquiera de los tres roles y una pregunta textual en
`POST /api/rag/buscar` o `POST /api/rag/responder`. Por ejemplo, el cuerpo es
`{"pregunta":"¿Cómo corregir E17?"}`. La pantalla para interactuar con estas APIs
está disponible desde el botón **Chatbot** de la barra superior (etapa 6).
Consultar [ARQUITECTURA_RAG.md](ARQUITECTURA_RAG.md) para contratos y limitaciones.

## Usar la pantalla del chatbot: etapa 6

Iniciá sesión y presioná **Chatbot**. Escribí una descripción o usá **Extraer texto
de archivo** con PDF, DOCX o JPG/JPEG. Revisá el resultado del OCR, agregá el
fragmento relevante a la consulta y presioná **Consultar**. Si supera los 1000
caracteres, acortalo antes de enviar. Las fuentes permiten abrir el problema y
descargar el documento privado con tu sesión. Ver [CHATBOT.md](CHATBOT.md).

El frontend usa los mismos servicios locales ya preparados. En el entorno con
Vite, los cambios se cargan al actualizar el navegador; para una instalación de
producción, recompilar el frontend. No hace falta reiniciar PostgreSQL ni aplicar
migraciones. El historial se mantiene en memoria y no se guarda como conocimiento.
