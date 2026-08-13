package mx.gob.imss.edi.bitacora.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import mx.gob.imss.edi.bitacora.common.response.RespuestaGenerica;
import mx.gob.imss.edi.bitacora.models.request.FiltroEventosRequest;
import mx.gob.imss.edi.bitacora.models.request.RegistroEventoRequest;
import mx.gob.imss.edi.bitacora.models.response.EventoConsultaResponse;
import mx.gob.imss.edi.bitacora.models.response.RegistroEventoResponse;
import mx.gob.imss.edi.bitacora.services.BitacoraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class BitacoraController {

    private final BitacoraService bitacoraService;

    public BitacoraController(BitacoraService bitacoraService) {
        this.bitacoraService = bitacoraService;
    }

    @PostMapping("/eventos")
    @Operation(summary = "Registra un evento inmutable de EDI")
    public ResponseEntity<RespuestaGenerica<RegistroEventoResponse>> registrar(
            @RequestHeader("X-Transaccion-Id") Long idTransaccion,
            @Valid @RequestBody RegistroEventoRequest request,
            HttpServletRequest servletRequest) {
        RegistroEventoResponse response = bitacoraService.registrar(
                idTransaccion, request, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(RespuestaGenerica.exitosa("Evento registrado correctamente", response));
    }

    @GetMapping("/transacciones/{idTransaccion}/eventos")
    @Operation(summary = "Consulta cronologicamente los eventos de una transaccion")
    public ResponseEntity<RespuestaGenerica<List<EventoConsultaResponse>>> consultarPorTransaccion(
            @PathVariable Long idTransaccion) {
        List<EventoConsultaResponse> response = bitacoraService.consultarPorTransaccion(idTransaccion);
        return ResponseEntity.ok(RespuestaGenerica.exitosa("Consulta exitosa", response));
    }

    @GetMapping("/eventos")
    @Operation(summary = "Consulta eventos mediante filtros autorizados")
    public ResponseEntity<RespuestaGenerica<List<EventoConsultaResponse>>> consultar(
            @ModelAttribute FiltroEventosRequest filtros) {
        List<EventoConsultaResponse> response = bitacoraService.consultar(filtros);
        return ResponseEntity.ok(RespuestaGenerica.exitosa("Consulta exitosa", response));
    }
}
