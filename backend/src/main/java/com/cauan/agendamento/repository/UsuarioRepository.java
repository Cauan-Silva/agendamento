package com.cauan.agendamento.repository;

import com.cauan.agendamento.model.Perfil;
import com.cauan.agendamento.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("select u from Usuario u left join fetch u.profissional where lower(u.email) = lower(:email)")
    Optional<Usuario> findByEmailIgnoreCase(@Param("email") String email);

    /** Usado a cada requisição do painel: traz o profissional junto para não precisar de outra consulta. */
    @Query("select u from Usuario u left join fetch u.profissional where u.id = :id")
    Optional<Usuario> buscarComProfissional(@Param("id") Long id);

    @Query("select u from Usuario u left join fetch u.profissional order by lower(u.nome)")
    List<Usuario> listarTodos();

    long countByPerfilAndAtivoTrue(Perfil perfil);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByProfissionalIdAndIdNot(Long profissionalId, Long id);

    boolean existsByProfissionalId(Long profissionalId);
}
