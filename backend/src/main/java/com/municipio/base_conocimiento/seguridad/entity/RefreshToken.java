package com.municipio.base_conocimiento.seguridad.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="usuario_id") private Usuario usuario;
    @Column(name="token_hash",nullable=false,unique=true,length=64) private String tokenHash;
    @Column(name="vence_en",nullable=false) private Instant venceEn;
    @Column(name="revocado_en") private Instant revocadoEn;
    @Column(name="creado_en",nullable=false,updatable=false) private Instant creadoEn=Instant.now();
    protected RefreshToken() {}
    public RefreshToken(Usuario usuario,String tokenHash,Instant venceEn){this.usuario=usuario;this.tokenHash=tokenHash;this.venceEn=venceEn;}
    public boolean esValido(Instant ahora){return revocadoEn==null&&venceEn.isAfter(ahora)&&usuario.isActivo();}
    public void revocar(){if(revocadoEn==null)revocadoEn=Instant.now();}
    public Usuario getUsuario(){return usuario;} public String getTokenHash(){return tokenHash;}
}
