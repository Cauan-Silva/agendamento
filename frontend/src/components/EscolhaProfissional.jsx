const DIAS_CURTOS = {
  MONDAY: 'seg',
  TUESDAY: 'ter',
  WEDNESDAY: 'qua',
  THURSDAY: 'qui',
  FRIDAY: 'sex',
  SATURDAY: 'sáb',
  SUNDAY: 'dom',
}

export default function EscolhaProfissional({ profissionais, servico, selecionado, onEscolher }) {
  const disponiveis = profissionais.filter((p) => p.servicos.some((s) => s.id === servico.id))

  if (disponiveis.length === 0) {
    return (
      <p className="aviso">
        Ninguém da equipe faz {servico.nome.toLowerCase()} no momento. Volte e escolha outro
        serviço.
      </p>
    )
  }

  return (
    <section>
      <h2 className="titulo-etapa">Com quem?</h2>
      <ul className="lista-opcoes">
        {disponiveis.map((p) => (
          <li key={p.id}>
            <button
              type="button"
              className="opcao"
              aria-pressed={selecionado?.id === p.id}
              onClick={() => onEscolher(p)}
            >
              <span className="opcao__inicial" aria-hidden="true">
                {p.nome.charAt(0)}
              </span>
              <span className="opcao__nome">{p.nome}</span>
              <span className="opcao__detalhe">
                {p.diasTrabalho.map((d) => DIAS_CURTOS[d]).join(', ')}, das{' '}
                {p.horaInicio.slice(0, 5)} às {p.horaFim.slice(0, 5)}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
