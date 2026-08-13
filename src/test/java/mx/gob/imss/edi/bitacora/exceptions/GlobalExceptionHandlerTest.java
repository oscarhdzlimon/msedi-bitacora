package mx.gob.imss.edi.bitacora.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    @Test
    void usaRespuestaGenericaParaErrorFuncional() {
        var response = new GlobalExceptionHandler().handleEdi(
                new EdiException(HttpStatus.BAD_REQUEST, "EVENTO_NO_SOPORTADO", "No soportado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().exito()).isFalse();
        assertThat(response.getBody().respuesta()).containsEntry("codigo", "EVENTO_NO_SOPORTADO");
    }
}
