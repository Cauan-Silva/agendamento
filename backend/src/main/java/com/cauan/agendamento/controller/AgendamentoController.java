package com.cauan.agendamento.controller;

import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoRequest;
import com.cauan.agendamento.dto.AgendamentoDtos.AgendamentoResponse;
import com.cauan.agendamento.dto.AgendamentoDtos.DisponibilidadeResponse;
import com.cauan.agendamento.security.UsuarioAutenticado;
import com.cauan.agendamento.service.AgendamentoService;
import com.cauan.agendamento.service.DisponibilidadeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final DisponibilidadeService disponibilidadeService;

    public AgendamentoController(AgendamentoService agendamentoService,
                                 DisponibilidadeService disponibilidadeService) {
        this.agendamentoService = agendamentoService;
        this.disponibilidadeService = disponibilidadeService;
    }

    /** Ex.: GET /api/disponibilidade?profissionalId=1&servicoId=2&data=2026-10-20 */
    @GetMapping("/disponibilidade")
    public DisponibilidadeResponse disponibilidade(
            @RequestParam Long profissionalId,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return disponibilidadeService.consultar(profissionalId, servicoId, data);
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponse criar(@Valid @RequestBody AgendamentoRequest req) {
        return agendamentoService.criar(req);
    }

    /**
     * Agenda do dia (requer login). Ex.: GET /api/agendamentos?data=2026-10-20
     * Use profissionalId para filtrar por uma pessoa da equipe.
     * Login de profissional: sempre só a própria agenda, qualquer que seja o filtro.
     */
    @GetMapping("/agendamentos")
    public List<AgendamentoResponse> listar(
            @RequestParam(required = false) Long profissionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Authentication autenticacao) {
        Long somenteDele = UsuarioAutenticado.de(autenticacao).somenteDoProfissional();
        return agendamentoService.listarDoDia(somenteDele != null ? somenteDele : profissionalId, data);
    }

    @GetMapping("/agendamentos/{id}")
    public AgendamentoResponse buscar(@PathVariable Long id, Authentication autenticacao) {
        return agendamentoService.buscar(id, UsuarioAutenticado.de(autenticacao).somenteDoProfissional());
    }

    @PatchMapping("/agendamentos/{id}/cancelar")
    public AgendamentoResponse cancelar(@PathVariable Long id, Authentication autenticacao) {
        return agendamentoService.cancelar(id, UsuarioAutenticado.de(autenticacao).somenteDoProfissional());
    }

    @PatchMapping("/agendamentos/{id}/concluir")
    public AgendamentoResponse concluir(@PathVariable Long id, Authentication autenticacao) {
        return agendamentoService.concluir(id, UsuarioAutenticado.de(autenticacao).somenteDoProfissional());
    }
}
