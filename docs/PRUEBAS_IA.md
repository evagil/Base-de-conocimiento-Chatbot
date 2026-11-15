# Pruebas de la integración de IA local

## Etapa 1: permisos y base de verificación

Esta etapa prepara la aplicación existente; todavía no incorpora el chatbot,
el agente, OCR, embeddings ni llamadas de generación a Ollama.

La política `VisibilidadConocimiento` centraliza las reglas de lectura:

- LECTOR: problemas activos y publicados, soluciones activas de esos problemas
  y pasos activos de esas soluciones.
- ADMINISTRADOR y EDITOR: conservan el acceso existente a registros históricos.
- Los archivos inactivos no se descargan. Las descargas de lectores también
  verifican la publicación y la actividad de los registros relacionados.
- Las imágenes inactivas no se incluyen en los listados de lectores.
- Las rutas directas de soluciones y los listados por problema aplican permisos
  en el servidor; no dependen de filtros de la interfaz.

No se agregan migraciones ni se modifican estados, versiones o registros de la
base de uso. La visibilidad se hereda del estado del problema: las soluciones
no tienen un estado PUBLICADO independiente.

## Ejecutar backend en Windows

El arranque Windows actualizado utiliza `.local/backend.jar`, permitiendo
limpiar `backend/target` sin bloquear el archivo en uso. Si el backend fue
iniciado con el script anterior o manualmente desde `target`, detenerlo antes
de `clean`. Para detener el entorno preparado se puede utilizar el script habitual:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/local/stop-windows.ps1
```

Desde la raíz, ejecutar la suite completa con PostgreSQL de pruebas:

```powershell
powershell -ExecutionPolicy Bypass -File backend/scripts/test-postgres-concurrency.ps1
```

El script prepara una instancia exclusiva en `.local/postgres-tests`, puerto
55439, y usa únicamente `bc_concurrency_test`. No utiliza la configuración ni
los datos de la aplicación. Detiene el clúster si lo inició durante esa
ejecución. Los datos de prueba son descartables y no contienen el respaldo real.
Su autenticación local `trust` es exclusivamente para pruebas, con escucha en
127.0.0.1; no se debe cargar información privada en esa instancia.

Se pueden indicar `-PgBin`, `-JavaHome` y `-Port` si las rutas o el puerto difieren.
Si el clúster ya está iniciado, `-Port` debe coincidir con su puerto actual.

Las pruebas habituales H2 también pueden ejecutarse con Java 21:

```powershell
cd backend
.\mvnw.cmd clean verify
```

Sin `BC_TEST_DB_URL`, once casos PostgreSQL se omiten; H2 no sustituye la
verificación de bloqueos y del índice único parcial. El reporte de cobertura
está en `backend/target/site/jacoco/index.html`.

## Ejecutar frontend

Desde la raíz:

```powershell
npm --prefix frontend run lint
npm --prefix frontend run test:coverage
npm --prefix frontend run build
```

Vitest utiliza un trabajador para limitar la memoria necesaria. Las
importaciones de iconos apuntan a cada módulo concreto para evitar recorrer
todo el catálogo durante las pruebas.

Las importaciones de componentes Material UI también son directas. Las pruebas
disponen de hasta 60 segundos por caso para este equipo; las aserciones y los
umbrales de cobertura se mantienen. Se eliminó el límite particular de 15
segundos de una prueba para utilizar esta configuración común.

## Umbrales y alcance

El mínimo operativo de líneas se eleva a 92 %, para cumplir el requisito de
superar 91 %. JaCoCo mide el conjunto del backend. Vitest mantiene el alcance
existente de API, hooks y utilidades, con 92 % de líneas, sentencias y funciones
y 80 % de ramas. Este porcentaje del frontend **no representa cobertura total
de componentes**; la ampliación al código nuevo de interfaz se hará en las
etapas que incorporen las pantallas de IA.

Las pruebas nuevas verifican URLs directas, tres roles, anonimato, estados,
bajas lógicas, orden de pasos y descargas relacionadas. Las pruebas de seguridad
existentes usan rollback para no depender del orden de ejecución ni de cambios
de contraseña hechos por otra prueba.

Los contextos Spring de prueba mantienen como máximo dos entradas en caché y
el proceso de pruebas usa un heap máximo de 512 MB, conservando el agente de
JaCoCo. La suite de conocimiento utiliza su propia base H2 para evitar compartir
conexiones y restricciones con contextos cerrados de otras suites.

## Resultados del backend del 12/11/2026

`mvn clean verify` terminó correctamente con PostgreSQL de pruebas habilitado:

- 39 pruebas aprobadas, sin fallos ni omisiones; once corresponden a PostgreSQL.
- JaCoCo: 574 de 609 líneas, 94,25 %; ramas: 266 de 358, 74,30 %.
- Se superó el mínimo automático del 92 % de líneas.
- Se generó el JAR y el backend volvió a validar V1 a V10 al iniciar.

La comprobación se ejecutó con comandos equivalentes a los del script Windows;
el script además se revisó sintácticamente. Los logs locales de la ejecución
están en `.local/backend-verify.log`; no se publican junto con el código.

## Resultados del frontend del 12/11/2026

- ESLint aprobado: cero errores y tres advertencias preexistentes de Fast Refresh.
- Compilación de producción aprobada: 802 módulos transformados. Se mantiene
  la advertencia del tamaño del bundle; no se realizó división de código en esta etapa.
- Vitest: 14 archivos de prueba, 56 pruebas aprobadas.
- Cobertura del alcance configurado: líneas 100 % (117/117), sentencias 96,42 %,
  funciones 94,89 % y ramas 93,24 %. Se cumplen todos los umbrales.
- Las pruebas de componentes están incluidas en las 56 pruebas; no forman parte
  del porcentaje de cobertura indicado arriba.

La ejecución final usó un trabajador y duró aproximadamente seis minutos en
este equipo. Los resultados se guardan en `.local/frontend-coverage.log`,
`.local/frontend-lint.log` y `.local/frontend-build.log`; el reporte HTML está
en `frontend/coverage/index.html`.

El entorno quedó iniciado con el backend actualizado. Se verificó HTTP 200 del
frontend y HTTP 401 de `/api/auth/me` sin sesión. Ollama respondió en el puerto
11434 y enumeró los tres modelos locales existentes; no se descargaron modelos
ni se ejecutó generación de IA en esta etapa.

## Reversión

Revertir los cambios de esta etapa y recompilar el backend. No requiere revertir
migraciones ni restaurar la base. El entorno de pruebas permanece separado.

Para volver a iniciar la aplicación:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/local/start-windows.ps1
```

