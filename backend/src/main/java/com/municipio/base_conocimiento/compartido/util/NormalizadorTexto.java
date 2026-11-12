package com.municipio.base_conocimiento.compartido.util;

import java.text.Normalizer;
import java.util.Locale;

public final class NormalizadorTexto {
    private NormalizadorTexto() {}

    public static String nombre(String valor) {
        if (valor == null) return null;
        String sinAcentos = Normalizer.normalize(valor.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
