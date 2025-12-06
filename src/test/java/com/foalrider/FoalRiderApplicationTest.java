package com.foalrider;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Main application context test.
 * Verifies that the Spring application context loads successfully.
 * 
 * NOTE: This is an integration test that requires external services (Redis, DB).
 * It is disabled by default for unit test runs. Enable when running integration tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Integration test - requires external services (Redis). Enable for integration test runs.")
class FoalRiderApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the application context loads successfully
    }
}
