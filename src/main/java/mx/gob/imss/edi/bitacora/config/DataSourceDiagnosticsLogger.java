package mx.gob.imss.edi.bitacora.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DataSourceDiagnosticsLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceDiagnosticsLogger.class);
    private final DataSource dataSource;

    public DataSourceDiagnosticsLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDataSourceConfiguration() {
        if (dataSource instanceof HikariDataSource hikari) {
            LOGGER.info("Configuracion pool BD poolName={} jdbcUrl={} maximumPoolSize={} minimumIdle={} "
                            + "connectionTimeoutMs={} validationTimeoutMs={} idleTimeoutMs={} "
                            + "maxLifetimeMs={} leakDetectionThresholdMs={}",
                    hikari.getPoolName(), sanitizarJdbcUrl(hikari.getJdbcUrl()),
                    hikari.getMaximumPoolSize(), hikari.getMinimumIdle(),
                    hikari.getConnectionTimeout(), hikari.getValidationTimeout(),
                    hikari.getIdleTimeout(), hikari.getMaxLifetime(),
                    hikari.getLeakDetectionThreshold());
            return;
        }
        LOGGER.info("Configuracion datasource type={}", dataSource.getClass().getName());
    }

    private String sanitizarJdbcUrl(String jdbcUrl) {
        if (!StringUtils.hasText(jdbcUrl)) {
            return "-";
        }
        return jdbcUrl.replaceAll("(?i)(password=)[^&;]+", "$1****")
                .replaceAll("(?i)(user=)[^&;]+", "$1****");
    }
}
