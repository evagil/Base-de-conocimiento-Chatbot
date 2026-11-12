-- Earlier no-op saves could insert history for a version that remained current.
-- Preserve that history and advance only affected live versions. Open forms must reload.
UPDATE soluciones s
SET version = (SELECT MAX(v.numero_version) + 1 FROM versiones_solucion v WHERE v.solucion_id = s.id)
WHERE EXISTS (
    SELECT 1 FROM versiones_solucion v WHERE v.solucion_id = s.id AND v.numero_version >= s.version
);

UPDATE problemas p
SET version = (SELECT MAX(v.numero_version) + 1 FROM versiones_problema v WHERE v.problema_id = p.id)
WHERE EXISTS (
    SELECT 1 FROM versiones_problema v WHERE v.problema_id = p.id AND v.numero_version >= p.version
);
