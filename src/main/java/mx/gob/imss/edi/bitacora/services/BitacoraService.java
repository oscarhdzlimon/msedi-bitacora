package mx.gob.imss.edi.bitacora.services;

import java.util.List;
import mx.gob.imss.edi.bitacora.models.request.FiltroEventosRequest;
import mx.gob.imss.edi.bitacora.models.request.RegistroEventoRequest;
import mx.gob.imss.edi.bitacora.models.response.EventoConsultaResponse;
import mx.gob.imss.edi.bitacora.models.response.RegistroEventoResponse;

public interface BitacoraService {

    RegistroEventoResponse registrar(Long idTransaccion, RegistroEventoRequest request, String terminal);

    List<EventoConsultaResponse> consultarPorTransaccion(Long idTransaccion);

    List<EventoConsultaResponse> consultar(FiltroEventosRequest filtros);
}
