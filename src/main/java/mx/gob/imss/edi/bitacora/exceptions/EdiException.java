package mx.gob.imss.edi.bitacora.exceptions;

import org.springframework.http.HttpStatus;

public class EdiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public EdiException(HttpStatus status, String codigo, String mensaje) {
        this(status, codigo, mensaje, null);
    }

    public EdiException(HttpStatus status, String codigo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
