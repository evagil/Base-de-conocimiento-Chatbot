package com.municipio.base_conocimiento.archivos.service;

import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DetectorImagen {
    public TipoImagen detectar(byte[] b) {
        if (b.length>=8 && Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a}) && decodificable(b)) return new TipoImagen("image/png","png");
        if (b.length>=3 && (b[0]&255)==0xff && (b[1]&255)==0xd8 && (b[2]&255)==0xff && decodificable(b)) return new TipoImagen("image/jpeg","jpg");
        if (b.length>=16 && "RIFF".equals(new String(b,0,4,StandardCharsets.US_ASCII)) && "WEBP".equals(new String(b,8,4,StandardCharsets.US_ASCII)) && new String(b,12,3,StandardCharsets.US_ASCII).startsWith("VP8")) return new TipoImagen("image/webp","webp");
        throw new ArchivoInvalidoException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"IMAGEN_INVALIDA","El contenido real no corresponde a PNG, JPEG o WebP.");
    }
    private boolean decodificable(byte[] bytes) { try { return ImageIO.read(new ByteArrayInputStream(bytes)) != null; } catch (Exception ex) { return false; } }
    public record TipoImagen(String mime,String extension){}
}
