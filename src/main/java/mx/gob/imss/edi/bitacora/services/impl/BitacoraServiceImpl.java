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

        Long transaccionNegocio = transaccionAutorizacion(cveEvento, request, principal);
        if (transaccionNegocio != null && !transaccionNegocio.equals(idTransaccion)) {
            transaccion = transaccionMapper.buscarContexto(transaccionNegocio);
            if (transaccion == null) throw new EdiException(HttpStatus.NOT_FOUND,
                    "TRANSACCION_NO_ENCONTRADA", "La transaccion original no existe");
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
                transaccion.idTransaccion(), request, terminal, principal, transaccion, evento,
                idMensaje, codigoMensaje, operacion);
        registro.setRefClaveDeduplicacion(claveResumen(cveEvento, request));
        if (registro.getRefClaveDeduplicacion() != null) {
            // Serializar solo este resumen/etapa. Permite A -> B -> A, sin repetir A -> A.
            bitacoraMapper.bloquearResumen(registro);
            var anterior = bitacoraMapper.ultimoResumen(registro);
            String huella = registro.getRefClaveDeduplicacion();
            if (anterior != null && anterior.refClaveDeduplicacion().startsWith(huella + ":")) {
                return new RegistroEventoResponse(anterior.idEventoBitacora());
            }
            registro.setRefClaveDeduplicacion(huella + ":" + (anterior == null ? 0 : anterior.idEventoBitacora()));
        }
        int insertados = bitacoraMapper.insertar(registro);
        if (insertados == 0 && registro.getRefClaveDeduplicacion() != null) {
            Long existente = bitacoraMapper.buscarDuplicado(registro);
            if (existente != null) return new RegistroEventoResponse(existente);
        }
        if (insertados != 1 || registro.getIdEventoBitacora() == null) {
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
        registro.setIdTransaccionActor(principal.idTransaccion());
        completarActor(registro, principal);
        registro.setRefError(datosSensiblesService.sanitizarTexto(request.error()));
        registro.setRefDetalle(serializarDetalle(codigoMensaje, request.detalle()));
        registro.setRefSesion(servicioAuditoria(principal) ? principal.sesionActor() : principal.jti());
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

    private static final java.util.Set<String> EVENTOS_AUTORIZACION = java.util.Set.of(
            "AUTORIZACION_APROBADA", "AUTORIZACION_NO_AUTORIZADA",
            "CERTIFICADO_AUTORIZACION_GENERADO", "CERTIFICADO_AUTORIZACION_ERROR",
            "DOCUMENTO_AUTORIZACION_CORREO_ENVIADO", "DOCUMENTO_AUTORIZACION_CORREO_ERROR");

    private boolean servicioAuditoria(JwtPrincipal actor) {
        return "msedi-expedicion".equals(actor.subject()) && "SERVICIO_BITACORA".equals(actor.tokenType())
                && "edi:bitacora:autorizacion".equals(actor.scope())
                && StringUtils.hasText(actor.sesionActor()) && StringUtils.hasText(actor.matricula());
    }

    private Long transaccionAutorizacion(String evento, RegistroEventoRequest request, JwtPrincipal actor) {
        if (!EVENTOS_AUTORIZACION.contains(evento)) return null;
        if (!servicioAuditoria(actor)
                || !"PERFIL_DPM_AUTORIZADOR".equalsIgnoreCase(actor.perfil())) {
            throw new EdiException(HttpStatus.FORBIDDEN, "PERFIL_NO_AUTORIZADO",
                    "Se requiere publicacion interna de Expedicion con identidad del autorizador");
        }
        Long autorizacion = identificador(request.detalle(), "idAutorizacion");
        Long incapacidad = identificador(request.detalle(), "idIncapacidad");
        var contexto = autorizacion == null ? null : transaccionMapper.buscarAutorizacion(autorizacion);
        if (contexto == null || !java.util.Objects.equals(contexto.idIncapacidad(), incapacidad)
                || !StringUtils.hasText(actor.cveOoad()) || !StringUtils.hasText(actor.cveUnidad())
                || !actor.cveOoad().equals(contexto.cveOoad())
                || !actor.cveUnidad().equals(contexto.cveUnidad())
                || (StringUtils.hasText(request.cveFolioIncapacidad())
                    && !request.cveFolioIncapacidad().equals(contexto.folioIncapacidad()))) {
            throw new EdiException(HttpStatus.FORBIDDEN, "AUTORIZACION_TRAZA_NO_PERMITIDA",
                    "La autorizacion no corresponde a la incapacidad o al alcance del medico");
        }
        return contexto.idTransaccion();
    }

    private Long identificador(Map<String, Object> detalle, String campo) {
        Object valor = detalle == null ? null : detalle.get(campo);
        try {
            Long id = valor == null ? null : Long.valueOf(valor.toString());
            return id != null && id > 0 ? id : null;
        } catch (NumberFormatException ex) { return null; }
    }

    private void completarActor(EventoBitacoraRegistro registro, JwtPrincipal actor) {
        registro.setCveUsuario(StringUtils.hasText(actor.user()) ? actor.user() : "SERVICIO:" + actor.subject());
        // Los claims firmados identifican al medico. El nombre se toma del acceso ya consumido,
        // nunca del detalle del evento ni del payload de la incapacidad original.
        if ((!"SESION_EDI".equals(actor.tokenType()) && !servicioAuditoria(actor))
                || !StringUtils.hasText(actor.matricula())) return;
        registro.setCveUsuario(actor.matricula());
        registro.setRefPerfilUsuario(actor.perfil());
        String payload = transaccionMapper.buscarPayloadActor(actor.idTransaccion());
        if (!StringUtils.hasText(payload)) return;
        try {
            JsonNode medico = objectMapper.readTree(payload).path("medico");
            if (!actor.matricula().equals(medico.path("matricula").asText())
                    || !java.util.Objects.equals(actor.perfil(), medico.path("perfil").asText(null))) return;
            String nombre = medico.path("nombreCompleto").asText("").strip();
            if (nombre.isEmpty()) nombre = java.util.stream.Stream.of("nombres", "apellidoPaterno", "apellidoMaterno")
                    .map(c -> medico.path(c).asText("").strip()).filter(v -> !v.isEmpty())
                    .collect(java.util.stream.Collectors.joining(" "));
            registro.setRefNombreUsuario(nombre.isEmpty() ? null : nombre.substring(0, Math.min(nombre.length(), 150)));
        } catch (JsonProcessingException ex) {
            // Conservar matricula/perfil firmados aunque un acceso historico tenga JSON invalido.
        }
    }

    private String claveResumen(String evento, RegistroEventoRequest request) {
        if (!java.util.Set.of("NSSA_ANTECEDENTES_CONSULTADOS", "SIST_DICTAMENES_CONSULTADOS").contains(evento)
                || !"EXITOSA".equals(request.resultado()) || request.detalle() == null) return null;
        Object etapa = request.detalle().get("etapa");
        Object huella = request.detalle().get("huellaResultado");
        if (!(etapa instanceof String fase) || !java.util.Set.of("CAPTURA", "REGISTRO").contains(fase)
                || !(huella instanceof String hash) || !hash.matches("[a-f0-9]{64}")) return null;
        return fase + ":" + hash;
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
                    "El detalle del evento no se puede serializar", exception);
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
                evento.stpOcurrencia(), evento.stpAlta(), evento.idTransaccionActor(),
                evento.perfilUsuario(), evento.nombreUsuario())).toList();
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
