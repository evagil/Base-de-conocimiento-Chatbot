ALTER TABLE usuarios ADD COLUMN usuario VARCHAR(60);
ALTER TABLE usuarios ADD COLUMN usuario_normalizado VARCHAR(60);

UPDATE usuarios
SET usuario = COALESCE(NULLIF(lower(substring(email_normalizado from 1 for position('@' in email_normalizado) - 1)), ''), 'usuario-' || id),
    usuario_normalizado = COALESCE(NULLIF(lower(substring(email_normalizado from 1 for position('@' in email_normalizado) - 1)), ''), 'usuario-' || id)
WHERE usuario IS NULL;

ALTER TABLE usuarios ALTER COLUMN usuario SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN usuario_normalizado SET NOT NULL;
CREATE UNIQUE INDEX ux_usuarios_usuario_normalizado ON usuarios(usuario_normalizado);
