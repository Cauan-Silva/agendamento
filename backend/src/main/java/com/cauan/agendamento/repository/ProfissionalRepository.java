package com.cauan.agendamento.repository;

import com.cauan.agendamento.model.Profissional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    List<Profissional> findByAtivoTrueOrderByNome();

    /**
     * Busca o profissional bloqueando a linha até o fim da transação.
     * Assim, duas requisições tentando agendar o mesmo profissional ao mesmo
     * tempo são executadas uma depois da outra, e a segunda enxerga o
     * agendamento criado pela primeira — evitando horários duplicados.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profissional p where p.id = :id")
    Optional<Profissional> findByIdParaAtualizar(@Param("id") Long id);
}
