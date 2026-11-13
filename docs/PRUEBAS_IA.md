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
