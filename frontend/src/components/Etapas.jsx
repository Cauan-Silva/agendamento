const NOMES = ['Serviço', 'Profissional', 'Horário', 'Seus dados']

/**
 * Barra de progresso do agendamento. Etapas já concluídas viram botões
 * para a pessoa voltar e trocar a escolha.
 */
export default function Etapas({ atual, onVoltarPara }) {
  return (
    <nav aria-label="Etapas do agendamento">
      <ol className="etapas">
        {NOMES.map((nome, i) => {
          const estado = i < atual ? 'feita' : i === atual ? 'atual' : 'futura'
          return (
            <li key={nome} className={`etapa etapa--${estado}`}>
              {estado === 'feita' ? (
                <button type="button" onClick={() => onVoltarPara(i)}>
                  <span className="etapa__num">{i + 1}</span>
                  <span className="etapa__nome">{nome}</span>
                </button>
              ) : (
                <span aria-current={estado === 'atual' ? 'step' : undefined}>
                  <span className="etapa__num">{i + 1}</span>
                  <span className="etapa__nome">{nome}</span>
                </span>
              )}
            </li>
          )
        })}
      </ol>
    </nav>
  )
}
