package com.biolab.notification.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Custom health indicator — checks database connectivity.
 * Exposed at: /actuator/health
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource ds) { this.dataSource = ds; }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                return Health.up()
                    .withDetail("database", conn.getMetaData().getDatabaseProductName())
                    .withDetail("url", conn.getMetaData().getURL())
                    .build();
            }
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
        return Health.down().withDetail("reason", "Connection invalid").build();
    }
}
