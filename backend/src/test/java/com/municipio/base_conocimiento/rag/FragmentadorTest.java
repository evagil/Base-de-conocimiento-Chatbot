package com.municipio.base_conocimiento.rag;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.rag.config.ConfiguracionIndice;
import com.municipio.base_conocimiento.rag.service.Fragmentador;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class FragmentadorTest {
    static ConfiguracionIndice config(boolean habilitado,boolean embeddings){return new ConfiguracionIndice(habilitado,embeddings,200,30,128,100,3,Duration.ofSeconds(120));}
    @Test void normalizaYDivideConSolapamientoSinRomperUnicode(){
        var fragmentador=new Fragmentador(config(true,false));
        assertThat(fragmentador.dividir(null)).isEmpty();assertThat(fragmentador.dividir(" \n\t ")).isEmpty();
        assertThat(fragmentador.dividir("aplicacio\u0301n\u0000\nE17")).containsExactly("aplicación E17");
        String texto="x".repeat(199)+"😀"+"y".repeat(220);
        var partes=fragmentador.dividir(texto);
        assertThat(partes).allSatisfy(p->{assertThat(p.length()).isLessThanOrEqualTo(200);assertThat(Character.isLowSurrogate(p.charAt(0))).isFalse();assertThat(Character.isHighSurrogate(p.charAt(p.length()-1))).isFalse();});
        assertThat(partes.get(1)).startsWith(partes.getFirst().substring(169));
        assertThat(fragmentador.dividir("Palabra ".repeat(80))).hasSizeGreaterThan(1);
        var limitado=new Fragmentador(new ConfiguracionIndice(true,false,200,0,1,1,0,Duration.ofSeconds(1)));
        assertThatThrownBy(()->limitado.dividir("x".repeat(300))).hasMessage("LIMITE_FRAGMENTOS");
    }
    @Test void validaConfiguracionAntesDeArrancar(){
        for(int opcion=1;opcion<=13;opcion++){
            int i=opcion;
            assertThatThrownBy(()->new ConfiguracionIndice(true,true,i==1?199:i==2?1201:600,i==3?-1:i==4?300:80,
                i==5?0:i==6?257:128,i==7?0:i==8?201:100,i==9?-1:i==10?6:3,
                i==11?null:Duration.ofSeconds(i==12?0:i==13?121:120))).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
