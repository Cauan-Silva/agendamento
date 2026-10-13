import { preco } from '../utils/formato.js'

export default function EscolhaServico({ servicos, selecionado, onEscolher }) {
  if (servicos.length === 0) {
    return <p className="aviso">Nenhum serviço disponível para agendamento no momento.</p>
  }

  return (
    <section>
      <h2 className="titulo-etapa">O que você vai fazer?</h2>
      <ul className="lista-opcoes">
        {servicos.map((s) => (
          <li key={s.id}>
            <button
              type="button"
              className="opcao"
              aria-pressed={selecionado?.id === s.id}
              onClick={() => onEscolher(s)}
            >
              <span className="opcao__nome">{s.nome}</span>
              <span className="opcao__detalhe">{s.duracaoMinutos} min</span>
              <span className="opcao__preco">{preco(s.preco)}</span>
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
