package com.cauan.agendamento.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "profissional")
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    /** Início do expediente (ex.: 09:00). */
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    /** Fim do expediente (ex.: 18:00). */
    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    /** Intervalo de almoço (opcional). Vale para todos os dias de trabalho. */
    @Column(name = "almoco_inicio")
    private LocalTime almocoInicio;

    @Column(name = "almoco_fim")
    private LocalTime almocoFim;

    @ElementCollection
    @CollectionTable(name = "profissional_dia", joinColumns = @JoinColumn(name = "profissional_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "dia", nullable = false, length = 10)
    private Set<DayOfWeek> diasTrabalho = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "profissional_servico",
            joinColumns = @JoinColumn(name = "profissional_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id"))
    private Set<Servico> servicos = new HashSet<>();

    @Column(nullable = false)
    private boolean ativo = true;

    /** Indica se este profissional realiza o serviço informado. */
    public boolean atende(Long servicoId) {
        return servicos.stream().anyMatch(s -> s.getId().equals(servicoId));
    }

    public boolean trabalhaEm(DayOfWeek dia) {
        return diasTrabalho.contains(dia);
    }

    /** Verdadeiro se o intervalo [inicio, fim) cai, mesmo que em parte, no horário de almoço. */
    public boolean almocoSobrepoe(LocalDateTime inicio, LocalDateTime fim) {
        if (almocoInicio == null || almocoFim == null) {
            return false;
        }
        LocalDateTime almocoDe = inicio.toLocalDate().atTime(almocoInicio);
        LocalDateTime almocoAte = inicio.toLocalDate().atTime(almocoFim);
        return inicio.isBefore(almocoAte) && fim.isAfter(almocoDe);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }

    public LocalTime getAlmocoInicio() { return almocoInicio; }
    public void setAlmocoInicio(LocalTime almocoInicio) { this.almocoInicio = almocoInicio; }

    public LocalTime getAlmocoFim() { return almocoFim; }
    public void setAlmocoFim(LocalTime almocoFim) { this.almocoFim = almocoFim; }

    public Set<DayOfWeek> getDiasTrabalho() { return diasTrabalho; }
    public void setDiasTrabalho(Set<DayOfWeek> diasTrabalho) { this.diasTrabalho = diasTrabalho; }

    public Set<Servico> getServicos() { return servicos; }
    public void setServicos(Set<Servico> servicos) { this.servicos = servicos; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
