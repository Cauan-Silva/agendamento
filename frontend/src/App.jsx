import { useCallback, useEffect, useState } from 'react'
import { api } from './services/api.js'
import Etapas from './components/Etapas.jsx'
import EscolhaServico from './components/EscolhaServico.jsx'
import EscolhaProfissional from './components/EscolhaProfissional.jsx'
import EscolhaHorario from './components/EscolhaHorario.jsx'
import DadosCliente from './components/DadosCliente.jsx'
import Comprovante from './components/Comprovante.jsx'

const NOME_NEGOCIO = import.meta.env.VITE_NOME_NEGOCIO ?? 'Barbearia Central'

const ETAPA = { SERVICO: 0, PROFISSIONAL: 1, HORARIO: 2, DADOS: 3, PRONTO: 4 }

export default function App() {
  const [servicos, setServicos] = useState([])
  const [profissionais, setProfissionais] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erroInicial, setErroInicial] = useState(null)

  const [etapa, setEtapa] = useState(ETAPA.SERVICO)
  const [servico, setServico] = useState(null)
  const [profissional, setProfissional] = useState(null)
  const [data, setData] = useState(null)
  const [horario, setHorario] = useState(null)
  const [agendamento, setAgendamento] = useState(null)

  const [enviando, setEnviando] = useState(false)
  const [errosCampos, setErrosCampos] = useState({})
  const [erroEnvio, setErroEnvio] = useState(null)
  const [avisoHorario, setAvisoHorario] = useState(null)
  const [versaoHorarios, setVersaoHorarios] = useState(0)

  const carregar = useCallback(() => {
    setCarregando(true)
    setErroInicial(null)
    Promise.all([api.listarServicos(), api.listarProfissionais()])
      .then(([s, p]) => {
        setServicos(s)
        setProfissionais(p)
      })
      .catch((e) => setErroInicial(e.message))
      .finally(() => setCarregando(false))
  }, [])

  useEffect(carregar, [carregar])

  function escolherServico(s) {
    setServico(s)
    if (profissional && !profissional.servicos.some((x) => x.id === s.id)) {
      setProfissional(null)
    }
    setHorario(null)
    setEtapa(ETAPA.PROFISSIONAL)
  }

  function escolherProfissional(p) {
    if (p.id !== profissional?.id) setData(null)
    setProfissional(p)
    setHorario(null)
    setAvisoHorario(null)
    setEtapa(ETAPA.HORARIO)
  }

  function escolherHorario(d, h) {
    setData(d)
    setHorario(h)
    setAvisoHorario(null)
    setErrosCampos({})
    setErroEnvio(null)
    setEtapa(ETAPA.DADOS)
  }

  async function confirmar(cliente) {
    setEnviando(true)
    setErrosCampos({})
    setErroEnvio(null)
    try {
      const criado = await api.criarAgendamento({
        profissionalId: profissional.id,
        servicoId: servico.id,
        inicio: `${data}T${horario.slice(0, 5)}:00`,
        ...cliente,
      })
      setAgendamento(criado)
      setEtapa(ETAPA.PRONTO)
    } catch (e) {
      if (e.status === 409) {
        // Outra pessoa reservou o horário enquanto esta preenchia os dados.
        setAvisoHorario('Esse horário acabou de ser reservado por outra pessoa. Escolha outro.')
        setHorario(null)
        setVersaoHorarios((v) => v + 1)
        setEtapa(ETAPA.HORARIO)
      } else if (e.status === 400) {
        setErrosCampos(e.campos)
      } else {
        setErroEnvio(e.message)
      }
    } finally {
      setEnviando(false)
    }
  }

  function recomecar() {
    setServico(null)
    setProfissional(null)
    setData(null)
    setHorario(null)
    setAgendamento(null)
    setEtapa(ETAPA.SERVICO)
  }

  return (
    <div className="pagina">
      <header className="topo">
        <p className="topo__negocio">{NOME_NEGOCIO}</p>
        <h1 className="topo__titulo">Agende seu horário</h1>
      </header>

      <main className="conteudo">
        {carregando && <p className="aviso">Carregando a agenda…</p>}

        {erroInicial && (
          <div className="aviso aviso--alerta" role="alert">
            <p>{erroInicial}</p>
            <button type="button" className="botao-secundario" onClick={carregar}>
              Tentar de novo
            </button>
          </div>
        )}

        {!carregando && !erroInicial && (
          <>
            {etapa < ETAPA.PRONTO && <Etapas atual={etapa} onVoltarPara={setEtapa} />}

            {etapa === ETAPA.SERVICO && (
              <EscolhaServico servicos={servicos} selecionado={servico} onEscolher={escolherServico} />
            )}

            {etapa === ETAPA.PROFISSIONAL && (
              <EscolhaProfissional
                profissionais={profissionais}
                servico={servico}
                selecionado={profissional}
                onEscolher={escolherProfissional}
              />
            )}

            {etapa === ETAPA.HORARIO && (
              <EscolhaHorario
                key={`${profissional.id}-${servico.id}`}
                profissional={profissional}
                servico={servico}
                dataInicial={data}
                versao={versaoHorarios}
                aviso={avisoHorario}
                onEscolher={escolherHorario}
              />
            )}

            {etapa === ETAPA.DADOS && (
              <>
                {erroEnvio && (
                  <p className="aviso aviso--alerta" role="alert">
                    {erroEnvio}
                  </p>
                )}
                <DadosCliente
                  servico={servico}
                  profissional={profissional}
                  data={data}
                  horario={horario}
                  enviando={enviando}
                  erros={errosCampos}
                  onConfirmar={confirmar}
                />
              </>
            )}

            {etapa === ETAPA.PRONTO && (
              <Comprovante agendamento={agendamento} nomeNegocio={NOME_NEGOCIO} onNovo={recomecar} />
            )}
          </>
        )}

        <p className="rodape">
          <a href="#/painel">Área da equipe</a>
        </p>
      </main>
    </div>
  )
}
