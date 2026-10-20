import { useCallback, useState } from 'react'
import { apagarSessao, lerSessao, salvarSessao } from '../services/sessao.js'
import Login from './Login.jsx'
import Agenda from './Agenda.jsx'
import Servicos from './Servicos.jsx'
import Equipe from './Equipe.jsx'
import Bloqueios from './Bloqueios.jsx'
import Relatorio from './Relatorio.jsx'
import Usuarios from './Usuarios.jsx'
import MinhaConta from './MinhaConta.jsx'

const NOME_NEGOCIO = import.meta.env.VITE_NOME_NEGOCIO ?? 'Barbearia Central'

// "perfis": quem vê a aba. O profissional vê só a própria agenda e a própria conta
// (a API aplica as mesmas regras; aqui é só para não mostrar o que não pode usar).
const ABAS = [
  { id: 'agenda', nome: 'Agenda', Componente: Agenda, perfis: ['ADMIN', 'PROFISSIONAL'] },
  { id: 'bloqueios', nome: 'Folgas e feriados', Componente: Bloqueios, perfis: ['ADMIN'] },
  { id: 'servicos', nome: 'Serviços', Componente: Servicos, perfis: ['ADMIN'] },
  { id: 'equipe', nome: 'Equipe', Componente: Equipe, perfis: ['ADMIN'] },
  { id: 'relatorio', nome: 'Relatório', Componente: Relatorio, perfis: ['ADMIN'] },
  { id: 'acessos', nome: 'Acessos', Componente: Usuarios, perfis: ['ADMIN'] },
  { id: 'conta', nome: 'Minha conta', Componente: MinhaConta, perfis: ['ADMIN', 'PROFISSIONAL'] },
]

export default function Painel() {
  const [sessao, setSessao] = useState(lerSessao)
  const [aviso, setAviso] = useState(null)
  const [aba, setAba] = useState('agenda')

  function entrar(resposta) {
    salvarSessao(resposta)
    setAviso(null)
    setAba('agenda')
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

  const abas = sessao ? ABAS.filter((a) => a.perfis.includes(sessao.perfil)) : []
  const { Componente } = abas.find((a) => a.id === aba) ?? ABAS[0]

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
        <h1 className="topo__titulo">
          {sessao?.perfil === 'PROFISSIONAL' ? `Agenda de ${sessao.profissionalNome}` : 'Painel da equipe'}
        </h1>
      </header>

      <main className="conteudo conteudo--largo">
        {sessao ? (
          <>
            <nav className="abas" aria-label="Seções do painel">
              {abas.map((a) => (
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
            <Componente token={sessao.token} sessao={sessao} onSessaoExpirada={expirou} />
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
