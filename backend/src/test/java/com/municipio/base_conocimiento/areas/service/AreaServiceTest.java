package com.municipio.base_conocimiento.areas.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.municipio.base_conocimiento.areas.dto.AreaRequest;
import com.municipio.base_conocimiento.areas.repository.AreaRepository;
import com.municipio.base_conocimiento.areas.repository.SubareaRepository;
import com.municipio.base_conocimiento.compartido.exception.ConflictoException;
import com.municipio.base_conocimiento.problemas.repository.ProblemaRepository;
import org.junit.jupiter.api.Test;

class AreaServiceTest {
    @Test
    void rechazaUnNombreDuplicadoAunqueCambienAcentosYMayusculas() {
        AreaRepository areas = mock(AreaRepository.class);
        SubareaRepository subareas = mock(SubareaRepository.class);
        ProblemaRepository problemas = mock(ProblemaRepository.class);
        when(areas.existsByNombreNormalizado("rentas")).thenReturn(true);
        AreaService service = new AreaService(areas, subareas, problemas);

        assertThatThrownBy(() -> service.crear(new AreaRequest(" RÉNTAS ", null, 0)))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ya existe un área con ese nombre.");
    }
}
