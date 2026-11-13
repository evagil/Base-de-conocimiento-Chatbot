package com.municipio.base_conocimiento.soluciones.service;

import com.municipio.base_conocimiento.auditoria.entity.VersionSolucion;
import com.municipio.base_conocimiento.auditoria.repository.VersionSolucionRepository;
import com.municipio.base_conocimiento.compartido.exception.ConflictoException;
import com.municipio.base_conocimiento.compartido.exception.RecursoNoEncontradoException;
import com.municipio.base_conocimiento.compartido.sanitization.SanitizadorHtml;
import com.municipio.base_conocimiento.problemas.entity.Problema;
import com.municipio.base_conocimiento.problemas.repository.ProblemaRepository;
import com.municipio.base_conocimiento.soluciones.dto.*;
import com.municipio.base_conocimiento.soluciones.entity.*;
import com.municipio.base_conocimiento.soluciones.repository.*;
import com.municipio.base_conocimiento.seguridad.service.VisibilidadConocimiento;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly = true)
public class SolucionService {
    private final SolucionRepository soluciones; private final PasoSolucionRepository pasos;
    private final ProblemaRepository problemas; private final SanitizadorHtml sanitizador;
    private final VersionSolucionRepository versiones;
    private final VisibilidadConocimiento visibilidad;
    public SolucionService(SolucionRepository soluciones, PasoSolucionRepository pasos, ProblemaRepository problemas,
            SanitizadorHtml sanitizador, VersionSolucionRepository versiones, VisibilidadConocimiento visibilidad) {
        this.soluciones=soluciones; this.pasos=pasos; this.problemas=problemas; this.sanitizador=sanitizador; this.versiones=versiones; this.visibilidad=visibilidad;
    }
    public List<SolucionResponse> listar(Long problemaId) {
        Problema problema = problemas.findById(problemaId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el problema " + problemaId + "."));
        visibilidad.verificar(problema);
        return soluciones.findByProblemaIdOrderByOrdenAscIdAsc(problemaId).stream()
                .filter(s -> !visibilidad.soloPublicados() || visibilidad.visible(s))
                .map(this::response).toList();
    }
    public boolean tieneActivas(Long problemaId) { return soluciones.existsByProblemaIdAndActivoTrue(problemaId); }
    public SolucionResponse obtener(Long id) { Solucion solucion=buscar(id);visibilidad.verificar(solucion);return response(solucion); }
    @Transactional public SolucionResponse crear(Long problemaId, SolucionRequest r) {
        Problema p=problemas.bloquearPorId(problemaId).orElseThrow(()->new RecursoNoEncontradoException("No existe el problema "+problemaId+"."));
        validarProblemaActivo(p);
        String html=limpiarValido(r.descripcionHtml()); if(r.principal()) desmarcarPrincipal(problemaId,null);
        return response(soluciones.save(new Solucion(p,sanitizador.limpiarTitulo(r.titulo()),html,sanitizador.extraerTexto(html),r.principal(),r.orden())));
    }
    @Transactional public SolucionResponse actualizar(Long id, SolucionRequest r, long versionEsperada) {
        bloquearProblemaDe(id);
        Solucion s = buscar(id);
        s.verificarVersion(versionEsperada);
        String html = limpiarValido(r.descripcionHtml());
        String titulo = sanitizador.limpiarTitulo(r.titulo());
        if (titulo != null) titulo = titulo.strip();
        if (java.util.Objects.equals(s.getTitulo(), titulo)
                && s.getDescripcionHtml().equals(html)
                && s.getOrden() == r.orden() && s.isPrincipal() == r.principal()) {
            return response(s);
        }
        versiones.save(new VersionSolucion(s, snapshot(s), r.motivoCambio()));
        if (r.principal()) desmarcarPrincipal(s.getProblema().getId(), id);
        s.actualizar(titulo, html, sanitizador.extraerTexto(html), r.orden());
        s.marcarPrincipal(r.principal());
        soluciones.flush();
        return response(s);
    }
    @Transactional public SolucionResponse marcarPrincipal(Long id, long versionEsperada) { bloquearProblemaDe(id); Solucion s=buscar(id);s.verificarVersion(versionEsperada); if(!s.isActivo())throw new ConflictoException("SOLUCION_INACTIVA","No se puede recomendar una solución inactiva."); desmarcarPrincipal(s.getProblema().getId(),id); s.marcarPrincipal(true); soluciones.flush(); return response(s); }
    @Transactional public SolucionResponse cambiarActivo(Long id, boolean activo, long versionEsperada) { Problema p=bloquearProblemaDe(id); if(activo)validarProblemaActivo(p); Solucion s=buscar(id);s.verificarVersion(versionEsperada); s.cambiarActivo(activo); soluciones.flush(); return response(s); }
    @Transactional public PasoResponse crearPaso(Long solucionId, PasoRequest r) { Solucion s=buscar(solucionId); validarOrden(s.getId(),r.orden(),null); String html=limpiarValido(r.contenidoHtml()); return pasoResponse(pasos.save(new PasoSolucion(s,r.titulo(),html,sanitizador.extraerTexto(html),r.orden()))); }
    @Transactional public PasoResponse actualizarPaso(Long id, PasoRequest r, long versionEsperada) { PasoSolucion p=buscarPaso(id);p.verificarVersion(versionEsperada); validarOrden(p.getSolucion().getId(),r.orden(),id); String html=limpiarValido(r.contenidoHtml()); p.actualizar(r.titulo(),html,sanitizador.extraerTexto(html),r.orden()); pasos.flush(); return pasoResponse(p); }
    @Transactional public PasoResponse cambiarActivoPaso(Long id, boolean activo, long versionEsperada) { PasoSolucion p=buscarPaso(id);p.verificarVersion(versionEsperada); p.cambiarActivo(activo); pasos.flush(); return pasoResponse(p); }
    public Solucion buscar(Long id) { return soluciones.findById(id).orElseThrow(()->new RecursoNoEncontradoException("No existe la solución "+id+".")); }
    public PasoSolucion buscarPaso(Long id) { return pasos.findById(id).orElseThrow(()->new RecursoNoEncontradoException("No existe el paso "+id+".")); }
    private Problema bloquearProblemaDe(Long solucionId) {
        Long problemaId = soluciones.problemaIdDe(solucionId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe la solución " + solucionId + "."));
        return problemas.bloquearPorId(problemaId).orElseThrow(
                () -> new RecursoNoEncontradoException("No existe el problema " + problemaId + "."));
    }
    private void validarProblemaActivo(Problema p) { if(!p.isActivo())throw new ConflictoException("PROBLEMA_INACTIVO","No se pueden agregar ni reactivar soluciones de un problema eliminado."); }
    private void desmarcarPrincipal(Long problemaId, Long excepto) { soluciones.findByProblemaIdAndPrincipalTrueAndActivoTrue(problemaId).filter(s->!s.getId().equals(excepto)).ifPresent(s->s.marcarPrincipal(false)); soluciones.flush(); }
    private void validarOrden(Long solucionId,int orden,Long id) { boolean existe=id==null?pasos.existsBySolucionIdAndOrden(solucionId,orden):pasos.existsBySolucionIdAndOrdenAndIdNot(solucionId,orden,id); if(existe)throw new ConflictoException("ORDEN_DUPLICADO","Ya existe un paso con ese orden."); }
    private String limpiarValido(String valor) { String html=sanitizador.limpiar(valor); if(sanitizador.extraerTexto(html).isBlank())throw new ConflictoException("CONTENIDO_VACIO","El contenido no puede quedar vacío después de sanitizarlo."); return html; }
    private String snapshot(Solucion s) { return "titulo="+s.getTitulo()+"\nprincipal="+s.isPrincipal()+"\norden="+s.getOrden()+"\nhtml="+s.getDescripcionHtml(); }
    private SolucionResponse response(Solucion s) { return new SolucionResponse(s.getId(),s.getProblema().getId(),s.getTitulo(),s.getDescripcionHtml(),s.isPrincipal(),s.getOrden(),s.isActivo(),pasos.findBySolucionIdOrderByOrdenAsc(s.getId()).stream().filter(p -> !visibilidad.soloPublicados() || visibilidad.visible(p)).map(this::pasoResponse).toList(),s.getCreadoEn(),s.getModificadoEn(),s.getVersion()); }
    private PasoResponse pasoResponse(PasoSolucion p) { return new PasoResponse(p.getId(),p.getTitulo(),p.getContenidoHtml(),p.getOrden(),p.isActivo(),p.getVersion()); }
}
