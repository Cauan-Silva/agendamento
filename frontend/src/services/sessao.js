// Guarda o login do painel neste navegador até o token expirar.
const CHAVE = 'agendamento.sessao'

export function lerSessao() {
  try {
    const sessao = JSON.parse(localStorage.getItem(CHAVE))
    if (sessao && new Date(sessao.expiraEm) > new Date()) return sessao
  } catch {
    // armazenamento indisponível ou conteúdo inválido
  }
  return null
}

export function salvarSessao(sessao) {
  try {
    localStorage.setItem(CHAVE, JSON.stringify(sessao))
  } catch {
    // sem armazenamento: o login vale só enquanto a aba estiver aberta
  }
}

export function apagarSessao() {
  try {
    localStorage.removeItem(CHAVE)
  } catch {
    // nada a fazer
  }
}
