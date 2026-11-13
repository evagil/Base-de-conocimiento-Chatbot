package com.municipio.base_conocimiento.problemas.service;

import com.municipio.base_conocimiento.areas.entity.Subarea;
import com.municipio.base_conocimiento.areas.repository.SubareaRepository;
import com.municipio.base_conocimiento.auditoria.entity.VersionProblema;
import com.municipio.base_conocimiento.auditoria.repository.VersionProblemaRepository;
import com.municipio.base_conocimiento.compartido.exception.ConflictoException;
import com.municipio.base_conocimiento.compartido.exception.RecursoNoEncontradoException;
import com.municipio.base_conocimiento.compartido.sanitization.SanitizadorHtml;
import com.municipio.base_conocimiento.compartido.util.NormalizadorTexto;
import com.municipio.base_conocimiento.problemas.dto.*;
import com.municipio.base_conocimiento.problemas.entity.*;
import com.municipio.base_conocimiento.problemas.repository.*;
import com.municipio.base_conocimiento.soluciones.service.SolucionService;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.municipio.base_conocimiento.seguridad.service.SeguridadActual;
import com.municipio.base_conocimiento.seguridad.service.VisibilidadConocimiento;

@Service @Transactional(readOnly = true)
public class ProblemaService {
    private final ProblemaRepository problemas; private final EtiquetaRepository etiquetas;
    private final SubareaRepository subareas; private final SanitizadorHtml sanitizador;
    private final VersionProblemaRepository versiones; private final SolucionService soluciones;
    private final SeguridadActual seguridad;
    private final VisibilidadConocimiento visibilidad;
    public ProblemaService(ProblemaRepository problemas, EtiquetaRepository etiquetas, SubareaRepository subareas,
            SanitizadorHtml sanitizador, VersionProblemaRepository versiones, SolucionService soluciones, SeguridadActual seguridad, VisibilidadConocimiento visibilidad) {
        this.problemas=problemas; this.etiquetas=etiquetas; this.subareas=subareas; this.sanitizador=sanitizador;
        this.versiones=versiones; this.soluciones=soluciones;
        this.seguridad=seguridad;this.visibilidad=visibilidad;
    }
    public Page<ProblemaResponse> listar(Long subareaId, Boolean activo, Pageable p) {
        Page<Problema> pagina = seguridad.esLector() ? (subareaId==null?problemas.findByEstadoAndActivo(EstadoProblema.PUBLICADO,true,p):problemas.findBySubareaIdAndEstadoAndActivo(subareaId,EstadoProblema.PUBLICADO,true,p))
                : subareaId != null && activo != null ? problemas.findBySubareaIdAndActivo(subareaId, activo, p)
                : subareaId != null ? problemas.findBySubareaId(subareaId,p)
                : activo != null ? problemas.findByActivo(activo,p) : problemas.findAll(p);
        return pagina.map(this::response);
    }
    public ProblemaDetalleResponse obtener(Long id) { Problema p=buscar(id);visibilidad.verificar(p);return new ProblemaDetalleResponse(response(p), soluciones.listar(id)); }
    public List<String> listarEtiquetas(){return etiquetas.findByActivoTrueOrderByNombreAsc().stream().map(Etiqueta::getNombre).toList();}
    @Transactional public ProblemaResponse crear(ProblemaRequest r) {
        Subarea s=buscarSubareaActiva(r.subareaId()); String html=sanitizador.limpiar(r.descripcionHtml());
        validarContenido(html); Problema p=new Problema(s,r.titulo(),html,sanitizador.extraerTexto(html),r.estado());
        p.reemplazarEtiquetas(resolverEtiquetas(r.etiquetas())); return response(problemas.save(p));
    }
    @Transactional public ProblemaResponse actualizar(Long id, ProblemaRequest r, long versionEsperada) {
        Problema p = buscar(id);
        p.verificarVersion(versionEsperada);
        String html = sanitizador.limpiar(r.descripcionHtml());
        validarContenido(html);
        String titulo = r.titulo().strip();
        Subarea subarea = p.getSubarea().getId().equals(r.subareaId())
                ? p.getSubarea() : buscarSubareaActiva(r.subareaId());
        // Resolve queries before changing managed state, avoiding an early JPA flush.
        Set<Etiqueta> nuevasEtiquetas = resolverEtiquetas(r.etiquetas());
        if (p.getTitulo().equals(titulo) && p.getDescripcionHtml().equals(html)
                && p.getEstado() == r.estado() && p.getSubarea().equals(subarea)
                && p.getEtiquetas().equals(nuevasEtiquetas)) {
            return response(p);
        }
        versiones.save(new VersionProblema(p, snapshot(p), r.motivoCambio()));
        p.actualizar(titulo, html, sanitizador.extraerTexto(html), r.estado());
        p.moverA(subarea);
        if (!p.getEtiquetas().equals(nuevasEtiquetas)) p.reemplazarEtiquetas(nuevasEtiquetas);
        problemas.flush();
        return response(p);
    }
    @Transactional public ProblemaResponse cambiarEstado(Long id, EstadoProblema estado, long versionEsperada) { Problema p=buscar(id);p.verificarVersion(versionEsperada); p.cambiarEstado(estado); problemas.flush(); return response(p); }
    @Transactional public ProblemaResponse cambiarActivo(Long id, boolean activo, long versionEsperada) { Problema p=problemas.bloquearPorId(id).orElseThrow(()->new RecursoNoEncontradoException("No existe el problema "+id+"."));p.verificarVersion(versionEsperada); if(!activo && soluciones.tieneActivas(id))throw new ConflictoException("PROBLEMA_CON_SOLUCIONES","Primero eliminá todas las soluciones del problema."); p.cambiarActivo(activo); problemas.flush(); return response(p); }
    public Problema buscar(Long id) { return problemas.findById(id).orElseThrow(()->new RecursoNoEncontradoException("No existe el problema "+id+".")); }
    private Subarea buscarSubareaActiva(Long id) { Subarea s=subareas.findById(id).orElseThrow(()->new RecursoNoEncontradoException("No existe la subárea "+id+".")); if(!s.isActivo()||!s.getArea().isActivo()) throw new ConflictoException("SUBAREA_INACTIVA","La subárea o su área están inactivas."); return s; }
    private Set<Etiqueta> resolverEtiquetas(Set<String> nombres) { Set<Etiqueta> resultado=new LinkedHashSet<>(); if(nombres==null)return resultado; for(String n:nombres){String norm=NormalizadorTexto.nombre(n); resultado.add(etiquetas.findByNombreNormalizado(norm).orElseGet(()->etiquetas.save(new Etiqueta(n,norm))));} return resultado; }
    private void validarContenido(String html) { if(sanitizador.extraerTexto(html).isBlank()) throw new ConflictoException("CONTENIDO_VACIO","La descripción no puede quedar vacía después de sanitizarla."); }
    private String snapshot(Problema p) { return "titulo="+p.getTitulo()+"\nestado="+p.getEstado()+"\nsubarea="+p.getSubarea().getId()+"\nhtml="+p.getDescripcionHtml(); }
    public ProblemaResponse response(Problema p) { return new ProblemaResponse(p.getId(),p.getSubarea().getId(),p.getSubarea().getNombre(),p.getSubarea().getArea().getId(),p.getSubarea().getArea().getNombre(),p.getTitulo(),p.getDescripcionHtml(),p.getEstado(),p.isActivo(),p.getEtiquetas().stream().map(Etiqueta::getNombre).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),p.getCreadoEn(),p.getModificadoEn(),p.getVersion()); }
}
