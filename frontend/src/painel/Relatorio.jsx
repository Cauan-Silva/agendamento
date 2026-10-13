import { useEffect, useState } from 'react'
import { api } from '../services/api.js'
import { preco } from '../utils/formato.js'
import { useErroPainel } from './useErroPainel.js'

const fmtMes = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
const fmtDiaGrafico = new Intl.DateTimeFormat('pt-BR', { weekday: 'short', day: 'numeric', month: 'short' })

function mesAtual() {
  const hoje = new Date()
  return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}`
}

function paraData(iso) {
  const [a, m, d] = iso.split('-').map(Number)
  return new Date(a, m - 1, d)
}

const plural = (n, um, varios) => `${n} ${n === 1 ? um : varios}`

export default function Relatorio({ token, onSessaoExpirada }) {
  const [mes, setMes] = useState(mesAtual)
  const [dados, setDados] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro, tratarErro] = useErroPainel(onSessaoExpirada)

  useEffect(() => {
    if (!mes) return
    const [ano, m] = mes.split('-').map(Number)
    let cancelado = false
    setCarregando(true)
    setErro(null)
    api
      .relatorioMensal(ano, m, token)
      .then((r) => !cancelado && setDados(r))
      .catch((e) => !cancelado && tratarErro(e))
      .finally(() => !cancelado && setCarregando(false))
    return () => {
      cancelado = true
    }
  }, [mes, token, tratarErro, setErro])

  const titulo = mes ? fmtMes.format(paraData(`${mes}-01`)) : ''
  const vazio = dados && dados.atendimentos === 0 && dados.cancelados === 0

  return (
    <section>
      <div className="secao-topo">
        <h2 className="titulo-etapa primeira-maiuscula">{titulo}</h2>
        <label className="agenda-nav__data">
          <span className="visualmente-oculto">Mês do relatório</span>
          <input type="month" value={mes} onChange={(e) => e.target.value && setMes(e.target.value)} />
        </label>
      </div>

      {erro && (
        <p className="aviso aviso--alerta" role="alert">
          {erro}
        </p>
      )}
      {carregando && <p className="aviso">Calculando…</p>}
      {!carregando && vazio && <p className="aviso">Nenhum agendamento neste mês.</p>}

      {!carregando && dados && !vazio && (
        <>
          <dl className="numeros">
            <div>
              <dt>Faturado</dt>
              <dd>{preco(dados.faturamento)}</dd>
              <p>{plural(dados.concluidos, 'atendimento concluído', 'atendimentos concluídos')}</p>
            </div>
            <div>
              <dt>Previsto no mês</dt>
              <dd>{preco(dados.previsto)}</dd>
              <p>{plural(dados.atendimentos, 'agendamento', 'agendamentos')}, sem contar cancelados</p>
            </div>
            <div>
              <dt>Cancelados</dt>
              <dd>{dados.cancelados}</dd>
              <p>
                {dados.atendimentos + dados.cancelados > 0 &&
                  `${Math.round((dados.cancelados / (dados.atendimentos + dados.cancelados)) * 100)}% do total`}
              </p>
            </div>
          </dl>

          <GraficoPorDia dias={dados.porDia} />

          <div className="tabelas">
            <div>
              <h3 className="subtitulo">Por profissional</h3>
              <table className="tabela">
                <thead>
                  <tr>
                    <th scope="col">Nome</th>
                    <th scope="col" className="num">
                      Atendimentos
                    </th>
                    <th scope="col" className="num">
                      Faturado
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {dados.porProfissional.map((p) => (
                    <tr key={p.nome}>
                      <th scope="row">{p.nome}</th>
                      <td className="num">
                        {p.concluidos} de {p.atendimentos}
                      </td>
                      <td className="num">{preco(p.faturamento)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <p className="nota nota--tabela">Atendimentos: concluídos de agendados.</p>
            </div>

            <div>
              <h3 className="subtitulo">Por serviço</h3>
              <table className="tabela">
                <thead>
                  <tr>
                    <th scope="col">Serviço</th>
                    <th scope="col" className="num">
                      Vezes
                    </th>
                    <th scope="col" className="num">
                      Faturado
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {dados.porServico.map((s) => (
                    <tr key={s.nome}>
                      <th scope="row">{s.nome}</th>
                      <td className="num">{s.quantidade}</td>
                      <td className="num">{preco(s.faturamento)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </section>
  )
}

/** Barras de atendimentos por dia, com dica ao passar o mouse ou focar com o teclado. */
function GraficoPorDia({ dias }) {
  const [ativo, setAtivo] = useState(null)
  const maximo = Math.max(1, ...dias.map((d) => d.atendimentos))
  const marcados = new Set([1, 5, 10, 15, 20, 25, dias.length])

  return (
    <figure className="grafico">
      <figcaption className="subtitulo">Atendimentos por dia</figcaption>

      <div className="grafico__area" onMouseLeave={() => setAtivo(null)}>
        <span className="grafico__escala">{maximo}</span>
        <div className="grafico__barras">
          {dias.map((d, i) => {
            const rotulo = `${fmtDiaGrafico.format(paraData(d.data)).replace(/\./g, '')}: ${plural(
              d.atendimentos,
              'atendimento',
              'atendimentos',
            )}`
            return (
              <div
                key={d.data}
                className="grafico__coluna"
                tabIndex={0}
                aria-label={rotulo}
                onMouseEnter={() => setAtivo(i)}
                onFocus={() => setAtivo(i)}
                onBlur={() => setAtivo(null)}
              >
                <div
                  className="grafico__barra"
                  style={{ height: `${(d.atendimentos / maximo) * 100}%` }}
                  data-zero={d.atendimentos === 0 || undefined}
                />
                {ativo === i && (
                  <div
                    className={`grafico__dica ${i > dias.length - 6 ? 'grafico__dica--esquerda' : ''}`}
                    role="status"
                  >
                    {rotulo}
                  </div>
                )}
                <span className="grafico__dia" aria-hidden="true">
                  {marcados.has(i + 1) ? i + 1 : ''}
                </span>
              </div>
            )
          })}
        </div>
      </div>

      <details className="grafico__tabela">
        <summary>Ver como tabela</summary>
        <table className="tabela">
          <thead>
            <tr>
              <th scope="col">Dia</th>
              <th scope="col" className="num">
                Atendimentos
              </th>
            </tr>
          </thead>
          <tbody>
            {dias
              .filter((d) => d.atendimentos > 0)
              .map((d) => (
                <tr key={d.data}>
                  <th scope="row">{fmtDiaGrafico.format(paraData(d.data)).replace(/\./g, '')}</th>
                  <td className="num">{d.atendimentos}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </details>
    </figure>
  )
}
