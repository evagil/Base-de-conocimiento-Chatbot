package com.municipio.base_conocimiento.ia.client;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Limita los bytes durante la recepción, antes de construir el JSON completo. */
final class RespuestaLimitada implements HttpResponse.BodySubscriber<byte[]> {
    static final class LimiteExcedido extends RuntimeException { }
    private final int maximo;
    private final ByteArrayOutputStream contenido = new ByteArrayOutputStream();
    private final CompletableFuture<byte[]> resultado = new CompletableFuture<>();
    private Flow.Subscription subscription;

    RespuestaLimitada(int maximo) { this.maximo = maximo; }
    @Override public CompletionStage<byte[]> getBody() { return resultado; }
    @Override public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }
    @Override public void onNext(List<ByteBuffer> buffers) {
        for (ByteBuffer buffer : buffers) {
            if (buffer.remaining() > maximo - contenido.size()) {
                subscription.cancel();
                resultado.completeExceptionally(new LimiteExcedido());
                return;
            }
            byte[] fragmento = new byte[buffer.remaining()];
            buffer.get(fragmento);
            contenido.writeBytes(fragmento);
        }
        subscription.request(1);
    }
    @Override public void onError(Throwable error) { resultado.completeExceptionally(error); }
    @Override public void onComplete() { resultado.complete(contenido.toByteArray()); }
}
