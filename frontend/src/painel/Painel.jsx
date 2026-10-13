import { useCallback, useState } from 'react'
import { apagarSessao, lerSessao, salvarSessao } from '../services/sessao.js'
import Login from './Login.jsx'
import Agenda from './Agenda.jsx'

const NOME_NEGOCIO = import.meta.env.VITE_NOME_NEGOCIO ?? 'Barbearia Central'

export default function Painel() {
  const [sessao, setSessao] = useState(lerSessao)
  const [aviso, setAviso] = useState(null)

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
    setAviso('Sua sessão expirou. Entre de novo para ver a agenda.')
  }, [])

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
        <h1 className="topo__titulo">Agenda da equipe</h1>
      </header>

      <main className="conteudo conteudo--largo">
        {sessao ? (
          <Agenda token={sessao.token} onSessaoExpirada={expirou} />
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
