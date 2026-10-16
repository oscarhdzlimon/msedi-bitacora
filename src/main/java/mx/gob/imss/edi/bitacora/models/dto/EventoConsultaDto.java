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
        OffsetDateTime stpAlta,
        Long idTransaccionActor,
        String perfilUsuario,
        String nombreUsuario) {
    @org.apache.ibatis.annotations.AutomapConstructor
    public EventoConsultaDto { }

    public EventoConsultaDto(Long idEventoBitacora, Long idTransaccion, String cveTransaccion,
            String cveEvento, String descripcionEvento, String cveOperacion, String resultado,
            String usuario, String objeto, String cveFolioIncapacidad, String error, String detalle,
            OffsetDateTime stpOcurrencia, OffsetDateTime stpAlta) {
        this(idEventoBitacora, idTransaccion, cveTransaccion, cveEvento, descripcionEvento,
                cveOperacion, resultado, usuario, objeto, cveFolioIncapacidad, error, detalle,
                stpOcurrencia, stpAlta, null, null, null);
    }
}
