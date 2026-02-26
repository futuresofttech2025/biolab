package com.biolab.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test to verify the Discovery Server application context loads
 * successfully with all beans initialized.
 *
 * @author BioLab Engineering Team
 */
@SpringBootTest
@ActiveProfiles("test")
class DiscoveryServerApplicationTest {

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        // Verifies all beans are wired correctly and the Eureka server starts
    }
}
