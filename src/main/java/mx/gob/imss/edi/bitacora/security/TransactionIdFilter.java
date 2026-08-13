package mx.gob.imss.edi.bitacora.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import mx.gob.imss.edi.bitacora.exceptions.ApiErrorResponse;
import mx.gob.imss.edi.bitacora.filters.CorrelationIdFilter;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TransactionIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Transaccion-Id";
    private final ObjectMapper objectMapper;

    public TransactionIdFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().contains("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Object authenticatedPrincipal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(authenticatedPrincipal instanceof JwtPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        String value = request.getHeader(HEADER_NAME);
        if (value == null || value.isBlank()) {
            writeError(request, response, 400, "TRANSACCION_REQUERIDA",
                    "El header X-Transaccion-Id es obligatorio");
            return;
        }
        try {
            if (!Long.valueOf(value).equals(principal.idTransaccion())) {
                writeError(request, response, 403, "TRANSACCION_NO_CORRESPONDE",
                        "La transaccion no corresponde con el Bearer Token");
                return;
            }
        } catch (NumberFormatException exception) {
            writeError(request, response, 400, "TRANSACCION_INVALIDA",
                    "El header X-Transaccion-Id debe ser numerico");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, int status,
            String codigo, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                OffsetDateTime.now(), status, status == 400 ? "Bad Request" : "Forbidden",
                codigo, mensaje, request.getRequestURI(),
                (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE), Map.of()));
    }
}
