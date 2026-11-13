# Índice del conocimiento: etapa 4 de IA

Esta etapa prepara las fuentes para la recuperación posterior. No agrega todavía
búsqueda híbrida, respuestas de chatbot ni herramientas del agente: corresponden
a las etapas 5, 6 y 7. El conocimiento original sigue siendo la fuente de verdad.

## Datos y arquitectura

El módulo `rag` utiliza JDBC y las interfaces de IA existentes dentro del
monolito. Las migraciones nuevas son V11 (tablas y vista portables) y V12
(índice GIN de texto español, únicamente PostgreSQL). Las migraciones anteriores
no se modifican. No se agregan dependencias Maven ni servicios externos.

```mermaid
flowchart LR
    CRUD[Guardado existente] --> Datos[(Conocimiento original)]
    Barrido[Planificador y lease] --> Datos
    Datos --> Snapshot[Snapshot y huella]
    Snapshot --> Fragmentos[Texto y fragmentación]
    Fragmentos --> Ollama[Embeddings locales opcionales]
    Fragmentos -->|Modo textual| Indice
    Ollama --> Indice[(Índice derivado)]
    Indice --> Vista[Fuentes vigentes]
    Datos --> Vista
    Vista --> Futuro[Recuperación: etapa 5]
```

| Tabla/vista | Contenido |
|---|---|
| `rag_control` | Cursor de barrido, generación de reconstrucción y lease del trabajador |
| `rag_estado` | Huella de las fuentes, estado, intentos, código de error y próxima oportunidad |
| `rag_fragmentos` | Texto derivado, origen, versiones, página y vector opcional |
| `rag_fragmentos_vigentes` | Fragmentos listos cuyo origen continúa autorizado y vigente |

Los fragmentos conservan tipo e identificador de origen, problema, solución,
paso y archivo cuando corresponden, área, subárea, estado, activo, versión del
origen y del problema, fecha de modificación y posición. Para PDF se conserva
la página extraída; DOCX y texto del editor no inventan paginación.
La navegación se podrá construir con los identificadores y las rutas existentes
de problemas, soluciones y descarga privada; no se guardan rutas físicas públicas.

Se indexan exclusivamente problemas PUBLICADOS y activos con área y subárea
activas. Se usan título, descripción textual del editor, área, subárea, etiquetas
activas, soluciones activas, pasos activos y PDF/DOCX activos de esos tres niveles.
Las imágenes no se incorporan a este índice textual. Un PDF escaneado puede
aportar texto reconocido por el OCR de la etapa 3. Los adjuntos usan su hash
SHA-256 registrado y una ruta verificada dentro del storage antes de extraerse;
no se modifican ni se duplican los binarios originales.

Los archivos cargados para una consulta temporal del chatbot no se publican en
el índice. Solo los adjuntos que ya pertenecen al conocimiento entran en este
proceso. TXT y texto escrito en el futuro chat serán consultas, no publicaciones
automáticas. El texto del editor ya cuenta con una representación plana existente.

## Actualización y concurrencia

Un barrido incremental lee snapshots consistentes con aislamiento REPEATABLE READ.
Su huella SHA-256 incluye fuentes, contenido, metadatos y configuración del
fragmentador/modelo. Detecta altas, ediciones, publicación, archivo, baja lógica,
movimientos de área/subárea, etiquetas, soluciones, pasos y adjuntos.
Los snapshots sin cambios se omiten sin llamar a Ollama ni reescribir fragmentos.

Se eligió reconciliación periódica para cubrir también cambios realizados por
las distintas rutas existentes, sin introducir llamadas de IA en sus transacciones.
El guardado normal no depende del índice. La actualización es eventual: no hay
una promesa de sincronización inmediata ni una cola de eventos por cada edición.
Cada ciclo recorre hasta diez problemas en orden de ID y luego continúa desde
un cursor persistido. Al llegar al final vuelve al comienzo. El tiempo de un
barrido depende del tamaño del catálogo, archivos y latencia del modelo.

