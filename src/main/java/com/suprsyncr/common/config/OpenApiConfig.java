package com.suprsyncr.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration for the USP Backend.
 * Configures API documentation with JWT security scheme for Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Configures OpenAPI documentation with API info and JWT security scheme.
     * 
     * API Documentation:
     * - Title: Unified Seller Platform API
     * - Description: Backend API for managing products, inventory, and orders across multiple e-commerce marketplaces
     * - Version: 1.0.0
     * 
     * Security:
     * - JWT Bearer token authentication
     * - Tokens must be included in the Authorization header as "Bearer {token}"
     * 
     * @return Configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Unified Seller Platform API")
                        .description("Backend API for managing products, inventory, and orders across multiple e-commerce marketplaces (Shopify, Blinkit, WooCommerce)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("USP Support")
                                .email("support@usp.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from /api/v1/auth/login or /api/v1/auth/register")));
    }
}