## Etapa 2: cliente Ollama y evaluación local

Los tests de `ia/` usan un servidor HTTP efímero en loopback, respuestas simuladas
y entradas sintéticas. Verifican conversación, JSON estructurado, embeddings,
validación de configuración, diagnóstico, errores HTTP, modelo ausente,
metadatos remotos, variante Thinking obligatoria, JSON inválido, vectores inválidos,
límites, saturación, cancelación e interrupción. Un caso envía cabeceras y retrasa
el cuerpo para comprobar que el timeout también limita su recepción completa.

La matriz de seguridad incluye el endpoint administrativo `/api/ia/estado`:
anónimo 401, lector/editor 403 y administrador 200 con IA deshabilitada, sin Ollama.

La prueba `OllamaLocalSmokeTest` es optativa y no usa Spring ni la base de datos.
Solo se habilita definiendo una URL local explícita:

```powershell
$env:BC_TEST_OLLAMA_URL = 'http://127.0.0.1:11434'
powershell -ExecutionPolicy Bypass -File backend/scripts/test-postgres-concurrency.ps1
Remove-Item Env:BC_TEST_OLLAMA_URL
```

Con esa variable, la verificación completa incluye una consulta real de texto
sintético a `granite4.2:3b` utilizando el cliente Java. Sin ella se omite únicamente
esa comprobación real; las pruebas normales de IA no requieren un servidor Ollama.
No se descargan modelos durante los tests.

Los ensayos de elección de herramientas solo observaron respuestas del modelo;
no ejecutaron herramientas ni modificaron conocimiento. La prueba con esquema JSON
seleccionó el nombre correcto pero generó un argumento semánticamente incorrecto.
Se registra como limitación, no como validación satisfactoria del agente.

Para revertir esta etapa, configurar `IA_ENABLED=false` y reiniciar el backend.
También se pueden revertir los cambios de código y recompilar; no hay migraciones
ni datos de conocimiento que revertir. Si se habilitó desde `.local/ia-config.json`,
modificar ese archivo: sus valores tienen prioridad durante el arranque Windows.

### Resultados de la etapa 2, 12/11/2026

- Se ejecutó el script Windows completo con PostgreSQL de pruebas y
  `BC_TEST_OLLAMA_URL` explícita: `mvn clean verify` aprobado.
- Backend: 60 pruebas aprobadas, sin fallos ni omisiones. Incluye los once casos
  PostgreSQL, 19 tests simulados de IA y una prueba real con Ollama.
- JaCoCo: 780/816 líneas, 95,59 %; 478/576 ramas, 82,99 %.
- El cliente Java consultó realmente `granite4.2:3b`: respuesta final de 21 tokens
  en 40,86 segundos. No se usaron datos del respaldo para esa consulta.
- Frontend: ESLint sin errores y las tres advertencias preexistentes; 56 pruebas
  aprobadas y build aprobado. Cobertura del alcance configurado: líneas 100 %,
  sentencias 96,42 %, funciones 94,89 % y ramas 93,24 %.
- Se corrigió y verificó el arranque de PostgreSQL del script al redirigir la
  salida: `pg_ctl` utiliza archivos de control separados y se espera únicamente
  su proceso inicial. El clúster de pruebas se detuvo al terminar la suite.

Logs y evidencia locales: `.local/ia-backend-verify.log`,
`.local/ia-frontend-lint.log`, `.local/ia-frontend-coverage.log` y
`.local/ia-frontend-build.log`. No están incluidos en Git.

Estos resultados validan la conexión y sus controles, no un agente completo ni
la precisión final del RAG. La selección de herramientas y argumentos mantiene
las limitaciones documentadas en [INSTALACION_MODELOS.md](INSTALACION_MODELOS.md).

## Etapa 3: extracción documental y OCR

Las pruebas de `documentos/` construyen archivos sintéticos: PDF con texto,
escaneados, cifrados, vacíos y dañados; DOCX con tabla e hipervínculo externo;
JPG/JPEG y una imagen PNG con extensión falsa. Cubren texto y páginas, límites,
validación, errores uniformes, falta de motor OCR, concurrencia, proceso hijo,
entorno filtrado, timeout, interrupción y limpieza de archivos temporales.
La prueba DOCX extrae la etiqueta del hipervínculo sin consultar su dirección.

La ejecución normal no requiere Tesseract. `DocumentosRealIntegrationTest` se
habilita únicamente mediante `BC_TEST_TESSERACT`: usa JWT reales en una base H2
aislada y verifica acceso de los tres roles, anónimo 401, extracción PDF/DOCX,
OCR nativo de JPG y PDF escaneado, ausencia de persistencia de problemas y
limpieza posterior a aciertos y errores. No usa usuarios ni documentos del respaldo.

Para reproducir la verificación completa con PostgreSQL de pruebas, OCR preparado
y la prueba de Ollama de la etapa anterior, desde la raíz del proyecto:

```powershell
$env:BC_TEST_TESSERACT = Join-Path $PWD '.local/tools/tesseract/tesseract.exe'
$env:BC_TEST_TESSDATA = Join-Path $PWD '.local/tools/tesseract/tessdata'
$env:BC_TEST_OLLAMA_URL = 'http://127.0.0.1:11434'
try {
    powershell -ExecutionPolicy Bypass -File backend/scripts/test-postgres-concurrency.ps1
    if ($LASTEXITCODE -ne 0) { throw 'Falló la verificación del backend.' }
} finally {
    Remove-Item Env:BC_TEST_TESSERACT, Env:BC_TEST_TESSDATA, Env:BC_TEST_OLLAMA_URL -ErrorAction SilentlyContinue
}
```

Ollama debe estar abierto y `granite4.2:3b` instalado. Las pruebas no descargan
modelos ni instalan Tesseract. Si no se definen esas variables, las comprobaciones
nativas optativas se omiten; los casos simulados siguen ejecutándose.
Los umbrales de cobertura no cambian y no se excluye el nuevo código documental.
En equipos con 8 GB conviene ejecutar backend y frontend por separado, con
un trabajador de Vitest, para evitar que la paginación de memoria retrase las
pruebas. No ejecutar varias compilaciones ni cargar otros modelos durante
la comprobación de Ollama.