El lease se toma en una transacción corta sobre la única fila de control y
vence a los diez minutos. Solo un trabajador del conjunto de instancias procesa
el índice a la vez. Extracción e inferencia ocurren fuera de esa transacción;
no se mantienen bloqueos sobre las entidades de conocimiento mientras corre IA.
Si cae el proceso, el lease vence y el barrido vuelve a visitar sus fuentes.

Antes de publicar se releen las fuentes y se compara su huella. Si cambiaron,
se descarta el resultado y una vuelta posterior lo recalcula. La sustitución de
fragmentos y el estado LISTO son atómicos. Una generación de reconstrucción o
lease distinto impide que un trabajador anterior publique resultados tardíos.
Las versiones y controles de concurrencia del CRUD existente se conservan.

La vista `rag_fragmentos_vigentes` verifica nuevamente publicación y actividad
del problema y sus padres, actividad de solución/paso/archivo, versiones del
origen y del problema y estado LISTO con huella coincidente. Una baja o un cambio
de versión puede dejar de ser visible antes del siguiente barrido. No se debe
consultar `rag_fragmentos` directamente desde la futura búsqueda ni tratar el
estado copiado del índice como una autorización. La etapa 5 deberá volver a
validar fuentes y permisos antes de entregar contexto o citas al usuario.

## Fragmentación y embeddings

El fragmentador normaliza Unicode NFC, espacios y controles. Divide el texto en
fragmentos de hasta 600 unidades UTF-16 con solapamiento de 80, buscando límites
de palabra y sin romper pares sustitutos. No se trunca un problema demasiado
grande: se registra un fallo explícito para revisión y reintento.

