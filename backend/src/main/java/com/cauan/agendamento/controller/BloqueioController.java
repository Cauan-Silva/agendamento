package com.cauan.agendamento.controller;

import com.cauan.agendamento.dto.BloqueioDtos.BloqueioRequest;
import com.cauan.agendamento.dto.BloqueioDtos.BloqueioResponse;
import com.cauan.agendamento.service.BloqueioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Folgas e feriados (requer login). */
@RestController
@RequestMapping("/api/bloqueios")
public class BloqueioController {

    private final BloqueioService service;

    public BloqueioController(BloqueioService service) {
        this.service = service;
    }

    @GetMapping
    public List<BloqueioResponse> listar() {
        return service.listarVigentes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioResponse criar(@Valid @RequestBody BloqueioRequest req) {
        return service.criar(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
