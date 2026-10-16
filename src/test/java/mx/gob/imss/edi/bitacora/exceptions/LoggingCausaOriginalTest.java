package mx.gob.imss.edi.bitacora.exceptions;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.SQLException;
import mx.gob.imss.edi.bitacora.filters.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class LoggingCausaOriginalTest {
    @RestController
    static class Controlador {
        private final RuntimeException error;
        Controlador(RuntimeException error) { this.error=error; }
        @GetMapping("/prueba-log") String consultar() { throw error; }
    }

    @Test void conservaCausaEnErrorTransformadoSinExponerDetallesTecnicos() throws Exception {
        var original = new SQLException("Fallo de conexion de prueba","08001",7);
        RuntimeException error = new EdiException(HttpStatus.SERVICE_UNAVAILABLE,"PRUEBA_LOG","Mensaje publico",original);
        assertThat(error.getCause()).isSameAs(original);
        verificar(error, 503);
    }

    @Test void falloDeBaseSinTransformarTambienRegistraCausaSql() throws Exception {
        verificar(new DataAccessResourceFailureException("Consulta fallida",
                new SQLException("Fallo de conexion de prueba","08001",7)), 503);
    }

    private void verificar(RuntimeException error, int estado) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.setContext(logger.getLoggerContext()); appender.start(); logger.addAppender(appender);
        try {
            var mvc = MockMvcBuilders.standaloneSetup(new Controlador(error))
                    .setControllerAdvice(new GlobalExceptionHandler()).build();
            var response = mvc.perform(get("/prueba-log")
                    .requestAttr(CorrelationIdFilter.REQUEST_ATTRIBUTE,"corr-log"))
                    .andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(estado);
            assertThat(response.getContentAsString()).doesNotContain("Fallo de conexion de prueba","08001","Consulta fallida");
            var evento = appender.list.stream().filter(e -> e.getFormattedMessage().contains("sqlState=")).findFirst().orElseThrow();
            assertThat(evento.getFormattedMessage()).contains("rootCause=java.sql.SQLException",
                    "rootMessage=Fallo de conexion de prueba","sqlState=08001","sqlErrorCode=7","correlacionId=corr-log");
            assertThat(evento.getThrowableProxy()).isNotNull();
        } finally {
            logger.detachAppender(appender); appender.stop();
        }
    }
}
