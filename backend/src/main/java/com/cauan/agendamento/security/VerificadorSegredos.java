package com.cauan.agendamento.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Impede a API de subir em produção com os segredos de exemplo do application.yml.
 *
 * Os valores padrão (JWT_SEGREDO e ADMIN_SENHA) só são aceitos com o perfil "dev" ou "test"
 * ativo — o perfil "docker" (banco local do docker-compose) inclui o "dev".
 */
@Component
public class VerificadorSegredos {

    static final String SEGREDO_JWT_PADRAO = "segredo-de-desenvolvimento-troque-em-producao-123";
    static final String SENHA_ADMIN_PADRAO = "admin123";
    static final int TAMANHO_MINIMO_SEGREDO = 32;

    private static final Logger log = LoggerFactory.getLogger(VerificadorSegredos.class);

    public VerificadorSegredos(Environment ambiente,
                               @Value("${agenda.jwt.segredo}") String segredoJwt,
                               @Value("${agenda.admin.senha}") String senhaAdmin) {
        boolean desenvolvimento = ambiente.acceptsProfiles(Profiles.of("dev | test"));
        verificar(segredoJwt, senhaAdmin, desenvolvimento);
        if (desenvolvimento && (SEGREDO_JWT_PADRAO.equals(segredoJwt) || SENHA_ADMIN_PADRAO.equals(senhaAdmin))) {
            log.warn("Usando segredos de desenvolvimento (perfil dev/test). Nunca publique a API assim.");
        }
    }

    /**
     * @param desenvolvimento true quando o perfil "dev" ou "test" está ativo
     * @throws IllegalStateException se a configuração não é segura para subir
     */
    static void verificar(String segredoJwt, String senhaAdmin, boolean desenvolvimento) {
        if (segredoJwt == null || segredoJwt.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException("A variável JWT_SEGREDO (agenda.jwt.segredo) precisa ter pelo menos "
                    + TAMANHO_MINIMO_SEGREDO + " caracteres.");
        }
        if (desenvolvimento) {
            return;
        }
        if (SEGREDO_JWT_PADRAO.equals(segredoJwt)) {
            throw new IllegalStateException("A API não sobe com o segredo JWT de exemplo. Defina a variável "
                    + "JWT_SEGREDO com um texto aleatório de pelo menos " + TAMANHO_MINIMO_SEGREDO
                    + " caracteres (ou rode com o perfil \"dev\" no seu computador).");
        }
        if (senhaAdmin == null || SENHA_ADMIN_PADRAO.equals(senhaAdmin)) {
            throw new IllegalStateException("A API não sobe com a senha padrão do administrador (admin123). "
                    + "Defina a variável ADMIN_SENHA com uma senha forte (ou rode com o perfil \"dev\" no seu "
                    + "computador).");
        }
    }
}
