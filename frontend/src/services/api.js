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

  // Painel da equipe (exige login)
  entrar: (email, senha) =>
    requisicao('/auth/login', { method: 'POST', body: JSON.stringify({ email, senha }) }),
  agendaDoDia: (data, token) => requisicao(`/agendamentos?data=${data}`, { token }),
  concluir: (id, token) => requisicao(`/agendamentos/${id}/concluir`, { method: 'PATCH', token }),
  cancelar: (id, token) => requisicao(`/agendamentos/${id}/cancelar`, { method: 'PATCH', token }),
}
