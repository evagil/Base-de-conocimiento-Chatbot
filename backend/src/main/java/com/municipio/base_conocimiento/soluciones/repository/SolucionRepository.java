package com.municipio.base_conocimiento.soluciones.repository;

import com.municipio.base_conocimiento.soluciones.entity.Solucion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolucionRepository extends JpaRepository<Solucion, Long> {
    @org.springframework.data.jpa.repository.Query("select s.problema.id from Solucion s where s.id = :id")
    Optional<Long> problemaIdDe(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByProblemaIdAndActivoTrue(Long problemaId);
    List<Solucion> findByProblemaIdOrderByOrdenAscIdAsc(Long problemaId);
    Optional<Solucion> findByProblemaIdAndPrincipalTrueAndActivoTrue(Long problemaId);
}
