const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

export class ErroApi extends Error {
  constructor(mensagem, status, campos) {
    super(mensagem)
    this.status = status
    this.campos = campos ?? {}
  }
}

async function requisicao(caminho, opcoes = {}) {
  let resposta
  try {
    resposta = await fetch(BASE + caminho, {
      headers: { 'Content-Type': 'application/json' },
      ...opcoes,
    })
  } catch {
    throw new ErroApi(
      'Sem conexão com a agenda. Confira se a API está rodando em localhost:8080.',
      0,
    )
  }

  if (!resposta.ok) {
    let problema = {}
    try {
      problema = await resposta.json()
    } catch {
      // resposta sem corpo JSON
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
  listarServicos: () => requisicao('/servicos'),
  listarProfissionais: () => requisicao('/profissionais'),
  buscarHorarios: (profissionalId, servicoId, data) =>
    requisicao(
      `/disponibilidade?profissionalId=${profissionalId}&servicoId=${servicoId}&data=${data}`,
    ),
  criarAgendamento: (dados) =>
    requisicao('/agendamentos', { method: 'POST', body: JSON.stringify(dados) }),
}
