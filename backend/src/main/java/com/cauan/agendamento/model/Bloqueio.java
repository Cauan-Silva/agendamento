package com.cauan.agendamento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Período em que não se pode agendar.
 * Com profissional: folga ou compromisso daquela pessoa.
 * Sem profissional (nulo): vale para todo o negócio, como um feriado.
 */
@Entity
@Table(name = "bloqueio")
public class Bloqueio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id")
    private Profissional profissional;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(length = 100)
    private String motivo;

    public boolean sobrepoe(LocalDateTime outroInicio, LocalDateTime outroFim) {
        return inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Profissional getProfissional() { return profissional; }
    public void setProfissional(Profissional profissional) { this.profissional = profissional; }

    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }

    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
