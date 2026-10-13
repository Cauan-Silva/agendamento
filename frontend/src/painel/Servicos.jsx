import { useEffect, useState } from 'react'
import { api } from '../services/api.js'
import { preco } from '../utils/formato.js'
import { useErroPainel } from './useErroPainel.js'

const VAZIO = { nome: '', duracaoMinutos: '30', preco: '' }

export default function Servicos({ token, onSessaoExpirada }) {
  const [servicos, setServicos] = useState([])
  const [form, setForm] = useState(null) // null = fechado; { id?, ...campos } = aberto
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro, tratarErro] = useErroPainel(onSessaoExpirada)

  function carregar() {
    api.listarServicos().then(setServicos).catch(tratarErro)
  }

  useEffect(carregar, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function salvar(evento) {
    evento.preventDefault()
    setSalvando(true)
    setErro(null)
    try {
      await api.salvarServico(
        {
          nome: form.nome.trim(),
          duracaoMinutos: Number(form.duracaoMinutos),
          preco: Number(String(form.preco).replace(',', '.')),
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

  async function desativar(servico) {
    if (!window.confirm(`Tirar "${servico.nome}" da lista de serviços? Os agendamentos já feitos continuam.`)) return
    try {
      await api.desativarServico(servico.id, token)
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
        <h2 className="titulo-etapa">Serviços</h2>
        {!form && (
          <button type="button" className="botao-principal botao-principal--compacto" onClick={() => setForm(VAZIO)}>
            Novo serviço
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
          <h3 className="subtitulo">{form.id ? `Editar ${form.nome}` : 'Novo serviço'}</h3>
          <label className="campo">
            <span>Nome</span>
            <input type="text" required maxLength={100} {...campo('nome')} />
          </label>
          <div className="campos-lado">
            <label className="campo">
              <span>Duração (minutos)</span>
              <input type="number" required min={5} max={480} step={5} {...campo('duracaoMinutos')} />
            </label>
            <label className="campo">
              <span>Preço (R$)</span>
              <input type="text" inputMode="decimal" required placeholder="45,00" {...campo('preco')} />
            </label>
          </div>
          <div className="botoes">
            <button type="submit" className="botao-principal" disabled={salvando}>
              {salvando ? 'Salvando…' : 'Salvar serviço'}
            </button>
            <button type="button" className="botao-secundario" onClick={() => setForm(null)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      <ul className="lista-cadastro">
        {servicos.map((s) => (
          <li key={s.id}>
            <div>
              <p className="lista-cadastro__nome">{s.nome}</p>
              <p className="lista-cadastro__detalhe">
                {s.duracaoMinutos} min, {preco(s.preco)}
              </p>
            </div>
            <div className="lista-cadastro__acoes">
              <button
                type="button"
                className="botao-pequeno"
                onClick={() =>
                  setForm({
                    id: s.id,
                    nome: s.nome,
                    duracaoMinutos: String(s.duracaoMinutos),
                    preco: String(s.preco).replace('.', ','),
                  })
                }
              >
                Editar
              </button>
              <button type="button" className="botao-pequeno" onClick={() => desativar(s)}>
                Desativar
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
