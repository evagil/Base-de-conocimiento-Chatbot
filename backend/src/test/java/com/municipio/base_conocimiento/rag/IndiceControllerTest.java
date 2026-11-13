package com.municipio.base_conocimiento.rag;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.municipio.base_conocimiento.compartido.web.ManejadorGlobalExcepciones;
import com.municipio.base_conocimiento.rag.controller.IndiceController;
import com.municipio.base_conocimiento.rag.repository.IndiceRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IndiceControllerTest {
    @Test void exponeSoloContadoresYEncolaReconstruccionYReintentos()throws Exception{
        var repo=mock(IndiceRepository.class);when(repo.resumen()).thenReturn(Map.of("fragmentos",4L));
        var mvc=MockMvcBuilders.standaloneSetup(new IndiceController(repo,FragmentadorTest.config(true,true))).build();
        mvc.perform(get("/api/ia/indice")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.cantidades.fragmentos").value(4)).andExpect(jsonPath("$.habilitado").value(true));
        mvc.perform(post("/api/ia/indice/reconstruir")).andExpect(status().isAccepted());verify(repo).reconstruir(false);
        mvc.perform(post("/api/ia/indice/reintentar")).andExpect(status().isAccepted());verify(repo).reconstruir(true);
        var desactivado=MockMvcBuilders.standaloneSetup(new IndiceController(repo,FragmentadorTest.config(false,false)))
            .setControllerAdvice(new ManejadorGlobalExcepciones()).build();
        desactivado.perform(post("/api/ia/indice/reconstruir")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("INDICE_DESHABILITADO"));
    }
}
