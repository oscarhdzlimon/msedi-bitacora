package mx.gob.imss.edi.bitacora.security;

public record JwtPrincipal(
        String subject,
        String jti,
        Long idTransaccion,
        String cveTransaccion,
        String sistemaOrigen,
        String user) {
}
