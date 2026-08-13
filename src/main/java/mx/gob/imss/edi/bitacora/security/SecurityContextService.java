package mx.gob.imss.edi.bitacora.security;

import mx.gob.imss.edi.bitacora.exceptions.EdiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityContextService {

    public JwtPrincipal principalActual() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new EdiException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO",
                    "El Bearer Token es requerido, invalido o ha expirado");
        }
        return principal;
    }

    public JwtPrincipal validarTransaccion(Long idTransaccion) {
        JwtPrincipal principal = principalActual();
        if (idTransaccion == null || !idTransaccion.equals(principal.idTransaccion())) {
            throw new EdiException(HttpStatus.FORBIDDEN, "TRANSACCION_NO_CORRESPONDE",
                    "La transaccion no corresponde con el Bearer Token");
        }
        return principal;
    }
}
