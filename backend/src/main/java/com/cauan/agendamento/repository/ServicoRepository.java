package com.cauan.agendamento.repository;

import com.cauan.agendamento.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByAtivoTrueOrderByNome();
}
