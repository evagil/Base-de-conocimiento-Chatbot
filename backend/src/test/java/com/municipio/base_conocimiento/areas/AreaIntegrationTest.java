package com.municipio.base_conocimiento.areas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.municipio.base_conocimiento.areas.dto.AreaRequest;
import com.municipio.base_conocimiento.areas.dto.AreaResponse;
import com.municipio.base_conocimiento.areas.dto.SubareaRequest;
import com.municipio.base_conocimiento.areas.dto.SubareaResponse;
import com.municipio.base_conocimiento.areas.service.AreaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import com.municipio.base_conocimiento.compartido.exception.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AreaIntegrationTest {
    @Autowired
    private AreaService service;

    @Test
    void creaAreaYSubareaYConservaLaRelacion() {
        AreaResponse area = service.crear(new AreaRequest("Rentas", "Gestión tributaria", 1));
        SubareaResponse subarea = service.crearSubarea(area.id(),
                new SubareaRequest("Catastro", "Información catastral", 1));

        assertThat(service.obtener(area.id()).nombre()).isEqualTo("Rentas");
        assertThat(service.listarSubareas(area.id(), true))
                .extracting(SubareaResponse::nombre)
                .containsExactly("Catastro");
        assertThat(subarea.areaId()).isEqualTo(area.id());

        AreaResponse actualizada = service.actualizar(area.id(), new AreaRequest("Rentas municipal", "Actualizada", 2),service.obtener(area.id()).version());
        assertThat(actualizada.nombre()).isEqualTo("Rentas municipal");
        assertThat(service.listar(null, PageRequest.of(0, 10))).hasSize(1);
        assertThat(service.listar(true, PageRequest.of(0, 10))).hasSize(1);
        service.actualizarSubarea(subarea.id(), new SubareaRequest("Catastro municipal", null, 2),service.obtenerSubarea(subarea.id()).version());
        assertThat(service.obtenerSubarea(subarea.id()).nombre()).isEqualTo("Catastro municipal");
        service.cambiarEstadoSubarea(subarea.id(), false,service.obtenerSubarea(subarea.id()).version());
        assertThat(service.listarSubareas(area.id(), false)).hasSize(1);
        service.cambiarEstado(area.id(), false,service.obtener(area.id()).version());
        assertThatThrownBy(() -> service.cambiarEstadoSubarea(subarea.id(), true,service.obtenerSubarea(subarea.id()).version())).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> service.crearSubarea(area.id(), new SubareaRequest("Nueva", null, 1))).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> service.obtener(99999L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.obtenerSubarea(99999L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
