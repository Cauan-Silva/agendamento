package com.cauan.agendamento.model;

/** O que cada login pode fazer no painel. */
public enum Perfil {
    /** Dono ou gerente: agenda de todos, cadastros, folgas, relatório e acessos. */
    ADMIN,
    /** Pessoa da equipe: só a própria agenda. */
    PROFISSIONAL
}
