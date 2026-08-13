package mx.gob.imss.edi.bitacora.config;

import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeZoneConfig {

    private final ZoneId zoneId;

    public TimeZoneConfig(@Value("${edi.time-zone:America/Mexico_City}") String timeZone) {
        this.zoneId = ZoneId.of(timeZone);
    }

    @PostConstruct
    void configureDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }

    @Bean
    Clock ediClock() {
        return Clock.system(zoneId);
    }
}
