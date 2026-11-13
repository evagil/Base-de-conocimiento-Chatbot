package com.municipio.base_conocimiento.compartido.web;

import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import com.municipio.base_conocimiento.compartido.exception.ConflictoException;
import com.municipio.base_conocimiento.compartido.exception.RecursoNoEncontradoException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {
    @ExceptionHandler(com.municipio.base_conocimiento.documentos.exception.DocumentoException.class)
    ResponseEntity<ErrorResponse> errorDocumento(com.municipio.base_conocimiento.documentos.exception.DocumentoException ex) {
        return respuesta(ex.getStatus(), ex.getCodigo(), ex.getMessage(), Map.of());
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> multipartGrande(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "ARCHIVO_DEMASIADO_GRANDE", "El archivo supera el tamaño máximo permitido.", Map.of());
    }
    @ExceptionHandler(com.municipio.base_conocimiento.ia.exception.IaException.class)
    ResponseEntity<ErrorResponse> errorIa(com.municipio.base_conocimiento.ia.exception.IaException ex) {
        return respuesta(ex.getStatus(), ex.getCodigo(), ex.getMessage(), Map.of());
    }
    @ExceptionHandler(ArchivoInvalidoException.class)
    ResponseEntity<ErrorResponse> archivoInvalido(ArchivoInvalidoException ex) {
        return respuesta(ex.getStatus(), ex.getCodigo(), ex.getMessage(), Map.of());
    }
    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictoException.class)
    ResponseEntity<ErrorResponse> conflicto(ConflictoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return respuesta(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Hay datos que deben corregirse.", campos);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> versionDesactualizada(ObjectOptimisticLockingFailureException ex) {
        return respuesta(HttpStatus.CONFLICT, "VERSION_DESACTUALIZADA",
                "Otra persona modificó este registro. Tus cambios no se guardaron. Conservá tu texto y consultá la versión actual antes de volver a editar.", Map.of());
    }

    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> operacionEnCurso(RuntimeException ex) {
        return respuesta(HttpStatus.CONFLICT, "OPERACION_EN_CURSO",
                "Hay otra operación sobre este registro. Tus cambios no se guardaron; intentá nuevamente.", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integridad(RuntimeException ex) {
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO_DE_DATOS",
                "La operación entra en conflicto con información existente o modificada recientemente.", Map.of());
    }

    private ResponseEntity<ErrorResponse> respuesta(
            HttpStatus estado, String codigo, String mensaje, Map<String, String> campos) {
        return ResponseEntity.status(estado)
                .body(new ErrorResponse(codigo, mensaje, campos, Instant.now()));
    }
}
