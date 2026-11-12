# Prompt y requerimientos originales

> Documento de referencia incorporado sin cambiar el alcance funcional.

## Enmiendas aprobadas posteriormente

1. Un problema puede tener cero, una o varias soluciones y, como máximo, una solución principal activa.
2. Cada etapa debe finalizar con más del 90 % de cobertura automatizada. El umbral operativo será 91 % y el proceso de construcción deberá fallar si no se alcanza.

<Rol>
Actuá como arquitecto de software y desarrollador full stack sénior especializado en React, Java 21, Spring Boot, PostgreSQL, APIs REST, seguridad, autenticación y diseño de aplicaciones administrativas.
Debés guiarme como a una desarrolladora junior que tiene lógica de programación y experiencia básica, pero que está aprendiendo arquitectura y Spring Boot. Explicá las decisiones importantes sin dar conceptos por sabidos.
El código deberá ser claro, organizado, mantenible y acompañado por explicaciones y pruebas simples.
</Rol>
<Objetivo>
Quiero diseñar y desarrollar una aplicación web que funcione como una base de conocimiento interna para registrar, organizar, buscar, consultar y actualizar problemas o errores frecuentes junto con sus soluciones.
Cada problema pertenecerá a un área y una subárea. Las áreas, subáreas y problemas deberán visualizarse mediante una estructura jerárquica en forma de árbol.
RENTAS
    Catastro
        Problema 1
        Problema 2
    Comercio
        Problema 1
        Problema 2
        Problema X
    Notificaciones
        Problema X
        Problema Y
        Problema Z

Al seleccionar un problema del árbol, se deberá mostrar toda su información: título, descripción del error, imágenes, soluciones, pasos e imágenes de las soluciones.
</Objetivo>
<Decisiones de arquitectura>
1.	Frontend: React con JavaScript y Vite.
2.	Backend: Java 21 con Spring Boot y Maven.
3.	Base de datos: PostgreSQL.
4.	Comunicación: API REST mediante JSON.
5.	Arquitectura del backend: monolito modular.
6.	No utilizar microservicios.
7.	Seguridad inicial: Spring Security, JWT y BCrypt.
8.	Migraciones de base de datos: Flyway.
9.	Documentación de la API: OpenAPI/Swagger.
10.	Componentes visuales: Material UI.
11.	Editor enriquecido: evaluar TipTap para descripciones y pasos.
12.	Imágenes: guardar archivos en una carpeta protegida y almacenar en PostgreSQL sus metadatos y relaciones.
13.	Búsqueda inicial: utilizar las capacidades de búsqueda de PostgreSQL.
14.	Entorno inicial: ejecución local y sin Docker.
15.	Frontend y backend: proyectos independientes dentro de una carpeta principal.
base-conocimiento/
├── backend/
├── frontend/
├── storage/
└── README.md

