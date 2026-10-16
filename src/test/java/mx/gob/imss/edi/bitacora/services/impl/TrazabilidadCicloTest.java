package mx.gob.imss.edi.bitacora.services.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import mx.gob.imss.edi.bitacora.models.dto.*;
import mx.gob.imss.edi.bitacora.models.request.RegistroEventoRequest;
import mx.gob.imss.edi.bitacora.repository.*;
import mx.gob.imss.edi.bitacora.security.*;
import mx.gob.imss.edi.bitacora.services.DatosSensiblesService;
import mx.gob.imss.edi.bitacora.exceptions.EdiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TrazabilidadCicloTest {
    final EventoMapper eventos = mock(EventoMapper.class);
    final MensajeMapper mensajes = mock(MensajeMapper.class);
    final TransaccionConsultaMapper transacciones = mock(TransaccionConsultaMapper.class);
    final EventoBitacoraMapper registros = mock(EventoBitacoraMapper.class);
    final SecurityContextService seguridad = mock(SecurityContextService.class);
    final BitacoraServiceImpl servicio = new BitacoraServiceImpl(eventos, mensajes, transacciones,
            registros, seguridad, new DatosSensiblesService(), new ObjectMapper());

    JwtPrincipal actor(String tipo, String perfil, String ooad, String unidad) {
        return new JwtPrincipal("SERVICIO_BITACORA".equals(tipo) ? "msedi-expedicion" : "edi-user",
                "sesion-40", 40L, "tx40", "SIMF", "simf_edi", tipo, perfil, ooad, unidad, "00123456",
                "SERVICIO_BITACORA".equals(tipo) ? "edi:bitacora:autorizacion" : null, "sesion-40");
    }
    void preparar(String evento, JwtPrincipal actor) {
        when(seguridad.validarTransaccion(40L)).thenReturn(actor);
        when(eventos.buscarActivoPorClave(evento)).thenReturn(new EventoCatalogoDto(7L, evento));
        when(transacciones.buscarContexto(40L)).thenReturn(new TransaccionContextoDto(40L, 1L, "SIMF", "tx40"));
    }
    RegistroEventoRequest solicitud(String evento, String resultado, Map<String,Object> detalle) {
        return new RegistroEventoRequest(evento, "PROCESO", resultado,
                OffsetDateTime.now(), null, "INCAPACIDAD", null, detalle);
    }
    void insercion() {
        when(registros.insertar(any())).thenAnswer(i -> {
            ((EventoBitacoraRegistro)i.getArgument(0)).setIdEventoBitacora(99L); return 1;
        });
    }
    EventoBitacoraRegistro capturar() {
        var c = ArgumentCaptor.forClass(EventoBitacoraRegistro.class);
        verify(registros).insertar(c.capture()); return c.getValue();
    }
    @Test void autorizacionConservaTransaccionOriginalEIdentidadDelAutorizador() {
        preparar("AUTORIZACION_APROBADA", actor("SERVICIO_BITACORA", "Perfil_DPM_Autorizador", "02", "UMF01"));
        when(transacciones.buscarAutorizacion(8L)).thenReturn(new AutorizacionTrazaDto(39L, 4L, "F4", "02", "UMF01"));
        // El convocante original puede ser distinto del convocante del autorizador.
        when(transacciones.buscarContexto(39L)).thenReturn(new TransaccionContextoDto(39L, 2L, "MOCE", "tx39"));
        when(transacciones.buscarPayloadActor(40L)).thenReturn("""
                {"medico":{"matricula":"00123456","perfil":"Perfil_DPM_Autorizador",
                "nombres":"MEDICO","apellidoPaterno":"AUTORIZADOR","apellidoMaterno":"PRUEBA"}}
                """);
        insercion();
        servicio.registrar(40L, solicitud("AUTORIZACION_APROBADA", "EXITOSO",
                Map.of("idAutorizacion", 8L, "idIncapacidad", 4L)), "local");
        var r = capturar();
        assertThat(r.getIdTransaccion()).isEqualTo(39L);
        assertThat(r.getIdTransaccionActor()).isEqualTo(40L);
        assertThat(r.getIdSistemaOrigen()).isEqualTo(2L);
        assertThat(r.getCveUsuario()).isEqualTo("00123456");
        assertThat(r.getRefPerfilUsuario()).isEqualTo("Perfil_DPM_Autorizador");
        assertThat(r.getRefNombreUsuario()).isEqualTo("MEDICO AUTORIZADOR PRUEBA");
        assertThat(r.getRefSesion()).isEqualTo("sesion-40");
    }
    @Test void noPermiteAutorizarConPerfilTratante() {
        preparar("AUTORIZACION_APROBADA", actor("SERVICIO_BITACORA", "Perfil_DPM_Medico", "02", "UMF01"));
        assertThatThrownBy(() -> servicio.registrar(40L, solicitud("AUTORIZACION_APROBADA", "EXITOSO",
                Map.of("idAutorizacion", 8L, "idIncapacidad", 4L)), "local")).isInstanceOf(EdiException.class);
        verifyNoInteractions(registros);
    }
    @Test void noPermiteAsignarEventoAOtraIncapacidad() {
        preparar("AUTORIZACION_APROBADA", actor("SERVICIO_BITACORA", "Perfil_DPM_Autorizador", "02", "UMF01"));
        when(transacciones.buscarAutorizacion(8L)).thenReturn(new AutorizacionTrazaDto(39L, 4L, "F4", "02", "UMF01"));
        assertThatThrownBy(() -> servicio.registrar(40L, solicitud("AUTORIZACION_APROBADA", "EXITOSO",
                Map.of("idAutorizacion", 8L, "idIncapacidad", 99L)), "local")).isInstanceOf(EdiException.class);
        verifyNoInteractions(registros);
    }
    @Test void noPermiteUnidadFueraDeAlcanceNiTokenDeServicio() {
        preparar("AUTORIZACION_APROBADA", actor("SERVICIO_BITACORA", "Perfil_DPM_Autorizador", "02", "OTRA"));
        when(transacciones.buscarAutorizacion(8L)).thenReturn(new AutorizacionTrazaDto(39L, 4L, "F4", "02", "UMF01"));
        var request = solicitud("AUTORIZACION_APROBADA", "EXITOSO", Map.of("idAutorizacion", 8L, "idIncapacidad", 4L));
        assertThatThrownBy(() -> servicio.registrar(40L, request, "local")).isInstanceOf(EdiException.class);
        when(seguridad.validarTransaccion(40L)).thenReturn(actor("SERVICIO", "Perfil_DPM_Autorizador", "02", "UMF01"));
        assertThatThrownBy(() -> servicio.registrar(40L, request, "local")).isInstanceOf(EdiException.class);
        verifyNoInteractions(registros);
    }
    @Test void resumenDuplicadoDevuelveElRegistroExistente() {
        preparar("SIST_DICTAMENES_CONSULTADOS", actor("SERVICIO", null, null, null));
        when(registros.insertar(any())).thenReturn(0);
        when(registros.buscarDuplicado(any())).thenReturn(77L);
        var request = solicitud("SIST_DICTAMENES_CONSULTADOS", "EXITOSA",
                Map.of("etapa", "CAPTURA", "huellaResultado", "a".repeat(64)));
        assertThat(servicio.registrar(40L, request, "local").idEventoBitacora()).isEqualTo(77L);
        assertThat(capturar().getRefClaveDeduplicacion()).isEqualTo("CAPTURA:" + "a".repeat(64) + ":0");
    }
    @Test void incidenciasNoSeDeduplicanNiRecibenNombreDeMedicoDeUnServicio() {
        preparar("SIST_CONSULTA_NO_DISPONIBLE", actor("SERVICIO", null, null, null)); insercion();
        servicio.registrar(40L, solicitud("SIST_CONSULTA_NO_DISPONIBLE", "ERROR",
                Map.of("etapa", "CAPTURA", "huellaResultado", "a".repeat(64), "nombreUsuario", "FALSO")), "local");
        var r = capturar();
        assertThat(r.getRefClaveDeduplicacion()).isNull();
        assertThat(r.getCveUsuario()).isEqualTo("simf_edi");
        assertThat(r.getRefPerfilUsuario()).isNull();
        assertThat(r.getRefNombreUsuario()).isNull();
    }
    @Test void noAtribuyeElNombreSiPayloadNoCorrespondeALaMatriculaFirmada() {
        preparar("EVENTO", actor("SESION_EDI", "Perfil_DPM_Medico", "02", "UMF01")); insercion();
        when(transacciones.buscarPayloadActor(40L)).thenReturn("{\"medico\":{\"matricula\":\"OTRA\",\"nombreCompleto\":\"OTRO MEDICO\"}}");
        servicio.registrar(40L, solicitud("EVENTO", "EXITOSO", Map.of()), "local");
        assertThat(capturar().getRefNombreUsuario()).isNull();
    }
    @Test void cambioDeResultadoConservaElRegresoAUnResultadoAnterior() {
        preparar("SIST_DICTAMENES_CONSULTADOS", actor("SERVICIO", null, null, null)); insercion();
        when(registros.ultimoResumen(any())).thenReturn(new ResumenAnteriorDto(80L,
                "CAPTURA:" + "b".repeat(64) + ":70"));
        servicio.registrar(40L, solicitud("SIST_DICTAMENES_CONSULTADOS", "EXITOSA",
                Map.of("etapa", "CAPTURA", "huellaResultado", "a".repeat(64))), "local");
        assertThat(capturar().getRefClaveDeduplicacion()).isEqualTo("CAPTURA:" + "a".repeat(64) + ":80");
    }
    @Test void mismoResultadoConsecutivoNoGeneraOtraFila() {
        preparar("SIST_DICTAMENES_CONSULTADOS", actor("SERVICIO", null, null, null));
        when(registros.ultimoResumen(any())).thenReturn(new ResumenAnteriorDto(80L,
                "CAPTURA:" + "a".repeat(64) + ":70"));
        var response = servicio.registrar(40L, solicitud("SIST_DICTAMENES_CONSULTADOS", "EXITOSA",
                Map.of("etapa", "CAPTURA", "huellaResultado", "a".repeat(64))), "local");
        assertThat(response.idEventoBitacora()).isEqualTo(80L);
        verify(registros, never()).insertar(any());
    }
    @Test void tokenDeSesionNoPuedePublicarDirectamenteUnaAutorizacion() {
        preparar("AUTORIZACION_APROBADA", actor("SESION_EDI", "Perfil_DPM_Autorizador", "02", "UMF01"));
        assertThatThrownBy(() -> servicio.registrar(40L, solicitud("AUTORIZACION_APROBADA", "EXITOSO",
                Map.of("idAutorizacion", 8L, "idIncapacidad", 4L)), "local")).isInstanceOf(EdiException.class);
        verifyNoInteractions(registros);
    }
}
