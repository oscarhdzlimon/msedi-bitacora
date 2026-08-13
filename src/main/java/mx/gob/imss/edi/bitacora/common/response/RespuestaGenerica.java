package mx.gob.imss.edi.bitacora.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RespuestaGenerica<T>(boolean exito, String mensaje, T respuesta) {

    public static <T> RespuestaGenerica<T> exitosa(String mensaje, T respuesta) {
        return new RespuestaGenerica<>(true, mensaje, respuesta);
    }

    public static <T> RespuestaGenerica<T> fallida(String mensaje, T respuesta) {
        return new RespuestaGenerica<>(false, mensaje, respuesta);
    }
}
