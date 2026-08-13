package mx.gob.imss.edi.bitacora.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatosSensiblesTextoTest {

    private final DatosSensiblesService service = new DatosSensiblesService();

    @Test
    void saneaSecretosEIdentificadoresEnTextoLibre() {
        String texto = "Bearer abc.def password=secreto NSS 12345678901 CURP SARS781031MDFNMS05";

        assertThat(service.sanitizarTexto(texto))
                .contains("Bearer [REDACTADO]", "password=[REDACTADO]", "*******8901", "**************MS05")
                .doesNotContain("abc.def", "secreto", "12345678901", "SARS781031MDFNMS05");
    }
}
