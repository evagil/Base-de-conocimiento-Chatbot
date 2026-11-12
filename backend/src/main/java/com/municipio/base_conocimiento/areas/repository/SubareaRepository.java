package com.municipio.base_conocimiento.areas.repository;

import com.municipio.base_conocimiento.areas.entity.Subarea;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubareaRepository extends JpaRepository<Subarea, Long> {
    boolean existsByAreaIdAndNombreNormalizado(Long areaId, String nombreNormalizado);
    boolean existsByAreaIdAndNombreNormalizadoAndIdNot(Long areaId, String nombreNormalizado, Long id);
    List<Subarea> findByAreaId(Long areaId, Sort sort);
    List<Subarea> findByAreaIdAndActivo(Long areaId, boolean activo, Sort sort);
    boolean existsByAreaIdAndActivo(Long areaId, boolean activo);
}
