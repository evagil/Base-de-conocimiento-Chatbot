# Chatbot local: extracción de documentos (etapa 3 de 8)

La base de conocimientos necesita poder aprovechar manuales y capturas sin
enviarlos a servicios externos. Esta etapa incorpora la extracción de texto
previa al chatbot. El endpoint devuelve texto y referencias de página; todavía
no genera respuestas, no indexa archivos ni guarda conocimiento automáticamente.
La interfaz de la etapa 6 está descrita en [CHATBOT.md](CHATBOT.md).

## Arquitectura y decisiones

Se conserva el monolito Java 21/Spring Boot y la autenticación JWT existente.
`DocumentoController` delega en `ProcesadorDocumentos`, que crea una carpeta
privada, copia el archivo con límite y llama a `ExtractorAislado`. Un proceso
Java independiente ejecuta PDFBox **3.0.8** o Apache POI **6.0.0**. Las imágenes
normalizadas se pasan al ejecutable local de Tesseract. El resultado se limpia,
normaliza a Unicode NFC y devuelve antes de cerrar y borrar el trabajo temporal.
El worker funciona tanto desde Maven como desde el JAR ejecutable Spring Boot.

La configuración permite deshabilitar toda la extracción sin impedir el arranque
del resto del sistema. No agrega tablas, migraciones, servicios Python ni modelos
de IA. El esquema sigue en V10 y el almacenamiento histórico de adjuntos conserva
su funcionamiento. Los documentos consultados aquí no se copian al storage.

El equipo detectado tiene 8 GB de RAM y dos núcleos físicos: se admite un solo
trabajo documental por backend y Tesseract utiliza como máximo dos hilos OpenMP.
El procesamiento es local y no requiere Ollama. La configuración de modelos
`granite4.2:3b` y sus limitaciones están en [INSTALACION_MODELOS.md](INSTALACION_MODELOS.md).

## Contrato HTTP

`POST /api/documentos/extraer`, `multipart/form-data`, campo **archivo**.
Requiere un JWT válido de ADMINISTRADOR, EDITOR o LECTOR. Sin sesión devuelve
401; con la función desactivada devuelve 503 `DOCUMENTOS_DESHABILITADOS`.
El permiso de consultar un archivo propio no concede permiso de publicar
conocimiento ni de acceder a registros de otros roles.

Ejemplo desde PowerShell, usando un token de sesión propio sin guardarlo en Git:

```powershell
curl.exe --fail-with-body -H "Authorization: Bearer $env:BC_ACCESS_TOKEN" `
  -F "archivo=@C:/manuales/manual.pdf" http://127.0.0.1:8080/api/documentos/extraer
