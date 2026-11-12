package com.municipio.base_conocimiento.compartido.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizadorTextoTest {
    @Test
    void normalizaAcentosMayusculasYEspacios() {
        assertThat(NormalizadorTexto.nombre("  Obras   Públicas ")).isEqualTo("obras publicas");
    }
}
