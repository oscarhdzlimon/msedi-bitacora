package mx.gob.imss.edi.bitacora.models.dto;

import java.time.OffsetDateTime;

public record EventoConsultaDto(
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
        String detalle,
        OffsetDateTime stpOcurrencia,
        OffsetDateTime stpAlta) {
}
