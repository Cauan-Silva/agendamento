package com.cauan.agendamento.controller;

import com.cauan.agendamento.dto.AuthDtos.LoginRequest;
import com.cauan.agendamento.dto.AuthDtos.LoginResponse;
import com.cauan.agendamento.security.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Ex.: POST /api/auth/login  { "email": "admin@agendamento.local", "senha": "admin123" } */
    @PostMapping("/login")
    public LoginResponse entrar(@Valid @RequestBody LoginRequest req) {
        return authService.entrar(req);
    }
}