```

Respuesta de ejemplo:

```json
{
  "nombre": "manual.pdf",
  "tipoMime": "application/pdf",
  "paginas": 2,
  "secciones": [
    {"pagina": 1, "texto": "ERROR E17: reiniciar la aplicación.", "metodo": "TEXTO"},
    {"pagina": 2, "texto": "Verificar la conexión.", "metodo": "OCR"}
  ],
  "caracteres": 57,
  "ocrUsado": true
}
```

`caracteres` cuenta las unidades UTF-16 del texto
normalizado. Las respuestas satisfactorias indican `Cache-Control: no-store` y
no exponen rutas internas. Los errores utilizan el contrato uniforme
`codigo`/`mensaje` del backend, sin contenidos del archivo ni errores nativos.

## Formatos y referencias

| Entrada | Procesamiento | Referencia |
|---|---|---|
| PDF con texto | Extracción ordenada por posición, página por página | Página real, desde 1 |
| PDF escaneado | Páginas sin texto y con contenido se renderizan para OCR | Página real, desde 1 |
| DOCX | Párrafos, tablas, encabezados y texto accesible mediante POI | `paginas` y `pagina` nulos |
| JPG/JPEG | Verificación de formato y dimensiones antes de decodificar, normalización PNG y OCR | Página 1 |

La extensión debe coincidir con el contenido. Se rechazan PDF cifrados, macros,
archivos dañados, formatos distintos y nombres que incluyan rutas o controles.
El MIME enviado por el cliente no determina el formato. Se reutilizan los
detectores existentes de documentos e imágenes dentro del worker.

Los PDF con texto y capturas en una misma página conservan el texto extraíble:
no se ejecuta OCR adicional sobre las imágenes de esa página. Las páginas
totalmente vacías se omiten. Si no hay texto utilizable, se informa
`DOCUMENTO_SIN_TEXTO` (422). No se presenta un PDF escaneado como una extracción
exitosa vacía. DOCX no tiene paginación estable sin un motor de maquetación;
por eso no se inventan páginas. No se hace OCR de imágenes embebidas en DOCX.

TXT y texto del editor continuarán siendo entradas textuales de la futura
interfaz y del indexador; este endpoint binario solo admite PDF, DOCX y JPG/JPEG.
No se agregó visión generativa: Tesseract reconoce texto, no interpreta escenas.

## Límites y configuración

Los valores se declaran en `application.yml` y `backend/.env.example`:

| Variable | Predeterminado | Función |
|---|---|---|
| `DOCUMENTS_ENABLED` | `false` | Habilitar explícitamente la extracción |
| `DOCUMENT_TEMP_PATH` | `../.local/documentos-temporales` | Directorio dedicado, fuera del storage público |
| `DOCUMENT_TIMEOUT` | `60s` | Presupuesto total de procesamiento; máximo 120 s |
| `DOCUMENT_MAX_FILE_SIZE` | `10485760` | 10 MiB de entrada, también durante la copia |
| `DOCUMENT_MAX_PAGES` | `10` | Máximo de páginas PDF |
| `DOCUMENT_MAX_OCR_PAGES` | `3` | Máximo de páginas con OCR |
| `DOCUMENT_MAX_CHARS` | `40000` | Texto total; no se devuelve truncado |
| `DOCUMENT_MAX_PIXELS` | `6000000` | Píxeles por imagen normalizada o página renderizada |
| `DOCUMENT_MAX_TEMP_BYTES` | `33554432` | Presupuesto de archivos temporales por trabajo, 32 MiB |
| `DOCUMENT_RENDER_DPI` | `120` | Resolución para PDF escaneado |
| `TESSERACT_COMMAND` | `tesseract` | Ruta del ejecutable o comando disponible en PATH |
| `TESSERACT_DATA_PATH` | vacío | Carpeta de archivos `.traineddata`; vacío usa instalación nativa |
| `TESSERACT_LANGUAGES` | `spa+eng` | Idiomas instalados, máximo tres |

La solicitud multipart completa tiene un límite global de 11 MiB.
El umbral multipart también es de 11 MiB: la recepción inicial se mantiene
en memoria, dentro del límite de solicitud, antes de la copia al trabajo privado.
Esto evita temporales de carga en la carpeta global del contenedor y añade hasta
11 MiB de memoria por solicitud en recepción, incluidos los adjuntos existentes.
El worker
Java usa `-Xmx256m` y metaspace de 128 MiB; esos límites no equivalen a toda la
memoria residente del proceso. El timeout termina el proceso y sus descendientes.
Los límites de páginas, dimensiones, caracteres y escritura reducen el consumo
antes de devolver resultados. La expansión ZIP de DOCX tiene controles propios.

El presupuesto temporal limita las escrituras de imágenes propias y comprueba
el uso después del worker y del OCR. No es una cuota del sistema de archivos.
El proceso nativo OCR tiene límites de entrada, hilos y tiempo, pero no una
cuota de memoria impuesta por el sistema operativo. El worker corre con la
identidad del backend: es aislamiento de proceso, no un sandbox del sistema.

El entorno del proceso hijo conserva solo variables necesarias del sistema y
reubica TEMP/TMP en el trabajo privado: no hereda JWT, contraseñas de base ni
opciones Java arbitrarias. Los comandos se ejecutan mediante listas de argumentos,
sin un intérprete de shell. No se siguen vínculos externos de DOCX ni se ejecutan
macros, JavaScript de PDF o instrucciones extraídas de los documentos.

## Temporales, privacidad y errores

Cada operación usa una carpeta aleatoria `consulta-*`. El directorio dedicado
y los trabajos se restringen al propietario mediante ACL de Windows o permisos
0700 de POSIX. La respuesta no incluye rutas. No se registran cuerpos de archivos
ni texto reconocido y no se guardan datos en PostgreSQL.

El bloque de cierre elimina el trabajo tanto al acertar como al fallar. Un
limpiador se ejecuta cada minuto y retira trabajos inactivos de más de diez
minutos que hayan quedado por un cierre abrupto, hasta 100 entradas por pasada.
No sigue enlaces ni borra carpetas ajenas a su raíz. Si el sistema impide borrar
un archivo, se informa un error local y se reintenta mediante el limpiador:
no se garantiza eliminación inmediata ante fallos de disco o permisos.
Es borrado lógico del sistema de archivos, no borrado seguro de bloques.

| HTTP | Código | Acción sugerida |
|---|---|---|
| 400 | `ARCHIVO_VACIO`, `NOMBRE_ARCHIVO_INVALIDO` | Corregir entrada |
| 413 | `LIMITE_*`, `ARCHIVO_DEMASIADO_GRANDE` | Dividir el archivo o reducir tamaño |
| 415 | `FORMATO_INVALIDO` | Usar uno de los formatos admitidos |
| 422 | `DOCUMENTO_PROTEGIDO`, `DOCUMENTO_INVALIDO`, `DOCUMENTO_NO_PROCESABLE`, `DOCUMENTO_SIN_TEXTO` | Revisar el archivo o aportar una copia legible |
| 429 | `DOCUMENTO_OCUPADO` | Esperar a que termine la operación actual |
| 503 | `OCR_NO_DISPONIBLE`, `PROCESADOR_NO_DISPONIBLE` | Revisar instalación nativa/Java |
| 504 | `DOCUMENTO_TIMEOUT` | Consultar un documento más pequeño |
| 500 | `DOCUMENTO_ERROR_LOCAL` | Revisar almacenamiento y permisos |

## Instalación de OCR

En este equipo se preparó Tesseract portable **5.4.0.20240606** en
`.local/tools/tesseract`; no se modificaron el registro, el PATH global ni otras
instalaciones. El instalador Windows es el distribuido por UB Mannheim y enlazado
desde la [documentación de Tesseract](https://tesseract-ocr.github.io/tessdoc/Downloads.html):

- [Instalador Windows verificado](https://github.com/UB-Mannheim/tesseract/releases/download/v5.4.0.20240606/tesseract-ocr-w64-setup-5.4.0.20240606.exe).
- SHA-256: `C885FFF6998E0608BA4BB8AB51436E1C6775C2BAFC2559A19B423E18678B60C9`.
- Se extrajo con 7-Zip y se verificaron `tesseract.exe --version` y `--list-langs`.
- Español proviene de [tessdata_fast, revisión fijada](https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/87416418657359cb625c412a48b6e1d6d41c29bd/spa.traineddata).
  SHA-256: `6F2E04D02774A18F01BED44B1111F2CD7F3BA7AC9DC4373CD3F898A40EA6B464`.
- Idiomas disponibles: `spa`, `eng` y `osd`. La configuración usa `spa+eng`.

Para otra instalación Windows se puede usar el instalador de esa distribución
y configurar la ruta absoluta a `tesseract.exe` y `tessdata`. Verificar los hashes
de los binarios descargados antes de utilizarlos. Para Ubuntu:

```bash
sudo apt install tesseract-ocr tesseract-ocr-spa tesseract-ocr-eng
tesseract --version
tesseract --list-langs
```

Las dependencias Java provienen de Maven; consultar las versiones en las páginas
oficiales de [PDFBox](https://pdfbox.apache.org/download.html) y
[Apache POI](https://poi.apache.org/download.cgi). Los binarios nativos, idiomas,
documentos y configuración local no se publican en Git.

## Ejecución y reversión

En Windows, `scripts/local/start-windows.ps1` lee opcionalmente
`.local/documentos-config.json`, con una lista explícita de variables permitidas:

```json
{
  "DOCUMENTS_ENABLED": "true",
  "DOCUMENT_TEMP_PATH": "C:/ruta/al/proyecto/.local/documentos-temporales",
  "TESSERACT_COMMAND": "C:/ruta/al/proyecto/.local/tools/tesseract/tesseract.exe",
  "TESSERACT_DATA_PATH": "C:/ruta/al/proyecto/.local/tools/tesseract/tessdata",
  "TESSERACT_LANGUAGES": "spa+eng"
}
```

Sustituir las rutas por las reales y reiniciar el backend después de recompilar,
según [USO_WINDOWS.md](USO_WINDOWS.md). Los PDF con texto y DOCX no necesitan OCR;
si falta Tesseract, JPG y PDF escaneados fallan con un mensaje explícito.
El archivo JSON local tiene prioridad sobre el entorno del lanzador.

Para revertir, definir `DOCUMENTS_ENABLED=false` en el lugar desde donde se
configuró y reiniciar. También se pueden revertir los archivos de esta etapa y
recompilar. No es necesario restaurar bases de datos ni modificar el storage.

## Evaluación y trabajo pendiente

Las pruebas generan PDF, DOCX, imágenes y texto sintéticos. Cubren extracción,
referencias, cifrado, falsificación de formatos, límites, fallos de procesos,
timeout, concurrencia, permisos y limpieza. Las comprobaciones reales de OCR son
optativas y se habilitan con rutas explícitas; ver [PRUEBAS_IA.md](PRUEBAS_IA.md).

El OCR puede equivocarse; el texto reconocido se deberá mostrar para revisión
en la futura interfaz. Las pruebas sintéticas no demuestran precisión sobre
todos los manuales ni ausencia de contenido malicioso. En las próximas etapas
el texto documental se tratará como datos no confiables, nunca como instrucciones.
No se deben usar documentos ajenos sin autorización ni distribuir datos privados.

Esta etapa cubre preparación de datos y evaluación de factibilidad. La etapa 4
agrega el índice descrito en [ARQUITECTURA_RAG.md](ARQUITECTURA_RAG.md); la 5
agregará recuperación con fuentes, la 6 integrará el chatbot y
la 7 el agente textual de solo lectura. El agente no procesará imágenes, no
creará tickets y no modificará la base. Despliegue, monitoreo y evaluación
integral continúan en la etapa 8; no se entrenan modelos desde cero.
