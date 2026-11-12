package com.municipio.base_conocimiento.seguridad.repository;
import com.municipio.base_conocimiento.seguridad.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UsuarioRepository extends JpaRepository<Usuario,Long>{Optional<Usuario> findByEmailNormalizado(String email);Optional<Usuario> findByUsuarioNormalizado(String usuario);}