`embeddinggemma:300m` es el modelo local elegido para embeddings, independiente
de `granite4.2:3b` para conversación. Se instaló desde la
[biblioteca oficial de Ollama](https://ollama.com/library/embeddinggemma:300m).
La descarga ronda 622 MB. Las llamadas pasan por el cliente local de la etapa 2,
con límites, un solo turno de IA y `keep_alive=0`. No se descargan modelos desde
el indexador ni durante los tests. El texto documental sigue siendo datos no
confiables y no se ejecutan sus instrucciones.

En esta instalación Windows no está disponible `vector.control` de pgvector.
Por eso V11 almacena vectores como JSON en una columna TEXT junto al modelo y
la dimensión, sin forzar extensiones ni compilar componentes nativos. V12 agrega
el índice textual nativo `to_tsvector('spanish', texto)`. La futura búsqueda
semántica deberá usar una comparación acotada en Java, con filtros previos, o
una migración posterior a pgvector: todavía no se ofrece búsqueda vectorial ni
un índice ANN. Esta alternativa es apropiada para validar el catálogo local;
su escalabilidad deberá medirse antes de usar catálogos grandes.

`RAG_EMBEDDINGS_ENABLED=false` permite preparar un índice textual sin Ollama.
Con embeddings activados, un fallo del modelo deja el problema en ERROR; no se
presenta una vectorización incompleta como exitosa. Cambiar modelo, fragmentación
o modo textual cambia la huella y obliga a regenerar. Si se reemplaza el contenido
de una etiqueta Ollama por otro modelo con el mismo nombre, reconstruir el índice
explícitamente: el nombre del modelo no identifica de forma inmutable sus pesos.

## Fallos y límites

Un problema fallido conserva un código técnico seguro, sin documentos ni prompts
en logs. Se retiran sus fragmentos derivados para no servir una preparación
parcial. Se realizan hasta tres intentos para la misma huella, con esperas de
60 segundos multiplicados por el número de intento. Un cambio de fuentes válidas
reinicia esa cuenta; también existe un reintento administrativo explícito.
Si falla la lectura del snapshot antes de poder calcular una huella, agotados
los intentos es necesario solicitar un reintento administrativo tras corregirlo.
Los códigos y estados persisten entre reinicios.

Valores predeterminados:

| Variable | Valor |
|---|---|
| `RAG_INDEX_ENABLED` | `false` |
| `RAG_EMBEDDINGS_ENABLED` | `false` |
| `RAG_CHUNK_SIZE` / `RAG_CHUNK_OVERLAP` | `600` / `80` |
| `RAG_MAX_CHUNKS` | `128` por problema |
| `RAG_MAX_SOURCES` | `100` fuentes por problema |
| `RAG_MAX_ATTACHMENTS` | `3` PDF/DOCX por problema |
| `RAG_INDEX_TIMEOUT` | `120s` |
| `RAG_INDEX_INTERVAL` | `15000` ms entre ciclos |
| `RAG_INDEX_INITIAL_DELAY` | `30000` ms antes del primer ciclo |

El texto de las fuentes se limita a 200 000 caracteres en conjunto y hasta 200
etiquetas activas. Las filas se leen por lotes acotados y se interrumpe la lectura
al alcanzar un límite, sin acumular todo el catálogo. Los límites
de archivos y OCR de la etapa 3 siguen vigentes. El presupuesto de indexación
se comprueba entre operaciones y antes de publicar; una llamada en curso tiene
su propio timeout y puede terminar después de ese presupuesto. No se publica
ese resultado tardío. Dos hilos de planificación permiten que la limpieza
documental siga funcionando durante una indexación larga.

## Administración y arranque

Las operaciones requieren JWT de ADMINISTRADOR. LECTOR y EDITOR reciben 403;
anónimo recibe 401. La API devuelve DTO y contadores, nunca entidades JPA,
vectores, contenido privado de fuentes ni rutas físicas.

| Método y ruta | Resultado |
|---|---|
| `GET /api/ia/indice` | Habilitación, modo embeddings y contadores/cursor |
| `POST /api/ia/indice/reconstruir` | 202; invalida trabajos anteriores y prepara reconstrucción |
| `POST /api/ia/indice/reintentar` | 202; habilita un nuevo recorrido de los fallidos |

Las órdenes no ejecutan extracción o inferencia dentro de la solicitud HTTP.
Si la función está desactivada, las operaciones POST responden 503
`INDICE_DESHABILITADO`; el diagnóstico GET continúa disponible.
Los contadores reflejan el último recorrido, no garantizan que todo el catálogo
ya haya sido visitado. No confundir un estado LISTO con respuestas RAG validadas.
Un problema no publicado o inactivo puede quedar LISTO con cero fragmentos:
el trabajo comprobó que debe permanecer fuera de la recuperación.

En Windows el lanzador admite `.local/indice-config.json`, por ejemplo:

```json
{
  "RAG_INDEX_ENABLED": "true",
  "RAG_EMBEDDINGS_ENABLED": "true"
}
```

Activar también `IA_ENABLED=true`, instalar el modelo y abrir Ollama. Para
adjuntos, activar `DOCUMENTS_ENABLED=true` y configurar OCR según
[CHATBOT_MULTIMODAL.md](CHATBOT_MULTIMODAL.md). El JSON local tiene prioridad
sobre el entorno del lanzador. Recompilar y reiniciar el backend para aplicar
cambios. Los datos derivados contienen texto privado: respaldar y proteger la
base con las mismas reglas que el conocimiento original.

## Verificación y reversión

Las pruebas normales usan una base H2 independiente, documentos sintéticos y
un doble de embeddings. La suite completa también aplica las migraciones reales
de PostgreSQL y prueba el modelo solo cuando se habilita expresamente mediante
`BC_TEST_OLLAMA_URL`. Comandos y resultados en [PRUEBAS_IA.md](PRUEBAS_IA.md).

Para desactivar: `RAG_INDEX_ENABLED=false` y reiniciar. Se conserva el CRUD y el
índice deja de actualizarse. No hace falta borrar ni restaurar conocimiento.
Después de aplicar V11/V12 no modificar esas migraciones ni retroceder su historial
Flyway; una reversión de esquema requiere una migración posterior expresamente
revisada. Los datos derivados pueden reconstruirse desde las fuentes originales.
