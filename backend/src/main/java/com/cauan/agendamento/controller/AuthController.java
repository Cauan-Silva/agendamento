package com.cauan.agendamento.controller;

import com.cauan.agendamento.dto.AuthDtos.LoginRequest;
import com.cauan.agendamento.dto.AuthDtos.LoginResponse;
import com.cauan.agendamento.dto.UsuarioDtos.TrocarSenhaRequest;
import com.cauan.agendamento.dto.UsuarioDtos.UsuarioResponse;
import com.cauan.agendamento.security.AuthService;
import com.cauan.agendamento.security.UsuarioAutenticado;
import com.cauan.agendamento.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    /** Ex.: POST /api/auth/login  { "email": "admin@agendamento.local", "senha": "admin123" } */
    @PostMapping("/login")
    public LoginResponse entrar(@Valid @RequestBody LoginRequest req) {
        return authService.entrar(req);
    }

    /** Dados de quem está logado. */
    @GetMapping("/eu")
    public UsuarioResponse eu(Authentication autenticacao) {
        return usuarioService.buscar(UsuarioAutenticado.de(autenticacao).usuarioId());
    }

    /** Ex.: PATCH /api/auth/senha  { "senhaAtual": "admin123", "novaSenha": "outra-senha" } */
    @PatchMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@Valid @RequestBody TrocarSenhaRequest req, Authentication autenticacao) {
        usuarioService.trocarPropriaSenha(UsuarioAutenticado.de(autenticacao).usuarioId(),
                req.senhaAtual(), req.novaSenha());
    }
}
