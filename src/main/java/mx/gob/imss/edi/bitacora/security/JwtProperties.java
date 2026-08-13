package mx.gob.imss.edi.bitacora.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edi.seguridad.jwt")
public record JwtProperties(String secretKey, String issuer, String audience) {
}
