package com.cauan.agendamento.controller;

import com.cauan.agendamento.dto.UsuarioDtos.RedefinirSenhaRequest;
import com.cauan.agendamento.dto.UsuarioDtos.UsuarioAtualizacaoRequest;
import com.cauan.agendamento.dto.UsuarioDtos.UsuarioRequest;
import com.cauan.agendamento.dto.UsuarioDtos.UsuarioResponse;
import com.cauan.agendamento.security.UsuarioAutenticado;
import com.cauan.agendamento.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Logins da equipe. Só administrador (veja SegurancaConfig). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioAtualizacaoRequest req,
                                     Authentication autenticacao) {
        return service.atualizar(id, req, UsuarioAutenticado.de(autenticacao).usuarioId());
    }

    @PatchMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@PathVariable Long id, @Valid @RequestBody RedefinirSenhaRequest req) {
        service.redefinirSenha(id, req.novaSenha());
    }
}
