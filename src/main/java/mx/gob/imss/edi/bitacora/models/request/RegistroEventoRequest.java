package mx.gob.imss.edi.bitacora.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Map;

public record RegistroEventoRequest(
        @NotBlank String cveEvento,
        @NotBlank String cveOperacion,
        @NotBlank String resultado,
        @NotNull OffsetDateTime stpOcurrencia,
        String codigoMensaje,
        String objeto,
        String error,
        Map<String, Object> detalle) {
}
