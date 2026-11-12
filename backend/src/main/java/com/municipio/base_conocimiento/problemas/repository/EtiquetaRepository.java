package com.municipio.base_conocimiento.problemas.repository;

import com.municipio.base_conocimiento.problemas.entity.Etiqueta;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {
    Optional<Etiqueta> findByNombreNormalizado(String nombreNormalizado);
    List<Etiqueta> findByActivoTrueOrderByNombreAsc();
}
