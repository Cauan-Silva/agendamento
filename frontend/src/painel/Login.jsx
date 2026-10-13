import { useState } from 'react'
import { api } from '../services/api.js'

export default function Login({ aviso, onEntrar }) {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [entrando, setEntrando] = useState(false)
  const [erro, setErro] = useState(null)

  async function enviar(evento) {
    evento.preventDefault()
    setEntrando(true)
    setErro(null)
    try {
      onEntrar(await api.entrar(email.trim(), senha))
    } catch (e) {
      setErro(e.message)
      setEntrando(false)
    }
  }

  return (
    <section className="login">
      <h2 className="titulo-etapa">Entrar no painel</h2>
      {aviso && <p className="aviso">{aviso}</p>}
      {erro && (
        <p className="aviso aviso--alerta" role="alert">
          {erro}
        </p>
      )}
      <form className="formulario" onSubmit={enviar}>
        <label className="campo">
          <span>E-mail</span>
          <input
            type="email"
            autoComplete="username"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <label className="campo">
          <span>Senha</span>
          <input
            type="password"
            autoComplete="current-password"
            required
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
          />
        </label>
        <button type="submit" className="botao-principal" disabled={entrando}>
          {entrando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </section>
  )
}
