import { useEffect, useState } from 'react'
import { api } from '../services/api.js'
import { dataIso, dataPorExtenso, hora } from '../utils/formato.js'
import { useErroPainel } from './useErroPainel.js'

function novoForm() {
  const hoje = dataIso(new Date())
  return {
    quem: '',
    tipo: 'dia',
    dataInicio: hoje,
    dataFim: hoje,
    data: hoje,
    horaInicio: '12:00',
    horaFim: '14:00',
    motivo: '',
  }
}

/** "Segunda-feira, 2 de novembro (dia inteiro)" ou "..., das 12:00 às 14:00". */
function descreverPeriodo(inicio, fim) {
  const [diaIni, horaIni] = inicio.split('T')
  const [diaFim, horaFim] = fim.split('T')
  const diaInteiro = hora(horaIni) === '00:00' && hora(horaFim) === '23:59'

  if (diaIni === diaFim) {
    return diaInteiro
      ? `${dataPorExtenso(diaIni)} (dia inteiro)`
      : `${dataPorExtenso(diaIni)}, das ${hora(horaIni)} às ${hora(horaFim)}`
  }
  return diaInteiro
    ? `De ${dataPorExtenso(diaIni)} até ${dataPorExtenso(diaFim)}`
    : `De ${dataPorExtenso(diaIni)}, ${hora(horaIni)}, até ${dataPorExtenso(diaFim)}, ${hora(horaFim)}`
}

export default function Bloqueios({ token, onSessaoExpirada }) {
  const [bloqueios, setBloqueios] = useState([])
  const [profissionais, setProfissionais] = useState([])
  const [form, setForm] = useState(null)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro, tratarErro] = useErroPainel(onSessaoExpirada)

  function carregar() {
    Promise.all([api.listarBloqueios(token), api.listarProfissionais()])
      .then(([b, p]) => {
        setBloqueios(b)
        setProfissionais(p)
      })
      .catch(tratarErro)
  }

  useEffect(carregar, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function salvar(evento) {
    evento.preventDefault()
    const periodo =
      form.tipo === 'dia'
        ? { inicio: `${form.dataInicio}T00:00`, fim: `${form.dataFim}T23:59` }
        : { inicio: `${form.data}T${form.horaInicio}`, fim: `${form.data}T${form.horaFim}` }

    setSalvando(true)
    setErro(null)
    try {
      await api.criarBloqueio(
        {
          profissionalId: form.quem ? Number(form.quem) : null,
          ...periodo,
          motivo: form.motivo.trim() || null,
        },
        token,
      )
      setForm(null)
      carregar()
    } catch (e) {
      tratarErro(e)
    } finally {
      setSalvando(false)
    }
  }

  async function remover(b) {
    if (!window.confirm('Remover este bloqueio? Os horários voltam a ficar disponíveis para agendamento.')) return
    try {
      await api.removerBloqueio(b.id, token)
      carregar()
    } catch (e) {
      tratarErro(e)
    }
  }

  const campo = (nome) => ({
    value: form[nome],
    onChange: (e) => setForm({ ...form, [nome]: e.target.value }),
  })

  return (
    <section>
      <div className="secao-topo">
        <h2 className="titulo-etapa">Folgas e feriados</h2>
        {!form && (
          <button
            type="button"
            className="botao-principal botao-principal--compacto"
            onClick={() => setForm(novoForm())}
          >
            Bloquear horário
          </button>
        )}
      </div>
      <p className="nota">
        Horários bloqueados somem da página de agendamento. O almoço de cada pessoa é configurado na
        aba Equipe.
      </p>

      {erro && (
        <p className="aviso aviso--alerta" role="alert">
          {erro}
        </p>
      )}

      {form && (
        <form className="formulario cartao-form" onSubmit={salvar}>
          <h3 className="subtitulo">Novo bloqueio</h3>

          <label className="campo">
            <span>Para quem</span>
            <select {...campo('quem')}>
              <option value="">Todo o negócio (feriado, fechado)</option>
              {profissionais.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nome}
                </option>
              ))}
            </select>
          </label>

          <fieldset className="grupo-caixas">
            <legend>Período</legend>
            <label className="caixa caixa--pilula">
              <input
                type="radio"
                name="tipo"
                checked={form.tipo === 'dia'}
                onChange={() => setForm({ ...form, tipo: 'dia' })}
              />
              <span>Dias inteiros</span>
            </label>
            <label className="caixa caixa--pilula">
              <input
                type="radio"
                name="tipo"
                checked={form.tipo === 'horario'}
                onChange={() => setForm({ ...form, tipo: 'horario' })}
              />
              <span>Algumas horas</span>
            </label>
          </fieldset>

          {form.tipo === 'dia' ? (
            <div className="campos-lado">
              <label className="campo">
                <span>De</span>
                <input type="date" required {...campo('dataInicio')} />
              </label>
              <label className="campo">
                <span>Até</span>
                <input type="date" required min={form.dataInicio} {...campo('dataFim')} />
              </label>
            </div>
          ) : (
            <div className="campos-lado campos-lado--tres">
              <label className="campo">
                <span>Dia</span>
                <input type="date" required {...campo('data')} />
              </label>
              <label className="campo">
                <span>Das</span>
                <input type="time" required {...campo('horaInicio')} />
              </label>
              <label className="campo">
                <span>Às</span>
                <input type="time" required {...campo('horaFim')} />
              </label>
            </div>
          )}

          <label className="campo">
            <span>Motivo (opcional)</span>
            <input type="text" maxLength={100} placeholder="Ex.: Feriado, consulta médica" {...campo('motivo')} />
          </label>

          <div className="botoes">
            <button type="submit" className="botao-principal" disabled={salvando}>
              {salvando ? 'Salvando…' : 'Salvar bloqueio'}
            </button>
            <button type="button" className="botao-secundario" onClick={() => setForm(null)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      {bloqueios.length === 0 && !form && <p className="aviso">Nenhuma folga ou feriado cadastrado.</p>}

      <ul className="lista-cadastro">
        {bloqueios.map((b) => (
          <li key={b.id}>
            <div>
              <p className="lista-cadastro__nome">{b.profissionalNome ?? 'Todo o negócio'}</p>
              <p className="lista-cadastro__detalhe primeira-maiuscula">{descreverPeriodo(b.inicio, b.fim)}</p>
              {b.motivo && <p className="lista-cadastro__detalhe">{b.motivo}</p>}
            </div>
            <div className="lista-cadastro__acoes">
              <button type="button" className="botao-pequeno" onClick={() => remover(b)}>
                Remover
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
