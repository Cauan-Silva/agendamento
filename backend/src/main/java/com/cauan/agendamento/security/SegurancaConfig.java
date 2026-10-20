package com.cauan.agendamento.security;

import com.cauan.agendamento.repository.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Quem pode acessar o quê.
 *
 * Público (cliente agendando): ver serviços, profissionais e horários livres, e criar agendamento.
 * Público com código: o cliente consulta, cancela ou remarca o próprio horário pelo link do e-mail.
 * Profissional logado: vê e atualiza só a própria agenda, e troca a própria senha.
 * Administrador logado: tudo — agenda de todos, cadastros, folgas, relatório e acessos.
 *
 * O login devolve um token JWT; o front-end envia esse token no cabeçalho
 * "Authorization: Bearer ..." em cada chamada restrita.
 */
@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, UsuarioRepository usuarioRepository) throws Exception {
        // Criado aqui (e não como @Component) para o Spring MVC não registrá-lo como conversor de parâmetros.
        ConversorJwt conversorJwt = new ConversorJwt(usuarioRepository);
        http
                .csrf(AbstractHttpConfigurer::disable) // API sem cookies de sessão: CSRF não se aplica
                .cors(Customizer.withDefaults())       // usa as origens configuradas em AppConfig
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/servicos/**", "/api/profissionais/**",
                                "/api/disponibilidade").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/agendamentos").permitAll()
                        // Cliente pelo link do e-mail: o código secreto faz o papel do login
                        .requestMatchers(HttpMethod.GET, "/api/meu-agendamento/*").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/meu-agendamento/*/cancelar",
                                "/api/meu-agendamento/*/remarcar").permitAll()
                        // Qualquer pessoa logada: os próprios dados e a própria senha
                        .requestMatchers("/api/auth/eu", "/api/auth/senha").authenticated()
                        // Agenda: o profissional vê e atualiza só a própria (filtrado no controller)
                        .requestMatchers(HttpMethod.GET, "/api/agendamentos", "/api/agendamentos/*")
                                .hasAnyRole("ADMIN", "PROFISSIONAL")
                        .requestMatchers(HttpMethod.PATCH, "/api/agendamentos/*/cancelar",
                                "/api/agendamentos/*/concluir").hasAnyRole("ADMIN", "PROFISSIONAL")
                        // Cadastros, folgas, relatório e acessos: só administrador
                        .anyRequest().hasRole("ADMIN"))
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversorJwt)));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey chaveJwt(@Value("${agenda.jwt.segredo}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("agenda.jwt.segredo (JWT_SEGREDO) precisa ter pelo menos 32 caracteres");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
