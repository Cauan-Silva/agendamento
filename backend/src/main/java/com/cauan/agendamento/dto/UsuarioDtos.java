package com.cauan.agendamento.dto;

import com.cauan.agendamento.model.Perfil;
import com.cauan.agendamento.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    /**
     * Novo login. Exemplo:
     * { "nome": "Ana", "email": "ana@barbearia.com", "senha": "troque123",
     *   "perfil": "PROFISSIONAL", "profissionalId": 1 }
     */
    public record UsuarioRequest(
            @NotBlank @Size(max = 100) String nome,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 6, max = 72, message = "a senha precisa ter de 6 a 72 caracteres") String senha,
            @NotNull Perfil perfil,
            Long profissionalId) {
    }

    /** Edição pelo administrador (a senha muda por outra rota). */
    public record UsuarioAtualizacaoRequest(
            @NotBlank @Size(max = 100) String nome,
            @NotBlank @Email @Size(max = 150) String email,
            @NotNull Perfil perfil,
            Long profissionalId,
            @NotNull Boolean ativo) {
    }

    /** Administrador define uma senha nova para alguém que esqueceu a sua. */
    public record RedefinirSenhaRequest(
            @NotBlank @Size(min = 6, max = 72, message = "a senha precisa ter de 6 a 72 caracteres") String novaSenha) {
    }

    /** A própria pessoa troca a senha, confirmando a atual. */
    public record TrocarSenhaRequest(
            @NotBlank String senhaAtual,
            @NotBlank @Size(min = 6, max = 72, message = "a senha precisa ter de 6 a 72 caracteres") String novaSenha) {
    }

    public record UsuarioResponse(Long id,
                                  String nome,
                                  String email,
                                  Perfil perfil,
                                  Long profissionalId,
                                  String profissionalNome,
                                  boolean ativo) {

        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(
                    u.getId(),
                    u.getNome(),
                    u.getEmail(),
                    u.getPerfil(),
                    u.getProfissional() == null ? null : u.getProfissional().getId(),
                    u.getProfissional() == null ? null : u.getProfissional().getNome(),
                    u.isAtivo());
        }
    }
}
