package com.municipio.base_conocimiento.compartido.sanitization;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class SanitizadorHtml {
    private final Safelist permitidos = Safelist.basicWithImages()
            .addTags("h1", "h2", "h3", "pre", "code")
            .addAttributes("a", "target", "rel")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    public String limpiar(String html) {
        if (html == null || html.isBlank()) return "";
        Document.OutputSettings salida = new Document.OutputSettings().prettyPrint(false);
        return Jsoup.clean(html, "", permitidos, salida);
    }

    public String limpiarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) return null;
        String limpio = Jsoup.clean(titulo, "", new Safelist().addTags("strong", "b", "em", "i", "s", "strike", "code"),
                new Document.OutputSettings().prettyPrint(false));
        return extraerTexto(limpio).isBlank() ? null : limpio;
    }

    public String extraerTexto(String htmlLimpio) {
        return Jsoup.parse(htmlLimpio == null ? "" : htmlLimpio).text();
    }
}
