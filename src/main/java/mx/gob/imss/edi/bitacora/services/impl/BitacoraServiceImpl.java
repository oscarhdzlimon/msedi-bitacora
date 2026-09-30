package mx.gob.imss.edi.bitacora.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mx.gob.imss.edi.bitacora.common.constants.OperacionBitacora;
import mx.gob.imss.edi.bitacora.exceptions.EdiException;
import mx.gob.imss.edi.bitacora.models.dto.EventoBitacoraRegistro;
import mx.gob.imss.edi.bitacora.models.dto.EventoCatalogoDto;
import mx.gob.imss.edi.bitacora.models.dto.EventoConsultaDto;
import mx.gob.imss.edi.bitacora.models.dto.TransaccionContextoDto;
import mx.gob.imss.edi.bitacora.models.request.FiltroEventosRequest;
import mx.gob.imss.edi.bitacora.models.request.RegistroEventoRequest;
import mx.gob.imss.edi.bitacora.models.response.EventoConsultaResponse;
import mx.gob.imss.edi.bitacora.models.response.RegistroEventoResponse;
import mx.gob.imss.edi.bitacora.repository.EventoBitacoraMapper;
import mx.gob.imss.edi.bitacora.repository.EventoMapper;
import mx.gob.imss.edi.bitacora.repository.MensajeMapper;
import mx.gob.imss.edi.bitacora.repository.TransaccionConsultaMapper;
import mx.gob.imss.edi.bitacora.security.JwtPrincipal;
import mx.gob.imss.edi.bitacora.security.SecurityContextService;
import mx.gob.imss.edi.bitacora.services.BitacoraService;
import mx.gob.imss.edi.bitacora.services.DatosSensiblesService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BitacoraServiceImpl implements BitacoraService {

    private final EventoMapper eventoMapper;
    private final MensajeMapper mensajeMapper;
    private final TransaccionConsultaMapper transaccionMapper;
    private final EventoBitacoraMapper bitacoraMapper;
    private final SecurityContextService securityContextService;
    private final DatosSensiblesService datosSensiblesService;
    private final ObjectMapper objectMapper;

    public BitacoraServiceImpl(
            EventoMapper eventoMapper,
            MensajeMapper mensajeMapper,
            TransaccionConsultaMapper transaccionMapper,
            EventoBitacoraMapper bitacoraMapper,
            SecurityContextService securityContextService,
            DatosSensiblesService datosSensiblesService,
            ObjectMapper objectMapper) {
        this.eventoMapper = eventoMapper;
        this.mensajeMapper = mensajeMapper;
        this.transaccionMapper = transaccionMapper;
        this.bitacoraMapper = bitacoraMapper;
        this.securityContextService = securityContextService;
        this.datosSensiblesService = datosSensiblesService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RegistroEventoResponse registrar(Long idTransaccion, RegistroEventoRequest request, String terminal) {
        JwtPrincipal principal = securityContextService.validarTransaccion(idTransaccion);
        String cveEvento = request.cveEvento().strip().toUpperCase(Locale.ROOT);
        EventoCatalogoDto evento = eventoMapper.buscarActivoPorClave(cveEvento);
        if (evento == null) {
            throw new EdiException(HttpStatus.BAD_REQUEST, "EVENTO_NO_SOPORTADO",
                    "La clave de evento no existe o no se encuentra activa");
        }

        TransaccionContextoDto transaccion = transaccionMapper.buscarContexto(idTransaccion);
        if (transaccion == null) {
            throw new EdiException(HttpStatus.NOT_FOUND, "TRANSACCION_NO_ENCONTRADA",
                    "La transaccion indicada no existe");
        }
        if (!transaccion.cveSistemaOrigen().equalsIgnoreCase(principal.sistemaOrigen())) {
            throw new EdiException(HttpStatus.FORBIDDEN, "SISTEMA_ORIGEN_NO_CORRESPONDE",
                    "El sistema origen no corresponde con la transaccion");
        }

        String codigoMensaje = codigoMensaje(request);
        Long idMensaje = null;
        if (codigoMensaje != null) {
            idMensaje = mensajeMapper.idActivoPorClave(codigoMensaje);
        }
        if (codigoMensaje != null && idMensaje == null) {
            throw new EdiException(HttpStatus.BAD_REQUEST, "MENSAJE_NO_SOPORTADO",
                    "La clave de mensaje no existe o no se encuentra activa");
        }
        String operacion = OperacionBitacora.normalizar(request.cveOperacion())
                .orElseThrow(() -> new EdiException(HttpStatus.BAD_REQUEST, "OPERACION_NO_SOPORTADA",
                        "La operacion de bitacora no esta soportada"));

        EventoBitacoraRegistro registro = crearRegistro(
                idTransaccion, request, terminal, principal, transaccion, evento,
                idMensaje, codigoMensaje, operacion);
        if (bitacoraMapper.insertar(registro) != 1 || registro.getIdEventoBitacora() == null) {
            throw new EdiException(HttpStatus.INTERNAL_SERVER_ERROR, "EVENTO_NO_REGISTRADO",
                    "No fue posible confirmar el registro del evento");
        }
        return new RegistroEventoResponse(registro.getIdEventoBitacora());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoConsultaResponse> consultarPorTransaccion(Long idTransaccion) {
        securityContextService.validarTransaccion(idTransaccion);
        validarExistenciaTransaccion(idTransaccion);
        return mapear(bitacoraMapper.consultarPorTransaccion(idTransaccion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoConsultaResponse> consultar(FiltroEventosRequest filtros) {
        JwtPrincipal principal = securityContextService.principalActual();
        Long idTransaccion = filtros.idTransaccion() == null
                ? principal.idTransaccion() : filtros.idTransaccion();
        securityContextService.validarTransaccion(idTransaccion);
        validarExistenciaTransaccion(idTransaccion);
        if (StringUtils.hasText(filtros.nss())) {
            throw new EdiException(HttpStatus.BAD_REQUEST, "FILTRO_FUNCIONAL_NO_DISPONIBLE",
                    "El filtro NSS estara disponible cuando exista la incapacidad persistida");
        }
        if (filtros.fechaInicio() != null && filtros.fechaFin() != null
                && filtros.fechaInicio().isAfter(filtros.fechaFin())) {
            throw new EdiException(HttpStatus.BAD_REQUEST, "RANGO_FECHAS_INVALIDO",
                    "La fecha inicial no puede ser posterior a la fecha final");
        }
        return mapear(bitacoraMapper.consultar(
                idTransaccion, filtros.cveTransaccion(), filtros.cveUsuario(),
                normalizarOpcional(filtros.folioIncapacidad()),
                filtros.fechaInicio(), filtros.fechaFin(), filtros.cveEvento(), filtros.resultado()));
    }

    private EventoBitacoraRegistro crearRegistro(
            Long idTransaccion,
            RegistroEventoRequest request,
            String terminal,
            JwtPrincipal principal,
            TransaccionContextoDto transaccion,
            EventoCatalogoDto evento,
            Long idMensaje,
            String codigoMensaje,
            String operacion) {
        EventoBitacoraRegistro registro = new EventoBitacoraRegistro();
        registro.setIdEvento(evento.idEvento());
        registro.setIdMensaje(idMensaje);
        registro.setIdTransaccion(idTransaccion);
        registro.setIdSistemaOrigen(transaccion.idSistemaOrigen());
        registro.setRefNombreUsuario(principal.user());
        registro.setRefError(datosSensiblesService.sanitizarTexto(request.error()));
        registro.setRefDetalle(serializarDetalle(codigoMensaje, request.detalle()));
        registro.setRefSesion(principal.jti());
        registro.setRefTerminal(terminal);
        registro.setRefObjeto(request.objeto());
        registro.setCveFolioIncapacidad(normalizarOpcional(request.cveFolioIncapacidad()));
        registro.setCveOperacion(operacion);
        registro.setRefResultado(request.resultado());
        registro.setStpOcurrencia(request.stpOcurrencia());
        registro.setCveUsuarioAlta(StringUtils.hasText(principal.user())
                ? principal.user() : "SERVICIO:" + principal.subject());
        return registro;
    }

    private String serializarDetalle(String codigoMensaje, Map<String, Object> detalle) {
        Map<String, Object> contenido = new LinkedHashMap<>();
        if (codigoMensaje != null) {
            contenido.put("codigoMensaje", codigoMensaje);
        }
        Map<String, Object> sanitizado = datosSensiblesService.sanitizar(detalle);
        if (!sanitizado.isEmpty()) {
            contenido.put("detalle", sanitizado);
        }
        if (contenido.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(contenido);
        } catch (JsonProcessingException exception) {
            throw new EdiException(HttpStatus.BAD_REQUEST, "DETALLE_INVALIDO",
                    "El detalle del evento no se puede serializar");
        }
    }

    private List<EventoConsultaResponse> mapear(List<EventoConsultaDto> eventos) {
        if (eventos == null) {
            return List.of();
        }
        return eventos.stream().map(evento -> new EventoConsultaResponse(
                evento.idEventoBitacora(), evento.idTransaccion(), evento.cveTransaccion(),
                evento.cveEvento(), evento.descripcionEvento(), evento.cveOperacion(), evento.resultado(),
                evento.usuario(), evento.objeto(), evento.cveFolioIncapacidad(),
                evento.error(), leerDetalle(evento.detalle()),
                evento.stpOcurrencia(), evento.stpAlta())).toList();
    }

    private JsonNode leerDetalle(String detalle) {
        if (!StringUtils.hasText(detalle)) {
            return null;
        }
        try {
            return objectMapper.readTree(detalle);
        } catch (JsonProcessingException exception) {
            return objectMapper.getNodeFactory().textNode(detalle);
        }
    }

    private void validarExistenciaTransaccion(Long idTransaccion) {
        if (transaccionMapper.buscarContexto(idTransaccion) == null) {
            throw new EdiException(HttpStatus.NOT_FOUND, "TRANSACCION_NO_ENCONTRADA",
                    "La transaccion indicada no existe");
        }
    }

    private String normalizarOpcional(String value) {
        return StringUtils.hasText(value) ? value.strip().toUpperCase(Locale.ROOT) : null;
    }

    private String codigoMensaje(RegistroEventoRequest request) {
        String codigoMensaje = normalizarOpcional(request.codigoMensaje());
        if (codigoMensaje != null) {
            return codigoMensaje;
        }
        Map<String, Object> detalle = request.detalle();
        if (detalle == null || detalle.isEmpty()) {
            return null;
        }
        codigoMensaje = normalizarOpcional(valorTexto(detalle.get("codigoMensaje")));
        if (codigoMensaje != null) {
            return codigoMensaje;
        }
        codigoMensaje = normalizarOpcional(valorTexto(detalle.get("cveMensaje")));
        if (codigoMensaje != null) {
            return codigoMensaje;
        }
        Object mensaje = detalle.get("mensaje");
        if (mensaje instanceof Map<?, ?> mensajeMap) {
            return normalizarOpcional(valorTexto(mensajeMap.get("codigo")));
        }
        return null;
    }

    private String valorTexto(Object value) {
        return value == null ? null : value.toString();
    }
}
