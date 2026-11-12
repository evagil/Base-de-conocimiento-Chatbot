package com.municipio.base_conocimiento.archivos.service;

import com.municipio.base_conocimiento.archivos.dto.ArchivoResponse;
import com.municipio.base_conocimiento.archivos.entity.Archivo;
import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import com.municipio.base_conocimiento.archivos.repository.ArchivoRepository;
import com.municipio.base_conocimiento.compartido.exception.RecursoNoEncontradoException;
import com.municipio.base_conocimiento.problemas.repository.ProblemaRepository;
import com.municipio.base_conocimiento.soluciones.repository.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.municipio.base_conocimiento.seguridad.service.SeguridadActual;

@Service @Transactional(readOnly=true)
public class ArchivoService {
    public enum Propietario { PROBLEMA, SOLUCION, PASO }
    private final ArchivoRepository archivos; private final ProblemaRepository problemas; private final SolucionRepository soluciones;
    private final PasoSolucionRepository pasos; private final DetectorImagen detector; private final JdbcTemplate jdbc;
    private final Path raiz; private final long maximo;
    private final SeguridadActual seguridad;
    private final DetectorDocumento documentos;
    public ArchivoService(ArchivoRepository archivos,ProblemaRepository problemas,SolucionRepository soluciones,PasoSolucionRepository pasos,
            DetectorImagen detector,DetectorDocumento documentos,JdbcTemplate jdbc,SeguridadActual seguridad,@Value("${app.storage.path}") String path,@Value("${app.storage.max-image-size}") long maximo) {
        this.archivos=archivos;this.problemas=problemas;this.soluciones=soluciones;this.pasos=pasos;this.detector=detector;this.jdbc=jdbc;
        this.raiz=Paths.get(path).toAbsolutePath().normalize();this.maximo=maximo;
        this.seguridad=seguridad;this.documentos=documentos;
    }
    @Transactional public ArchivoResponse cargar(Propietario tipo,Long propietarioId,MultipartFile multipart,String alternativo,int orden) {
        return cargarArchivo(tipo, propietarioId, multipart, alternativo, orden, false);
    }
    @Transactional public ArchivoResponse cargarDocumento(Propietario tipo,Long propietarioId,MultipartFile multipart,String nombre,int orden) {
        return cargarArchivo(tipo, propietarioId, multipart, nombre, orden, true);
    }
    private ArchivoResponse cargarArchivo(Propietario tipo,Long propietarioId,MultipartFile multipart,String alternativo,int orden,boolean documento) {
        validarPropietario(tipo,propietarioId); if(multipart.isEmpty())throw invalido(HttpStatus.BAD_REQUEST,"ARCHIVO_VACIO","El archivo está vacío.");
        if(multipart.getSize()>maximo)throw invalido(HttpStatus.PAYLOAD_TOO_LARGE,"ARCHIVO_DEMASIADO_GRANDE","El archivo supera el tamaño máximo configurado.");
        if(alternativo==null||alternativo.isBlank()||alternativo.length()>300)throw invalido(HttpStatus.BAD_REQUEST,"TEXTO_ALTERNATIVO_INVALIDO","El texto alternativo es obligatorio y admite hasta 300 caracteres.");
        Path destino=null;
        try {
            byte[] contenido=multipart.getBytes();
            String mime, extension;
            if(documento){var detected=documentos.detectar(contenido,multipart.getOriginalFilename());mime=detected.mime();extension=detected.extension();}
            else{var detected=detector.detectar(contenido);mime=detected.mime();extension=detected.extension();}
            LocalDate hoy=LocalDate.now();
            String interno=UUID.randomUUID()+"."+extension; String carpeta=tipo.name().toLowerCase()+"s/"+hoy.getYear()+"/"+String.format("%02d",hoy.getMonthValue());
            String relativa=carpeta+"/"+interno; destino=resolverSegura(relativa); Files.createDirectories(destino.getParent());
            Files.write(destino,contenido,StandardOpenOption.CREATE_NEW);
            String original=Paths.get(multipart.getOriginalFilename()==null?"archivo":multipart.getOriginalFilename().replace("\\","/")).getFileName().toString();
            if(original.length()>255)throw invalido(HttpStatus.BAD_REQUEST,"NOMBRE_INVALIDO","El nombre del archivo admite hasta 255 caracteres.");
            Archivo a=archivos.saveAndFlush(new Archivo(original,interno,mime,contenido.length,relativa,sha256(contenido),alternativo,orden));
            vincular(tipo,propietarioId,a.getId()); return response(a);
        } catch(IOException|NoSuchAlgorithmException ex) { limpiar(destino); throw new IllegalStateException("No se pudo almacenar el archivo.",ex); }
        catch(RuntimeException ex) { limpiar(destino); throw ex; }
    }
    public List<ArchivoResponse> listar(Propietario tipo,Long propietarioId){return listarArchivos(tipo,propietarioId).stream().filter(a->a.tipoMime().startsWith("image/")).toList();}
    public List<ArchivoResponse> listarDocumentos(Propietario tipo,Long propietarioId){return listarArchivos(tipo,propietarioId).stream().filter(a->!a.tipoMime().startsWith("image/") && a.activo()).toList();}
    private List<ArchivoResponse> listarArchivos(Propietario tipo,Long propietarioId){validarPropietario(tipo,propietarioId);if(seguridad.esLector()&&!propietarioPublicado(tipo,propietarioId))throw new RecursoNoEncontradoException("Las imágenes no están disponibles.");return (switch(tipo){case PROBLEMA->archivos.findByProblema(propietarioId);case SOLUCION->archivos.findBySolucion(propietarioId);case PASO->archivos.findByPaso(propietarioId);}).stream().map(this::response).toList();}
    public ContenidoArchivo contenido(Long id) { Archivo a=buscar(id); if(!a.isActivo()||(seguridad.esLector()&&!archivoPublicado(id)))throw new RecursoNoEncontradoException("La imagen no está disponible."); try{return new ContenidoArchivo(a.getTipoMime(),a.getNombreOriginal(),Files.readAllBytes(resolverSegura(a.getRutaRelativa())));}catch(IOException ex){throw new RecursoNoEncontradoException("El contenido físico de la imagen no está disponible.");} }
    @Transactional public ArchivoResponse actualizar(Long id,String alternativo,int orden, long versionEsperada){if(alternativo==null||alternativo.isBlank()||alternativo.length()>300)throw invalido(HttpStatus.BAD_REQUEST,"TEXTO_ALTERNATIVO_INVALIDO","El texto alternativo es obligatorio.");Archivo a=buscar(id);a.verificarVersion(versionEsperada);a.actualizar(alternativo,orden);archivos.flush(); return response(a);}
    @Transactional public ArchivoResponse cambiarActivo(Long id,boolean activo, long versionEsperada){Archivo a=buscar(id);a.verificarVersion(versionEsperada);a.cambiarActivo(activo);archivos.flush(); return response(a);}
    private Archivo buscar(Long id){return archivos.findById(id).orElseThrow(()->new RecursoNoEncontradoException("No existe la imagen "+id+"."));}
    private void validarPropietario(Propietario t,Long id){boolean existe=switch(t){case PROBLEMA->problemas.existsById(id);case SOLUCION->soluciones.existsById(id);case PASO->pasos.existsById(id);};if(!existe)throw new RecursoNoEncontradoException("No existe el recurso relacionado "+id+".");}
    private void vincular(Propietario t,Long id,Long archivo){switch(t){case PROBLEMA->jdbc.update("INSERT INTO archivo_problemas (archivo_id,problema_id) VALUES (?,?)",archivo,id);case SOLUCION->jdbc.update("INSERT INTO archivo_soluciones (archivo_id,solucion_id) VALUES (?,?)",archivo,id);case PASO->jdbc.update("INSERT INTO archivo_pasos (archivo_id,paso_id) VALUES (?,?)",archivo,id);}}
    private boolean propietarioPublicado(Propietario tipo,Long id){String sql=switch(tipo){case PROBLEMA->"SELECT COUNT(*) FROM problemas WHERE id=? AND activo=TRUE AND estado='PUBLICADO'";case SOLUCION->"SELECT COUNT(*) FROM soluciones s JOIN problemas p ON p.id=s.problema_id WHERE s.id=? AND s.activo=TRUE AND p.activo=TRUE AND p.estado='PUBLICADO'";case PASO->"SELECT COUNT(*) FROM pasos_solucion ps JOIN soluciones s ON s.id=ps.solucion_id JOIN problemas p ON p.id=s.problema_id WHERE ps.id=? AND ps.activo=TRUE AND s.activo=TRUE AND p.activo=TRUE AND p.estado='PUBLICADO'";};return jdbc.queryForObject(sql,Integer.class,id)>0;}
    private boolean archivoPublicado(Long id){String sql="SELECT COUNT(*) FROM (SELECT ap.archivo_id FROM archivo_problemas ap JOIN problemas p ON p.id=ap.problema_id WHERE p.activo=TRUE AND p.estado='PUBLICADO' UNION ALL SELECT ars.archivo_id FROM archivo_soluciones ars JOIN soluciones s ON s.id=ars.solucion_id JOIN problemas p ON p.id=s.problema_id WHERE s.activo=TRUE AND p.activo=TRUE AND p.estado='PUBLICADO' UNION ALL SELECT apa.archivo_id FROM archivo_pasos apa JOIN pasos_solucion ps ON ps.id=apa.paso_id JOIN soluciones s ON s.id=ps.solucion_id JOIN problemas p ON p.id=s.problema_id WHERE ps.activo=TRUE AND s.activo=TRUE AND p.activo=TRUE AND p.estado='PUBLICADO') visibles WHERE archivo_id=?";return jdbc.queryForObject(sql,Integer.class,id)>0;}
    private Path resolverSegura(String relativa){Path p=raiz.resolve(relativa).normalize();if(!p.startsWith(raiz))throw new SecurityException("Ruta de almacenamiento inválida.");return p;}
    private String sha256(byte[] b)throws NoSuchAlgorithmException{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    private ArchivoInvalidoException invalido(HttpStatus s,String c,String m){return new ArchivoInvalidoException(s,c,m);}
    private void limpiar(Path destino){if(destino!=null)try{Files.deleteIfExists(destino);}catch(IOException ignored){}}
    private ArchivoResponse response(Archivo a){return new ArchivoResponse(a.getId(),a.getNombreOriginal(),a.getTipoMime(),a.getTamanioBytes(),a.getHashSha256(),a.getTextoAlternativo(),a.getOrden(),a.isActivo(),a.getCreadoEn(),"/api/archivos/"+a.getId()+"/contenido",a.getVersion());}
    public record ContenidoArchivo(String mime,String nombre,byte[] bytes){}
}
