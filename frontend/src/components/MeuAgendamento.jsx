import { useCallback, useEffect, useState } from 'react'
import { api } from '../services/api.js'
import { dataPorExtenso, hora } from '../utils/formato.js'
import EscolhaHorario from './EscolhaHorario.jsx'

const NOME_NEGOCIO = import.meta.env.VITE_NOME_NEGOCIO ?? 'Barbearia Central'

/** "2026-10-20T14:00:00" -> "segunda-feira, 20 de outubro, às 14:00" */
function quando(dataHora) {
  const [data, horario] = dataHora.split('T')
  return `${dataPorExtenso(data)}, às ${hora(horario)}`
}

/**
 * Página aberta pelo link do e-mail (#/meu-agendamento/<código>).
 * O cliente vê o próprio horário e pode cancelar ou remarcar, sem login.
 */
export default function MeuAgendamento({ codigo }) {
  const [agendamento, setAgendamento] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)

  const [modo, setModo] = useState('ver') // ver | remarcar | confirmar
  const [profissional, setProfissional] = useState(null)
  const [novo, setNovo] = useState(null) // { data, horario }
  const [aviso, setAviso] = useState(null)
  const [avisoHorario, setAvisoHorario] = useState(null)
  const [versaoHorarios, setVersaoHorarios] = useState(0)
  const [enviando, setEnviando] = useState(false)

  const carregar = useCallback(() => {
    setCarregando(true)
    setErro(null)
    api
      .meuAgendamento(codigo)
      .then(setAgendamento)
      .catch((e) =>
        setErro(e.status === 404 ? 'Não encontramos este agendamento. Confira o link do e-mail.' : e.message),
      )
      .finally(() => setCarregando(false))
  }, [codigo])

  useEffect(carregar, [carregar])

  async function iniciarRemarcacao() {
    setAviso(null)
    try {
      const equipe = await api.listarProfissionais()
      const pessoa = equipe.find((p) => p.id === agendamento.profissionalId)
      if (!pessoa) {
        setAviso(`${agendamento.profissionalNome} não está atendendo pelo site. Fale com ${NOME_NEGOCIO}.`)
        return
      }
      setProfissional(pessoa)
      setAvisoHorario(null)
      setModo('remarcar')
    } catch (e) {
      setAviso(e.message)
    }
  }

  async function confirmarRemarcacao() {
    setEnviando(true)
    setAviso(null)
    try {
      const atualizado = await api.remarcarMeuAgendamento(codigo, `${novo.data}T${novo.horario.slice(0, 5)}:00`)
      setAgendamento(atualizado)
      setModo('ver')
      setAviso('Pronto! Seu horário foi remarcado.')
    } catch (e) {
      if (e.status === 409) {
        setAvisoHorario('Esse horário acabou de ser reservado por outra pessoa. Escolha outro.')
        setVersaoHorarios((v) => v + 1)
        setModo('remarcar')
      } else {
        setAviso(e.message)
        setModo('ver')
      }
    } finally {
      setEnviando(false)
    }
  }

  async function cancelar() {
    if (!window.confirm(`Cancelar o seu horário de ${quando(agendamento.inicio)}?`)) return
    setEnviando(true)
    setAviso(null)
    try {
      setAgendamento(await api.cancelarMeuAgendamento(codigo))
    } catch (e) {
      setAviso(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pagina">
      <header className="topo">
        <p className="topo__negocio">{NOME_NEGOCIO}</p>
        <h1 className="topo__titulo">Seu horário</h1>
      </header>

      <main className="conteudo">
        {carregando && <p className="aviso">Carregando o seu agendamento…</p>}
        {erro && (
          <p className="aviso aviso--alerta" role="alert">
            {erro}
          </p>
        )}

        {agendamento && !carregando && (
          <>
            {aviso && (
              <p className="aviso" role="status">
                {aviso}
              </p>
            )}

            {modo === 'ver' && (
              <Detalhes
                agendamento={agendamento}
                enviando={enviando}
                onRemarcar={iniciarRemarcacao}
                onCancelar={cancelar}
              />
            )}

            {modo === 'remarcar' && (
              <>
                <EscolhaHorario
                  profissional={profissional}
                  servico={{ id: agendamento.servicoId, nome: agendamento.servicoNome }}
                  versao={versaoHorarios}
                  aviso={avisoHorario}
                  onEscolher={(data, horario) => {
                    setNovo({ data, horario })
                    setModo('confirmar')
                  }}
                />
                <button type="button" className="botao-secundario" onClick={() => setModo('ver')}>
                  Manter o horário atual
                </button>
              </>
            )}

            {modo === 'confirmar' && (
              <section>
                <h2 className="titulo-etapa">Confirmar a troca</h2>
                <dl className="resumo">
                  <div>
                    <dt>Era</dt>
                    <dd>{quando(agendamento.inicio)}</dd>
                  </div>
                  <div>
                    <dt>Passa a ser</dt>
                    <dd>{quando(`${novo.data}T${novo.horario}`)}</dd>
                  </div>
                  <div>
                    <dt>Serviço</dt>
                    <dd>
                      {agendamento.servicoNome} com {agendamento.profissionalNome}
                    </dd>
                  </div>
                </dl>
                <div className="acoes-cliente">
                  <button
                    type="button"
                    className="botao-principal"
                    disabled={enviando}
                    onClick={confirmarRemarcacao}
                  >
                    {enviando ? 'Remarcando…' : 'Confirmar novo horário'}
                  </button>
                  <button type="button" className="botao-secundario" onClick={() => setModo('remarcar')}>
                    Escolher outro
                  </button>
                </div>
              </section>
            )}
          </>
        )}

        <p className="rodape">
          <a href="#/">Fazer um novo agendamento</a>
        </p>
      </main>
    </div>
  )
}

function Detalhes({ agendamento, enviando, onRemarcar, onCancelar }) {
  const [data, horaInicio] = agendamento.inicio.split('T')
  const cancelado = agendamento.status === 'CANCELADO'
  const concluido = agendamento.status === 'CONCLUIDO'

  return (
    <section aria-live="polite">
      {cancelado && <p className="aviso aviso--alerta">Este horário foi cancelado.</p>}
      {concluido && <p className="aviso">Este atendimento já foi realizado. Obrigado pela visita!</p>}

      <div className={`ticket${cancelado ? ' ticket--cancelado' : ''}`}>
        <div className="ticket__corpo">
          <p className="ticket__negocio">{NOME_NEGOCIO}</p>
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
              <dd>{hora(agendamento.fim.split('T')[1])}</dd>
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

      {agendamento.podeAlterar && (
        <>
          <p className="nota">Você pode remarcar ou cancelar até {quando(agendamento.alterarAte)}.</p>
          <div className="acoes-cliente">
            <button type="button" className="botao-principal" disabled={enviando} onClick={onRemarcar}>
              Remarcar
            </button>
            <button type="button" className="botao-perigo" disabled={enviando} onClick={onCancelar}>
              {enviando ? 'Cancelando…' : 'Cancelar horário'}
            </button>
          </div>
        </>
      )}

      {!agendamento.podeAlterar && !cancelado && !concluido && (
        <p className="nota">
          Já passou do prazo para mudar pelo site. Se não puder ir, fale direto com {NOME_NEGOCIO} e informe o
          número do agendamento.
        </p>
      )}
    </section>
  )
}
