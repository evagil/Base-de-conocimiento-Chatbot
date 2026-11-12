package com.municipio.base_conocimiento.problemas.dto;

import com.municipio.base_conocimiento.soluciones.dto.SolucionResponse;
import java.util.List;

public record ProblemaDetalleResponse(ProblemaResponse problema, List<SolucionResponse> soluciones) {}
