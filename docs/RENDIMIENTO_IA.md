# Tiempo de respuesta de la IA local

## Cambios del 14/11/2026

El ajuste más reciente compacta los prompts sin omitir el modelo. El chatbot
recibe hasta nueve extractos literales acotados y el modelo elige su número con
`{"cita":n}`; cero indica abstención. El servidor compone el texto desde el extracto
elegido y valida su pertenencia a las fuentes vigentes. Así el modelo no necesita
volver a generar la cita completa. La API pública y las referencias se mantienen.

El agente recibe títulos y extractos de búsqueda de hasta 400 caracteres, sin
metadatos de enlaces que no usa para decidir. Las soluciones leídas se incluyen
una sola vez en la memoria, conservando su texto admitido completo. Se acortaron
las instrucciones manteniendo límites, tipos, IDs autorizados y rechazo de
instrucciones contenidas en los datos. No se cachean respuestas ni se reemplaza
la selección del modelo por una respuesta automática.

La protección de imágenes guardadas usa OCR local y caché de copias protegidas;
se documenta en [PROTECCION_IMAGENES.md](PROTECCION_IMAGENES.md).

### Medición posterior a la compactación

La evaluación HTTP con el mismo corpus sintético terminó APROBADA:

| Flujo | Antes de compactar | Después |
|---|---:|---:|
| Chatbot | 83,24 s | 54,88 s |
| Agente | 122,32 s | 75,35 s |
| Agente sin solución | 6,79 s | 5,60 s |

Se conservaron seis positivos en primer lugar, dos negativos sin coincidencias,
citas literales y huellas iguales de trece tablas de conocimiento. El ensayo HTTP
duró 253,73 s. Las mejoras observadas fueron aproximadamente 34 % para chatbot
y 38 % para agente respecto de la corrida anterior, con participación del modelo.
El informe anterior está en `.local/evaluacion-integral-anterior-contexto.json`;
el nuevo, en `.local/evaluacion-integral.json`.

La prueba aislada del agente con dos decisiones y permanencia de 120s pasó de
96,97 a 65,77 s; sin permanencia midió 103,30 s. El chatbot aislado midió 19,46 s;
estos tiempos no incluyen la búsqueda real ni representan el tiempo completo HTTP.

La pregunta real de RRHH se verificó además contra la base restaurada, con
`default_transaction_read_only=on`, búsqueda híbrida real y modelo real: respondió
CON_FUENTES en 118792 ms, dos inferencias y tres herramientas, con la solución
documentada esperada. El informe `.local/medicion-agente-real-contexto.json` no
incluye pregunta ni respuesta. Esa medición es del servicio Java y no del navegador.
Los 75 s del caso sintético no son un límite máximo para otras consultas.

El agente ejecuta directamente la búsqueda inicial obligatoria. No necesita una
inferencia para confirmar esa única acción permitida. El recorrido habitual
conserva las tres herramientas, pero usa dos decisiones del modelo en lugar de
tres: elegir el problema del que leer soluciones y elegir la solución cuyos
pasos se consultan. Una búsqueda sin coincidencias termina sin conversación.

El cliente conserva el modelo de conversación hasta 120 segundos sin actividad.
Esto permite reutilizar su carga y preparación entre decisiones. No se guardan
respuestas para reutilizarlas entre usuarios. La selección del modelo, los
presupuestos de contexto, los límites de salida, los permisos y las verificaciones
de vigencia de las fuentes se mantienen. Los embeddings siguen enviando
`keep_alive=0` para liberar memoria al terminar cada operación.

## Uso del flujo solicitado

La selección de la pantalla determina el flujo: Agente usa su orquestador y el
modelo de agente para elegir la solución y los pasos; Chatbot usa su servicio de
respuesta con fuentes y el modelo de conversación. No se intercambian esos modos
ni se responde por coincidencia de título para evitar la participación del modelo.
Si no hay fuentes utilizables, el flujo seleccionado se abstiene con los controles
existentes, sin generar una respuesta sin evidencia.

Se retiró el ensayo de respuesta documental directa a pedido de la usuaria porque
omitía el modelo. Los 5,97 segundos medidos con ese ensayo no corresponden al
comportamiento actual. Se conservan la permanencia de Ollama y la búsqueda inicial
obligatoria automática: el modelo sigue eligiendo las soluciones y los pasos.

## Comparación anterior con el modelo real

