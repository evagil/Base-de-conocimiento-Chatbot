package com.municipio.base_conocimiento.documentos.proceso;

import java.util.List;

public final class ProtocoloWorker {
    private ProtocoloWorker() { }
    public record Limites(String extension, int maxPaginas, int maxOcrPaginas, int maxCaracteres,
            long maxPixeles, long maxTemporalBytes, int dpi) { }
    public record Parte(Integer pagina, String texto, String imagen) { }
    public record Resultado(Integer paginas, List<Parte> partes, String error) { }
}
