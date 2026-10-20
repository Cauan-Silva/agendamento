package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.UsuarioDtos.UsuarioAtualizacaoRequest;
import com.cauan.agendamento.dto.UsuarioDtos.UsuarioRequest;
import com.cauan.agendamento.dto.UsuarioDtos.UsuarioResponse;
import com.cauan.agendamento.exception.RegraNegocioException;
import com.cauan.agendamento.model.Perfil;
import com.cauan.agendamento.model.Profissional;
import com.cauan.agendamento.model.Usuario;
import com.cauan.agendamento.repository.ProfissionalRepository;
import com.cauan.agendamento.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;
    @Mock
    private ProfissionalRepository profissionalRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService service;
    private Profissional ana;
    private Usuario dono;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, profissionalRepository, passwordEncoder);
        ana = new Profissional();
        ana.setId(1L);
        ana.setNome("Ana");
        dono = new Usuario("Dono", "dono@barbearia.com", "hash");
        dono.setId(10L);
    }

    @Test
    void criaLoginDeProfissionalLigadoAAgenda() {
        when(repository.existsByEmailIgnoreCase("ana@barbearia.com")).thenReturn(false);
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(repository.existsByProfissionalId(1L)).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-novo");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse criado = service.criar(
                new UsuarioRequest("Ana", " Ana@Barbearia.com ", "senha123", Perfil.PROFISSIONAL, 1L));

        assertThat(criado.email()).isEqualTo("ana@barbearia.com");
        assertThat(criado.perfil()).isEqualTo(Perfil.PROFISSIONAL);
        assertThat(criado.profissionalNome()).isEqualTo("Ana");
    }

    @Test
    void loginDeProfissionalPrecisaDeUmProfissional() {
        when(repository.existsByEmailIgnoreCase("ana@barbearia.com")).thenReturn(false);

        assertThatThrownBy(() -> service.criar(
                new UsuarioRequest("Ana", "ana@barbearia.com", "senha123", Perfil.PROFISSIONAL, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Escolha qual profissional");
        verify(repository, never()).save(any());
    }

    @Test
    void profissionalTemNoMaximoUmLogin() {
        when(repository.existsByEmailIgnoreCase("ana2@barbearia.com")).thenReturn(false);
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(repository.existsByProfissionalId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(
                new UsuarioRequest("Ana", "ana2@barbearia.com", "senha123", Perfil.PROFISSIONAL, 1L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já tem um login");
    }

    @Test
    void emailRepetidoNaoCriaLogin() {
        when(repository.existsByEmailIgnoreCase("dono@barbearia.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(
                new UsuarioRequest("Outro", "dono@barbearia.com", "senha123", Perfil.ADMIN, null)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void ninguemTiraOProprioAcessoDeAdministrador() {
        when(repository.buscarComProfissional(10L)).thenReturn(Optional.of(dono));
        when(repository.existsByEmailIgnoreCaseAndIdNot("dono@barbearia.com", 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.atualizar(10L,
                new UsuarioAtualizacaoRequest("Dono", "dono@barbearia.com", Perfil.ADMIN, null, false), 10L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("próprio acesso");
        assertThat(dono.isAtivo()).isTrue();
    }

    @Test
    void sempreSobraUmAdministradorAtivo() {
        when(repository.buscarComProfissional(10L)).thenReturn(Optional.of(dono));
        when(repository.existsByEmailIgnoreCaseAndIdNot("dono@barbearia.com", 10L)).thenReturn(false);
        when(repository.countByPerfilAndAtivoTrue(Perfil.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.atualizar(10L,
                new UsuarioAtualizacaoRequest("Dono", "dono@barbearia.com", Perfil.PROFISSIONAL, 1L, true), 99L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("pelo menos um administrador");
        assertThat(dono.getPerfil()).isEqualTo(Perfil.ADMIN);
    }

    @Test
    void trocaDeSenhaConfereASenhaAtual() {
        when(repository.buscarComProfissional(10L)).thenReturn(Optional.of(dono));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.trocarPropriaSenha(10L, "errada", "nova-senha"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("senha atual");
        assertThat(dono.getSenhaHash()).isEqualTo("hash");
    }
}
