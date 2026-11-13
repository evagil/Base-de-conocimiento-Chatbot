package com.municipio.base_conocimiento.documentos.worker;

import java.io.*;

public final class SalidaLimitada extends FilterOutputStream {
    private final long maximo;
    private final String codigo;
    private long escritos;
    public SalidaLimitada(OutputStream salida, long maximo, String codigo) {
        super(salida); this.maximo = maximo; this.codigo = codigo;
    }
    @Override public void write(int valor) throws IOException {
        comprobar(1); out.write(valor);
    }
    @Override public void write(byte[] bytes, int inicio, int cantidad) throws IOException {
        comprobar(cantidad); out.write(bytes, inicio, cantidad);
    }
    private void comprobar(int cantidad) {
        if (cantidad > maximo - escritos) throw new LimiteDocumento(codigo);
        escritos += cantidad;
    }
}
