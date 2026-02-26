package com.biolab.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring application context loads successfully.
 *
 * @author BioLab Engineering Team
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServiceApplicationTest {

    @Test
    void contextLoads() {
        // Verifies that all beans wire correctly
    }
}
