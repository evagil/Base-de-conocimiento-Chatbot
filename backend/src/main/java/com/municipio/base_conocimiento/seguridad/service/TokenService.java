package com.municipio.base_conocimiento.seguridad.service;

import com.municipio.base_conocimiento.seguridad.entity.Usuario;
import com.municipio.base_conocimiento.seguridad.entity.RefreshToken;
import com.municipio.base_conocimiento.seguridad.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {
    private final JwtEncoder encoder; private final RefreshTokenRepository refreshTokens;
    private final long accessExpiration; private final long refreshExpiration;
    public TokenService(JwtEncoder encoder,RefreshTokenRepository refreshTokens,
            @Value("${app.security.access-expiration}") long accessExpiration,
            @Value("${app.security.refresh-expiration}") long refreshExpiration){this.encoder=encoder;this.refreshTokens=refreshTokens;this.accessExpiration=accessExpiration;this.refreshExpiration=refreshExpiration;}
    public String accessToken(Usuario usuario){Instant now=Instant.now();JwtClaimsSet claims=JwtClaimsSet.builder().issuer("base-conocimiento").issuedAt(now).expiresAt(now.plusSeconds(accessExpiration)).subject(usuario.getUsuarioNormalizado()).claim("nombre",usuario.getNombre()).claim("rol",usuario.getRol().name()).build();JwsHeader header=JwsHeader.with(MacAlgorithm.HS256).build();return encoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();}
    @Transactional public String crearRefresh(Usuario usuario){String raw=UUID.randomUUID()+"."+UUID.randomUUID();refreshTokens.save(new RefreshToken(usuario,hash(raw),Instant.now().plusSeconds(refreshExpiration)));return raw;}
    @Transactional public Usuario consumir(String raw){RefreshToken token=refreshTokens.findByTokenHash(hash(raw)).orElseThrow(()->new IllegalArgumentException("Refresh token inválido."));if(!token.esValido(Instant.now()))throw new IllegalArgumentException("Refresh token vencido o revocado.");token.revocar();return token.getUsuario();}
    @Transactional public void revocar(String raw){refreshTokens.findByTokenHash(hash(raw)).ifPresent(RefreshToken::revocar);}
    public long getAccessExpiration(){return accessExpiration;}
    static String hash(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(bytes);}catch(Exception e){throw new IllegalStateException(e);}}
}
