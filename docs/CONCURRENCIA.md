# Concurrencia y modificaciones seguras

## Contrato de la API

Las representaciones de áreas, subáreas, problemas, soluciones, pasos, archivos y
usuarios incluyen `version`. Toda modificación de un registro existente
(`PUT` o `PATCH`) exige el parámetro de consulta `version` que el cliente recibió
al abrir ese registro. Las altas no necesitan una versión previa.

Ejemplo: un formulario abierto con la versión 4 envía
`PUT /api/soluciones/12?version=4`. El backend compara esa versión antes de
modificar y conserva `@Version` de JPA para detectar carreras entre la comparación
y la escritura. Hace flush antes de construir la respuesta, que devuelve la nueva
versión. La transacción incluye el historial: un intento rechazado tampoco
agrega una versión histórica.

- Versión ausente: HTTP 400; no hay modificación.
- Versión desactualizada: HTTP 409, código `VERSION_DESACTUALIZADA`.
- Bloqueo fallido: HTTP 409, código `OPERACION_EN_CURSO`.
- Violación de unicidad: HTTP 409, código `CONFLICTO_DE_DATOS`.

El frontend envía la versión del formulario y conserva su contenido cuando falla
el guardado. No obtiene una versión nueva para reintentar silenciosamente ni
reintenta un HTTP 409. El usuario puede copiar su texto, consultar el registro
actual y volver a editar. No hay edición colaborativa en tiempo real ni fusión
automática de textos.

## Solución recomendada

Las escrituras de soluciones que pueden alterar la recomendación bloquean primero
la fila del problema (`SELECT ... FOR UPDATE`), siempre en ese orden. Los problemas
distintos pueden modificarse independientemente. Al reemplazar una recomendación,
se persiste primero la desmarcación de la anterior.

La migración PostgreSQL V4 agrega un índice único parcial sobre `problema_id`
para filas con `principal=true AND activo=true`. Incluso una escritura que
eluda el servicio no puede dejar dos soluciones recomendadas activas.

La migración no elige ni elimina datos existentes. Si ya hay recomendaciones
duplicadas, falla para que se revise cuál conservar:

```sql
SELECT problema_id, COUNT(*)
FROM soluciones
WHERE principal AND activo
GROUP BY problema_id
HAVING COUNT(*) > 1;
```

## Sesiones

El consumo y la revocación de refresh tokens bloquean la fila del token dentro
de la transacción. Una renovación simultánea del mismo token puede tener un solo
ganador.

El cliente comparte una renovación pendiente entre solicitudes. Cuando el
navegador admite Web Locks, también coordina pestañas del mismo origen. Sin
Web Locks la coordinación del cliente se limita a la pestaña; el servidor sigue
impidiendo reutilizar el token. La respuesta de una renovación no restaura una
sesión que se cerró o fue reemplazada mientras esperaba.

## Pruebas reproducibles con PostgreSQL

Desde la raíz del proyecto, en Linux con PostgreSQL y Java 21 instalados:

```bash
bash backend/scripts/test-postgres-concurrency.sh
```

El script crea un clúster temporal propio, escucha solamente en 127.0.0.1:55439,
crea `bc_concurrency_test` y ejecuta Maven verify. Detiene ese clúster al terminar;
nunca utiliza `DB_URL` ni la base de la aplicación. Si el puerto está ocupado,
falla sin detener el proceso que lo ocupa. Se puede elegir otro puerto:

```bash
BC_TEST_PORT=55440 bash backend/scripts/test-postgres-concurrency.sh
```

`BC_PG_BIN` permite indicar la carpeta que contiene initdb y pg_ctl si pg_config
no encuentra la instalación deseada. En caso de error se conservan los logs en
la carpeta temporal indicada. Se requiere acceso a las dependencias Maven ya
instaladas o a su repositorio durante la primera ejecución.

Las pruebas PostgreSQL se habilitan con `BC_TEST_DB_URL` y exigen el nombre de
base `bc_concurrency_test`. Las pruebas H2 habituales no sustituyen las pruebas
del índice parcial ni de los bloqueos PostgreSQL.

## Aplicación en el entorno existente

Actualizar backend y frontend juntos: clientes antiguos que no envíen versión
no podrán modificar registros. Después de actualizar, recargar el navegador.

