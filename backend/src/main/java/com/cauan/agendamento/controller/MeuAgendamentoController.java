package com.cauan.agendamento.controller;

import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoClienteResponse;
import com.cauan.agendamento.dto.AgendamentoDtos.RemarcarRequest;
import com.cauan.agendamento.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas do cliente, sem login: o código secreto do link enviado por e-mail
 * identifica o agendamento.
 */
@RestController
@RequestMapping("/api/meu-agendamento/{codigo}")
public class MeuAgendamentoController {

    private final AgendamentoService service;

    public MeuAgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    @GetMapping
    public AgendamentoClienteResponse consultar(@PathVariable String codigo) {
        return service.buscarPeloCodigo(codigo);
    }

    @PatchMapping("/cancelar")
    public AgendamentoClienteResponse cancelar(@PathVariable String codigo) {
        return service.cancelarPeloCliente(codigo);
    }

    @PatchMapping("/remarcar")
    public AgendamentoClienteResponse remarcar(@PathVariable String codigo, @Valid @RequestBody RemarcarRequest req) {
        return service.remarcarPeloCliente(codigo, req.inicio());
    }
}
