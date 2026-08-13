package mx.gob.imss.edi.bitacora.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String codigo,
        String mensaje,
        String path,
        String correlacionId,
        Map<String, String> validaciones) {
}
