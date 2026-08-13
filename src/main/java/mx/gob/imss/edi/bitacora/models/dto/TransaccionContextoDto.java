package mx.gob.imss.edi.bitacora.models.dto;

public record TransaccionContextoDto(
        Long idTransaccion,
        Long idSistemaOrigen,
        String cveSistemaOrigen,
        String cveTransaccion) {
}
