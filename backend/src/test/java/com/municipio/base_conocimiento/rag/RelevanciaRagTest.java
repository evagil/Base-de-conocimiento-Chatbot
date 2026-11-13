package com.municipio.base_conocimiento.rag;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.rag.service.RelevanciaRag;
import com.municipio.base_conocimiento.rag.config.ConfiguracionConsultaRag;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;

class RelevanciaRagTest {
    @Test void normalizaBuscaPalabrasYCodigosSinConfundirE17ConE170(){
        var r=new RelevanciaRag();assertThat(r.terminos("¿Cómo corregir la APLICACIÓN, aplicación y E17?")).containsExactly("corregir","aplicacion","e17");
        assertThat(r.textual(List.of("error","e17"),"Error E17")).isEqualTo(1);
        assertThat(r.textual(List.of(),"Texto")).isZero();
        assertThat(r.coincideCodigo("E17","Error E170")).isFalse();assertThat(r.coincideCodigo("E17","Error E18")).isFalse();
        assertThat(r.coincideCodigo("E17 o E18","Error E18")).isTrue();assertThat(r.coincideCodigo("sin acceso","Detalle")).isTrue();
        assertThat(r.coincideCodigo("e17","ERROR E17.")).isTrue();assertThat(r.coincideCodigo("E17","XE17")).isFalse();
    }
    @Test void cosenoEsEstableConMagnitudesGrandesPequenasYVectoresInvalidos(){
        var r=new RelevanciaRag();assertThat(r.coseno(List.of(1.0,0.0),List.of(1.0,0.0))).isEqualTo(1);
        assertThat(r.coseno(List.of(1.0,0.0),List.of(0.0,1.0))).isZero();
        assertThat(r.coseno(List.of(1.0),List.of(-1.0))).isEqualTo(-1);
        assertThat(r.coseno(List.of(Double.MAX_VALUE,Double.MAX_VALUE),List.of(Double.MIN_VALUE,Double.MIN_VALUE))).isCloseTo(1,within(1e-12));
        for(List<Double> invalido:Arrays.<List<Double>>asList(null,List.of(),List.of(0.0),List.of(Double.NaN),List.of(Double.POSITIVE_INFINITY),Arrays.asList((Double)null)))
            assertThat(r.coseno(invalido,List.of(1.0))).isEqualTo(-1);
        assertThat(r.coseno(List.of(1.0),null)).isEqualTo(-1);
        assertThat(r.coseno(List.of(1.0),List.of(1.0,2.0))).isEqualTo(-1);
        assertThat(r.coseno(Collections.nCopies(8193,1.0),Collections.nCopies(8193,1.0))).isEqualTo(-1);
    }
    @Test void validaTodosLosLimitesDeConsulta(){
        for(int opcion=1;opcion<=15;opcion++){int i=opcion;
            assertThatThrownBy(()->new ConfiguracionConsultaRag(true,true,i==1?19:i==2?2001:1000,i==3?9:i==4?2001:1000,
                i==5?0:i==6?6:3,i==7?299:i==8?4001:1600,i==9?Double.NaN:i==10?0.2:i==11?1.1:0.65,
                i==12?null:Duration.ofSeconds(i==13?-1:i==14?0:i>=15?301:240))).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
