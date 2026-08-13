package mx.gob.imss.edi.bitacora.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import mx.gob.imss.edi.bitacora.exceptions.EdiException;
import mx.gob.imss.edi.bitacora.models.dto.EventoBitacoraRegistro;
import mx.gob.imss.edi.bitacora.models.dto.EventoCatalogoDto;
import mx.gob.imss.edi.bitacora.models.dto.EventoConsultaDto;
import mx.gob.imss.edi.bitacora.models.dto.TransaccionContextoDto;
import mx.gob.imss.edi.bitacora.models.request.FiltroEventosRequest;
import mx.gob.imss.edi.bitacora.models.request.RegistroEventoRequest;
import mx.gob.imss.edi.bitacora.repository.EventoBitacoraMapper;
import mx.gob.imss.edi.bitacora.repository.EventoMapper;
import mx.gob.imss.edi.bitacora.repository.MensajeMapper;
import mx.gob.imss.edi.bitacora.repository.TransaccionConsultaMapper;
import mx.gob.imss.edi.bitacora.security.JwtPrincipal;
import mx.gob.imss.edi.bitacora.security.SecurityContextService;
import mx.gob.imss.edi.bitacora.services.DatosSensiblesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BitacoraServiceImplTest {

    @Mock private EventoMapper eventoMapper;
    @Mock private MensajeMapper mensajeMapper;
    @Mock private TransaccionConsultaMapper transaccionMapper;
    @Mock private EventoBitacoraMapper bitacoraMapper;
    @Mock private SecurityContextService securityContextService;

    private BitacoraServiceImpl service;
    private JwtPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new JwtPrincipal("msedi-autenticacion", "jti-1", 12L, "tx-1", "SIMF", "usr-1");
        service = new BitacoraServiceImpl(
                eventoMapper, mensajeMapper, transaccionMapper, bitacoraMapper,
                securityContextService, new DatosSensiblesService(), new ObjectMapper());
    }

    @Test
    void hu003Ca01RegistraEventoValidoConIdentidadDerivada() {
        when(securityContextService.validarTransaccion(12L)).thenReturn(principal);
        when(eventoMapper.buscarActivoPorClave("TOKEN_EDI_GENERADO"))
                .thenReturn(new EventoCatalogoDto(8L, "TOKEN_EDI_GENERADO"));
        when(transaccionMapper.buscarContexto(12L))
                .thenReturn(new TransaccionContextoDto(12L, 2L, "SIMF", "tx-1"));
        when(mensajeMapper.existeActivo("MSG001")).thenReturn(true);
        when(bitacoraMapper.insertar(any())).thenAnswer(invocation -> {
            EventoBitacoraRegistro registro = invocation.getArgument(0);
            registro.setIdEventoBitacora(99L);
            return 1;
        });
        var request = new RegistroEventoRequest(
                "token_edi_generado", "PROCESO", "EXITOSA", OffsetDateTime.now(),
                "msg001", "ACCESO_EDI", null,
                Map.of("nss", "12345678901", "password", "secreto"));

        var response = service.registrar(12L, request, "127.0.0.1");

        assertThat(response.idEventoBitacora()).isEqualTo(99L);
        ArgumentCaptor<EventoBitacoraRegistro> captor = ArgumentCaptor.forClass(EventoBitacoraRegistro.class);
        verify(bitacoraMapper).insertar(captor.capture());
        EventoBitacoraRegistro registro = captor.getValue();
        assertThat(registro.getCveOperacion()).isEqualTo("proceso");
        assertThat(registro.getRefNombreUsuario()).isEqualTo("usr-1");
        assertThat(registro.getRefSesion()).isEqualTo("jti-1");
        assertThat(registro.getRefDetalle())
                .contains("MSG001", "*******8901", "[REDACTADO]")
                .doesNotContain("12345678901", "secreto");
    }

    @Test
    void hu003RechazaEventoInexistente() {
        when(securityContextService.validarTransaccion(12L)).thenReturn(principal);
        var request = new RegistroEventoRequest(
                "NO_EXISTE", "proceso", "ERROR", OffsetDateTime.now(), null, null, null, null);

        assertThatThrownBy(() -> service.registrar(12L, request, "127.0.0.1"))
                .isInstanceOf(EdiException.class)
                .extracting("codigo").isEqualTo("EVENTO_NO_SOPORTADO");
        verify(bitacoraMapper, never()).insertar(any());
    }

    @Test
    void hu003Ca04MapeaDetalleYConservaOrdenDelMapper() {
        when(securityContextService.validarTransaccion(12L)).thenReturn(principal);
        when(transaccionMapper.buscarContexto(12L))
                .thenReturn(new TransaccionContextoDto(12L, 2L, "SIMF", "tx-1"));
        OffsetDateTime primera = OffsetDateTime.now().minusMinutes(2);
        OffsetDateTime segunda = OffsetDateTime.now().minusMinutes(1);
        when(bitacoraMapper.consultarPorTransaccion(12L)).thenReturn(List.of(
                evento(1L, primera, "{\"codigoMensaje\":\"MSG001\"}"),
                evento(2L, segunda, null)));

        var response = service.consultarPorTransaccion(12L);

        assertThat(response).extracting(item -> item.idEventoBitacora()).containsExactly(1L, 2L);
        assertThat(response.getFirst().detalle().get("codigoMensaje").asText()).isEqualTo("MSG001");
    }

    @Test
    void noExponeFiltroNssHastaQueExistaRelacionFuncional() {
        when(securityContextService.principalActual()).thenReturn(principal);
        when(securityContextService.validarTransaccion(12L)).thenReturn(principal);
        when(transaccionMapper.buscarContexto(12L))
                .thenReturn(new TransaccionContextoDto(12L, 2L, "SIMF", "tx-1"));
        var filtros = new FiltroEventosRequest(12L, null, "12345678901", null,
                null, null, null, null, null);

        assertThatThrownBy(() -> service.consultar(filtros))
                .isInstanceOf(EdiException.class)
                .extracting("codigo").isEqualTo("FILTRO_FUNCIONAL_NO_DISPONIBLE");
    }

    private EventoConsultaDto evento(Long id, OffsetDateTime ocurrencia, String detalle) {
        return new EventoConsultaDto(id, 12L, "tx-1", "TOKEN_EDI_GENERADO", "Token generado",
                "proceso", "EXITOSA", "usr-1", "ACCESO_EDI", null, detalle,
                ocurrencia, ocurrencia.plusSeconds(1));
    }
}
