package com.municipio.base_conocimiento.ia;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.municipio.base_conocimiento.compartido.web.ManejadorGlobalExcepciones;
import com.municipio.base_conocimiento.ia.client.ClienteOllama;
import com.municipio.base_conocimiento.ia.controller.EstadoIaController;
import com.municipio.base_conocimiento.ia.dto.EstadoIa;
import com.municipio.base_conocimiento.ia.exception.IaException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EstadoIaControllerTest {
    @Test void devuelveEstadoYErroresConElContratoUniforme() throws Exception {
        var cliente = mock(ClienteOllama.class);
        var mvc = MockMvcBuilders.standaloneSetup(new EstadoIaController(cliente)).setControllerAdvice(new ManejadorGlobalExcepciones()).build();
        when(cliente.estado()).thenReturn(new EstadoIa(false, false, null, List.of()));
        mvc.perform(get("/api/ia/estado")).andExpect(status().isOk()).andExpect(jsonPath("$.habilitada").value(false));
        when(cliente.estado()).thenThrow(new IaException(HttpStatus.SERVICE_UNAVAILABLE, "IA_NO_DISPONIBLE", "Abrí Ollama."));
        mvc.perform(get("/api/ia/estado")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("IA_NO_DISPONIBLE")).andExpect(jsonPath("$.mensaje").value("Abrí Ollama."));
    }
}
