// Nomes usados pela API (java.time.DayOfWeek), na ordem de Date.getDay().
const DIAS_API = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

/** "2026-10-19" no fuso local (toISOString usaria UTC e poderia mudar o dia). */
export function dataIso(data) {
  const ano = data.getFullYear()
  const mes = String(data.getMonth() + 1).padStart(2, '0')
  const dia = String(data.getDate()).padStart(2, '0')
  return `${ano}-${mes}-${dia}`
}

/** Próximos dias (a partir de hoje) em que o profissional trabalha. */
export function proximosDiasDeTrabalho(diasTrabalho, quantidadeDeDias = 21) {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  const dias = []
  for (let i = 0; i < quantidadeDeDias; i++) {
    const dia = new Date(hoje)
    dia.setDate(hoje.getDate() + i)
    if (diasTrabalho.includes(DIAS_API[dia.getDay()])) {
      dias.push(dia)
    }
  }
  return dias
}

const fmtDiaSemana = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' })
const fmtDiaMes = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' })
const fmtExtenso = new Intl.DateTimeFormat('pt-BR', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
})
const fmtPreco = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export const diaSemanaCurto = (data) => fmtDiaSemana.format(data).replace('.', '')
export const diaMesCurto = (data) => fmtDiaMes.format(data).replace('.', '')
export const preco = (valor) => fmtPreco.format(valor)

/** "2026-10-19" -> "segunda-feira, 19 de outubro" */
export function dataPorExtenso(iso) {
  const [ano, mes, dia] = iso.split('-').map(Number)
  return fmtExtenso.format(new Date(ano, mes - 1, dia))
}

/** "09:00:00" ou "09:00" -> "09:00" */
export const hora = (valor) => valor.slice(0, 5)

/** Mostra só números, no formato (48) 99999-0000 enquanto a pessoa digita. */
export function mascaraTelefone(valor) {
  const n = valor.replace(/\D/g, '').slice(0, 11)
  if (n.length <= 2) return n
  if (n.length <= 6) return `(${n.slice(0, 2)}) ${n.slice(2)}`
  if (n.length <= 10) return `(${n.slice(0, 2)}) ${n.slice(2, 6)}-${n.slice(6)}`
  return `(${n.slice(0, 2)}) ${n.slice(2, 7)}-${n.slice(7)}`
}
