package com.cauan.agendamento.repository;

import com.cauan.agendamento.model.Agendamento;
import com.cauan.agendamento.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    /**
     * Agendamentos ativos (não cancelados) do profissional que ocupam
     * algum minuto do intervalo [inicio, fim).
     */
    @Query("""
            select a from Agendamento a
            where a.profissional.id = :profissionalId
              and a.status <> :ignorar
              and a.inicio < :fim
              and a.fim > :inicio
            order by a.inicio
            """)
    List<Agendamento> buscarOcupados(@Param("profissionalId") Long profissionalId,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim,
                                     @Param("ignorar") StatusAgendamento ignorar);

    List<Agendamento> findByProfissionalIdAndInicioBetweenOrderByInicio(Long profissionalId,
                                                                        LocalDateTime de,
                                                                        LocalDateTime ate);
}
