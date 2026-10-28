package mx.gob.imss.edi.bitacora.common.enums;

import java.util.Arrays;
import java.util.Optional;

/** Códigos estables y respaldos locales; los textos de catálogo se resuelven desde base. */
public enum MensajeEdi {
    AUTORIZACION_TRAZA_NO_PERMITIDA("AUTORIZACION_TRAZA_NO_PERMITIDA", "La autorizacion no corresponde a la incapacidad o al alcance del medico"),
    BITACORA_NO_DISPONIBLE("BITACORA_NO_DISPONIBLE", "La bitacora no se encuentra disponible"),
    DETALLE_INVALIDO("DETALLE_INVALIDO", "El detalle del evento no se puede serializar"),
    ERROR_INTERNO("ERROR_INTERNO", "Ocurrio un error interno al procesar la solicitud"),
    EVENTO_NO_REGISTRADO("EVENTO_NO_REGISTRADO", "No fue posible confirmar el registro del evento"),
    EVENTO_NO_SOPORTADO("EVENTO_NO_SOPORTADO", "La clave de evento no existe o no se encuentra activa"),
    FILTRO_FUNCIONAL_NO_DISPONIBLE("FILTRO_FUNCIONAL_NO_DISPONIBLE", "El filtro NSS estara disponible cuando exista la incapacidad persistida"),
    MENSAJE_NO_SOPORTADO("MENSAJE_NO_SOPORTADO", "La clave de mensaje no existe o no se encuentra activa"),
    OPERACION_NO_SOPORTADA("OPERACION_NO_SOPORTADA", "La operacion de bitacora no esta soportada"),
    PERFIL_NO_AUTORIZADO("PERFIL_NO_AUTORIZADO", "Se requiere publicacion interna de Expedicion con identidad del autorizador"),
    RANGO_FECHAS_INVALIDO("RANGO_FECHAS_INVALIDO", "La fecha inicial no puede ser posterior a la fecha final"),
    SISTEMA_ORIGEN_NO_CORRESPONDE("SISTEMA_ORIGEN_NO_CORRESPONDE", "El sistema origen no corresponde con la transaccion"),
    SOLICITUD_INVALIDA("SOLICITUD_INVALIDA", "La solicitud contiene datos invalidos"),
    TOKEN_INVALIDO_TECNICO("TOKEN_INVALIDO", "El Bearer Token es requerido, invalido o ha expirado"),
    TRANSACCION_INVALIDA("TRANSACCION_INVALIDA", "El header X-Transaccion-Id debe ser numerico"),
    TRANSACCION_NO_CORRESPONDE("TRANSACCION_NO_CORRESPONDE", "La transaccion no corresponde con el Bearer Token"),
    TRANSACCION_NO_ENCONTRADA("TRANSACCION_NO_ENCONTRADA", "La transaccion indicada no existe"),
    TRANSACCION_NO_ENCONTRADA_DETALLE_2("TRANSACCION_NO_ENCONTRADA", "La transaccion original no existe"),
    TRANSACCION_REQUERIDA("TRANSACCION_REQUERIDA", "El header X-Transaccion-Id es obligatorio");

    private final String codigo;
    private final String textoRespaldo;

    MensajeEdi(String codigo, String textoRespaldo) {
        this.codigo = codigo;
        this.textoRespaldo = textoRespaldo;
    }

    public String codigo() { return codigo; }
    public String textoRespaldo() { return textoRespaldo; }

    /** Retorna la definición principal; conserva los códigos desconocidos en los contratos dinámicos. */
    public static Optional<MensajeEdi> buscar(String codigo) {
        return Arrays.stream(values()).filter(mensaje -> mensaje.codigo.equals(codigo)).findFirst();
    }
}
