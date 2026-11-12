package com.municipio.base_conocimiento.archivos.repository;
import com.municipio.base_conocimiento.archivos.entity.Archivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface ArchivoRepository extends JpaRepository<Archivo,Long>{
    @Query(value="SELECT a.* FROM archivos a JOIN archivo_problemas r ON r.archivo_id=a.id WHERE r.problema_id=?1 ORDER BY a.orden,a.id",nativeQuery=true) List<Archivo> findByProblema(Long id);
    @Query(value="SELECT a.* FROM archivos a JOIN archivo_soluciones r ON r.archivo_id=a.id WHERE r.solucion_id=?1 ORDER BY a.orden,a.id",nativeQuery=true) List<Archivo> findBySolucion(Long id);
    @Query(value="SELECT a.* FROM archivos a JOIN archivo_pasos r ON r.archivo_id=a.id WHERE r.paso_id=?1 ORDER BY a.orden,a.id",nativeQuery=true) List<Archivo> findByPaso(Long id);
}
