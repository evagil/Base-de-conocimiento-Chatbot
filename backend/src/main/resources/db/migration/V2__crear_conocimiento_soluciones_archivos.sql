CREATE TABLE problemas (
    id BIGSERIAL PRIMARY KEY,
    subarea_id BIGINT NOT NULL REFERENCES subareas(id),
    titulo VARCHAR(200) NOT NULL,
    descripcion_html TEXT NOT NULL,
    descripcion_texto TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modificado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_problemas_estado CHECK (estado IN ('BORRADOR','EN_REVISION','PUBLICADO','RESUELTO','ARCHIVADO'))
);

CREATE TABLE etiquetas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL,
    nombre_normalizado VARCHAR(60) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modificado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE problema_etiquetas (
    problema_id BIGINT NOT NULL REFERENCES problemas(id),
    etiqueta_id BIGINT NOT NULL REFERENCES etiquetas(id),
    PRIMARY KEY (problema_id, etiqueta_id)
);

CREATE TABLE soluciones (
    id BIGSERIAL PRIMARY KEY,
    problema_id BIGINT NOT NULL REFERENCES problemas(id),
    titulo VARCHAR(200),
    descripcion_html TEXT NOT NULL,
    descripcion_texto TEXT NOT NULL,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modificado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_soluciones_orden CHECK (orden >= 0)
);

CREATE TABLE pasos_solucion (
    id BIGSERIAL PRIMARY KEY,
    solucion_id BIGINT NOT NULL REFERENCES soluciones(id),
    titulo VARCHAR(200),
    contenido_html TEXT NOT NULL,
    contenido_texto TEXT NOT NULL,
    orden INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modificado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_paso_solucion_orden UNIQUE (solucion_id, orden),
    CONSTRAINT ck_pasos_orden CHECK (orden >= 1)
);

CREATE TABLE archivos (
    id BIGSERIAL PRIMARY KEY,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_interno VARCHAR(100) NOT NULL UNIQUE,
    tipo_mime VARCHAR(50) NOT NULL,
    tamanio_bytes BIGINT NOT NULL,
    ruta_relativa VARCHAR(500) NOT NULL UNIQUE,
    hash_sha256 VARCHAR(64) NOT NULL,
    texto_alternativo VARCHAR(300) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modificado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_archivo_tamanio CHECK (tamanio_bytes > 0),
    CONSTRAINT ck_archivo_orden CHECK (orden >= 0)
);

CREATE TABLE archivo_problemas (
    archivo_id BIGINT PRIMARY KEY REFERENCES archivos(id),
    problema_id BIGINT NOT NULL REFERENCES problemas(id)
);

CREATE TABLE archivo_soluciones (
    archivo_id BIGINT PRIMARY KEY REFERENCES archivos(id),
    solucion_id BIGINT NOT NULL REFERENCES soluciones(id)
);

CREATE TABLE archivo_pasos (
    archivo_id BIGINT PRIMARY KEY REFERENCES archivos(id),
    paso_id BIGINT NOT NULL REFERENCES pasos_solucion(id)
);

CREATE TABLE versiones_problema (
    id BIGSERIAL PRIMARY KEY,
    problema_id BIGINT NOT NULL REFERENCES problemas(id),
    numero_version BIGINT NOT NULL,
    contenido_anterior TEXT NOT NULL,
    motivo VARCHAR(300),
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_version_problema UNIQUE (problema_id, numero_version)
);

CREATE TABLE versiones_solucion (
    id BIGSERIAL PRIMARY KEY,
    solucion_id BIGINT NOT NULL REFERENCES soluciones(id),
    numero_version BIGINT NOT NULL,
    contenido_anterior TEXT NOT NULL,
    motivo VARCHAR(300),
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_version_solucion UNIQUE (solucion_id, numero_version)
);

CREATE INDEX idx_problemas_subarea_estado ON problemas (subarea_id, activo, estado);
CREATE INDEX idx_soluciones_problema_orden ON soluciones (problema_id, activo, orden);
CREATE INDEX idx_pasos_solucion_orden ON pasos_solucion (solucion_id, activo, orden);
CREATE INDEX idx_archivo_problema ON archivo_problemas (problema_id);
CREATE INDEX idx_archivo_solucion ON archivo_soluciones (solucion_id);
CREATE INDEX idx_archivo_paso ON archivo_pasos (paso_id);
