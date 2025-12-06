package com.foalrider.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Bean
    public OpenAPI foalRiderOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("FoalRider API")
                        .description("RESTful API for FoalRider E-Commerce Platform - A complete clothing e-commerce backend")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FoalRider Team")
                                .email("support@foalrider.com")
                                .url("https://foalrider.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080" + contextPath)
                                .description("Development Server"),
                        new Server()
                                .url("https://api.foalrider.com" + contextPath)
                                .description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token. Example: eyJhbGciOiJIUzUxMiJ9...")));
    }
}
