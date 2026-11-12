package com.municipio.base_conocimiento.soluciones.repository;

import com.municipio.base_conocimiento.soluciones.entity.PasoSolucion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasoSolucionRepository extends JpaRepository<PasoSolucion, Long> {
    List<PasoSolucion> findBySolucionIdOrderByOrdenAsc(Long solucionId);
    boolean existsBySolucionIdAndOrdenAndIdNot(Long solucionId, int orden, Long id);
    boolean existsBySolucionIdAndOrden(Long solucionId, int orden);
}
