package mx.gob.imss.edi.bitacora.security;

public record JwtPrincipal(
        String subject,
        String jti,
        Long idTransaccion,
        String cveTransaccion,
        String sistemaOrigen,
        String user, String tokenType, String perfil, String cveOoad,
        String cveUnidad, String matricula, String scope, String sesionActor) {
    public JwtPrincipal(String subject, String jti, Long idTransaccion, String cveTransaccion,
            String sistemaOrigen, String user, String tokenType, String perfil, String cveOoad,
            String cveUnidad, String matricula) {
        this(subject,jti,idTransaccion,cveTransaccion,sistemaOrigen,user,tokenType,perfil,cveOoad,
                cveUnidad,matricula,null,null);
    }
    public JwtPrincipal(String subject, String jti, Long idTransaccion, String cveTransaccion,
            String sistemaOrigen, String user) {
        this(subject, jti, idTransaccion, cveTransaccion, sistemaOrigen, user,
                null, null, null, null, null);
    }
}
