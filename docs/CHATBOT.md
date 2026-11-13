# Chatbot dentro de la aplicación: etapa 6 de 8

La pantalla utiliza los servicios de extracción y RAG de las etapas 3 y 5.
Está disponible para ADMINISTRADOR, EDITOR y LECTOR después de iniciar sesión,
desde el botón **Chatbot** de la barra superior. En teléfonos ocupa la pantalla;
en escritorio se abre como una ventana sobre el conocimiento existente.

## Consultar

1. Escribí o pegá una descripción en **Tu consulta al conocimiento**. Indicá el
   código de error y los síntomas relevantes. La pantalla admite hasta 1000
   caracteres, igual que el valor predeterminado del backend.
2. Presioná **Consultar**. El estado muestra que está buscando evidencia y
   preparando una respuesta. En este equipo puede tardar alrededor de un minuto
   o más, según la carga y el modelo. No se promete una latencia determinada.
3. Revisá la respuesta y su fuente. **Abrir problema** lleva al registro original;
   se muestran también los identificadores de solución y paso, y la página si
   existe. **Descargar fuente** recupera un archivo privado con tu sesión.

Cada turno es una consulta independiente. No hay memoria conversacional del
modelo: para una nueva pregunta se debe incluir otra vez la descripción necesaria.
La pantalla conserva como máximo diez intercambios en memoria, sin guardar
preguntas ni respuestas en localStorage, sessionStorage o una tabla de conversación.
Cerrar la ventana conserva el borrador y el historial mientras siga montada la
pantalla. **Limpiar conversación**, recargar o cerrar sesión los elimina.
Los cambios de navegación también pueden desmontar la pantalla y borrar ese estado.

La respuesta sigue siendo extractiva: el modelo selecciona una cita literal y
el backend verifica contenido y vigencia. No genera instrucciones adicionales
sin evidencia. Sin fuentes suficientes indica que falta información. Si la búsqueda
alcanzó el límite de candidatos, se muestra una advertencia para precisar la consulta.
La referencia debe revisarse: una cita verificable no garantiza por sí sola que
sea la solución correcta para el caso descrito.

## Usar PDF, DOCX o una captura JPG/JPEG

1. Presioná **Extraer texto de archivo** y seleccioná un archivo de hasta 10 MB.
   El backend verifica contenido y extensión; el filtro del navegador es una ayuda.
2. Esperá la extracción. PDF escaneados y JPG/JPEG usan Tesseract local. Las
   imágenes se procesan mediante OCR, sin interpretación visual generativa.
3. Revisá **Texto extraído (editable)**. Se indica si hubo OCR y si hay paginación
   estable. Corregí errores de reconocimiento y dejá solo el fragmento relevante.
4. Presioná **Usar texto en consulta**. Se agrega a tu borrador sin recortarlo.
   Si supera los 1000 caracteres, **Consultar** queda deshabilitado hasta que lo
   acortes. El documento temporal ayuda a formular la pregunta; no se usa como
   evidencia autorizada ni se publica como conocimiento.

El archivo se envía al servicio local al seleccionarlo. Se conservan las reglas
de tamaño, páginas, timeout y eliminación del trabajo temporal de la etapa 3.
La pantalla no guarda el binario ni llama a los endpoints de publicación.
Una extracción fallida conserva la consulta escrita y muestra el error.

## Cancelación, errores y privacidad

Mientras una operación está activa se bloquean los controles que la duplicarían.
**Cancelar operación**, cerrar la ventana, limpiar o desmontar la pantalla cancela
la espera HTTP mediante AbortController e ignora cualquier respuesta tardía.
El servidor puede terminar una inferencia o extracción iniciada; cancelar desde
el navegador no implica interrumpir el proceso de Ollama ni liberar inmediatamente
su turno. Si devuelve ocupado, esperá unos instantes antes de reintentar.

La pantalla conserva el borrador ante fallos. Los errores del backend explican
función desactivada, Ollama no disponible, operación ocupada o límites excedidos.
El transporte mantiene JWT y renovación de sesión; consultas y extracción tienen
timeouts específicos de 300 y 180 segundos. No modifica el timeout general del CRUD.
Si se aumentan los presupuestos del backend, revisar también estos límites del cliente.

El texto se representa como texto React, sin ejecutar HTML ni Markdown del modelo.
La navegación se construye con identificadores numéricos válidos y la descarga
usa un blob autenticado; no se abren URLs arbitrarias de la respuesta. El backend
conserva auditoría de acceso con método, ruta y usuario, sin registrar el cuerpo
de las preguntas o respuestas. Proteger la sesión y la pantalla sigue siendo necesario.

## Activación y verificación

No requiere cambios en tablas o nuevas migraciones. Se necesita el backend de
etapa 5, Ollama abierto, `granite4.2:3b`, `embeddinggemma:300m`, un índice vigente,
`RAG_ENABLED=true` e `IA_ENABLED=true`. Para documentos, activar también
`DOCUMENTS_ENABLED=true` y configurar Tesseract. Los archivos privados de
configuración y los scripts están explicados en [USO_WINDOWS.md](USO_WINDOWS.md).

Las pruebas verifican consulta textual, revisión de extracción, límites, errores,
cancelación y respuestas tardías, ausencia de persistencia del historial,
referencias, descarga autenticada y apertura desde la aplicación. Los contratos
backend permanecen cubiertos por las 130 pruebas de etapa 5; esta etapa modifica
el frontend. Ver comandos y resultados en [PRUEBAS_IA.md](PRUEBAS_IA.md).

El agente textual con herramientas corresponde a la etapa 7. El chatbot de
esta etapa no ejecuta herramientas ni modifica registros.
