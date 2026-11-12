package com.municipio.base_conocimiento.archivos.service;
import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import java.util.Base64;
import org.junit.jupiter.api.Test;
class DetectorImagenTest {
    private final DetectorImagen detector=new DetectorImagen();
    @Test void detectaPngReal(){byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");assertThat(detector.detectar(png).mime()).isEqualTo("image/png");}
    @Test void rechazaFirmaSinImagenYContenidoArbitrario(){assertThatThrownBy(()->detector.detectar(new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a})).isInstanceOf(ArchivoInvalidoException.class);assertThatThrownBy(()->detector.detectar(new byte[]{1,2})).isInstanceOf(ArchivoInvalidoException.class);}
}