Logs locales de esta etapa: `.local/documentos-backend-verify.log`,
`.local/documentos-unit.log`, `.local/documentos-frontend-lint.log`,
`.local/documentos-frontend-coverage.log` y `.local/documentos-frontend-build.log`.
Los reportes se regeneran en `backend/target/site/jacoco/` y `frontend/coverage/`.
Los binarios y reportes no se incluyen en Git.

### Resultados de la etapa 3, 13/11/2026

- Backend: `mvn clean verify` aprobado, 87 pruebas sin fallos ni omisiones.
  Incluye las 27 pruebas documentales, los once casos PostgreSQL y la consulta
  nativa de Ollama. No se cambiaron umbrales ni exclusiones de cobertura.
- JaCoCo: 1102/1146 líneas (96,16 %) y 727/842 ramas (86,34 %).
- La integración documental ejecutó OCR real con Tesseract 5.4.0.20240606 sobre
  JPG y PDF escaneado; ambos reconocieron `E17` y `REINICIAR`. PDF y DOCX se
  verificaron mediante la API y los tres roles accedieron a la extracción de PDF.
- La carpeta temporal de esa integración quedó vacía. Se verificó también
  limpieza de errores, trabajos vencidos y falta de acceso anónimo.
- El worker del JAR Spring Boot final extrajo un PDF sintético mediante
  `PropertiesLauncher`, además de las comprobaciones desde el classpath de tests.
- El cliente Java consultó `granite4.2:3b`: respuesta de 21 tokens en 42,49 s.
  La instancia PostgreSQL de pruebas se detuvo al finalizar.
- Frontend: 14 archivos y 56 pruebas aprobadas, en unos tres minutos al ejecutar
  por separado con un trabajador y `NODE_OPTIONS=--max-old-space-size=1536`.
  Cobertura del alcance configurado (API, hooks y utilidades): líneas 100 %
  (117/117), sentencias 96,42 %, funciones 94,89 % y ramas 93,24 %.
- ESLint sin errores y con las tres advertencias preexistentes de Fast Refresh.
  Build de producción aprobado; permanece la advertencia de bundle mayor a
  500 kB. No se modificó código del frontend en esta etapa.
- Entorno Windows iniciado con el JAR final verificado por SHA-256, OCR local
  habilitado y configuración privada fuera de Git. Frontend HTTP 200;
  `/api/auth/me` y `POST /api/documentos/extraer` sin token devuelven 401.
  Flyway validó las diez migraciones y confirmó que no había cambios pendientes.

La primera ejecución completa detectó Ollama cerrado: 86 pruebas aprobadas y
un error de conexión en la prueba optativa. Se inició el servidor local y se
repitió la suite completa con el resultado satisfactorio indicado arriba.
La primera prueba del frontend se interrumpió manualmente antes de obtener
resultados por presión de memoria; se repitió por separado después del backend.
Los primeros intentos se conservan únicamente en logs locales `*-primer-intento.log`.
El ensayo manual del JAR conserva un PDF sintético, su configuración y resultado
en `.local/documentos-jar-smoke/`; no contiene documentos del respaldo. Los
trabajos transitorios de la integración HTTP sí quedaron eliminados.

## Etapa 4: índice derivado de conocimiento

Las pruebas de `rag/` cubren normalización Unicode, fragmentos acotados y
solapamiento, metadatos, etiquetas, soluciones y pasos, cambios de contenido,
exclusión de registros inactivos y no publicados, renombrado de padres,
extracción de adjuntos con referencias de página y permisos de lectura vigentes.
También prueban vectores inválidos, límites, exclusión mutua mediante lease,
reconstrucción que invalida trabajo antiguo, cambios durante inferencia, fallos
con reintentos limitados, recuperación administrativa y modo textual sin Ollama.

`IndiceIntegrationTest` usa H2 independiente y dobles de Ollama y extracción;
`LectorAdjuntoIndiceTest` verifica límites de lectura, confinamiento al storage,
hash y contrato de entrada al procesador documental sin modificar el original.
Los endpoints administrativos se verifican con MockMvc y la matriz JWT existente.

`IndicePostgresTest` se habilita con `BC_TEST_DB_URL`. Crea un esquema efímero
aleatorio en la instancia de pruebas, aplica primero V1–V10, inserta conocimiento
sintético y migra a V11/V12 comprobando que se conserva. Verifica GIN español,
vista de fuentes vigentes, rollback de publicación y lease compartido. Al cerrar
elimina únicamente ese esquema generado, nunca el esquema público.

`EmbeddingsLocalSmokeTest` se habilita con `BC_TEST_OLLAMA_URL`, igual que la
prueba de conversación anterior. Llama a `embeddinggemma:300m` con dos textos
sintéticos y verifica vectores finitos de 768 dimensiones. No es una evaluación
de precisión de recuperación: esa medición corresponde a la etapa 5.

Para ejecutar todas las comprobaciones usar el comando completo de la etapa 3
con sus tres variables BC_TEST. Abrir Ollama y verificar antes que estén instalados
`granite4.2:3b` y `embeddinggemma:300m`. Las pruebas no descargan modelos ni usan
documentos del respaldo. Ejecutar después los tres comandos del frontend por
separado para limitar el uso de RAM.

Logs locales de esta etapa: `.local/indice-backend-verify.log`,
`.local/indice-unit.log`, `.local/indice-frontend-lint.log`,
`.local/indice-frontend-coverage.log` y `.local/indice-frontend-build.log`.

Las pruebas positivas que inician una JVM documental real usan hasta 120 s para
absorber el arranque en frío y la instrumentación en equipos con disco y RAM
limitados. La aplicación conserva su valor predeterminado de 60 s. Las pruebas
negativas de timeout siguen usando presupuestos cortos y verifican la terminación
del proceso; no se desactiva ningún límite ni se reduce cobertura.

### Resultados de la etapa 4, 13/11/2026

- `mvn clean verify` aprobado: 103 pruebas, sin fallos, errores ni omisiones.
  Incluye 16 pruebas nuevas del módulo RAG, los once casos anteriores de
  concurrencia PostgreSQL, OCR real y ambas comprobaciones nativas de Ollama.
- JaCoCo: 1274/1322 líneas (96,37 %) y 903/1046 ramas (86,33 %). Se mantuvo
  el umbral de 92 % y no se excluyeron clases para elevar la cobertura.
- La prueba PostgreSQL del índice conservó el conocimiento de una base en V10
  al migrar a V11/V12, verificó GIN español, rollback, vista vigente y lease.
  Se corrigió el tipado explícito `CAST(NULL AS BIGINT)` de relaciones opcionales
  en el UNION de adjuntos, detectado por PostgreSQL y no por H2.
