package com.cauan.agendamento.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Adiciona o botão "Authorize" no Swagger: faça login em /api/auth/login,
 * copie o token e cole lá para testar as rotas restritas.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "API de agendamento", version = "0.2.0",
                description = "Agendamento para pequenos negócios"),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
