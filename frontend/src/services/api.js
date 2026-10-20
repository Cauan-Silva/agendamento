const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

export class ErroApi extends Error {
  constructor(mensagem, status, campos) {
    super(mensagem)
    this.status = status
    this.campos = campos ?? {}
  }
}

async function requisicao(caminho, { token, ...opcoes } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  let resposta
  try {
    resposta = await fetch(BASE + caminho, { ...opcoes, headers })
  } catch {
    throw new ErroApi('Sem conexão com a agenda. Confira se a API está no ar.', 0)
  }

  if (!resposta.ok) {
    let problema = {}
    try {
      problema = await resposta.json()
    } catch {
      // resposta sem corpo JSON (ex.: token expirado)
    }
    throw new ErroApi(
      problema.detail ?? `A agenda respondeu com erro ${resposta.status}.`,
      resposta.status,
      problema.campos,
    )
  }

  return resposta.status === 204 ? null : resposta.json()
}

export const api = {
  // Público (cliente agendando)
  listarServicos: () => requisicao('/servicos'),
  listarProfissionais: () => requisicao('/profissionais'),
  buscarHorarios: (profissionalId, servicoId, data) =>
    requisicao(
      `/disponibilidade?profissionalId=${profissionalId}&servicoId=${servicoId}&data=${data}`,
    ),
  criarAgendamento: (dados) =>
    requisicao('/agendamentos', { method: 'POST', body: JSON.stringify(dados) }),

  // Cliente pelo link do e-mail (o código substitui o login)
  meuAgendamento: (codigo) => requisicao(`/meu-agendamento/${encodeURIComponent(codigo)}`),
  cancelarMeuAgendamento: (codigo) =>
    requisicao(`/meu-agendamento/${encodeURIComponent(codigo)}/cancelar`, { method: 'PATCH' }),
  remarcarMeuAgendamento: (codigo, inicio) =>
    requisicao(`/meu-agendamento/${encodeURIComponent(codigo)}/remarcar`, {
      method: 'PATCH',
      body: JSON.stringify({ inicio }),
    }),

  // Painel da equipe (exige login)
  entrar: (email, senha) =>
    requisicao('/auth/login', { method: 'POST', body: JSON.stringify({ email, senha }) }),
  agendaDoDia: (data, token) => requisicao(`/agendamentos?data=${data}`, { token }),
  concluir: (id, token) => requisicao(`/agendamentos/${id}/concluir`, { method: 'PATCH', token }),
  cancelar: (id, token) => requisicao(`/agendamentos/${id}/cancelar`, { method: 'PATCH', token }),

  salvarServico: (dados, id, token) =>
    requisicao(id ? `/servicos/${id}` : '/servicos', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(dados),
      token,
    }),
  desativarServico: (id, token) => requisicao(`/servicos/${id}`, { method: 'DELETE', token }),

  salvarProfissional: (dados, id, token) =>
    requisicao(id ? `/profissionais/${id}` : '/profissionais', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(dados),
      token,
    }),
  desativarProfissional: (id, token) => requisicao(`/profissionais/${id}`, { method: 'DELETE', token }),

  listarBloqueios: (token) => requisicao('/bloqueios', { token }),
  criarBloqueio: (dados, token) =>
    requisicao('/bloqueios', { method: 'POST', body: JSON.stringify(dados), token }),
  removerBloqueio: (id, token) => requisicao(`/bloqueios/${id}`, { method: 'DELETE', token }),

  // Logins da equipe (administrador)
  listarUsuarios: (token) => requisicao('/usuarios', { token }),
  criarUsuario: (dados, token) => requisicao('/usuarios', { method: 'POST', body: JSON.stringify(dados), token }),
  atualizarUsuario: (id, dados, token) =>
    requisicao(`/usuarios/${id}`, { method: 'PUT', body: JSON.stringify(dados), token }),
  redefinirSenha: (id, novaSenha, token) =>
    requisicao(`/usuarios/${id}/senha`, { method: 'PATCH', body: JSON.stringify({ novaSenha }), token }),

  // Qualquer pessoa logada
  trocarMinhaSenha: (senhaAtual, novaSenha, token) =>
    requisicao('/auth/senha', { method: 'PATCH', body: JSON.stringify({ senhaAtual, novaSenha }), token }),

  relatorioMensal: (ano, mes, token) => requisicao(`/relatorios/mensal?ano=${ano}&mes=${mes}`, { token }),
}
