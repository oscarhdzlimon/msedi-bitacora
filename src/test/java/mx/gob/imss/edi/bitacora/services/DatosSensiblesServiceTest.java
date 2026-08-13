package mx.gob.imss.edi.bitacora.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DatosSensiblesServiceTest {

    private final DatosSensiblesService service = new DatosSensiblesService();

    @Test
    void redactaSecretosYEnmascaraIdentificadores() {
        Map<String, Object> resultado = service.sanitizar(Map.of(
                "apiKey", "secreto",
                "nss", "12345678901",
                "anidado", Map.of("password", "clave", "curp", "SARS781031MDFNMS05")));

        assertThat(resultado.get("apiKey")).isEqualTo("[REDACTADO]");
        assertThat(resultado.get("nss")).isEqualTo("*******8901");
        @SuppressWarnings("unchecked")
        Map<String, Object> anidado = (Map<String, Object>) resultado.get("anidado");
        assertThat(anidado)
                .containsEntry("password", "[REDACTADO]")
                .containsEntry("curp", "**************MS05");
    }
}
