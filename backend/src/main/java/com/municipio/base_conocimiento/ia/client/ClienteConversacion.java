package com.municipio.base_conocimiento.ia.client;

import com.municipio.base_conocimiento.ia.dto.*;
import java.util.List;
import java.util.Map;

public interface ClienteConversacion {
    enum Uso { CHATBOT, AGENTE }
    default RespuestaIa conversar(Uso uso, List<MensajeIa> mensajes) { return conversar(uso, mensajes, null); }
    RespuestaIa conversar(Uso uso, List<MensajeIa> mensajes, Map<String, Object> esquema);
}
