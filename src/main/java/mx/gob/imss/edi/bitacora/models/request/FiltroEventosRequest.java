package mx.gob.imss.edi.bitacora.models.request;

import java.time.OffsetDateTime;

public record FiltroEventosRequest(
        Long idTransaccion,
        String cveTransaccion,
        String nss,
        String folioIncapacidad,
        String cveUsuario,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        String cveEvento,
        String resultado) {
}