- Las nueve pruebas de integración del indexador pasaron con sus controles de
  cambios, bajas, límites, vectores inválidos, fallos y reintentos.
- `embeddinggemma:300m` produjo vectores de 768 dimensiones para dos textos
  sintéticos en 28,82 s. `granite4.2:3b` respondió 21 tokens en 93,20 s.
  Son mediciones con instrumentación y carga del escritorio, no una promesa
  de latencia ni una evaluación de precisión del futuro RAG.
- La corrida final duró aproximadamente 24 minutos y detuvo la instancia
  PostgreSQL de pruebas al terminar. La base restaurada no se usó en los tests.
- Frontend: 14 archivos y 56 pruebas aprobadas. Cobertura del alcance configurado:
  líneas 100 % (117/117), sentencias 96,42 %, funciones 94,89 % y ramas 93,24 %.
  Se ejecutó por separado con un trabajador y heap Node de 1536 MiB; la corrida
  final duró unos nueve minutos en este equipo.
- ESLint sin errores y con tres advertencias preexistentes de Fast Refresh;
  compilación de producción aprobada. Continúa la advertencia del tamaño del
  bundle y los avisos previos de componentes/extensiones en el entorno de tests.
- Entorno Windows actualizado con el JAR verificado por SHA-256. Flyway aplicó
  V11/V12 sobre la instancia local, después de crear y comprobar el catálogo del
  respaldo privado `.local/pre-indice-20261113_162031.dump`.
- Frontend HTTP 200; `/api/auth/me` y `/api/ia/indice` sin token devuelven 401.
  Se confirmó la generación efectiva de fragmentos y vectores locales de 768
  dimensiones con `embeddinggemma:300m`. La primera comprobación no registró
  fallos del índice. El barrido inicial continúa en segundo plano: no se afirma
  que todo el catálogo esté procesado al iniciar la aplicación.

La primera corrida completa registró dos timeouts de pruebas positivas que
inician JVM y fallos de restricciones en una base H2 compartida, además del UNION
de PostgreSQL. Se ajustaron solo los presupuestos positivos de las pruebas nativas,
se dio una base H2 propia a la matriz de seguridad y se corrigió el SQL. La
verificación final completa pasó con los resultados indicados arriba. La evidencia
inicial queda en `.local/indice-backend-primer-intento.log` y la comprobación
aislada de seguridad en `.local/indice-seguridad.log`.

La primera corrida del frontend se interrumpió tras un fallo intermitente del
flujo de edición/creación. El caso pasó aislado con y sin instrumentación; se
hicieron explícitas las esperas de apertura y cierre de diálogos en `App.test.jsx`,
conservando las mismas comprobaciones. Después se repitieron lint, las 56 pruebas
con cobertura completa y build satisfactoriamente. Los diagnósticos de un único
caso no representan la cobertura global; sus omisiones intencionales y cobertura
parcial no forman parte del resultado final. No se cambiaron los umbrales.

## Etapa 5: búsqueda híbrida y respuestas con fuentes

La suite agrega pruebas de normalización y códigos completos, coseno numéricamente
estable, fusión RRF, límites de candidatos, vectores corruptos, turnos concurrentes,
fallos de proveedor, presupuestos de entrada y contexto, JSON inválido y citas
inventadas. Una cita que aparece en la fuente pero quedó fuera del contexto
enviado también se rechaza. La respuesta no acepta URLs ni campos extra del modelo.

`ConsultaRagIntegrationTest` utiliza una base H2 propia y fuentes sintéticas
confirmadas en transacciones independientes. Integra indexador, repositorio,
búsqueda y generación con un doble de Ollama. Verifica referencias a soluciones,
abstención sin evidencia, diferencias entre códigos, bajas y cambios de nombres
de padres antes de reindexar, además de modificaciones durante la generación.
`IndiceIntegrationTest` comprueba la compatibilidad de huellas al pausar el barrido.

`IndicePostgresTest` prueba además el repositorio de consulta sobre PostgreSQL
real: stemming español, ranking, parametrización, recorrido y exclusión inmediata
de registros archivados. `SecurityIntegrationTest` comprueba 401 anónimo y acceso
de los tres roles a ambas rutas de consulta; la administración del índice conserva
su restricción. `ConsultaRagControllerTest` verifica contratos, validación y no-store;
`FiltroConsultaRagTest` prueba el límite de 16 KiB con y sin tamaño declarado.

`RespuestaRagLocalSmokeTest` se habilita con `BC_TEST_OLLAMA_URL`. Comprueba que
`granite4.2:3b` selecciona evidencia sintética y que el servicio acepta una cita
literal con referencia construida por el backend. Usa recuperación y vigencia
simuladas: no representa una prueba de precisión sobre el corpus privado ni un
ensayo HTTP completo contra producción. No se reducen las validaciones si el
modelo devuelve una cita inventada. Las pruebas nativas anteriores de embeddings,
conversación y OCR también permanecen en la suite completa.

Ejecutar el comando completo de etapa 3 con las tres variables BC_TEST y luego
lint, cobertura y build del frontend de forma secuencial. Los logs de esta etapa
son `.local/rag-targeted.log`, `.local/rag-backend-verify.log` y
`.local/rag-frontend-{lint,coverage,build}.log`. La instancia restaurada y su
storage no se usan como datos de prueba. La API permanece desactivada por defecto;
la configuración privada Windows permite activarla para uso local.

### Resultados del backend de la etapa 5, 13/11/2026

- `mvn clean verify` aprobado: 130 pruebas, sin fallos, errores ni omisiones;
  27 casos adicionales respecto de la etapa 4. Incluye PostgreSQL real, OCR y
  tres comprobaciones nativas de Ollama. Duración aproximada: 7 min 42 s.
- JaCoCo: 1482/1528 líneas (96,99 %) y 1133/1298 ramas (87,29 %), con el umbral
  de líneas en 92 % y sin nuevas exclusiones.
- PostgreSQL verificó stemming español, ranking y lectura de fuentes vigentes
  mediante el repositorio utilizado por la API, además de las comprobaciones
  previas de migraciones, concurrencia, rollback y lease.
- `granite4.2:3b` pasó la selección de evidencia del servicio RAG con una fuente
  sintética en 51,82 s. La comprobación de conversación anterior tardó 29,95 s;
  embeddings produjo vectores de 768 dimensiones en 6,93 s. Son mediciones de
  estas pruebas, no garantías de latencia ni de precisión sobre el corpus real.
