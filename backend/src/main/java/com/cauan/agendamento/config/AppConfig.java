package com.cauan.agendamento.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${agenda.fuso-horario}")
    private String fusoHorario;

    @Value("${agenda.cors-origens}")
    private String[] corsOrigens;

    /**
     * Relógio injetável: em produção usa a hora real; nos testes trocamos por
     * um relógio fixo para que "agora" seja sempre o mesmo instante.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(fusoHorario));
    }

    /** Libera o front-end (Vite roda em http://localhost:5173) a chamar a API. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsOrigens)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }
}
