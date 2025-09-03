package com.quickcoders.infolabsproducts.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Distributed Inventory API (Marketplace Brazil)")
                        .version("0.1.1")
                        .description("OpenAPI contract for a distributed inventory system with strong-consistency writes, " +
                                "reservation with TTL, CQRS (write canonical + read projections/cache), and idempotent " +
                                "mutations. Suitable for a marketplace with multiple stores/sellers across Brazil.")
                        .contact(new Contact()
                                .name("QuickCoders")
                                .email("info@quickcoders.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("https://api.example.com")
                                .description("Production"),
                        new Server()
                                .url("https://staging.api.example.com")
                                .description("Staging"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Authorization header using the Bearer scheme. Example: \"Bearer {token}\"")));
    }
}