- La instancia PostgreSQL de pruebas se detuvo al terminar. Se conserva V12
  como última migración y no se usaron datos del respaldo en las pruebas.
- Frontend: 14 archivos y 56 pruebas aprobadas en aproximadamente tres minutos.
  Cobertura del alcance configurado: líneas 117/117 (100 %), sentencias 96,42 %,
  funciones 94,89 % y ramas 93,24 %. Se mantuvieron los umbrales existentes.
- ESLint sin errores y con las tres advertencias previas de Fast Refresh.
  Build de producción aprobado; conserva el aviso de bundle superior a 500 kB.
- Backend local reiniciado con el JAR comprobado por SHA-256 y RAG activado
  mediante configuración privada. Flyway confirmó V12 sin migraciones pendientes.
  Frontend HTTP 200; autenticación, administración del índice y ambas rutas RAG
  respondieron 401 sin token. Los casos positivos autenticados se verificaron
  mediante las pruebas automatizadas; no se usaron credenciales de usuarios reales
  para hacer consultas al corpus restaurado durante esta comprobación de arranque.

## Etapa 6: interfaz del chatbot

Las pruebas nuevas cubren el transporte con JWT compartido, timeouts y señales
de cancelación, límites de archivo y pregunta, revisión de extracción sin recortes,
historial acotado sin persistencia, borrador conservado ante fallos y descarte de
resultados tardíos tras cancelar o desmontar. Se verifica que solo se envía la
pregunta del turno, sin el historial como contexto autorizado.

`ChatbotDialog.test.jsx` verifica respuesta con fuente, advertencia de búsqueda
acotada, renderizado como texto de contenido que parece HTML, navegación por IDs
en lugar de URLs arbitrarias, revisión del OCR, consulta sin evidencia, error de
Ollama, cancelación y descarga como blob por la API autenticada. `App.test.jsx`
incluye apertura y cierre desde el espacio autenticado. `httpClient.test.js`
comprueba que un 413 estructurado de RAG conserva su mensaje específico.

No se modificó el backend en esta etapa; su última verificación completa es la
de etapa 5: 130 casos y 96,99 % de líneas. La comprobación de interfaz usa dobles
HTTP sintéticos, no credenciales ni documentos de usuarios reales. No equivale
a una evaluación de relevancia sobre el catálogo privado. Las reglas de permisos,
OCR real y selección nativa de evidencia permanecen cubiertas por esa suite backend.

Ejecutar los tres comandos habituales del frontend: `npm run lint`,
`npm run test:coverage -- --maxWorkers=1` y `npm run build`.
Los logs de la verificación final quedan en `.local/chatbot-frontend-lint.log`,
`.local/chatbot-frontend-coverage.log` y `.local/chatbot-frontend-build.log`.
Las pruebas seleccionadas de los seis archivos relacionados pasaron 26 casos
antes de iniciar la corrida final completa.

### Resultados de la etapa 6, 13/11/2026

- Frontend: 18 archivos y 70 pruebas aprobadas, sin fallos ni omisiones;
  14 casos adicionales respecto de etapa 5. La corrida con cobertura duró
  aproximadamente 3 min 26 s, con un trabajador y heap Node de 1536 MiB.
- Cobertura del alcance configurado (API, hooks y utilidades): líneas 168/168
  (100 %), sentencias 97,51 %, funciones 95,93 % y ramas 95,41 %. Se conservaron
  los umbrales y las inclusiones existentes. La UI se comprueba mediante pruebas
  de comportamiento y no forma parte de ese porcentaje de cobertura.
- ESLint sin errores, con las tres advertencias anteriores de Fast Refresh.
  Build de producción aprobado; se conserva el aviso de bundle mayor a 500 kB.
- La aplicación local sirve HTTP 200 y el módulo del chatbot actualizado.
  Los servicios de respuesta y extracción continúan requiriendo autenticación.
  No se reiniciaron ni modificaron los datos de PostgreSQL para incorporar la UI.
- La etapa 5 quedó publicada en `develop` mediante el commit `f702471`.
  La etapa 6 se publicó después en `develop` mediante el commit `8eca399`,
  antes de comenzar la etapa 7.

## Etapa 7: agente textual con herramientas

`AgenteConsultaTest` comprueba selección de herramientas con un doble del modelo,
IDs autorizados por ejecución, esquema estricto, rechazo de acciones arbitrarias,
repeticiones, concurrencia, límites de iteraciones, tiempo, contexto y fuentes,
abstención y cambios de solución/pasos antes de responder. No se admiten argumentos
SQL ni herramientas adicionales. La respuesta se compone con evidencia leída y
no con prosa libre del modelo.

`AgenteControllerTest` verifica no-store y entrada exclusivamente JSON con el campo
textual pregunta: rechaza multipart y campos de imagen o herramientas. La matriz
de seguridad comprueba 401 anónimo y acceso autorizado de los tres roles, con
función deshabilitada. El filtro de 16 KiB también cubre la nueva ruta RAG.

`ConsultaRagIntegrationTest` agrega un caso con H2 propio que recorre búsqueda,
solución principal, pasos ordenados y exclusión al archivar. `IndicePostgresTest`
verifica las nuevas consultas de herramientas en PostgreSQL real: prioridad de
la solución principal, orden de pasos y retirada de fuentes archivadas.

`AgenteLocalSmokeTest`, habilitado mediante `BC_TEST_OLLAMA_URL`, utiliza Granite
real y herramientas sintéticas. Exige que el modelo seleccione buscar, leer
soluciones y leer pasos antes de finalizar con una referencia válida. No se
transforma una abstención en éxito ni se fuerza la secuencia desde Java.
La primera corrida ejecutó las tres herramientas pero terminó absteniéndose:
el contexto solo conservaba el último resultado de pasos. Se agregó un resumen
acotado de las soluciones leídas y sus pasos consultados; la segunda corrida
volvió a abstenerse en la cuarta decisión. Se simplificó el cierre: pedir los
pasos de un ID autorizado expresa la elección de solución y Java revalida y
compone la respuesta tras esa tercera herramienta. El modelo conserva la elección
entre soluciones y la abstención previa. No se relajaron controles de ejecución,
referencias ni las aserciones que exigen las tres herramientas con el modelo real.

Las pruebas frontend comprueban API textual separada, ausencia de inputs de
archivo en el agente, traza de consultas, advertencias de límites y apertura
desde la aplicación. Un caso verifica que el contenido se adapte a la altura de
la cabecera al acomodar botones en pantallas angostas. El chatbot conserva sus
controles multimodales y su historial independiente.
La navegación directa a soluciones usa IDs y anclas internas: las pruebas verifican
la ruta, rechazo de anclas inválidas y apertura de una solución secundaria contraída.

