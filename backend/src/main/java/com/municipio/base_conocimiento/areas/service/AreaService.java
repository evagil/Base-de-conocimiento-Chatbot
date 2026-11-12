package com.municipio.base_conocimiento.areas.service;

import com.municipio.base_conocimiento.areas.dto.AreaRequest;
import com.municipio.base_conocimiento.areas.dto.AreaResponse;
import com.municipio.base_conocimiento.areas.dto.SubareaRequest;
import com.municipio.base_conocimiento.areas.dto.SubareaResponse;
import com.municipio.base_conocimiento.areas.entity.Area;
import com.municipio.base_conocimiento.areas.entity.Subarea;
import com.municipio.base_conocimiento.areas.mapper.AreaMapper;
import com.municipio.base_conocimiento.areas.repository.AreaRepository;
import com.municipio.base_conocimiento.areas.repository.SubareaRepository;
import com.municipio.base_conocimiento.compartido.exception.ConflictoException;
import com.municipio.base_conocimiento.compartido.exception.RecursoNoEncontradoException;
import com.municipio.base_conocimiento.compartido.util.NormalizadorTexto;
import com.municipio.base_conocimiento.problemas.repository.ProblemaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AreaService {
    private static final Sort ORDEN_NOMBRE = Sort.by("orden").ascending().and(Sort.by("nombre").ascending());
    private final AreaRepository areaRepository;
    private final SubareaRepository subareaRepository;
    private final ProblemaRepository problemaRepository;

    public AreaService(AreaRepository areaRepository, SubareaRepository subareaRepository, ProblemaRepository problemaRepository) {
        this.areaRepository = areaRepository;
        this.subareaRepository = subareaRepository;
        this.problemaRepository = problemaRepository;
    }

    public Page<AreaResponse> listar(Boolean activo, Pageable pageable) {
        Page<Area> areas = activo == null ? areaRepository.findAll(pageable) : areaRepository.findByActivo(activo, pageable);
        return areas.map(AreaMapper::response);
    }

    public AreaResponse obtener(Long id) { return AreaMapper.response(buscarArea(id)); }

    @Transactional
    public AreaResponse crear(AreaRequest request) {
        String normalizado = NormalizadorTexto.nombre(request.nombre());
        validarNombreAreaDisponible(normalizado, null);
        return AreaMapper.response(areaRepository.save(new Area(
                request.nombre(), normalizado, request.descripcion(), request.orden())));
    }

    @Transactional
    public AreaResponse actualizar(Long id, AreaRequest request, long versionEsperada) {
        Area area = buscarArea(id);area.verificarVersion(versionEsperada);
        String normalizado = NormalizadorTexto.nombre(request.nombre());
        validarNombreAreaDisponible(normalizado, id);
        area.actualizar(request.nombre(), normalizado, request.descripcion(), request.orden());
        areaRepository.flush(); return AreaMapper.response(area);
    }

    @Transactional
    public AreaResponse cambiarEstado(Long id, boolean activo, long versionEsperada) {
        Area area = buscarArea(id);area.verificarVersion(versionEsperada);
        if (!activo && subareaRepository.existsByAreaIdAndActivo(id, true)) {
            throw new ConflictoException("AREA_CON_SUBAREAS", "Primero eliminá o mové las subáreas activas de esta área.");
        }
        area.cambiarEstado(activo);
        areaRepository.flush(); return AreaMapper.response(area);
    }

    public List<SubareaResponse> listarSubareas(Long areaId, Boolean activo) {
        buscarArea(areaId);
        List<Subarea> subareas = activo == null
                ? subareaRepository.findByAreaId(areaId, ORDEN_NOMBRE)
                : subareaRepository.findByAreaIdAndActivo(areaId, activo, ORDEN_NOMBRE);
        return subareas.stream().map(AreaMapper::response).toList();
    }

    public SubareaResponse obtenerSubarea(Long id) { return AreaMapper.response(buscarSubarea(id)); }

    @Transactional
    public SubareaResponse crearSubarea(Long areaId, SubareaRequest request) {
        Area area = buscarArea(areaId);
        if (!area.isActivo()) throw new ConflictoException("AREA_INACTIVA", "No se puede agregar una subárea a un área inactiva.");
        String normalizado = NormalizadorTexto.nombre(request.nombre());
        validarNombreSubareaDisponible(areaId, normalizado, null);
        return AreaMapper.response(subareaRepository.save(new Subarea(
                area, request.nombre(), normalizado, request.descripcion(), request.orden())));
    }

    @Transactional
    public SubareaResponse actualizarSubarea(Long id, SubareaRequest request, long versionEsperada) {
        Subarea subarea = buscarSubarea(id);subarea.verificarVersion(versionEsperada);
        String normalizado = NormalizadorTexto.nombre(request.nombre());
        validarNombreSubareaDisponible(subarea.getArea().getId(), normalizado, id);
        subarea.actualizar(request.nombre(), normalizado, request.descripcion(), request.orden());
        subareaRepository.flush(); return AreaMapper.response(subarea);
    }

    @Transactional
    public SubareaResponse cambiarEstadoSubarea(Long id, boolean activo, long versionEsperada) {
        Subarea subarea = buscarSubarea(id);subarea.verificarVersion(versionEsperada);
        if (activo && !subarea.getArea().isActivo()) {
            throw new ConflictoException("AREA_INACTIVA", "No se puede reactivar una subárea si su área está inactiva.");
        }
        if (!activo && problemaRepository.existsBySubareaIdAndActivo(id, true)) {
            throw new ConflictoException("SUBAREA_CON_PROBLEMAS", "Primero eliminá o mové los problemas activos de esta subárea.");
        }
        subarea.cambiarEstado(activo);
        subareaRepository.flush(); return AreaMapper.response(subarea);
    }

    private Area buscarArea(Long id) {
        return areaRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el área con identificador " + id + "."));
    }

    private Subarea buscarSubarea(Long id) {
        return subareaRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la subárea con identificador " + id + "."));
    }

    private void validarNombreAreaDisponible(String nombre, Long id) {
        boolean existe = id == null ? areaRepository.existsByNombreNormalizado(nombre)
                : areaRepository.existsByNombreNormalizadoAndIdNot(nombre, id);
        if (existe) throw new ConflictoException("AREA_NOMBRE_DUPLICADO", "Ya existe un área con ese nombre.");
    }

    private void validarNombreSubareaDisponible(Long areaId, String nombre, Long id) {
        boolean existe = id == null ? subareaRepository.existsByAreaIdAndNombreNormalizado(areaId, nombre)
                : subareaRepository.existsByAreaIdAndNombreNormalizadoAndIdNot(areaId, nombre, id);
        if (existe) throw new ConflictoException("SUBAREA_NOMBRE_DUPLICADO",
                "Ya existe una subárea con ese nombre dentro del área.");
    }
}
