package mx.gob.imss.edi.bitacora.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public JwtPrincipal validar(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey()).requireIssuer(properties.issuer())
                .build().parseSignedClaims(token).getPayload();
        validarAudiencia(claims.get("aud"));
        return new JwtPrincipal(
                claims.getSubject(), claims.getId(), asLong(claims.get("idTransaccion")),
                claims.get("cveTransaccion", String.class), claims.get("sistemaOrigen", String.class),
                claims.get("user", String.class));
    }

    private SecretKey signingKey() {
        if (!StringUtils.hasText(properties.secretKey())) {
            throw new JwtException("La llave JWT no esta configurada");
        }
        return Keys.hmacShaKeyFor(properties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    private void validarAudiencia(Object audienceClaim) {
        boolean valid = audienceClaim instanceof Collection<?> audiences
                ? audiences.contains(properties.audience())
                : properties.audience().equals(String.valueOf(audienceClaim));
        if (!valid) {
            throw new JwtException("Audiencia JWT invalida");
        }
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue()
                : value == null ? null : Long.valueOf(value.toString());
    }
}