Ejecutar la suite backend completa con las variables nativas de etapa 3 y Ollama
abierto; después ejecutar lint, cobertura y build del frontend de forma secuencial.
Logs: `.local/agente-targeted.log`, `.local/agente-seleccion.log`, `.local/agente-cierre.log`,
`.local/agente-backend-verify.log` y `.local/agente-frontend-{lint,coverage,build}.log`.
No se utilizan documentos, credenciales ni datos del corpus restaurado en los tests.
La evaluación de relevancia sobre una muestra más amplia corresponde a etapa 8.

### Resultados del backend de etapa 7, 13/11/2026

- `mvn clean verify` aprobado: 141 pruebas, sin fallos, errores ni omisiones;
  once casos adicionales respecto de etapa 5. Duración aproximada: 10 min 53 s.
- JaCoCo: 1575/1622 líneas (97,10 %) y 1244/1414 ramas (87,98 %). El umbral
  de líneas permanece en 92 % y no se agregaron exclusiones.
- El modelo real ejecutó las tres herramientas y devolvió solución y pasos
  verificados en 197,95 s dentro de la suite completa. La corrida aislada del
  cierre simplificado había pasado en 206,05 s. Son dos ensayos sintéticos del
  protocolo final, no una medición de precisión sobre el catálogo privado.
- Las comprobaciones nativas de conversación, embeddings y RAG también pasaron:
  41,72 s, 6,95 s y 58,57 s respectivamente. Embeddings conserva 768 dimensiones.
  Los tiempos incluyen instrumentación y cargas de modelos; no son garantías.
- PostgreSQL verificó prioridad de solución principal, orden de pasos y exclusión
  de registros archivados, además de los once casos anteriores de concurrencia.
- Las consultas H2 integradas y la matriz de permisos pasaron. El endpoint textual
  rechazó multipart y campos extra sin ejecutar el servicio del agente.
- PostgreSQL de pruebas se detuvo al finalizar. Se conserva V12 como última
  migración, con conocimiento y storage restaurados fuera de las pruebas.

### Resultados del frontend de etapa 7, 13/11/2026

- Corrida final: 18 archivos y 77 pruebas aprobadas, sin fallos ni omisiones;
  siete casos adicionales respecto de etapa 6. Duración aproximada: 3 min 32 s.
- Cobertura del alcance configurado (API, hooks y utilidades): líneas 173/173
  (100 %), sentencias 97,56 %, funciones 96,06 % y ramas 95,86 %. Los umbrales
  e inclusiones permanecen sin cambios. Las pruebas de UI verifican comportamiento
  y no forman parte de ese porcentaje.
- ESLint sin errores y con las tres advertencias previas de Fast Refresh.
  Build de producción aprobado; conserva la advertencia del bundle mayor a 500 kB.
- La primera corrida del frontend se inició antes de terminar la navegación a
  soluciones y mezcló un módulo transformado anterior con un test nuevo. Registró
  un error de exportación en caché. Se conserva su log privado en
  `.local/agente-frontend-primera-corrida.log`; se repitieron lint, toda la suite
  con cobertura y build con el código final estable, obteniendo los resultados
  aprobados anteriores. No se corrigió el porcentaje mediante exclusiones.
- Entorno local reiniciado con el JAR comprobado por SHA-256 y el agente activado
  mediante configuración privada. Se confirmó V12 sin migraciones pendientes,
  frontend HTTP 200 y respuesta 401 sin token en agente y chatbot. El frontend
  sirve el acceso al agente y las anclas de solución actualizadas.
- La comprobación positiva del protocolo se hizo con herramientas y datos de
  prueba, incluida la selección nativa de Ollama; no se usaron credenciales reales
  para realizar consultas al corpus restaurado durante el arranque.
- La etapa 6 está publicada en `develop` con `8eca399`. La etapa 7 se publicó
  posteriormente con `dccc70f`, antes de iniciar etapa 8; `main` no se modificó.

## Etapa 8: validación integral y evaluación reproducible

La guía [VALIDACION_INTEGRAL.md](VALIDACION_INTEGRAL.md) define el corpus sintético,
criterios de aceptación, aislamiento y comando de cierre. La evaluación nativa
`ValidacionIntegralIaTest` requiere `BC_TEST_INTEGRAL=true`, URL de PostgreSQL de
pruebas y Ollama local. Crea un esquema aleatorio y un servidor HTTP en puerto
aleatorio; utiliza JWT reales de cuentas de prueba, sin acceder al corpus privado.

El corpus versión 1 contiene tres temas, seis preguntas positivas y dos negativas.
Se mide Recall@3 y MRR@3 por la posición del primer fragmento del problema esperado.
Los umbrales previos son al menos 5/6 de recuperación, MRR >= 0,83 y ambas negativas
vacías. Se incluyen problemas no publicados con canarios sintéticos para verificar
la exclusión por permisos. Los modelos y límites relevantes se fijan en el test;
el informe final registra sus digests.

La prueba HTTP recorre login, creación del corpus, indexación con embeddings,
búsqueda de los tres roles, extracción real de PDF/DOCX, respuesta con fuentes,
abstención, una consulta con instrucciones adversarias, agente y rechazo de campos
de imagen. Compara huellas de trece tablas de conocimiento, relaciones, archivos
e historial antes y después de consultar. Las tablas de sesión, auditoría e índice
derivado se excluyen de esa comparación por sus escrituras legítimas.

La revisión también corrigió dos casos de navegación: al abrir una solución citada
del problema ya visible se actualiza el detalle, y una selección lenta anterior
no puede reemplazar la selección más reciente. Once pruebas seleccionadas de App
y useKnowledge pasaron antes del cierre completo, incluidos esos nuevos casos.

Las pruebas adversarias de JSON detectaron que se aceptaba la última ocurrencia
de una clave repetida. Se activaron lectura estricta de duplicados y rechazo de
tokens posteriores en la validación de acciones y citas. La repetición de los
dieciséis casos de agente y respuestas pasó sin ejecutar herramientas para un
objeto ambiguo ni aceptar su cita. La evidencia inicial queda en
`.local/validacion8-json.log` y el resultado corregido en
`.local/validacion8-json-estricto.log`.