El backend deberá dividirse por módulos funcionales, no solamente por capas globales:
16.	areas
17.	problemas
18.	soluciones
19.	archivos
20.	usuarios
21.	seguridad
22.	busqueda
23.	auditoria
Cada módulo podrá contener sus controladores, servicios, repositorios, entidades, DTO, mappers y excepciones, respetando la separación de responsabilidades.
No implementar microservicios, RabbitMQ, MongoDB, Elasticsearch, Keycloak ni Docker en la primera versión.
</Decisiones de arquitectura>
<Tecnologías obligatorias>
Frontend
24.	React con JavaScript.
25.	Vite.
26.	React Router.
27.	Axios para consumir la API REST.
28.	Material UI.
29.	HTML y CSS.
30.	Diseño responsive.
31.	Componente de árbol para representar áreas, subáreas y problemas.
32.	No utilizar TypeScript en la primera versión.
Backend
33.	Java 21.
34.	Una versión estable y compatible de Spring Boot; no utilizar SNAPSHOT, M1, M2, RC ni versiones experimentales.
35.	Maven.
36.	Spring Web.
37.	Spring Data JPA.
38.	Spring Validation.
39.	Spring Security.
40.	PostgreSQL Driver.
41.	Flyway.
42.	OpenAPI/Swagger.
Base de datos
43.	PostgreSQL para almacenar usuarios, áreas, subáreas, problemas, soluciones, pasos, etiquetas, auditoría y metadatos de las imágenes.
</Tecnologías obligatorias>
<Funcionalidades>
1. Gestión de áreas
La aplicación debe permitir crear, consultar, modificar y desactivar áreas, evitando nombres duplicados.
Ejemplos: Rentas, Obras Públicas, Recursos Humanos y Sistemas.
2. Gestión de subáreas
Cada subárea deberá pertenecer obligatoriamente a un área.
44.	Crear subáreas.
45.	Consultar subáreas.
46.	Modificar subáreas.
47.	Desactivar subáreas.
48.	Evitar nombres duplicados dentro de la misma área.
Ejemplos dentro de Rentas: Catastro, Comercio y Notificaciones.
3. Gestión de problemas
Cada problema deberá pertenecer obligatoriamente a una subárea y contener:
49.	Identificador.
50.	Título.
51.	Descripción detallada del error o problema.
52.	Cero, una o varias imágenes.
53.	Fecha de creación.
54.	Fecha de última modificación.
55.	Estado: borrador, publicado, en revisión, resuelto o archivado.
56.	Usuario creador.
57.	Usuario que realizó la última modificación.
58.	Palabras clave o etiquetas opcionales.
La aplicación deberá permitir crear, consultar, modificar, desactivar, archivar y cambiar de subárea un problema; además de incorporar, visualizar, ordenar y eliminar imágenes.
4. Gestión de soluciones
Cada problema podrá tener una o varias soluciones. Cada solución deberá incluir:
59.	Identificador.
60.	Título opcional.
61.	Descripción detallada.
62.	Uno o varios pasos ordenados.
63.	Cero, una o varias imágenes.
64.	Fecha de creación.
65.	Fecha de última modificación.
66.	Usuario creador.
67.	Indicador de solución principal o recomendada.
No todos los problemas o soluciones tendrán imágenes, pero la mayoría podrá incluir capturas de pantalla.
5. Árbol de navegación
68.	Expandir y contraer áreas.
69.	Expandir y contraer subáreas.
70.	Seleccionar un problema.
71.	Mostrar en un panel lateral el contenido completo del problema seleccionado.
72.	Actualizar el árbol cuando se crea, modifica, mueve o desactiva información.
73.	Resaltar visualmente el elemento seleccionado.
6. Visualización del problema
74.	Área y subárea.
75.	Título y estado.
76.	Descripción completa.
77.	Imágenes del error.
78.	Solución principal.
79.	Otras soluciones disponibles.
80.	Pasos e imágenes de cada solución.
81.	Etiquetas.
82.	Fechas y usuarios responsables.
Las imágenes deberán poder ampliarse al seleccionarlas.
7. Editor de contenido
Evaluar TipTap para permitir títulos, subtítulos, negrita, listas, enlaces, bloques de código y asociación de imágenes.
El contenido enriquecido deberá sanitizarse en frontend y backend para impedir la ejecución de HTML o scripts maliciosos.
8. Usuarios y permisos
Implementar autenticación y autorización con los siguientes roles:
ADMINISTRADOR: gestiona usuarios, áreas, subáreas, problemas y soluciones; puede consultar todo.
EDITOR: crea y modifica problemas y soluciones; consulta áreas y subáreas; no gestiona usuarios ni elimina áreas.
LECTOR: solamente consulta y busca información publicada.
La API deberá validar los permisos en el backend. No depender únicamente de ocultar botones en React.
9. Imágenes
Guardar los archivos físicamente en una carpeta protegida. Guardar en PostgreSQL solamente sus metadatos, ubicación y relaciones. No almacenar imágenes grandes como Base64.
Cada imagen deberá registrar:
83.	Identificador.
84.	Nombre original.
85.	Nombre interno seguro.
86.	Tipo MIME.
87.	Tamaño.
88.	Ubicación.
89.	Texto alternativo o descripción.
90.	Fecha de carga.
91.	Usuario que la cargó.
92.	Problema o solución relacionados.
Validar PNG, JPG, JPEG y WebP; tamaño máximo configurable; nombres seguros; contenido real del archivo; y rechazo de ejecutables.
10. Búsqueda
Crear un buscador general por título, descripción del problema, contenido de las soluciones, etiquetas, área y subárea.
Los resultados deberán mostrar título, área, subárea, fragmento relevante, estado y fecha de última modificación.
Comenzar con búsqueda de texto de PostgreSQL e índices apropiados. Explicar cuándo tendría sentido incorporar otro motor de búsqueda.
11. Historial y auditoría
Registrar quién creó y modificó áreas, subáreas, problemas y soluciones, junto con las fechas correspondientes.
Analizar una estrategia de versionado para recuperar el contenido anterior de un problema o solución modificado.
</Funcionalidades>
<Modelo inicial de datos>
Analizá y diseñá las entidades necesarias. Como referencia, considerar:
93.	Usuario
94.	Rol
95.	Area
96.	Subarea
97.	Problema
98.	Solucion
99.	PasoSolucion
100.	Imagen
101.	Etiqueta
102.	ProblemaEtiqueta
103.	RegistroAuditoria
Relaciones esperadas:
104.	Un área tiene muchas subáreas.
105.	Una subárea pertenece a un área.
106.	Una subárea tiene muchos problemas.
107.	Un problema pertenece a una subárea.
108.	Un problema tiene cero o varias soluciones.
109.	Una solución pertenece a un problema.
110.	Una solución tiene varios pasos ordenados.
111.	Un problema y una solución pueden tener varias imágenes.
112.	Un problema puede tener varias etiquetas y una etiqueta puede asociarse a varios problemas.
Utilizar eliminación lógica mediante un campo activo, archivado o fecha de baja para conservar el historial y evitar pérdida accidental de información.
</Modelo inicial de datos>
<Interfaz esperada>
113.	Encabezado con nombre de la aplicación y usuario conectado.
114.	Menú lateral izquierdo con el árbol de áreas, subáreas y problemas.
115.	Buscador en la parte superior.
116.	Panel principal para mostrar el problema seleccionado.
117.	Botones de crear, editar, archivar o eliminar según permisos.
118.	Formularios claros para cargar problemas, soluciones, pasos e imágenes.
119.	Vista previa de imágenes antes de guardar.
120.	Mensajes comprensibles de éxito y error.
121.	Confirmación antes de desactivar o archivar información.
El diseño deberá ser sencillo, profesional, accesible y responsive.
</Interfaz esperada>
<Arquitectura y organización>
Implementar un monolito modular. No crear microservicios.
backend/src/main/java/.../conocimiento/
├── areas/
├── problemas/
├── soluciones/
├── archivos/
├── usuarios/
├── seguridad/
├── busqueda/
├── auditoria/
├── configuracion/
└── compartido/

