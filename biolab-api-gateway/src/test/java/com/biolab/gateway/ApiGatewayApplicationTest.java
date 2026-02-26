package com.biolab.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test verifying the API Gateway application context loads.
 *
 * @author BioLab Engineering Team
 */
@SpringBootTest
@ActiveProfiles("test")
class ApiGatewayApplicationTest {

    @Test
    @DisplayName("API Gateway context should load successfully")
    void contextLoads() {
        // Verifies all beans are wired, routes configured, and filters registered
    }
}
