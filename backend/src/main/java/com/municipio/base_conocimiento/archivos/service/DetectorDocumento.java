package com.municipio.base_conocimiento.archivos.service;

import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DetectorDocumento {
    public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public record TipoDocumento(String mime, String extension) {}
    public TipoDocumento detectar(byte[] bytes, String nombre) {
        String extension = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
        if (extension.endsWith(".pdf") && bytes.length > 12
                && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")
                && new String(bytes, Math.max(0, bytes.length - 1024), Math.min(1024, bytes.length), StandardCharsets.ISO_8859_1).contains("%%EOF")) {
            return new TipoDocumento("application/pdf", "pdf");
        }
        if (extension.endsWith(".docx") && docx(bytes)) return new TipoDocumento(DOCX, "docx");
        throw new ArchivoInvalidoException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "DOCUMENTO_INVALIDO",
                "El archivo debe ser un PDF o Word (.docx) válido, sin macros.");
    }
    private boolean docx(byte[] bytes) {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            Map<String, byte[]> required = new HashMap<>();
            Set<String> names = new HashSet<>();
            long total = 0;
            byte[] buffer = new byte[8192];
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                String name = entry.getName();
                if (!names.add(name) || names.size() > 2000 || name.contains("..") || name.startsWith("/")
                        || name.toLowerCase(Locale.ROOT).contains("vbaproject")) return false;
                boolean keep = name.equals("[Content_Types].xml") || name.equals("word/document.xml");
                var data = new ByteArrayOutputStream();
                for (int n; (n = zip.read(buffer)) != -1;) {
                    total += n;
                    if (total > 50L * 1024 * 1024 || (keep && data.size() + n > 5 * 1024 * 1024)) return false;
                    if (keep) data.write(buffer, 0, n);
                }
                if (keep) required.put(name, data.toByteArray());
            }
            if (!required.keySet().containsAll(Set.of("[Content_Types].xml", "word/document.xml"))) return false;
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var parser = factory.newDocumentBuilder();
            var document = parser.parse(new ByteArrayInputStream(required.get("word/document.xml"))).getDocumentElement();
            if (!"document".equals(document.getLocalName()) || !Set.of(
                    "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
                    "http://purl.oclc.org/ooxml/wordprocessingml/main").contains(document.getNamespaceURI())) return false;
            var types = parser.parse(new ByteArrayInputStream(required.get("[Content_Types].xml")));
            var overrides = types.getElementsByTagNameNS("http://schemas.openxmlformats.org/package/2006/content-types", "Override");
            boolean main = false;
            for (int i = 0; i < overrides.getLength(); i++) {
                var item = (org.w3c.dom.Element) overrides.item(i);
                String type = item.getAttribute("ContentType");
                if (type.toLowerCase(Locale.ROOT).contains("macroenabled")) return false;
                if (item.getAttribute("PartName").equals("/word/document.xml")
                        && type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml")) main = true;
            }
            return main;
        } catch (Exception ex) { return false; }
    }
}
