package mx.gob.imss.edi.bitacora.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import mx.gob.imss.edi.bitacora.common.response.RespuestaGenerica;
import mx.gob.imss.edi.bitacora.filters.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EdiException.class)
    public ResponseEntity<RespuestaGenerica<Map<String, String>>> handleEdi(EdiException exception) {
        return ResponseEntity.status(exception.getStatus()).body(RespuestaGenerica.fallida(
                exception.getMessage(), Map.of("codigo", exception.getCodigo())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud contiene datos invalidos", request, errors);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabase(DataAccessException exception, HttpServletRequest request) {
        LOGGER.error("Error de persistencia de bitacora", exception);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "BITACORA_NO_DISPONIBLE",
                "La bitacora no se encuentra disponible", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Error no controlado", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrio un error interno al procesar la solicitud", request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status, String codigo, String mensaje, HttpServletRequest request,
            Map<String, String> validaciones) {
        var body = new ApiErrorResponse(
                OffsetDateTime.now(), status.value(), status.getReasonPhrase(), codigo, mensaje,
                request.getRequestURI(),
                (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE), validaciones);
        return ResponseEntity.status(status).body(body);
    }
}
