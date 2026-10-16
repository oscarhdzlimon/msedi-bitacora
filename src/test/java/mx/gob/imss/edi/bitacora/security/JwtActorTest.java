package mx.gob.imss.edi.bitacora.security;
import static org.assertj.core.api.Assertions.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
class JwtActorTest {
    final String secreto = "01234567890123456789012345678901";
    String token(String firma) {
        return Jwts.builder().issuer("edi").subject("edi-user").id("sesion40")
                .audience().add("edi-api").and().expiration(Date.from(Instant.now().plusSeconds(60)))
                .claim("idTransaccion",40L).claim("sistemaOrigen","SIMF").claim("user","simf_edi")
                .claim("tokenType","SESION_EDI").claim("perfil","Perfil_DPM_Autorizador")
                .claim("matricula","00123456").claim("cveOoad","02").claim("cveUnidad","UMF01")
                .signWith(Keys.hmacShaKeyFor(firma.getBytes(StandardCharsets.UTF_8))).compact();
    }
    @Test void identidadYAlcanceSeObtienenDeClaimsFirmados() {
        var actor = new JwtService(new JwtProperties(secreto,"edi","edi-api")).validar(token(secreto));
        assertThat(actor.idTransaccion()).isEqualTo(40L);
        assertThat(actor.matricula()).isEqualTo("00123456");
        assertThat(actor.perfil()).isEqualTo("Perfil_DPM_Autorizador");
        assertThat(actor.cveOoad()).isEqualTo("02");
        assertThat(actor.cveUnidad()).isEqualTo("UMF01");
        assertThat(actor.tokenType()).isEqualTo("SESION_EDI");
    }
    @Test void noAceptaIdentidadConFirmaAjena() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(secreto,"edi","edi-api"))
                .validar(token("otra-clave-secreta-de-al-menos-32-caracteres")))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
