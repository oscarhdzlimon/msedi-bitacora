package mx.gob.imss.edi.bitacora.security;

import mx.gob.imss.edi.bitacora.common.enums.MensajeEdi;

import mx.gob.imss.edi.bitacora.exceptions.EdiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityContextService {

    public JwtPrincipal principalActual() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new EdiException(HttpStatus.UNAUTHORIZED, MensajeEdi.TOKEN_INVALIDO_TECNICO.codigo(),
                    MensajeEdi.TOKEN_INVALIDO_TECNICO.textoRespaldo());
        }
        return principal;
    }

    public JwtPrincipal validarTransaccion(Long idTransaccion) {
        JwtPrincipal principal = principalActual();
        if (idTransaccion == null || !idTransaccion.equals(principal.idTransaccion())) {
            throw new EdiException(HttpStatus.FORBIDDEN, MensajeEdi.TRANSACCION_NO_CORRESPONDE.codigo(),
                    MensajeEdi.TRANSACCION_NO_CORRESPONDE.textoRespaldo());
        }
        return principal;
    }
}
