package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.UsuarioDtos.UsuarioAtualizacaoRequest;
import com.cauan.agendamento.dto.UsuarioDtos.UsuarioRequest;
import com.cauan.agendamento.dto.UsuarioDtos.UsuarioResponse;
import com.cauan.agendamento.exception.RecursoNaoEncontradoException;
import com.cauan.agendamento.exception.RegraNegocioException;
import com.cauan.agendamento.model.Perfil;
import com.cauan.agendamento.model.Profissional;
import com.cauan.agendamento.model.Usuario;
import com.cauan.agendamento.repository.ProfissionalRepository;
import com.cauan.agendamento.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logins do painel. Regras:
 * - e-mail único;
 * - perfil PROFISSIONAL precisa estar ligado a um profissional, e cada profissional tem no máximo um login;
 * - sempre sobra pelo menos um administrador ativo, e ninguém tira o próprio acesso de administrador.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final ProfissionalRepository profissionalRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository,
                          ProfissionalRepository profissionalRepository,
                          PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.profissionalRepository = profissionalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repository.listarTodos().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.de(obter(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest req) {
        String email = normalizar(req.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe um login com o e-mail " + email);
        }
        Profissional profissional = profissionalDoLogin(req.perfil(), req.profissionalId(), null);

        Usuario usuario = new Usuario(req.nome().trim(), email, passwordEncoder.encode(req.senha()));
        usuario.setPerfil(req.perfil());
        usuario.setProfissional(profissional);
        return UsuarioResponse.de(repository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest req, Long quemAlteraId) {
        Usuario usuario = obter(id);
        String email = normalizar(req.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new RegraNegocioException("Já existe um login com o e-mail " + email);
        }

        boolean deixaDeSerAdminAtivo = usuario.isAdmin() && usuario.isAtivo()
                && (req.perfil() != Perfil.ADMIN || !req.ativo());
        if (deixaDeSerAdminAtivo) {
            if (id.equals(quemAlteraId)) {
                throw new RegraNegocioException("Você não pode tirar o seu próprio acesso de administrador");
            }
            if (repository.countByPerfilAndAtivoTrue(Perfil.ADMIN) <= 1) {
                throw new RegraNegocioException("É preciso manter pelo menos um administrador ativo");
            }
        }

        usuario.setNome(req.nome().trim());
        usuario.setEmail(email);
        usuario.setPerfil(req.perfil());
        usuario.setProfissional(profissionalDoLogin(req.perfil(), req.profissionalId(), id));
        usuario.setAtivo(req.ativo());
        return UsuarioResponse.de(usuario);
    }

    /** O administrador define uma senha nova (ex.: alguém esqueceu a sua). */
    @Transactional
    public void redefinirSenha(Long id, String novaSenha) {
        obter(id).setSenhaHash(passwordEncoder.encode(novaSenha));
    }

    /** A própria pessoa troca a senha, confirmando a atual. */
    @Transactional
    public void trocarPropriaSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        Usuario usuario = obter(usuarioId);
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual não confere");
        }
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
    }

    /**
     * Profissional da agenda ligado ao login: obrigatório para PROFISSIONAL, opcional para ADMIN
     * (dono que também atende). {@code usuarioId} é o login sendo editado, ou null ao criar.
     */
    private Profissional profissionalDoLogin(Perfil perfil, Long profissionalId, Long usuarioId) {
        if (profissionalId == null) {
            if (perfil == Perfil.PROFISSIONAL) {
                throw new RegraNegocioException("Escolha qual profissional da agenda usa este login");
            }
            return null;
        }
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", profissionalId));
        boolean jaTemLogin = usuarioId == null
                ? repository.existsByProfissionalId(profissionalId)
                : repository.existsByProfissionalIdAndIdNot(profissionalId, usuarioId);
        if (jaTemLogin) {
            throw new RegraNegocioException(profissional.getNome() + " já tem um login");
        }
        return profissional;
    }

    private Usuario obter(Long id) {
        return repository.buscarComProfissional(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase();
    }
}
