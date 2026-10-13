import { useEffect, useState } from 'react'
import { api } from '../services/api.js'
import { useErroPainel } from './useErroPainel.js'

const DIAS = [
  ['MONDAY', 'Seg'],
  ['TUESDAY', 'Ter'],
  ['WEDNESDAY', 'Qua'],
  ['THURSDAY', 'Qui'],
  ['FRIDAY', 'Sex'],
  ['SATURDAY', 'Sáb'],
  ['SUNDAY', 'Dom'],
]

const VAZIO = {
  nome: '',
  horaInicio: '09:00',
  horaFim: '18:00',
  temAlmoco: true,
  almocoInicio: '12:00',
  almocoFim: '13:00',
  diasTrabalho: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
  servicoIds: [],
}

const hhmm = (valor) => (valor ? valor.slice(0, 5) : '')

function alternar(lista, item) {
  return lista.includes(item) ? lista.filter((i) => i !== item) : [...lista, item]
}

export default function Equipe({ token, onSessaoExpirada }) {
  const [profissionais, setProfissionais] = useState([])
  const [servicos, setServicos] = useState([])
  const [form, setForm] = useState(null)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro, tratarErro] = useErroPainel(onSessaoExpirada)

  function carregar() {
    Promise.all([api.listarProfissionais(), api.listarServicos()])
      .then(([p, s]) => {
        setProfissionais(p)
        setServicos(s)
      })
      .catch(tratarErro)
  }

  useEffect(carregar, []) // eslint-disable-line react-hooks/exhaustive-deps

  function editar(p) {
    setForm({
      id: p.id,
      nome: p.nome,
      horaInicio: hhmm(p.horaInicio),
      horaFim: hhmm(p.horaFim),
      temAlmoco: Boolean(p.almocoInicio),
      almocoInicio: hhmm(p.almocoInicio) || '12:00',
      almocoFim: hhmm(p.almocoFim) || '13:00',
      diasTrabalho: p.diasTrabalho,
      servicoIds: p.servicos.map((s) => s.id),
    })
  }

  async function salvar(evento) {
    evento.preventDefault()
    if (form.diasTrabalho.length === 0 || form.servicoIds.length === 0) {
      setErro('Marque pelo menos um dia de trabalho e um serviço.')
      return
    }
    setSalvando(true)
    setErro(null)
    try {
      await api.salvarProfissional(
        {
          nome: form.nome.trim(),
          horaInicio: form.horaInicio,
          horaFim: form.horaFim,
          almocoInicio: form.temAlmoco ? form.almocoInicio : null,
          almocoFim: form.temAlmoco ? form.almocoFim : null,
          diasTrabalho: form.diasTrabalho,
          servicoIds: form.servicoIds,
        },
        form.id,
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

  async function desativar(p) {
    if (!window.confirm(`Tirar ${p.nome} da equipe? Os agendamentos já feitos continuam na agenda.`)) return
    try {
      await api.desativarProfissional(p.id, token)
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
        <h2 className="titulo-etapa">Equipe</h2>
        {!form && (
          <button type="button" className="botao-principal botao-principal--compacto" onClick={() => setForm(VAZIO)}>
            Nova pessoa
          </button>
        )}
      </div>

      {erro && (
        <p className="aviso aviso--alerta" role="alert">
          {erro}
        </p>
      )}

      {form && (
        <form className="formulario cartao-form" onSubmit={salvar}>
          <h3 className="subtitulo">{form.id ? `Editar ${form.nome}` : 'Nova pessoa na equipe'}</h3>
          <label className="campo">
            <span>Nome</span>
            <input type="text" required maxLength={100} {...campo('nome')} />
          </label>

          <div className="campos-lado">
            <label className="campo">
              <span>Começa às</span>
              <input type="time" required {...campo('horaInicio')} />
            </label>
            <label className="campo">
              <span>Termina às</span>
              <input type="time" required {...campo('horaFim')} />
            </label>
          </div>

          <label className="caixa">
            <input
              type="checkbox"
              checked={form.temAlmoco}
              onChange={(e) => setForm({ ...form, temAlmoco: e.target.checked })}
            />
            <span>Tem intervalo de almoço</span>
          </label>
          {form.temAlmoco && (
            <div className="campos-lado">
              <label className="campo">
                <span>Almoço de</span>
                <input type="time" required {...campo('almocoInicio')} />
              </label>
              <label className="campo">
                <span>até</span>
                <input type="time" required {...campo('almocoFim')} />
              </label>
            </div>
          )}

          <fieldset className="grupo-caixas">
            <legend>Dias de trabalho</legend>
            {DIAS.map(([valor, rotulo]) => (
              <label key={valor} className="caixa caixa--pilula">
                <input
                  type="checkbox"
                  checked={form.diasTrabalho.includes(valor)}
                  onChange={() => setForm({ ...form, diasTrabalho: alternar(form.diasTrabalho, valor) })}
                />
                <span>{rotulo}</span>
              </label>
            ))}
          </fieldset>

          <fieldset className="grupo-caixas">
            <legend>Serviços que faz</legend>
            {servicos.map((s) => (
              <label key={s.id} className="caixa caixa--pilula">
                <input
                  type="checkbox"
                  checked={form.servicoIds.includes(s.id)}
                  onChange={() => setForm({ ...form, servicoIds: alternar(form.servicoIds, s.id) })}
                />
                <span>{s.nome}</span>
              </label>
            ))}
          </fieldset>

          <div className="botoes">
            <button type="submit" className="botao-principal" disabled={salvando}>
              {salvando ? 'Salvando…' : 'Salvar'}
            </button>
            <button type="button" className="botao-secundario" onClick={() => setForm(null)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      <ul className="lista-cadastro">
        {profissionais.map((p) => (
          <li key={p.id}>
            <div>
              <p className="lista-cadastro__nome">{p.nome}</p>
              <p className="lista-cadastro__detalhe">
                {p.diasTrabalho
                  .map((d) => DIAS.find(([v]) => v === d)?.[1])
                  .join(', ')}
                , das {hhmm(p.horaInicio)} às {hhmm(p.horaFim)}
                {p.almocoInicio && `, almoço ${hhmm(p.almocoInicio)}–${hhmm(p.almocoFim)}`}
              </p>
              <p className="lista-cadastro__detalhe">{p.servicos.map((s) => s.nome).join(', ')}</p>
            </div>
            <div className="lista-cadastro__acoes">
              <button type="button" className="botao-pequeno" onClick={() => editar(p)}>
                Editar
              </button>
              <button type="button" className="botao-pequeno" onClick={() => desativar(p)}>
                Desativar
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
