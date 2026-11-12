CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    email_normalizado VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modificado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMINISTRADOR','EDITOR','LECTOR'))
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    vence_en TIMESTAMP WITH TIME ZONE NOT NULL,
    revocado_en TIMESTAMP WITH TIME ZONE,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_refresh_tokens_usuario ON refresh_tokens(usuario_id);
CREATE INDEX ix_refresh_tokens_vence ON refresh_tokens(vence_en);

CREATE TABLE eventos_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_email VARCHAR(180) NOT NULL,
    accion VARCHAR(12) NOT NULL,
    recurso VARCHAR(500) NOT NULL,
    codigo_http INTEGER NOT NULL,
    direccion_ip VARCHAR(64),
    ocurrido_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_auditoria_fecha ON eventos_auditoria(ocurrido_en DESC);
CREATE INDEX ix_auditoria_usuario ON eventos_auditoria(usuario_email);
