package mx.gob.imss.edi.bitacora.models.dto;

public record AutorizacionTrazaDto(Long idTransaccion, Long idIncapacidad,
        String folioIncapacidad, String cveOoad, String cveUnidad) { }
