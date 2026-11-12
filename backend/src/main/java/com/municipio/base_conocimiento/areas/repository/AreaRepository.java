package com.municipio.base_conocimiento.areas.repository;

import com.municipio.base_conocimiento.areas.entity.Area;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, Long> {
    boolean existsByNombreNormalizado(String nombreNormalizado);
    boolean existsByNombreNormalizadoAndIdNot(String nombreNormalizado, Long id);
    Page<Area> findByActivo(boolean activo, Pageable pageable);
}
