package com.municipio.base_conocimiento.archivos.service;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;

public class DetectorDocumentoTest {
    public static byte[] pdf() { return "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n%%EOF".getBytes(StandardCharsets.US_ASCII); }
    public static byte[] docx() throws IOException { return zip("word/document.xml", "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body/></w:document>", false); }
    private static byte[] zip(String name, String xml, boolean macro) throws IOException {
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry(); zip.putNextEntry(new ZipEntry(name)); zip.write(xml.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            if(macro){zip.putNextEntry(new ZipEntry("word/vbaProject.bin"));zip.write(1);zip.closeEntry();}
        }
        return output.toByteArray();
    }
    @Test void detectaContenidoYNoConfiaEnExtension() throws Exception {
        var detector = new DetectorDocumento();
        assertThat(detector.detectar(pdf(), "manual.PDF").mime()).isEqualTo("application/pdf");
        assertThat(detector.detectar(docx(), "manual.docx").mime()).isEqualTo(DetectorDocumento.DOCX);
        for(String name:new String[]{"falso.pdf","falso.docx","archivo.exe"})
            assertThatThrownBy(()->detector.detectar("texto".getBytes(),name)).isInstanceOf(ArchivoInvalidoException.class);
        assertThatThrownBy(()->detector.detectar(pdf(),null)).isInstanceOf(ArchivoInvalidoException.class);
        assertThatThrownBy(()->detector.detectar(pdf(),"manual.doc")).isInstanceOf(ArchivoInvalidoException.class);
        assertThatThrownBy(()->detector.detectar("%PDF-1.7 sin final".getBytes(),"manual.pdf")).isInstanceOf(ArchivoInvalidoException.class);
    }
    @Test void rechazaZipGenericoMacrosXmlExternoYExpansionExcesiva() throws Exception {
        var detector = new DetectorDocumento();
        byte[][] invalid = {
            zip("otro.xml","<document/>",false), zip("word/document.xml","<document/>",false),
            zip("word/document.xml","<document/>",true), zip("../word/document.xml","<document/>",false),
            zip("word/document.xml","<!DOCTYPE x SYSTEM 'file:///etc/passwd'><x/>",false),
            zip("word/document.xml"," ".repeat(5*1024*1024+1),false),
        };
        for(byte[] bytes:invalid) assertThatThrownBy(()->detector.detectar(bytes,"falso.docx")).isInstanceOf(ArchivoInvalidoException.class);
    }
}
