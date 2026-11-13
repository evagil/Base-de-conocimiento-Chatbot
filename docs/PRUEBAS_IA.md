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
