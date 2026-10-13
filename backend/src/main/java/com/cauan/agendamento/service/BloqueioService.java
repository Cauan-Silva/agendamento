package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.BloqueioDtos.BloqueioRequest;
import com.cauan.agendamento.dto.BloqueioDtos.BloqueioResponse;
import com.cauan.agendamento.exception.RecursoNaoEncontradoException;
import com.cauan.agendamento.exception.RegraNegocioException;
import com.cauan.agendamento.model.Bloqueio;
import com.cauan.agendamento.repository.BloqueioRepository;
import com.cauan.agendamento.repository.ProfissionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BloqueioService {

    private final BloqueioRepository repository;
    private final ProfissionalRepository profissionalRepository;
    private final Clock clock;

    public BloqueioService(BloqueioRepository repository,
                           ProfissionalRepository profissionalRepository,
                           Clock clock) {
        this.repository = repository;
        this.profissionalRepository = profissionalRepository;
        this.clock = clock;
    }

    /** Bloqueios atuais e futuros. Os que já terminaram não aparecem. */
    @Transactional(readOnly = true)
    public List<BloqueioResponse> listarVigentes() {
        return repository.findByFimAfterOrderByInicio(LocalDateTime.now(clock)).stream()
                .map(BloqueioResponse::de)
                .toList();
    }

    @Transactional
    public BloqueioResponse criar(BloqueioRequest req) {
        if (!req.fim().isAfter(req.inicio())) {
            throw new RegraNegocioException("O fim do bloqueio deve ser depois do início");
        }

        Bloqueio bloqueio = new Bloqueio();
        if (req.profissionalId() != null) {
            bloqueio.setProfissional(profissionalRepository.findById(req.profissionalId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", req.profissionalId())));
        }
        bloqueio.setInicio(req.inicio().withSecond(0).withNano(0));
        bloqueio.setFim(req.fim().withSecond(0).withNano(0));
        bloqueio.setMotivo(req.motivo() == null || req.motivo().isBlank() ? null : req.motivo().trim());

        return BloqueioResponse.de(repository.save(bloqueio));
    }

    @Transactional
    public void remover(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Bloqueio", id);
        }
        repository.deleteById(id);
    }
}
