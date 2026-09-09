package com.foalrider.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThatCode;

/** Loads configuration only: no component scan, auto-configuration, database or network. */
class ConfigurationAuditProbe {
    @Test void testProfileMustLoadWithoutInvalidConfigData() {
        SpringApplication app = new SpringApplication(EmptyConfig.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setLogStartupInfo(false);
        assertThatCode(() -> {
            try (var ignored = app.run("--spring.profiles.active=test", "--spring.main.banner-mode=off")) {
                // This verifies config loading only, not full application startup.
            }
        }).as("TEST-001: test profile must be loadable").doesNotThrowAnyException();
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfig {}
}
