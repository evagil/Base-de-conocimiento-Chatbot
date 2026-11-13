package com.municipio.base_conocimiento.ia.client;

import com.municipio.base_conocimiento.ia.dto.ResultadoEmbeddings;
import java.util.List;

public interface ClienteEmbeddings {
    ResultadoEmbeddings vectorizar(List<String> textos);
}
