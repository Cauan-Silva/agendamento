package com.cauan.agendamento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {
    }

    public record LoginResponse(String token, String nome, Instant expiraEm) {
    }
}
