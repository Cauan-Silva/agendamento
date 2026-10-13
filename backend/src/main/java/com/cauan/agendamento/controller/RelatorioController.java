package com.cauan.agendamento.controller;

import com.cauan.agendamento.dto.RelatorioDtos.RelatorioMensal;
import com.cauan.agendamento.service.RelatorioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/** Relatórios para o dono (requer login). */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    /** Ex.: GET /api/relatorios/mensal?ano=2026&mes=10 */
    @GetMapping("/mensal")
    public RelatorioMensal mensal(@RequestParam int ano, @RequestParam int mes) {
        return service.mensal(YearMonth.of(ano, mes));
    }
}
