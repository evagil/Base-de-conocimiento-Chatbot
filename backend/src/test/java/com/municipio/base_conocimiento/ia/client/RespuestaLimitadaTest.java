package com.municipio.base_conocimiento.ia.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;

class RespuestaLimitadaTest {
    @Test void recibeFragmentosYRechazaElExcesoAntesDeAcumularlo() {
        var subscription = mock(Flow.Subscription.class);
        var respuesta = new RespuestaLimitada(4);
        respuesta.onSubscribe(subscription);
        respuesta.onNext(List.of(ByteBuffer.wrap(new byte[]{1, 2}), ByteBuffer.wrap(new byte[]{3, 4})));
        respuesta.onComplete();
        assertThat(respuesta.getBody().toCompletableFuture().join()).containsExactly(1, 2, 3, 4);
        var excesiva = new RespuestaLimitada(3);
        excesiva.onSubscribe(subscription);
        excesiva.onNext(List.of(ByteBuffer.wrap(new byte[]{1, 2}), ByteBuffer.wrap(new byte[]{3, 4})));
        assertThatThrownBy(() -> excesiva.getBody().toCompletableFuture().join()).hasCauseInstanceOf(RespuestaLimitada.LimiteExcedido.class);
        verify(subscription).cancel();
    }

    @Test void propagaUnaConexionInterrumpidaDuranteElCuerpo() {
        var respuesta = new RespuestaLimitada(1024);
        respuesta.onError(new IOException("Interrumpida"));
        assertThatThrownBy(() -> respuesta.getBody().toCompletableFuture().join()).hasCauseInstanceOf(IOException.class);
    }
}
