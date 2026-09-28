package mx.gob.imss.edi.bitacora.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlacion-Id";
    public static final String REQUEST_ATTRIBUTE = "edi.correlationId";
    private static final String MDC_KEY = "correlacionId";
    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        MDC.put(MDC_KEY, correlationId);

        long inicio = System.nanoTime();
        LOGGER.info("Inicia solicitud {} {} correlacionId={}", request.getMethod(),
                request.getRequestURI(), correlationId);
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            LOGGER.error("Excepcion en solicitud {} {} correlacionId={}", request.getMethod(),
                    request.getRequestURI(), correlationId, exception);
            throw exception;
        } finally {
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000L;
            if (response.getStatus() >= 400) {
                LOGGER.warn("Solicitud con error {} {} status={} duracionMs={} correlacionId={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(),
                        duracionMs, correlationId);
            }
            MDC.remove(MDC_KEY);
        }
    }
}
