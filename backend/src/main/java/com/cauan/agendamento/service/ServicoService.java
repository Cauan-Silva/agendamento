package com.cauan.agendamento.service;

import com.cauan.agendamento.dto.ServicoDtos.ServicoRequest;
import com.cauan.agendamento.dto.ServicoDtos.ServicoResponse;
import com.cauan.agendamento.exception.RecursoNaoEncontradoException;
import com.cauan.agendamento.model.Servico;
import com.cauan.agendamento.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository repository;

    public ServicoService(ServicoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listarAtivos() {
        return repository.findByAtivoTrueOrderByNome().stream().map(ServicoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscar(Long id) {
        return ServicoResponse.de(obter(id));
    }

    @Transactional
    public ServicoResponse criar(ServicoRequest req) {
        Servico servico = new Servico(req.nome().trim(), req.duracaoMinutos(), req.preco());
        return ServicoResponse.de(repository.save(servico));
    }

    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest req) {
        Servico servico = obter(id);
        servico.setNome(req.nome().trim());
        servico.setDuracaoMinutos(req.duracaoMinutos());
        servico.setPreco(req.preco());
        return ServicoResponse.de(servico);
    }

    /** Desativa em vez de apagar, para não perder o histórico de agendamentos. */
    @Transactional
    public void desativar(Long id) {
        obter(id).setAtivo(false);
    }

    Servico obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
    }
}