El primer ensayo HTTP aislado, previo al cierre completo, pasó con seis positivos
en primera posición y ambas negativas vacías: Recall@3=1 y MRR@3=1. La respuesta
con fuentes tardó 81,57 s y el agente 237,62 s. Se verificó ausencia de escrituras.
El informe se conserva en `.local/evaluacion-integral-primer-ensayo.json`. Es una
muestra sintética pequeña y no una estimación de precisión del catálogo privado.

El comando `scripts/local/verify-ia-windows.ps1` ejecuta después toda la suite
backend con OCR, PostgreSQL y modelos reales, exige un informe integral nuevo
y aprobado y continúa con lint, cobertura y build del frontend de forma secuencial.
`-SoloIntegral` es un modo de diagnóstico y no reemplaza esa verificación completa.

### Cierre del backend de etapa 8, 14/11/2026

- La corrida general iniciada el 13/11 quedó interrumpida antes de terminar.
  Se conservó en `.local/validacion8-backend-interrumpido.log` y se inició una
  corrida nueva el 14/11. El informe anterior no se aceptó como cierre completo.
- `mvn clean verify` aprobado: 142 pruebas, sin fallos, errores ni omisiones.
  Incluye la evaluación HTTP nativa, OCR, PostgreSQL y los ensayos anteriores.
  Duración del backend: 23 min 03 s en esta corrida.
- JaCoCo: 1577/1624 líneas (97,11 %) y 1246/1414 ramas (88,12 %). Se conserva
  el umbral del 92 % de líneas y no se agregaron exclusiones.
- El informe integral nuevo quedó APROBADO: seis positivos en primera posición,
  Recall@3=1 y MRR@3=1; ambas consultas negativas quedaron sin coincidencias.
  Se mantuvo la exclusión de los dos problemas no publicados.
- La respuesta positiva quedó CON_FUENTES con texto literal verificado. La consulta
  con instrucciones adversarias también devolvió una cita válida, sin el canario
  inventado. El agente positivo ejecutó las tres herramientas y quedó CON_FUENTES;
  la petición de crear tickets y borrar por SQL quedó SIN_SOLUCION, sin fuentes.
- Las huellas de las trece tablas de conocimiento, relaciones, archivos e historial
  permanecieron idénticas después de las consultas. Solo se permitieron las
  escrituras legítimas de sesión, auditoría e índice derivado fuera de esa comparación.
- Tiempos del ensayo HTTP: respuesta 82,98 s, agente 245,15 s y ensayo completo
  563,82 s. Son observaciones con carga de modelos e instrumentación, no garantías
  de latencia. Los nombres y digests quedan en el informe privado reproducible.
- La instancia PostgreSQL de pruebas se cerró al finalizar. Se conserva V12
  como última migración; no se evaluó ni modificó el corpus privado restaurado.

Estos resultados describen el corpus sintético versión 1, de tres temas y ocho
consultas. No estiman la precisión del catálogo real ni certifican resistencia
frente a cualquier instrucción adversaria. Esa ampliación requiere casos esperados
revisados por personas que conozcan el catálogo.

### Cierre del frontend de etapa 8, 14/11/2026

- 18 archivos y 80 pruebas aprobadas, sin fallos ni omisiones; tres casos nuevos
  respecto de etapa 7 para actualización de fuentes y descarte de respuestas antiguas.
  La corrida con cobertura duró aproximadamente 3 min 33 s.
- Cobertura del alcance configurado (API, hooks y utilidades): líneas 182/182
  (100 %), sentencias 97,69 %, funciones 96,09 % y ramas 95,48 %. Se conservaron
  los umbrales e inclusiones. La UI se prueba mediante comportamiento y no forma
  parte de ese porcentaje.
- ESLint sin errores y con las tres advertencias anteriores de Fast Refresh.
  Build de producción aprobado; permanece el aviso de bundle superior a 500 kB.
- El script completo terminó con el informe integral nuevo APROBADO. No usó
  resultados anteriores para dar por válido el cierre. Los logs finales tienen
  prefijo `.local/validacion8-` y el informe está en `.local/evaluacion-integral.json`.
- Entorno Windows reiniciado con el JAR verificado por SHA-256. Frontend HTTP 200,
  navegación actualizada servida y respuesta 401 sin sesión en chatbot y agente.
  Flyway confirmó V12 sin migraciones pendientes. Los casos positivos autenticados
  se verificaron con las cuentas sintéticas del servidor HTTP aislado, no con
  credenciales de usuarios del corpus restaurado.
- La etapa 7 está publicada en `develop` con `dccc70f`. Los cambios de etapa 8
  quedan sin commit ni push para su revisión; no se modificó `main`.

### Ajuste de rendimiento posterior, 14/11/2026

- La búsqueda inicial obligatoria del agente se ejecuta sin inferencia. El
  recorrido normal conserva las tres herramientas y pasa de tres a dos decisiones
  del modelo. Sin coincidencias no invoca al modelo de conversación.
- La conversación usa `OLLAMA_KEEP_ALIVE=120s` por defecto, configurable entre
  0 y 300 segundos enteros. Embeddings conserva descarga inmediata. No se
  almacenan respuestas para reutilizarlas ni se cambian los permisos o fuentes.
- Una primera suite quedó pausada por solicitud de la usuaria. Al reanudar, otra
  corrida encontró clases incompletas en `target`, reemplazadas durante las pruebas
  con el compilador del editor activo. No se aceptaron como validaciones completas.
  El script compila ahora en `.local/backend-build`; Maven directo mantiene `target`.
  El lanzador usa el JAR más reciente de ambas carpetas.
- Corrida aislada final: `clean verify` aprobado, 143 pruebas sin fallos, errores
  ni omisiones, incluyendo modelos, PostgreSQL, OCR y evaluación HTTP. Duración:
  18 min 14 s. JaCoCo: 1584/1631 líneas (97,12 %) y 1258/1426 ramas (88,22 %).
  Se mantiene el umbral del 92 % de líneas y no se agregan exclusiones.
- Comparación dentro de una misma corrida, con dos inferencias en ambos modos:
  permanencia 0s, 145,15 s; permanencia 120s, 96,97 s (33,2 % menos). Ambos
  resultados quedaron CON_FUENTES con solución y pasos esperados.
- Ensayo HTTP integral APROBADO: agente 122,32 s frente a los 245,15 s anteriores
  (50,1 % menos). Agente sin solución: 6,79 s. Primera respuesta del chatbot:
  83,24 s frente a 82,98 s; no mostró mejora material. Ensayo completo: 357,03 s.
  Se conservaron seis positivos en primera posición, dos negativos sin coincidencias,
  citas válidas, rechazo del canario adversario y huellas iguales de trece tablas.
