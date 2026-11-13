package com.municipio.base_conocimiento.documentos;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public final class DocumentosFixtures {
    private DocumentosFixtures() { }
    public static byte[] pdf(String... textos) throws IOException {
        try (PDDocument doc = new PDDocument(); var salida = new ByteArrayOutputStream()) {
            for (String texto : textos) {
                PDPage pagina = new PDPage(); doc.addPage(pagina);
                if (texto != null) {
                    try (var contenido = new PDPageContentStream(doc, pagina)) {
                        contenido.beginText(); contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        contenido.newLineAtOffset(30, 700); contenido.showText(texto); contenido.endText();
                    }
                }
            }
            doc.save(salida); return salida.toByteArray();
        }
    }
    public static byte[] imagen(String formato) throws IOException {
        BufferedImage imagen = new BufferedImage(2400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico = imagen.createGraphics();
        grafico.setColor(Color.WHITE); grafico.fillRect(0, 0, imagen.getWidth(), imagen.getHeight());
        grafico.setColor(Color.BLACK); grafico.setFont(new Font("SansSerif", Font.BOLD, 70));
        grafico.drawString("ERROR E17 REINICIAR APLICACION", 50, 210); grafico.dispose();
        try (var salida = new ByteArrayOutputStream()) { ImageIO.write(imagen, formato, salida); return salida.toByteArray(); }
    }
    public static byte[] escaneado(int paginas) throws IOException {
        try (PDDocument doc = new PDDocument(); var salida = new ByteArrayOutputStream()) {
            var imagen = PDImageXObject.createFromByteArray(doc, imagen("jpg"), "captura");
            for (int i = 0; i < paginas; i++) {
                var pagina = new PDPage(); doc.addPage(pagina);
                try (var contenido = new PDPageContentStream(doc, pagina)) { contenido.drawImage(imagen, 30, 500, 550, 92); }
            }
            doc.save(salida); return salida.toByteArray();
        }
    }
    public static byte[] protegido(String password) throws IOException {
        try (PDDocument doc = new PDDocument(); var salida = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            var proteccion = new StandardProtectionPolicy("owner-test", password, new AccessPermission());
            proteccion.setEncryptionKeyLength(128); doc.protect(proteccion); doc.save(salida); return salida.toByteArray();
        }
    }
    public static byte[] docx(String texto) throws IOException {
        try (var doc = new XWPFDocument(); var salida = new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText(texto);
            doc.createTable(1, 2).getRow(0).getCell(0).setText("Primera celda");
            doc.getTables().getFirst().getRow(0).getCell(1).setText("Segunda celda");
            doc.createParagraph().createHyperlinkRun("http://127.0.0.1:1/no-consultar").setText("Portal de ayuda");
            doc.write(salida); return salida.toByteArray();
        }
    }
}
