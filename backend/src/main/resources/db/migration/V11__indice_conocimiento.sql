-- Índice derivado: no modifica ni reemplaza el conocimiento original.
CREATE TABLE rag_control (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    cursor_id BIGINT NOT NULL DEFAULT 0,
    generacion BIGINT NOT NULL DEFAULT 0,
    token VARCHAR(36),
    vence_en TIMESTAMP WITH TIME ZONE
);
INSERT INTO rag_control(id) VALUES (1);
CREATE TABLE rag_estado (
    problema_id BIGINT PRIMARY KEY REFERENCES problemas(id),
    huella VARCHAR(64) NOT NULL,
    estado VARCHAR(12) NOT NULL CHECK (estado IN ('PENDIENTE','LISTO','ERROR')),
    intentos INTEGER NOT NULL DEFAULT 0,
    codigo_error VARCHAR(64),
    reintentar_en TIMESTAMP WITH TIME ZONE,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE rag_fragmentos (
    id BIGSERIAL PRIMARY KEY,
    problema_id BIGINT NOT NULL REFERENCES problemas(id),
    tipo VARCHAR(12) NOT NULL CHECK(tipo IN ('PROBLEMA','SOLUCION','PASO','ADJUNTO')),
    origen_id BIGINT NOT NULL,
    solucion_id BIGINT REFERENCES soluciones(id),
    paso_id BIGINT REFERENCES pasos_solucion(id),
    archivo_id BIGINT REFERENCES archivos(id),
    area_id BIGINT NOT NULL REFERENCES areas(id),
    subarea_id BIGINT NOT NULL REFERENCES subareas(id),
    estado VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL,
    version_origen BIGINT NOT NULL,
    version_problema BIGINT NOT NULL,
    modificado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    pagina INTEGER,
    posicion INTEGER NOT NULL,
    texto TEXT NOT NULL,
    huella VARCHAR(64) NOT NULL,
    modelo VARCHAR(200),
    dimension INTEGER,
    vector_json TEXT,
    indexado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_rag_fragmento UNIQUE(problema_id,posicion)
);
CREATE INDEX idx_rag_fragmentos_problema ON rag_fragmentos(problema_id);
CREATE INDEX idx_rag_estado_reintento ON rag_estado(estado,reintentar_en);

-- Puerta de lectura para la recuperación posterior: valida permisos actuales,
-- no confía solamente en el estado copiado al indexar.
CREATE VIEW rag_fragmentos_vigentes AS
SELECT f.* FROM rag_fragmentos f
JOIN rag_estado e ON e.problema_id=f.problema_id AND e.estado='LISTO' AND e.huella=f.huella
JOIN problemas p ON p.id=f.problema_id
JOIN subareas sub ON sub.id=p.subarea_id
JOIN areas a ON a.id=sub.area_id
LEFT JOIN soluciones s ON s.id=f.solucion_id
LEFT JOIN pasos_solucion ps ON ps.id=f.paso_id
LEFT JOIN archivos ar ON ar.id=f.archivo_id
WHERE p.activo=TRUE AND p.estado='PUBLICADO' AND sub.activo=TRUE AND a.activo=TRUE
AND p.version=f.version_problema AND sub.id=f.subarea_id AND a.id=f.area_id
AND (f.solucion_id IS NULL OR s.activo=TRUE)
AND (f.paso_id IS NULL OR ps.activo=TRUE)
AND (f.archivo_id IS NULL OR ar.activo=TRUE)
AND ((f.tipo='PROBLEMA' AND p.version=f.version_origen)
  OR (f.tipo='SOLUCION' AND s.version=f.version_origen)
  OR (f.tipo='PASO' AND ps.version=f.version_origen)
  OR (f.tipo='ADJUNTO' AND ar.version=f.version_origen));
