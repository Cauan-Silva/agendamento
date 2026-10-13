import { useCallback, useState } from 'react'
import { apagarSessao, lerSessao, salvarSessao } from '../services/sessao.js'
import Login from './Login.jsx'
import Agenda from './Agenda.jsx'
import Servicos from './Servicos.jsx'
import Equipe from './Equipe.jsx'
import Bloqueios from './Bloqueios.jsx'
import Relatorio from './Relatorio.jsx'

const NOME_NEGOCIO = import.meta.env.VITE_NOME_NEGOCIO ?? 'Barbearia Central'

const ABAS = [
  { id: 'agenda', nome: 'Agenda', Componente: Agenda },
  { id: 'bloqueios', nome: 'Folgas e feriados', Componente: Bloqueios },
  { id: 'servicos', nome: 'Serviços', Componente: Servicos },
  { id: 'equipe', nome: 'Equipe', Componente: Equipe },
  { id: 'relatorio', nome: 'Relatório', Componente: Relatorio },
]

export default function Painel() {
  const [sessao, setSessao] = useState(lerSessao)
  const [aviso, setAviso] = useState(null)
  const [aba, setAba] = useState('agenda')

  function entrar(resposta) {
    salvarSessao(resposta)
    setAviso(null)
    setSessao(resposta)
  }

  function sair() {
    apagarSessao()
    setSessao(null)
  }

  const expirou = useCallback(() => {
    apagarSessao()
    setSessao(null)
    setAviso('Sua sessão expirou. Entre de novo para continuar.')
  }, [])

  const { Componente } = ABAS.find((a) => a.id === aba)

  return (
    <div className="pagina">
      <header className="topo topo--largo">
        <div className="topo__linha">
          <p className="topo__negocio">{NOME_NEGOCIO}</p>
          {sessao && (
            <button type="button" className="topo__sair" onClick={sair}>
              Sair ({sessao.nome})
            </button>
          )}
        </div>
        <h1 className="topo__titulo">Painel da equipe</h1>
      </header>

      <main className="conteudo conteudo--largo">
        {sessao ? (
          <>
            <nav className="abas" aria-label="Seções do painel">
              {ABAS.map((a) => (
                <button
                  key={a.id}
                  type="button"
                  className="aba"
                  aria-current={a.id === aba ? 'page' : undefined}
                  onClick={() => setAba(a.id)}
                >
                  {a.nome}
                </button>
              ))}
            </nav>
            <Componente token={sessao.token} onSessaoExpirada={expirou} />
          </>
        ) : (
          <Login aviso={aviso} onEntrar={entrar} />
        )}
        <p className="rodape">
          <a href="#/">Ver a página de agendamento</a>
        </p>
      </main>
    </div>
  )
}
