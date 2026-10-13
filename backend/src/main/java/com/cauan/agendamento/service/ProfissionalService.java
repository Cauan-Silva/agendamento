package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.ProfissionalDtos.ProfissionalRequest;
import com.cauan.agendamento.dto.ProfissionalDtos.ProfissionalResponse;
import com.cauan.agendamento.exception.RecursoNaoEncontradoException;
import com.cauan.agendamento.exception.RegraNegocioException;
import com.cauan.agendamento.model.Profissional;
import com.cauan.agendamento.model.Servico;
import com.cauan.agendamento.repository.ProfissionalRepository;
import com.cauan.agendamento.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;

@Service
public class ProfissionalService {

    private final ProfissionalRepository repository;
    private final ServicoRepository servicoRepository;

    public ProfissionalService(ProfissionalRepository repository, ServicoRepository servicoRepository) {
        this.repository = repository;
        this.servicoRepository = servicoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfissionalResponse> listarAtivos() {
        return repository.findByAtivoTrueOrderByNome().stream().map(ProfissionalResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProfissionalResponse buscar(Long id) {
        return ProfissionalResponse.de(obter(id));
    }

    @Transactional
    public ProfissionalResponse criar(ProfissionalRequest req) {
        Profissional profissional = new Profissional();
        preencher(profissional, req);
        return ProfissionalResponse.de(repository.save(profissional));
    }

    @Transactional
    public ProfissionalResponse atualizar(Long id, ProfissionalRequest req) {
        Profissional profissional = obter(id);
        preencher(profissional, req);
        return ProfissionalResponse.de(profissional);
    }

    @Transactional
    public void desativar(Long id) {
        obter(id).setAtivo(false);
    }

    private void preencher(Profissional p, ProfissionalRequest req) {
        if (!req.horaFim().isAfter(req.horaInicio())) {
            throw new RegraNegocioException("O fim do expediente deve ser depois do início");
        }
        validarAlmoco(req);

        List<Servico> servicos = servicoRepository.findAllById(req.servicoIds());
        if (servicos.size() != req.servicoIds().size()) {
            throw new RegraNegocioException("Um ou mais serviços informados não existem");
        }

        p.setNome(req.nome().trim());
        p.setHoraInicio(req.horaInicio());
        p.setHoraFim(req.horaFim());
        p.setAlmocoInicio(req.almocoInicio());
        p.setAlmocoFim(req.almocoFim());
        p.setDiasTrabalho(new HashSet<>(req.diasTrabalho()));
        p.setServicos(new HashSet<>(servicos));
    }

    private void validarAlmoco(ProfissionalRequest req) {
        LocalTime inicio = req.almocoInicio();
        LocalTime fim = req.almocoFim();
        if (inicio == null && fim == null) {
            return;
        }
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("Informe o início e o fim do almoço, ou deixe os dois em branco");
        }
        if (!fim.isAfter(inicio)) {
            throw new RegraNegocioException("O fim do almoço deve ser depois do início");
        }
        if (inicio.isBefore(req.horaInicio()) || fim.isAfter(req.horaFim())) {
            throw new RegraNegocioException("O almoço precisa estar dentro do expediente");
        }
    }

    private Profissional obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", id));
    }
}