Flyway aplica V4 al iniciar el backend. El usuario que ejecuta la migración debe
ser propietario de la tabla `soluciones` o tener acceso mediante su rol
propietario. Los permisos SELECT/INSERT/UPDATE/DELETE otorgados anteriormente no
bastan para crear este índice. PostgreSQL permite configurar credenciales separadas
para Flyway con `SPRING_FLYWAY_USER` y `SPRING_FLYWAY_PASSWORD`, manteniendo
`DB_USERNAME` y `DB_PASSWORD` para el acceso normal de la aplicación. Ingresar
las contraseñas localmente y no guardarlas en Git.

La base real no se modifica al ejecutar la suite de pruebas.

## Alcance y siguientes controles

Esto evita pérdidas de actualizaciones y protege las invariantes verificadas;
no determina cuántos usuarios concurrentes soporta el servidor. Antes del despliegue
general se necesita una prueba de carga representativa para dimensionar conexiones,
memoria, consultas y tiempos de respuesta.

Las altas compuestas del frontend (problema, solución e imágenes) todavía son
varias solicitudes. Un fallo intermedio puede dejar una creación parcial; la
idempotencia de altas y la consistencia entre archivos físicos y transacciones
requieren un trabajo separado. Los índices únicos existentes rechazan altas
duplicadas concurrentes de nombres/etiquetas, pero no se fusionan automáticamente.

## Verificación realizada el 15/09/2026

- Backend: compilación limpia, 26 pruebas aprobadas; 7 ejecutadas sobre PostgreSQL 17.
- JaCoCo: 94,13 % de líneas y 59,72 % de ramas. Se cumple el umbral obligatorio de
  líneas del 91 %; las ramas restantes siguen siendo una oportunidad de mejora.
- Frontend: 27 pruebas aprobadas; cobertura de líneas del 100 % en los módulos
  API, hooks y utilidades incluidos en la configuración. No representa cobertura
  total de todos los componentes visuales.
- ESLint y compilación de producción del frontend aprobados.
- Probada migración desde V3, conservación de datos y rechazo de duplicados.
- Migración V4 aplicada en la base de uso; confirmada por el usuario en
  `flyway_schema_history` con `success = true` el 15/09/2026.

## Guardados sin cambios y reparación del historial (V5)

Un guardado sin cambios no debe crear una entrada histórica: JPA no incrementa
la versión de una entidad cuyo contenido no cambió. Antes de esta corrección,
la entrada se creaba igualmente y la siguiente edición podía colisionar con
la unicidad de `(solucion_id, numero_version)` o `(problema_id, numero_version)`.

Los servicios ahora comparan los valores normalizados y solo generan historial
cuando existe un cambio efectivo. La validación de la versión recibida permanece
obligatoria incluso cuando el contenido enviado coincide. En problemas se
resuelven las etiquetas y la subárea antes de modificar la entidad, para evitar
que una consulta provoque un flush anticipado y desplace la versión del historial.

V5 conserva todas las entradas históricas y el contenido vigente. Únicamente
adelanta la versión de los registros que tienen historial con una versión igual
o superior a la actual. Los registros sin esa anomalía no cambian. Al aplicarla,
se deben cerrar y volver a abrir los formularios para recibir la nueva versión.

Para confirmar la aplicación en la base de uso:

```sql
SELECT version, description, success
FROM flyway_schema_history
WHERE version = '5';
```

La reproducción del defecto y la reparación se prueban sobre bases temporales.
La captura de un HTTP 409 genérico, por sí sola, no identifica cuál restricción
falló: para confirmar un incidente concreto se necesita el error SQL del backend.

### Estados del problema (V6)

Los estados vigentes son BORRADOR, PUBLICADO y ARCHIVADO. Al iniciar el backend,
Flyway convierte EN_REVISION a BORRADOR y RESUELTO a PUBLICADO sin modificar el
contenido ni los registros históricos. Los problemas convertidos incrementan su
versión para rechazar formularios abiertos antes de la migración. La restricción
de base de datos y el enum de la API admiten únicamente los tres estados vigentes.
Los lectores acceden solo a PUBLICADO; administradores y editores pueden consultar
los borradores y acceder a los archivados mediante «Ver archivados».

### Eliminación de problemas y soluciones

La eliminación es lógica (`activo=false`) y conserva el historial. Un problema
solo puede eliminarse cuando no tiene soluciones activas. La baja del problema,
el alta de una solución y su reactivación adquieren el mismo bloqueo del problema:
si gana la baja, no se admiten nuevas soluciones; si gana el alta, se rechaza la
baja. Las solicitudes también incluyen la versión del registro para detectar
formularios desactualizados. El frontend oculta soluciones eliminadas y limpia
la selección al eliminar el problema.
