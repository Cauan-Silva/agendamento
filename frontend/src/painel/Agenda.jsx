import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../services/api.js'
import { dataIso, dataPorExtenso, hora, mascaraTelefone, preco } from '../utils/formato.js'

const STATUS = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
}

function somarDias(iso, dias) {
  const [a, m, d] = iso.split('-').map(Number)
  return dataIso(new Date(a, m - 1, d + dias))
}

export default function Agenda({ token, onSessaoExpirada }) {
  const hoje = dataIso(new Date())
  const [data, setData] = useState(hoje)
  const [itens, setItens] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)
  const [alterando, setAlterando] = useState(null)

  const tratarErro = useCallback(
    (e) => {
      if (e.status === 401) onSessaoExpirada()
      else setErro(e.message)
    },
    [onSessaoExpirada],
  )

  useEffect(() => {
    let cancelado = false
    setCarregando(true)
    setErro(null)
    api
      .agendaDoDia(data, token)
      .then((r) => !cancelado && setItens(r))
      .catch((e) => !cancelado && tratarErro(e))
      .finally(() => !cancelado && setCarregando(false))
    return () => {
      cancelado = true
    }
  }, [data, token, tratarErro])

  async function alterar(item, acao) {
    const pergunta =
      acao === 'cancelar'
        ? `Cancelar o horário de ${item.clienteNome} às ${hora(item.inicio.split('T')[1])}?`
        : null
    if (pergunta && !window.confirm(pergunta)) return

    setAlterando(item.id)
    setErro(null)
    try {
      const atualizado = await api[acao](item.id, token)
      setItens((lista) => lista.map((i) => (i.id === atualizado.id ? atualizado : i)))
    } catch (e) {
      tratarErro(e)
    } finally {
      setAlterando(null)
    }
  }

  const porProfissional = useMemo(() => {
    const grupos = new Map()
    for (const item of itens) {
      if (!grupos.has(item.profissionalNome)) grupos.set(item.profissionalNome, [])
      grupos.get(item.profissionalNome).push(item)
    }
    return [...grupos.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [itens])

  const ativos = itens.filter((i) => i.status !== 'CANCELADO')
  const previsto = ativos.reduce((soma, i) => soma + Number(i.valor), 0)

  return (
    <section>
      <div className="agenda-topo">
        <h2 className="titulo-etapa agenda-topo__data">{dataPorExtenso(data)}</h2>
        <div className="agenda-nav">
          <button type="button" className="botao-secundario" onClick={() => setData(somarDias(data, -1))}>
            Dia anterior
          </button>
          <button
            type="button"
            className="botao-secundario"
            onClick={() => setData(hoje)}
            disabled={data === hoje}
          >
            Hoje
          </button>
          <button type="button" className="botao-secundario" onClick={() => setData(somarDias(data, 1))}>
            Próximo dia
          </button>
          <label className="agenda-nav__data">
            <span className="visualmente-oculto">Ir para a data</span>
            <input type="date" value={data} onChange={(e) => e.target.value && setData(e.target.value)} />
          </label>
        </div>
      </div>

      {erro && (
        <p className="aviso aviso--alerta" role="alert">
          {erro}
        </p>
      )}
      {carregando && <p className="aviso">Carregando a agenda…</p>}

      {!carregando && !erro && itens.length === 0 && (
        <p className="aviso">Nenhum horário marcado para este dia.</p>
      )}

      {!carregando && itens.length > 0 && (
        <>
          <p className="agenda-resumo">
            {ativos.length} {ativos.length === 1 ? 'atendimento' : 'atendimentos'}, {preco(previsto)}{' '}
            previstos
          </p>

          {porProfissional.map(([nome, lista]) => (
            <div key={nome} className="agenda-grupo">
              <h3 className="agenda-grupo__nome">{nome}</h3>
              <ul className="agenda-lista">
                {lista.map((item) => {
                  const encerrado = item.status === 'CANCELADO' || item.status === 'CONCLUIDO'
                  const telefone = item.clienteTelefone
                  return (
                    <li key={item.id} className={`agenda-item agenda-item--${item.status.toLowerCase()}`}>
                      <p className="agenda-item__hora">
                        {hora(item.inicio.split('T')[1])}
                        <span className="agenda-item__fim">até {hora(item.fim.split('T')[1])}</span>
                      </p>
                      <div className="agenda-item__info">
                        <p className="agenda-item__cliente">{item.clienteNome}</p>
                        <p className="agenda-item__servico">
                          {item.servicoNome}, {preco(item.valor)}
                        </p>
                        <a
                          className="agenda-item__telefone"
                          href={`https://wa.me/55${telefone}`}
                          target="_blank"
                          rel="noreferrer"
                        >
                          {mascaraTelefone(telefone)}
                        </a>
                      </div>
                      <div className="agenda-item__acoes">
                        <span className="agenda-item__status">{STATUS[item.status]}</span>
                        {!encerrado && (
                          <>
                            <button
                              type="button"
                              className="botao-pequeno botao-pequeno--ok"
                              disabled={alterando === item.id}
                              onClick={() => alterar(item, 'concluir')}
                            >
                              Concluir
                            </button>
                            <button
                              type="button"
                              className="botao-pequeno"
                              disabled={alterando === item.id}
                              onClick={() => alterar(item, 'cancelar')}
                            >
                              Cancelar
                            </button>
                          </>
                        )}
                      </div>
                    </li>
                  )
                })}
              </ul>
            </div>
          ))}
        </>
      )}
    </section>
  )
}
