package com.cauan.agendamento.dto;

import com.cauan.agendamento.model.Profissional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public final class ProfissionalDtos {

    private ProfissionalDtos() {
    }

    /**
     * Exemplo de JSON:
     * { "nome": "Ana", "horaInicio": "09:00", "horaFim": "18:00",
     *   "almocoInicio": "12:00", "almocoFim": "13:00",
     *   "diasTrabalho": ["MONDAY", "TUESDAY"], "servicoIds": [1, 2] }
     * O almoço é opcional: envie os dois campos ou nenhum.
     */
    public record ProfissionalRequest(
            @NotBlank @Size(max = 100) String nome,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFim,
            LocalTime almocoInicio,
            LocalTime almocoFim,
            @NotEmpty Set<DayOfWeek> diasTrabalho,
            @NotEmpty Set<Long> servicoIds) {
    }

    public record ProfissionalResponse(Long id,
                                       String nome,
                                       LocalTime horaInicio,
                                       LocalTime horaFim,
                                       LocalTime almocoInicio,
                                       LocalTime almocoFim,
                                       List<DayOfWeek> diasTrabalho,
                                       List<ServicoDtos.ServicoResponse> servicos,
                                       boolean ativo) {

        public static ProfissionalResponse de(Profissional p) {
            return new ProfissionalResponse(
                    p.getId(),
                    p.getNome(),
                    p.getHoraInicio(),
                    p.getHoraFim(),
                    p.getAlmocoInicio(),
                    p.getAlmocoFim(),
                    p.getDiasTrabalho().stream().sorted().toList(),
                    p.getServicos().stream()
                            .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                            .map(ServicoDtos.ServicoResponse::de)
                            .toList(),
                    p.isAtivo());
        }
    }
}
