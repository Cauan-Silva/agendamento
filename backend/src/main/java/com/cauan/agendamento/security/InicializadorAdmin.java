package com.cauan.agendamento.security;

import com.cauan.agendamento.model.Usuario;
import com.cauan.agendamento.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Na primeira vez que a API sobe (nenhum usuário no banco), cria o login do dono
 * com o e-mail e a senha de agenda.admin.* — em produção, defina ADMIN_EMAIL e ADMIN_SENHA.
 */
@Component
public class InicializadorAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorAdmin.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String nome;
    private final String email;
    private final String senha;

    public InicializadorAdmin(UsuarioRepository repository,
                              PasswordEncoder passwordEncoder,
                              @Value("${agenda.admin.nome}") String nome,
                              @Value("${agenda.admin.email}") String email,
                              @Value("${agenda.admin.senha}") String senha) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        repository.save(new Usuario(nome, email.trim().toLowerCase(), passwordEncoder.encode(senha)));
        log.info("Usuário do painel criado: {}", email);
        if ("admin123".equals(senha)) {
            log.warn("Usando a senha padrão do painel. Defina ADMIN_SENHA antes de publicar a aplicação.");
        }
    }
}
