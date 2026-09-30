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
    public static final String TRANSACTION_HEADER_NAME = "X-Transaccion-Id";
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
        String idTransaccion = request.getHeader(TRANSACTION_HEADER_NAME);
        String ipOrigen = ipOrigen(request);
        String userAgent = request.getHeader("User-Agent");
        LOGGER.info("Inicia solicitud httpMethod={} path={} query={} correlacionId={} idTransaccion={} ipOrigen={} userAgent={}",
                request.getMethod(), request.getRequestURI(), valor(request.getQueryString()),
                correlationId, valor(idTransaccion), ipOrigen, valor(userAgent));
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            LOGGER.error("Excepcion en solicitud httpMethod={} path={} correlacionId={} idTransaccion={} ipOrigen={} statusParcial={}",
                    request.getMethod(), request.getRequestURI(), correlationId,
                    valor(idTransaccion), ipOrigen, response.getStatus(), exception);
            throw exception;
        } finally {
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000L;
            if (response.getStatus() >= 500) {
                LOGGER.error("Finaliza solicitud con error httpMethod={} path={} status={} duracionMs={} correlacionId={} idTransaccion={} ipOrigen={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(),
                        duracionMs, correlationId, valor(idTransaccion), ipOrigen);
            } else if (response.getStatus() >= 400) {
                LOGGER.warn("Finaliza solicitud rechazada httpMethod={} path={} status={} duracionMs={} correlacionId={} idTransaccion={} ipOrigen={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(),
                        duracionMs, correlationId, valor(idTransaccion), ipOrigen);
            } else {
                LOGGER.info("Finaliza solicitud httpMethod={} path={} status={} duracionMs={} correlacionId={} idTransaccion={} ipOrigen={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(),
                        duracionMs, correlationId, valor(idTransaccion), ipOrigen);
            }
            MDC.remove(MDC_KEY);
        }
    }

    private String ipOrigen(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].strip();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp.strip() : request.getRemoteAddr();
    }

    private String valor(String value) {
        return StringUtils.hasText(value) ? value.strip() : "-";
    }
}
