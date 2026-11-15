package com.municipio.base_conocimiento.archivos.privacidad;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.municipio.base_conocimiento.archivos.controller.ArchivoController;
import com.municipio.base_conocimiento.archivos.service.ArchivoService;
import com.municipio.base_conocimiento.compartido.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;

class ArchivoPrivacidadControllerTest {
 @Test void imagenSoloDevuelvePixelesProtegidosYNoPermiteCacheDelOriginal(){
  var archivos=mock(ArchivoService.class);var protector=mock(ProtectorImagenes.class);byte[] original={1},protegida={2};
  when(archivos.contenido(7L)).thenReturn(new ArchivoService.ContenidoArchivo("image/jpeg","nombre-privado.jpg",original));when(protector.proteger(original)).thenReturn(protegida);
  var respuesta=new ArchivoController(archivos,protector).contenido(7L);
  assertThat(respuesta.getBody()).isEqualTo(protegida);assertThat(respuesta.getHeaders().getContentType().toString()).isEqualTo("image/png");
  assertThat(respuesta.getHeaders().getCacheControl()).isEqualTo("no-store");assertThat(respuesta.getHeaders().getContentDisposition().getFilename()).isEqualTo("imagen-protegida.png");
 }
 @Test void documentosNoSeRedactanYPermisosSeCompruebanAntesDeProcesarImagen(){
  var archivos=mock(ArchivoService.class);var protector=mock(ProtectorImagenes.class);var controlador=new ArchivoController(archivos,protector);
  when(archivos.contenido(1L)).thenReturn(new ArchivoService.ContenidoArchivo("application/pdf","manual.pdf",new byte[]{5}));
  assertThat(controlador.contenido(1L).getBody()).containsExactly(5);verifyNoInteractions(protector);
  when(archivos.contenido(2L)).thenThrow(new RecursoNoEncontradoException("No visible"));assertThatThrownBy(()->controlador.contenido(2L)).isInstanceOf(RecursoNoEncontradoException.class);verifyNoInteractions(protector);
 }
}
