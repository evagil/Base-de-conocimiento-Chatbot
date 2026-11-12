package com.municipio.base_conocimiento.seguridad.repository;
import com.municipio.base_conocimiento.seguridad.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long>{@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
Optional<RefreshToken> findByTokenHash(String hash);}
