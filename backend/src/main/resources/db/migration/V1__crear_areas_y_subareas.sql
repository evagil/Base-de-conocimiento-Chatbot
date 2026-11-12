CREATE TABLE areas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    nombre_normalizado VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modificado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_areas_nombre_normalizado UNIQUE (nombre_normalizado),
    CONSTRAINT ck_areas_orden_no_negativo CHECK (orden >= 0)
);

CREATE TABLE subareas (
    id BIGSERIAL PRIMARY KEY,
    area_id BIGINT NOT NULL REFERENCES areas(id),
    nombre VARCHAR(100) NOT NULL,
    nombre_normalizado VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modificado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_subareas_area_nombre UNIQUE (area_id, nombre_normalizado),
    CONSTRAINT ck_subareas_orden_no_negativo CHECK (orden >= 0)
);

CREATE INDEX idx_areas_activo_orden ON areas (activo, orden, nombre);
CREATE INDEX idx_subareas_area_activo_orden ON subareas (area_id, activo, orden, nombre);
