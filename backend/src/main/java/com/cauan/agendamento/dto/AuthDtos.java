package com.cauan.agendamento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import com.cauan.agendamento.model.Perfil;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {
    }

    /** O front-end usa perfil e profissionalId para mostrar só as telas que a pessoa pode usar. */
    public record LoginResponse(String token,
                                Long usuarioId,
                                String nome,
                                Instant expiraEm,
                                Perfil perfil,
                                Long profissionalId,
                                String profissionalNome) {
    }
}
