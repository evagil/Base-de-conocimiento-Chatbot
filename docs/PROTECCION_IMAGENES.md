# Protección de las imágenes de la base de conocimiento

La protección se aplica a las imágenes guardadas de problemas, soluciones y pasos.
No modifica las imágenes temporales que se suben al chatbot ni los PDF o Word.
Los archivos originales y sus hashes se conservan en el storage privado.

El endpoint autenticado `/api/archivos/{id}/contenido` comprueba primero el acceso.
Para imágenes devuelve una copia PNG protegida, nunca el original. Se ocultan
mediante zonas opacas los importes detectados —símbolos, decimales y columnas
monetarias— y las filas rotuladas como contribuyente, proveedor, razón social,
nombre/apellido, titular y documentos de identidad, incluyendo variantes plurales.
La ocultación reemplaza los píxeles: no es un filtro CSS que se pueda desactivar.

El visor muestra «Preparando vista protegida» durante la primera preparación.
La copia se reutiliza por hash del contenido e idiomas en
`storage/.vistas-protegidas-v1`; cada acceso sigue comprobando permisos. La respuesta
HTTP usa `no-store`, un nombre genérico y elimina los metadatos originales al
normalizar a PNG. Las cargas del visor se serializan para no saturar el OCR local.
Si falla el procesamiento, no se entrega el original. En la vista ampliada se
puede reintentar. Una copia ya preparada no necesita ejecutar OCR nuevamente.

Las imágenes incrustadas en el HTML antiguo, externas o en base64, no se muestran
directamente: podrían omitir el procesamiento protegido. El texto se conserva y
se informa que hay que adjuntar la imagen a la galería para verla protegida.
También se retiran estilos de fondo capaces de cargar imágenes. No se modifica
el HTML original almacenado: esta restricción se aplica en su visualización.

## OCR local y límites

Se reutiliza la instalación de Tesseract y las opciones `TESSERACT_COMMAND`,
`TESSERACT_DATA_PATH` y `TESSERACT_LANGUAGES` del archivo privado
`.local/documentos-config.json`. La lectura se ejecuta en una JVM auxiliar con
heap de 256 MB, dos procesadores, límite de píxeles antes de decodificar y timeout
externo de 40 segundos. Tesseract tiene un límite interno de 25 segundos.
El proceso no recibe credenciales, no accede a la base ni envía imágenes a Ollama
o a servicios externos. Los temporales privados se eliminan al terminar.

PNG y JPEG se decodifican con ImageIO; WebP usa `imageio-webp` 3.15.0 de
[TwelveMonkeys](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.15.0).
La salida siempre es PNG y usa solo el primer fotograma de una imagen animada.
Un formato no procesable, imagen sobredimensionada, fallo de OCR o ausencia de
texto reconocible deja la imagen oculta con un mensaje.

La detección es automática y conservadora: puede ocultar texto no sensible situado
cerca de los rótulos. No garantiza detectar nombres sin etiqueta, texto manuscrito,
campos muy alejados de su rótulo ni texto que el OCR lea mal. La imagen debe
revisarse antes de compartirla. La protección no anonimiza nombres de archivo,
textos alternativos ni el contenido de otros documentos.

## Verificación

Las pruebas cubren valores monetarios con símbolo separado, decimales, columnas,
rótulos compuestos y plurales, posiciones OCR inválidas, píxeles ocultos y zonas
de control conservadas. El controlador comprueba que devuelve bytes protegidos,
mantiene documentos sin cambios y verifica acceso antes de procesar.

El ensayo con Tesseract y worker reales usa únicamente datos sintéticos: primera
preparación 7655 ms, reutilización 16 ms en la corrida inicial. La imagen de control
queda en `.local/imagen-protegida-ensayo.png`; no contiene datos del catálogo real.
Son mediciones del equipo, no una garantía para cualquier tamaño de imagen.

Para ejecutar con OCR real, seguir `scripts/local/verify-ia-windows.ps1`.
La suite incluye la prueba nativa cuando está definida `BC_TEST_TESSERACT`.
