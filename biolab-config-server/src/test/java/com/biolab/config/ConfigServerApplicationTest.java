package com.biolab.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test verifying the Config Server application context
 * loads successfully with native profile.
 *
 * @author BioLab Engineering Team
 */
@SpringBootTest
@ActiveProfiles({"test", "native"})
class ConfigServerApplicationTest {

    @Test
    @DisplayName("Config Server context should load successfully")
    void contextLoads() {
        // Verifies all beans and the config server backend initialize
    }
}
