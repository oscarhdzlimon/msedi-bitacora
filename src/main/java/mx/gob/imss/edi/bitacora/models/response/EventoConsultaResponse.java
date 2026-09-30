package mx.gob.imss.edi.bitacora.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventoConsultaResponse(
        Long idEventoBitacora,
        Long idTransaccion,
        String cveTransaccion,
        String cveEvento,
        String descripcionEvento,
        String cveOperacion,
        String resultado,
        String usuario,
        String objeto,
        String cveFolioIncapacidad,
        String error,
        JsonNode detalle,
        OffsetDateTime stpOcurrencia,
        OffsetDateTime stpAlta) {
}
