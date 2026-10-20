import { useEffect, useState } from 'react'
import { api } from '../services/api.js'
import { useErroPainel } from './useErroPainel.js'

const PERFIS = [
  ['PROFISSIONAL', 'Profissional — vê só a própria agenda'],
  ['ADMIN', 'Administrador — vê e altera tudo'],
]

const VAZIO = { nome: '', email: '', senha: '', perfil: 'PROFISSIONAL', profissionalId: '', ativo: true }

/** Logins da equipe: quem entra no painel e o que cada um pode ver. */
export default function Usuarios({ token, sessao, onSessaoExpirada }) {
  const [usuarios, setUsuarios] = useState([])
  const [profissionais, setProfissionais] = useState([])
  const [form, setForm] = useState(null)
  const [senhaDe, setSenhaDe] = useState(null) // { id, nome, senha }
  const [salvando, setSalvando] = useState(false)
  const [aviso, setAviso] = useState(null)
  const [erro, setErro, tratarErro] = useErroPainel(onSessaoExpirada)

  function carregar() {
    Promise.all([api.listarUsuarios(token), api.listarProfissionais()])
      .then(([u, p]) => {
        setUsuarios(u)
        setProfissionais(p)
      })
      .catch(tratarErro)
  }

  useEffect(carregar, []) // eslint-disable-line react-hooks/exhaustive-deps

  function novo() {
    setAviso(null)
    setErro(null)
    setSenhaDe(null)
    setForm(VAZIO)
  }

  function editar(u) {
    setAviso(null)
    setErro(null)
    setSenhaDe(null)
    setForm({
      id: u.id,
      nome: u.nome,
      email: u.email,
      perfil: u.perfil,
      profissionalId: u.profissionalId ?? '',
      ativo: u.ativo,
    })
  }

  async function salvar(evento) {
    evento.preventDefault()
    setSalvando(true)
    setErro(null)
    const dados = {
      nome: form.nome.trim(),
      email: form.email.trim(),
      perfil: form.perfil,
      profissionalId: form.profissionalId === '' ? null : Number(form.profissionalId),
    }
    try {
      if (form.id) {
        await api.atualizarUsuario(form.id, { ...dados, ativo: form.ativo }, token)
        setAviso(`Login de ${dados.nome} atualizado.`)
      } else {
        await api.criarUsuario({ ...dados, senha: form.senha }, token)
        setAviso(`Login criado. Passe para ${dados.nome} o e-mail e a senha; dá para trocar a senha em "Minha conta".`)
      }
      setForm(null)
      carregar()
    } catch (e) {
      tratarErro(e)
    } finally {
      setSalvando(false)
    }
  }

  async function salvarSenha(evento) {
    evento.preventDefault()
    setSalvando(true)
    setErro(null)
    try {
      await api.redefinirSenha(senhaDe.id, senhaDe.senha, token)
      setAviso(`Senha de ${senhaDe.nome} redefinida.`)
      setSenhaDe(null)
    } catch (e) {
      tratarErro(e)
    } finally {
      setSalvando(false)
    }
  }

  const campo = (nome) => ({
    value: form[nome],
    onChange: (e) => setForm({ ...form, [nome]: e.target.value }),
  })

  // Profissionais que ainda não têm login (mais o já ligado a este login, ao editar).
  const livres = profissionais.filter(
    (p) => !usuarios.some((u) => u.profissionalId === p.id && u.id !== form?.id),
  )

  return (
    <section>
      <div className="secao-topo">
        <h2 className="titulo-etapa">Acessos ao painel</h2>
        {!form && (
          <button type="button" className="botao-principal botao-principal--compacto" onClick={novo}>
            Novo login
          </button>
        )}
      </div>

      {erro && (
        <p className="aviso aviso--alerta" role="alert">
          {erro}
        </p>
      )}
      {aviso && (
        <p className="aviso" role="status">
          {aviso}
        </p>
      )}

      {form && (
        <form className="formulario cartao-form" onSubmit={salvar}>
          <h3 className="subtitulo">{form.id ? `Editar login de ${form.nome}` : 'Novo login'}</h3>
          <label className="campo">
            <span>Nome</span>
            <input type="text" required maxLength={100} {...campo('nome')} />
          </label>
          <label className="campo">
            <span>E-mail (usado para entrar)</span>
            <input type="email" required maxLength={150} autoComplete="off" {...campo('email')} />
          </label>
          {!form.id && (
            <label className="campo">
              <span>Senha inicial</span>
              <input
                type="password"
                required
                minLength={6}
                maxLength={72}
                autoComplete="new-password"
                {...campo('senha')}
              />
            </label>
          )}
          <label className="campo">
            <span>Acesso</span>
            <select {...campo('perfil')}>
              {PERFIS.map(([valor, rotulo]) => (
                <option key={valor} value={valor}>
                  {rotulo}
                </option>
              ))}
            </select>
          </label>
          <label className="campo">
            <span>
              Agenda de qual profissional
              {form.perfil === 'ADMIN' && <span className="campo__opcional"> (opcional, se também atende)</span>}
            </span>
            <select required={form.perfil === 'PROFISSIONAL'} {...campo('profissionalId')}>
              <option value="">{form.perfil === 'ADMIN' ? 'Nenhuma' : 'Escolha…'}</option>
              {livres.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nome}
                </option>
              ))}
            </select>
          </label>
          {form.id && (
            <label className="caixa">
              <input
                type="checkbox"
                checked={form.ativo}
                onChange={(e) => setForm({ ...form, ativo: e.target.checked })}
              />
              <span>Login ativo (desmarque para bloquear a entrada no painel)</span>
            </label>
          )}
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

      {senhaDe && (
        <form className="formulario cartao-form" onSubmit={salvarSenha}>
          <h3 className="subtitulo">Nova senha para {senhaDe.nome}</h3>
          <label className="campo">
            <span>Nova senha</span>
            <input
              type="password"
              required
              minLength={6}
              maxLength={72}
              autoComplete="new-password"
              value={senhaDe.senha}
              onChange={(e) => setSenhaDe({ ...senhaDe, senha: e.target.value })}
            />
          </label>
          <div className="botoes">
            <button type="submit" className="botao-principal" disabled={salvando}>
              {salvando ? 'Salvando…' : 'Definir senha'}
            </button>
            <button type="button" className="botao-secundario" onClick={() => setSenhaDe(null)}>
              Cancelar
            </button>
          </div>
        </form>
      )}

      <ul className="lista-cadastro">
        {usuarios.map((u) => (
          <li key={u.id} className={u.ativo ? undefined : 'lista-cadastro--inativo'}>
            <div>
              <p className="lista-cadastro__nome">
                {u.nome}
                {u.id === sessao.usuarioId && ' (você)'}
              </p>
              <p className="lista-cadastro__detalhe">{u.email}</p>
              <p className="lista-cadastro__detalhe">
                {u.perfil === 'ADMIN' ? 'Administrador' : 'Profissional'}
                {u.profissionalNome && `, agenda de ${u.profissionalNome}`}
                {!u.ativo && ' — desativado'}
              </p>
            </div>
            <div className="lista-cadastro__acoes">
              <button type="button" className="botao-pequeno" onClick={() => editar(u)}>
                Editar
              </button>
              <button
                type="button"
                className="botao-pequeno"
                onClick={() => {
                  setForm(null)
                  setAviso(null)
                  setSenhaDe({ id: u.id, nome: u.nome, senha: '' })
                }}
              >
                Nova senha
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
