package com.municipio.base_conocimiento.auditoria.entity;
import jakarta.persistence.*;import java.time.Instant;
@Entity @Table(name="eventos_auditoria")
public class EventoAuditoria{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="usuario_email",nullable=false,length=180) private String usuarioEmail;
 @Column(nullable=false,length=12) private String accion;
 @Column(nullable=false,length=500) private String recurso;
 @Column(name="codigo_http",nullable=false) private int codigoHttp;
 @Column(name="direccion_ip",length=64) private String direccionIp;
 @Column(name="ocurrido_en",nullable=false,updatable=false) private Instant ocurridoEn=Instant.now();
 protected EventoAuditoria(){} public EventoAuditoria(String usuario,String accion,String recurso,int codigo,String ip){this.usuarioEmail=usuario;this.accion=accion;this.recurso=recurso;this.codigoHttp=codigo;this.direccionIp=ip;}
 public Long getId(){return id;}public String getUsuarioEmail(){return usuarioEmail;}public String getAccion(){return accion;}public String getRecurso(){return recurso;}public int getCodigoHttp(){return codigoHttp;}public String getDireccionIp(){return direccionIp;}public Instant getOcurridoEn(){return ocurridoEn;}
}
