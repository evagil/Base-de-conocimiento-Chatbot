package com.municipio.base_conocimiento.problemas.repository;

import com.municipio.base_conocimiento.problemas.entity.Problema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;

public interface ProblemaRepository extends JpaRepository<Problema, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Problema p where p.id = :id")
    java.util.Optional<Problema> bloquearPorId(@org.springframework.data.repository.query.Param("id") Long id);

    Page<Problema> findBySubareaId(Long subareaId, Pageable pageable);
    Page<Problema> findByActivo(boolean activo, Pageable pageable);
    Page<Problema> findBySubareaIdAndActivo(Long subareaId, boolean activo, Pageable pageable);
    Page<Problema> findByEstadoAndActivo(EstadoProblema estado,boolean activo,Pageable pageable);
    Page<Problema> findBySubareaIdAndEstadoAndActivo(Long subareaId,EstadoProblema estado,boolean activo,Pageable pageable);
    boolean existsBySubareaIdAndActivo(Long subareaId, boolean activo);
}
