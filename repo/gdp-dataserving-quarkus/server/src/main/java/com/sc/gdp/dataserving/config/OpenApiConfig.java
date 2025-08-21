package com.sc.gdp.dataserving.config;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

import jakarta.ws.rs.core.Application;

/**
 * OpenAPI configuration for the GDP Data Serving API
 * This class configures the OpenAPI documentation and enables authentication in Swagger UI
 */
@OpenAPIDefinition(
    info = @Info(
        title = "GDP Data Serving API",
        version = "1.0.0",
        description = "REST API for GDP Data Serving with S3 file management and OIDC authentication",
        contact = @Contact(
            name = "GDP Data Serving Team",
            email = "support@sc.com"
        ),
        license = @License(
            name = "MIT",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8082", description = "Development server"),
        @Server(url = "https://api.gdp-dataserving.com", description = "Production server")
    }
)
@SecurityScheme(
    securitySchemeName = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token authentication. Obtain a token from your OIDC provider (Keycloak) and include it in the Authorization header as 'Bearer <token>'"
)
@SecurityScheme(
    securitySchemeName = "oidc",
    type = SecuritySchemeType.OPENIDCONNECT,
    openIdConnectUrl = "http://localhost:8180/auth/realms/quarkus/.well-known/openid_configuration",
    description = "OpenID Connect authentication via Keycloak"
)
public class OpenApiConfig extends Application {
    // This class serves as the OpenAPI configuration
    // The actual JAX-RS application configuration is handled by Quarkus automatically
}