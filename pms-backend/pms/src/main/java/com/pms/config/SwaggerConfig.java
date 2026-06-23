package com.pms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Swagger UI page (available at /swagger-ui.html) and
 * adds the "Authorize" lock-icon button so a JWT token can be pasted
 * in once and reused for every endpoint call from the docs page —
 * without this, every single request in Swagger would need the
 * Authorization header re-typed manually.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Project Management System API")
                        .description("Task 3 — PIP — JWT auth, role-based access (Admin, Manager, Member), "
                                + "project & task management with MySQL stored procedures.")
                        .version("1.0")
                        .contact(new Contact().name("PIP Task 3")))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