- El frontend no cambió durante este ajuste: se conserva la verificación anterior
  de 80 pruebas, lint y build. No se repitió esa suite por cambios solo de backend
  y scripts de arranque. No hay nuevas migraciones ni modelos descargados.
- Entorno de uso iniciado con el JAR aprobado, coincidente por SHA-256 con el
  artefacto aislado. Frontend HTTP 200; chatbot y agente devuelven 401 sin sesión.
  Flyway confirmó V12 sin migraciones pendientes.
- Logs finales: `.local/rendimiento-backend.log`; reportes y JAR en
  `.local/backend-build`. Informes privados: `.local/rendimiento-agente.json` y
  `.local/evaluacion-integral.json`. Comparaciones y límites en
  [RENDIMIENTO_IA.md](RENDIMIENTO_IA.md). Los cambios siguen sin commit ni push.

### Uso obligatorio del flujo solicitado, 14/11/2026

Se retiró el ensayo de respuesta documental directa porque omitía el modelo.
La usuaria requiere conservar Agente o Chatbot según el modo elegido. La medición
de 5,97 s del ensayo retirado no describe el agente actual. Se conservaron las
mejoras de permanencia y búsqueda inicial, y las validaciones de fuentes.

Se agregó una regresión que exige dos llamadas con Uso.AGENTE incluso para una
pregunta documental sin código y con una solución principal. La prueba existente
del chatbot exige Uso.CHATBOT. Se eliminaron la búsqueda directa por título y sus
pruebas específicas; no queda un selector que omita silenciosamente el modelo.

Verificación aislada posterior: `clean verify` aprobado en 6 min 08 s; 144 pruebas
contabilizadas, 139 aprobadas, cero fallos/errores y cinco ensayos largos de IA
omitidos expresamente, ya verificados antes. OCR y PostgreSQL reales incluidos.
JaCoCo: 1584/1631 líneas (97,12 %) y 1256/1426 ramas (88,08 %), con el mismo
umbral del 92 % de líneas. Log: `.local/agente-modelo-obligatorio-verify.log`.
Se conservan el protocolo con modelo y la prueba de Uso.CHATBOT; no se modifica
el frontend ni se agregan migraciones, dependencias o modelos.

### Contexto compacto y protección de imágenes guardadas, 14/11/2026

- El chatbot mantiene la elección del modelo y cambia su protocolo interno a
  `{"cita":n}`: selecciona una ventana literal, no vuelve a generar la cita.
  Conserva tipos enteros, rango, claves duplicadas, rechazo de JSON adicional,
  pertenencia al texto enviado y vigencia. Se probaron también decimales, strings,
  desbordes y campos extra del selector. El agente recibe contexto compacto,
  sin duplicar las soluciones leídas, y mantiene sus decisiones con modelo.
- Evaluación HTTP integral APROBADA: chatbot 54880 ms (antes 83236), agente
  75347 ms (antes 122322), negativo del agente 5598 ms. Recall@3=1 y MRR@3=1
  para los seis positivos; dos negativos sin coincidencias. Citas y herramientas
  válidas, sin canario adversario y huellas iguales de trece tablas. Duró 253734 ms.
- La pregunta real de RRHH se comprobó con modelo y búsqueda reales, sobre una
  conexión a la base restaurada con `default_transaction_read_only=on` verificado:
  CON_FUENTES en 118792 ms, dos inferencias y tres herramientas. El informe privado
  `.local/medicion-agente-real-contexto.json` no contiene preguntas ni respuestas.
  Es una medición del servicio, no un máximo de latencia ni del navegador.
- Protección solo de imágenes guardadas: copia PNG, OCR por posiciones, campos
  de identidad e importes, filas geométricas para bloques OCR separados y caché
  de la copia. Sin original ante fallos, sin datos enviados a modelos o servicios
  externos. Originales conservados. WebP incorpora `imageio-webp` 3.15.0.
  Se ocultan también imágenes incrustadas y fondos de HTML antiguo que no pasan
  por el visor protegido. No se modifica el HTML almacenado.
- Se aprobaron 33 pruebas enfocadas iniciales. La corrida general posterior
  ejecutó 153 casos y detectó una expectativa anterior en la prueba de permisos:
  una imagen de un píxel esperaba 200, pero ahora no se entrega si no se puede
  proteger. Se aisló ese test de permisos del OCR, que conserva su ensayo real.
  Se repitieron sus tres casos y los ocho del selector; se aprobaron los once.
  Se repitieron además cuatro casos geométricos y el ensayo de imagen con OCR
  real tras ampliar la detección por posición; aprobaron los cinco.
- Reportes Surefire consolidados: 153 casos únicos, cero fallos, errores u
  omisiones, incluidos OCR, PostgreSQL y Ollama reales. JaCoCo: 1685/1734 líneas
  (97,17 %) y 1360/1560 ramas (87,18 %). Umbral de líneas del 92 % sin nuevas
  exclusiones. Los cierres `verify` y el empaquetado aprobaron.
- Frontend: corrida general de 83 pruebas aprobada; cierre posterior del visor
  y HTML protegido de 15 pruebas aprobado, incluyendo un nuevo caso. Alcance
  actual: 84 casos únicos. Cobertura de API, hooks y utilidades: 182/182 líneas
  (100 %), sin incluir componentes en ese porcentaje. Lint sin errores y con
  las tres advertencias anteriores; build aprobado con el aviso de tamaño previo.
- Ensayo nativo de imagen actualizado: primera copia 7584 ms, caché 17 ms.
  Se revisó visualmente la imagen sintética y se verificó el worker desde el JAR
  empaquetado. El original sintético no se altera. Los límites y posibles omisiones
  del OCR se describen en [PROTECCION_IMAGENES.md](PROTECCION_IMAGENES.md).
- JAR desplegado coincidente por SHA-256, frontend HTTP 200 y chatbot, agente e
  imágenes HTTP 401 sin sesión. V12 se conserva. Logs: `.local/privacidad-rendimiento-general.log`,
  `.local/privacidad-rendimiento-cierre.log`, `.local/privacidad-geometria-cierre.log`
  y `.local/privacidad-frontend-cierre-*`.

Publicación del cierre: se incluyen la etapa 8, las mejoras de tiempo de respuesta
y la protección de imágenes en `develop`, para su posterior integración a `main`.
Las referencias anteriores a cambios sin commit describen el estado previo a esta
publicación. No se incluyen los datos restaurados, credenciales, storage ni logs privados.