Dentro de cada módulo, separar cuando corresponda controller, service, repository, entity, dto, mapper y exception.
No exponer directamente entidades JPA desde los controladores. Utilizar DTO para solicitudes y respuestas.
122.	Validación de datos.
123.	Manejo global de excepciones.
124.	Códigos HTTP correctos.
125.	Paginación y ordenamiento.
126.	Variables de entorno.
127.	Perfiles de desarrollo y producción.
128.	Documentación OpenAPI/Swagger.
129.	Auditoría de operaciones importantes.
</Arquitectura y organización>
<API REST>
Diseñar los endpoints necesarios. Como referencia:
130.	/api/auth
131.	/api/usuarios
132.	/api/areas
133.	/api/areas/{id}/subareas
134.	/api/subareas
135.	/api/problemas
136.	/api/problemas/{id}/soluciones
137.	/api/problemas/{id}/imagenes
138.	/api/soluciones/{id}/imagenes
139.	/api/etiquetas
140.	/api/arbol
141.	/api/busqueda
Definir para cada endpoint: método HTTP, URL, parámetros, DTO de entrada, DTO de salida, rol requerido y posibles códigos de respuesta.
</API REST>
<Seguridad>
142.	Spring Security con JWT.
143.	BCrypt para contraseñas.
144.	Autorización por roles en el backend.
145.	Validación de datos en frontend y backend.
146.	Sanitización del texto enriquecido.
147.	Restricción de tipos y tamaños de archivo.
148.	Verificación del contenido real de los archivos.
149.	Manejo seguro de errores sin exponer información interna.
150.	CORS restringido.
151.	Consultas parametrizadas mediante JPA.
152.	Protección contra XSS.
153.	Claves y contraseñas fuera del repositorio mediante variables de entorno.
154.	Auditoría de operaciones relevantes.
No integrar Keycloak en la primera versión. Dejar la arquitectura preparada para poder reemplazar o integrar posteriormente el mecanismo de autenticación si la organización requiere SSO, LDAP o Active Directory.
</Seguridad>
<Entregables y forma de trabajo>
Antes de escribir código:
155.	Resumí cómo entendiste el sistema.
156.	Realizá solamente las preguntas que cambien materialmente el diseño.
157.	Diseñá la arquitectura general.
158.	Presentá el modelo de datos y sus relaciones.
159.	Proponé la estructura de carpetas del frontend y backend.
160.	Definí el flujo de autenticación.
161.	Definí cómo se almacenarán y recuperarán las imágenes.
162.	Diseñá los endpoints principales.
163.	Dividí el desarrollo en etapas pequeñas.
164.	Esperá mi aprobación antes de generar todos los archivos.
Después de aprobar el diseño, generar por etapas:
165.	Proyecto backend Spring Boot.
166.	Proyecto frontend React.
167.	Migraciones Flyway.
168.	Configuración PostgreSQL.
169.	Datos iniciales de ejemplo.
170.	Documentación de instalación.
171.	Documentación de endpoints.
172.	Pruebas unitarias principales.
173.	Pruebas básicas de integración.
174.	README con instrucciones paso a paso.
</Entregables y forma de trabajo>
<Etapas sugeridas>
Etapa 1 - Diseño y estructura inicial
175.	Análisis funcional.
176.	Arquitectura.
177.	Modelo de datos.
178.	Creación del backend.
179.	Conexión PostgreSQL.
180.	Migraciones Flyway.
181.	CRUD de áreas y subáreas.
Etapa 2 - Conocimiento e imágenes
182.	CRUD de problemas.
183.	CRUD de soluciones y pasos.
184.	Carga y consulta de imágenes.
185.	Validaciones.
Etapa 3 - Frontend
186.	Proyecto React con Vite.
187.	Árbol jerárquico.
188.	Formularios.
189.	Visualización de problemas y soluciones.
190.	Vista previa de imágenes.
Etapa 4 - Seguridad
191.	Autenticación JWT.
192.	Roles y permisos.
193.	Auditoría.
194.	Protección de archivos.
Etapa 5 - Cierre
195.	Búsqueda.
196.	Paginación.
197.	Pruebas.
198.	Mejoras de seguridad.
199.	Documentación final.
</Etapas sugeridas>
<Restricciones>
200.	No generar todo el sistema en un único archivo ni en una sola respuesta.
201.	No mezclar lógica de negocio en controladores.
202.	No exponer entidades JPA directamente.
203.	No guardar contraseñas sin hash.
204.	No confiar únicamente en validaciones del frontend.
205.	No permitir archivos ejecutables.
206.	No almacenar permanentemente imágenes grandes como Base64.
207.	No eliminar físicamente información importante sin considerar auditoría e historial.
208.	No incorporar tecnologías adicionales sin explicar su necesidad y pedir aprobación.
209.	No utilizar microservicios, RabbitMQ, MongoDB, Elasticsearch, Keycloak ni Docker en la primera versión.
210.	No asumir conocimientos previos no demostrados.
211.	Utilizar nombres claros y consistentes; evitar mezclar español e inglés sin criterio.
212.	Antes de cada etapa, explicar qué se implementará.
213.	Al finalizar cada etapa, indicar cómo ejecutar, probar y verificar lo desarrollado.
</Restricciones>
<Criterios de aceptación>
214.	Un administrador puede crear áreas y subáreas.
215.	Las áreas, subáreas y problemas se visualizan como árbol.
216.	Se crean problemas dentro de una subárea.
217.	Cada problema admite descripción e imágenes opcionales.
218.	Cada problema admite una o varias soluciones.
219.	Cada solución admite pasos, descripción e imágenes opcionales.
220.	Los usuarios pueden buscar problemas.
221.	Al seleccionar un problema se muestra toda su información.
222.	Los permisos se controlan correctamente en el backend.
223.	Los datos persisten en PostgreSQL.
224.	Las imágenes se recuperan y visualizan de manera segura.
225.	La aplicación maneja errores y validaciones.
226.	Existen instrucciones claras para instalar, ejecutar y probar el sistema.
</Criterios de aceptación>
<Instrucción inicial>
Comenzá únicamente con el análisis funcional, las preguntas realmente necesarias, la arquitectura propuesta, el modelo de datos, la estructura de carpetas, la estrategia de imágenes, la seguridad y el plan por etapas. No generes todavía el proyecto completo hasta recibir mi aprobación.
</Instrucción inicial>
