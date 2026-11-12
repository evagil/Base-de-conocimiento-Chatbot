package com.municipio.base_conocimiento.compartido.sanitization;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
class SanitizadorHtmlTest {
    private final SanitizadorHtml sanitizador=new SanitizadorHtml();
    @Test void conservaFormatoSeguroYEliminaXss(){String limpio=sanitizador.limpiar("<h1>Título</h1><a href='javascript:x' onclick='x'>enlace</a><code>ok</code>");assertThat(limpio).contains("<h1>","<code>").doesNotContain("javascript","onclick");assertThat(sanitizador.extraerTexto(limpio)).contains("Título","enlace","ok");}
    @Test void manejaContenidoNuloOVacio(){assertThat(sanitizador.limpiar(null)).isEmpty();assertThat(sanitizador.limpiar("  ")).isEmpty();assertThat(sanitizador.extraerTexto(null)).isEmpty();}
    @Test void tituloConservaFormatoSinEnlacesNiScripts() {
        assertThat(sanitizador.limpiarTitulo("<p><strong>Menú</strong> <em>Configuración</em><script>alert(1)</script><a href='https://example.com'>Abrir</a></p>"))
                .contains("<strong>Menú</strong>", "<em>Configuración</em>", "Abrir")
                .doesNotContain("<p>", "<script", "<a", "alert");
        assertThat(sanitizador.limpiarTitulo("Título anterior")).isEqualTo("Título anterior");
        assertThat(sanitizador.limpiarTitulo(null)).isNull();
        assertThat(sanitizador.limpiarTitulo(" ")).isNull();
        assertThat(sanitizador.limpiarTitulo("<p></p>")).isNull();
    }
}
