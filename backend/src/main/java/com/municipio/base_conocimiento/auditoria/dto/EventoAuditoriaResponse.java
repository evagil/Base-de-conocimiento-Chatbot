package com.municipio.base_conocimiento.auditoria.dto;
import com.municipio.base_conocimiento.auditoria.entity.EventoAuditoria;import java.time.Instant;
public record EventoAuditoriaResponse(Long id,String usuario,String accion,String recurso,int codigoHttp,String direccionIp,Instant ocurridoEn){public static EventoAuditoriaResponse de(EventoAuditoria e){return new EventoAuditoriaResponse(e.getId(),e.getUsuarioEmail(),e.getAccion(),e.getRecurso(),e.getCodigoHttp(),e.getDireccionIp(),e.getOcurridoEn());}}
