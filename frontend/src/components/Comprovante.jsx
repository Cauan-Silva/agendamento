import { dataPorExtenso, hora } from '../utils/formato.js'

/** Tíquete de confirmação: a parte "destacável" mostra o número do agendamento. */
export default function Comprovante({ agendamento, nomeNegocio, onNovo }) {
  const [data, horaInicio] = agendamento.inicio.split('T')
  const horaFim = agendamento.fim.split('T')[1]

  return (
    <section className="comprovante" aria-live="polite">
      <h2 className="titulo-etapa">Horário reservado</h2>

      <div className="ticket">
        <div className="ticket__corpo">
          <p className="ticket__negocio">{nomeNegocio}</p>
          <p className="ticket__hora">{hora(horaInicio)}</p>
          <p className="ticket__data">{dataPorExtenso(data)}</p>
          <dl className="ticket__itens">
            <div>
              <dt>Serviço</dt>
              <dd>{agendamento.servicoNome}</dd>
            </div>
            <div>
              <dt>Com</dt>
              <dd>{agendamento.profissionalNome}</dd>
            </div>
            <div>
              <dt>Termina</dt>
              <dd>{hora(horaFim)}</dd>
            </div>
            <div>
              <dt>Cliente</dt>
              <dd>{agendamento.clienteNome}</dd>
            </div>
          </dl>
        </div>
        <div className="ticket__canhoto">
          <span className="ticket__rotulo">Nº</span>
          <span className="ticket__numero">{String(agendamento.id).padStart(4, '0')}</span>
        </div>
      </div>

      <p className="nota">
        Chegue alguns minutos antes. Para cancelar, fale com {nomeNegocio} e informe o número do
        agendamento.
      </p>

      <button type="button" className="botao-secundario" onClick={onNovo}>
        Agendar outro horário
      </button>
    </section>
  )
}
