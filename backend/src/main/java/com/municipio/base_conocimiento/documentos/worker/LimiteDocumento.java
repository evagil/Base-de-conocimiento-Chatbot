package com.municipio.base_conocimiento.documentos.worker;

public class LimiteDocumento extends RuntimeException {
    public LimiteDocumento(String codigo) { super(codigo); }
}
