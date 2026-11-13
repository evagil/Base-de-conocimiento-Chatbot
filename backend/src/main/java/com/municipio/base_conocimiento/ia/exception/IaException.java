package com.municipio.base_conocimiento.ia.exception;

import org.springframework.http.HttpStatus;

public class IaException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;

    public IaException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }
}
