package com.cauan.agendamento.repository;

import com.cauan.agendamento.model.Bloqueio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {

    /**
     * Bloqueios que afetam o profissional no intervalo [inicio, fim):
     * os dele e os do negócio inteiro (sem profissional).
     */
    @Query("""
            select b from Bloqueio b
            where (b.profissional is null or b.profissional.id = :profissionalId)
              and b.inicio < :fim
              and b.fim > :inicio
            """)
    List<Bloqueio> buscarNoPeriodo(@Param("profissionalId") Long profissionalId,
                                   @Param("inicio") LocalDateTime inicio,
                                   @Param("fim") LocalDateTime fim);

    /** Bloqueios que ainda não terminaram, para listar no painel. */
    List<Bloqueio> findByFimAfterOrderByInicio(LocalDateTime agora);
}
