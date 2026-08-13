package mx.gob.imss.edi.bitacora.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RespuestaGenericaTest {

    @Test
    void construyeUnaRespuestaExitosaTipada() {
        RespuestaGenerica<Long> respuesta = RespuestaGenerica.exitosa("ok", 1L);

        assertThat(respuesta.exito()).isTrue();
        assertThat(respuesta.respuesta()).isEqualTo(1L);
    }
}
