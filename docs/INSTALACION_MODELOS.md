# Modelos locales y capacidad del equipo

## Equipo evaluado el 12/11/2026

- Lenovo 20384, Windows 11 Pro.
- Intel Core i3-4005U de 1,70 GHz: dos núcleos físicos, cuatro hilos lógicos.
- Aproximadamente 7,9 GB de RAM total; 2,7 GB libres al comenzar la etapa 2.
- Intel HD Graphics integrada; sin GPU dedicada detectada.
- Ollama 0.32.6, disponible en 127.0.0.1:11434.

Se utilizaron únicamente modelos que ya estaban instalados. No se descargaron
pesos ni se modificó la instalación de Ollama. La inferencia evaluada se ejecutó
con contexto de 2048 tokens, dos hilos, temperatura 0 y descarga de RAM al terminar.

## Selección

| Modelo local | Tamaño del archivo aproximado | Resultado |
|---|---|---|
| `granite4.2:3b` | 2,24 GB | Seleccionado para conversación y candidato del agente |
| `gemma3:1b` | 0,82 GB | Más rápido; mostró sensibilidad a las instrucciones |
| `qwen3:4b` | 2,50 GB | No seleccionado: los pesos son Thinking-2507 |

El tamaño del archivo no representa toda la RAM necesaria: también hay memoria
de contexto y ejecución. No se mantienen varios modelos residentes simultáneamente.

La inspección de `/api/show` identificó el modelo local `qwen3:4b` como
`Qwen3-4B-Thinking-2507`, con `general.finetune=Thinking`. No es el Qwen3 Instruct
solicitado como primera opción. Con `think=false` y `/no_think` agotó 128 tokens
en unos 84–86 segundos sin producir una respuesta final utilizable. El cliente
rechaza esta variante antes de enviarle consultas.

Gemma contestó correctamente con instrucciones más simples, pero respondió
«Información insuficiente» en una primera consulta que sí contenía la solución.
Esto favoreció seleccionar Granite, pese a su mayor latencia.

Granite admite desactivar el pensamiento. Hay que enviar explícitamente
`think=false`; al omitirlo, una prueba agotó 128 tokens con el contenido final vacío.
La implementación Java lo envía siempre, sin depender de metadatos almacenados
en caché por el listado de modelos.

## Mediciones

Se utilizaron fuentes sintéticas sobre un error ficticio E17 y una consulta Z99
sin solución disponible. No se usaron documentos o registros reales.

| Caso con Granite y `think=false` | Tiempo total | Resultado |
|---|---:|---|
| Fuente e instrucciones separadas | 36,55 s | Indicó cerrar y volver a abrir la aplicación |
| Consulta sin fuente suficiente | 31,24 s | Respondió «Información insuficiente» |
| Solicitud de llamada nativa a herramienta | 103,92 s | Escribió una intención y pidió confirmación; no emitió `tool_calls` |
| Selección con esquema JSON | 49,06 s | Eligió `buscar_problema`, pero puso el nombre de la herramienta como consulta en vez del error E17 |
| Cliente Java real, fuente e instrucciones separadas | 40,86 s | Respuesta final de 21 tokens; prueba automatizada aprobada |

Los tiempos incluyen carga y preparación del modelo, ya que `keep_alive=0` libera
su memoria tras cada solicitud. Son observaciones de este equipo y estos prompts,
no garantías de latencia. La memoria máxima de proceso no se instrumentó en esta etapa.

La selección de herramientas mostró una limitación concreta. El esquema JSON
garantizó la forma del objeto, pero no la corrección semántica del argumento.
Granite queda como candidato del agente, no como agente ya validado. La etapa 7
debe probar secuencias, argumentos y límites antes de considerar terminado al agente.
Los argumentos de búsqueda deberán contrastarse con la consulta original y
las referencias solo podrán pertenecer a registros recuperados y autorizados.
Esta etapa no ejecuta herramientas por indicación del modelo.

Los resultados locales de evaluación están en `.local/ia-evaluation.json`,
`.local/ia-granite-final.json` y `.local/ia-granite-json.json`. Los ensayos fallidos
tienen archivos separados; no se confunden con la configuración final.

## Comprobar Ollama

```powershell
ollama --version
ollama list
ollama ps
```

`ollama list` enumera los modelos instalados; `ollama ps` muestra los que están
cargados en memoria. Para iniciar el servidor, abrir la aplicación de escritorio
o ejecutar `ollama serve`. Ver las opciones de memoria y contexto en la
[documentación oficial](https://docs.ollama.com/faq).

## Modelos pendientes

- Embeddings: `embeddinggemma:300m`, candidato de unos 622 MB según su
  [ficha oficial](https://ollama.com/library/embeddinggemma). Se verificará al
  preparar la indexación; las pruebas normales usan vectores simulados.
- Visión opcional: `qwen2.5vl:3b`, sin instalar. Su evaluación debe considerar
  el margen de RAM y la descarga antes de habilitarla.

No ejecutar descargas para completar esta etapa. La instalación de nuevos pesos
se hará como una tarea explícita en la etapa que los necesite. Las futuras
descargas requieren conexión a Internet; la generación con los modelos locales
seleccionados se procesa en esta computadora.

Configuración de la aplicación: [IA_LOCAL.md](IA_LOCAL.md).
