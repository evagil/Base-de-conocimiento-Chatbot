-- Preserve content and history while invalidating forms opened before conversion.
UPDATE problemas
SET estado = CASE estado
        WHEN 'EN_REVISION' THEN 'BORRADOR'
        WHEN 'RESUELTO' THEN 'PUBLICADO'
    END,
    version = version + 1,
    modificado_en = CURRENT_TIMESTAMP
WHERE estado IN ('EN_REVISION', 'RESUELTO');

ALTER TABLE problemas DROP CONSTRAINT ck_problemas_estado;
ALTER TABLE problemas ADD CONSTRAINT ck_problemas_estado
    CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'ARCHIVADO'));
