package com.municipio.base_conocimiento.rag;
import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.rag.service.ExtractosCandidatos;
import org.junit.jupiter.api.Test;
class ExtractosCandidatosTest {
 @Test void devuelveSoloVentanasLiteralesAcotadasYRelevantes(){
  String texto="Error E17. Cerrar y volver a abrir la aplicación para corregir E17. Otro procedimiento que no corresponde.";
  var r=ExtractosCandidatos.preparar(texto,"¿Cómo corregir E17?");assertThat(r).hasSize(2).allSatisfy(s->{assertThat(s.length()).isBetween(15,160);assertThat(texto).contains(s);});assertThat(r.getFirst()).contains("corregir E17");
  String largo="texto ".repeat(100)+"😀 Solución documentada para error E17.";
  assertThat(ExtractosCandidatos.preparar(largo,"E17")).hasSizeLessThanOrEqualTo(3).allSatisfy(s->{assertThat(largo).contains(s);assertThat(s.length()).isLessThanOrEqualTo(160);});
  assertThat(ExtractosCandidatos.preparar("corto","E17")).isEmpty();
 }
}
