import { useState } from 'react'
import { api } from '../services/api.js'
import { useErroPainel } from './useErroPainel.js'

const PERFIS = { ADMIN: 'Administrador', PROFISSIONAL: 'Profissional' }

/** Dados de quem está logado e troca da própria senha. */
export default function MinhaConta({ token, sessao, onSessaoExpirada }) {
  const [form, setForm] = useState({ atual: '', nova: '', repetir: '' })
  const [salvando, setSalvando] = useState(false)
  const [sucesso, setSucesso] = useState(false)
  const [erro, setErro, tratarErro] = useErroPainel(onSessaoExpirada)

  async function salvar(evento) {
    evento.preventDefault()
    setSucesso(false)
    if (form.nova !== form.repetir) {
      setErro('A nova senha e a repetição estão diferentes.')
      return
    }
    setSalvando(true)
    setErro(null)
    try {
      await api.trocarMinhaSenha(form.atual, form.nova, token)
      setForm({ atual: '', nova: '', repetir: '' })
      setSucesso(true)
    } catch (e) {
      // 401 aqui seria sessão expirada; senha atual errada volta como 422
      tratarErro(e)
    } finally {
      setSalvando(false)
    }
  }

  const campo = (nome) => ({
    value: form[nome],
    onChange: (e) => setForm({ ...form, [nome]: e.target.value }),
  })

  return (
    <section>
      <h2 className="titulo-etapa">Minha conta</h2>
      <dl className="resumo">
        <div>
          <dt>Nome</dt>
          <dd>{sessao.nome}</dd>
        </div>
        <div>
          <dt>Acesso</dt>
          <dd>
            {PERFIS[sessao.perfil]}
            {sessao.profissionalNome && ` (agenda de ${sessao.profissionalNome})`}
          </dd>
        </div>
      </dl>

      {erro && (
        <p className="aviso aviso--alerta" role="alert">
          {erro}
        </p>
      )}
      {sucesso && (
        <p className="aviso" role="status">
          Senha trocada. Use a nova no próximo login.
        </p>
      )}

      <form className="formulario cartao-form" onSubmit={salvar}>
        <h3 className="subtitulo">Trocar senha</h3>
        <label className="campo">
          <span>Senha atual</span>
          <input type="password" autoComplete="current-password" required {...campo('atual')} />
        </label>
        <label className="campo">
          <span>Nova senha</span>
          <input type="password" autoComplete="new-password" required minLength={6} maxLength={72} {...campo('nova')} />
        </label>
        <label className="campo">
          <span>Repita a nova senha</span>
          <input type="password" autoComplete="new-password" required minLength={6} maxLength={72} {...campo('repetir')} />
        </label>
        <div className="botoes">
          <button type="submit" className="botao-principal" disabled={salvando}>
            {salvando ? 'Salvando…' : 'Trocar senha'}
          </button>
        </div>
      </form>
    </section>
  )
}