`AgenteLocalSmokeTest` ejecuta secuencialmente la misma consulta sintética E17 con
Granite `granite4.2:3b`, primero con permanencia cero y después con 120 segundos.
Ambos recorridos deben terminar `CON_FUENTES`, ejecutar las tres herramientas,
realizar exactamente dos inferencias y devolver la solución y los pasos esperados.
El informe privado `.local/rendimiento-agente.json` contiene los tiempos por
llamada, sin preguntas, prompts, respuestas ni credenciales.

Medición del 14/11/2026 en esta PC (Core i3-4005U, 7,92 GiB de RAM, inferencia
en CPU), con backend de uso detenido y suite Java instrumentada con JaCoCo:

| Permanencia | Primera decisión | Segunda decisión | Total del agente |
|---|---:|---:|---:|
| 0 segundos | 73,47 s | 71,64 s | 145,15 s |
| 120 segundos | 74,62 s | 22,33 s | 96,97 s |

La permanencia redujo el total un 33,2 % en ese ensayo. Una medición previa
del mismo día dio 136,50 → 92,16 s (32,5 % menos). Esta comparación ya usa
la búsqueda automática en ambos recorridos; mide el efecto adicional de conservar
el modelo. No incluye búsqueda real en PostgreSQL ni embeddings: esas partes usan
dobles de prueba en este caso. La validación HTTP integral sí incluye esas partes.
No se establece un umbral automático de segundos porque la carga del equipo varía.

## Resultado anterior desde la API, antes de compactar

La evaluación HTTP integral del 14/11/2026 terminó APROBADA, con el mismo corpus
sintético, modelos y consultas que el cierre previo. Incluye PostgreSQL, JWT,
embeddings, recuperación y las lecturas verificadas de herramientas:

| Consulta | Antes del ajuste | Después del ajuste |
|---|---:|---:|
| Agente: solución y pasos para E17 | 245,15 s | 122,32 s |
| Primera respuesta del chatbot con fuentes | 82,98 s | 83,24 s |
| Agente sin solución disponible | No se había medido por separado | 6,79 s |

El agente redujo la espera aproximadamente un 50,1 % en este ensayo completo.
La primera respuesta del chatbot no mostró una mejora material: el costo de
cargar y procesar el contexto sigue presente. Son corridas separadas en esta PC,
no una garantía para todas las consultas. La comparación específica de permanencia
de la sección anterior usa el mismo recorrido dentro de una sola corrida.

Los seis positivos se recuperaron en primer lugar y los dos negativos quedaron
sin coincidencias (Recall@3=1, MRR@3=1). Chatbot, prueba adversaria y agente
conservaron fuentes verificadas. Las huellas de las trece tablas de conocimiento
permanecieron iguales. El ensayo completo duró 357,03 s, frente a los 563,82 s
del cierre anterior. Los valores nuevos están en `.local/evaluacion-integral.json`;
el anterior se conservó en `.local/evaluacion-integral-antes-rendimiento.json`.

## Configuración y límites

La propiedad `app.ia.permanencia-modelo` se configura mediante
`OLLAMA_KEEP_ALIVE` (predeterminado `120s`). Admite segundos enteros entre 0 y 300;
un valor inválido impide iniciar el cliente. El lanzador Windows acepta la opción
en `.local/ia-config.json`; ver [USO_WINDOWS.md](USO_WINDOWS.md#reducir-las-recargas-de-ollama).
`0s` recupera la descarga inmediata después de cada respuesta.

La primera inferencia sigue cargando y preparando el modelo. Ollama también puede
expulsarlo si necesita memoria, y alternar entre embeddings y conversación puede
requerir nuevas cargas. La mejora medida no garantiza el mismo porcentaje en
otras consultas, con otros modelos o bajo otra carga del equipo.

Para reproducir con PostgreSQL de pruebas, Ollama y Tesseract preparados:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/local/verify-ia-windows.ps1
```

La comparación forma parte de la prueba nativa existente; la suite también
comprueba la respuesta literal, rechazo de acciones inválidas, cambios concurrentes
de fuentes, abstención y ausencia de escrituras en el conocimiento.

El script Windows compila en `.local/backend-build`, separado de `backend/target`:
la compilación automática del editor puede reemplazar clases en `target` mientras
Maven ejecuta las pruebas. La carpeta aislada contiene el JAR, los reportes Surefire
y `site/jacoco`. Maven directo conserva `target` por defecto; el directorio puede
cambiarse con `-Dbc.build.directory=...`. El lanzador Windows usa el JAR más
reciente entre ambas salidas.
