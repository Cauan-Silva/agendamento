import { useEffect, useMemo, useState } from 'react'
import { api } from '../services/api.js'
import {
  dataIso,
  dataPorExtenso,
  diaMesCurto,
  diaSemanaCurto,
  hora,
  proximosDiasDeTrabalho,
} from '../utils/formato.js'

export default function EscolhaHorario({
  profissional,
  servico,
  dataInicial,
  versao,
  aviso,
  onEscolher,
}) {
  const dias = useMemo(
    () => proximosDiasDeTrabalho(profissional.diasTrabalho),
    [profissional.diasTrabalho],
  )
  const [data, setData] = useState(dataInicial ?? (dias[0] ? dataIso(dias[0]) : null))
  const [horarios, setHorarios] = useState([])
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    if (!data) return
    let cancelado = false
    setCarregando(true)
    setErro(null)
    api
      .buscarHorarios(profissional.id, servico.id, data)
      .then((r) => !cancelado && setHorarios(r.horarios))
      .catch((e) => !cancelado && setErro(e.message))
      .finally(() => !cancelado && setCarregando(false))
    return () => {
      cancelado = true
    }
  }, [profissional.id, servico.id, data, versao])

  if (dias.length === 0) {
    return <p className="aviso">{profissional.nome} não tem dias de atendimento cadastrados.</p>
  }

  return (
    <section>
      <h2 className="titulo-etapa">Quando?</h2>
      {aviso && (
        <p className="aviso aviso--alerta" role="alert">
          {aviso}
        </p>
      )}

      <div className="dias" role="group" aria-label="Dia do atendimento">
        {dias.map((d) => {
          const iso = dataIso(d)
          return (
            <button
              key={iso}
              type="button"
              className="dia"
              aria-pressed={iso === data}
              onClick={() => setData(iso)}
            >
              <span className="dia__semana">{diaSemanaCurto(d)}</span>
              <span className="dia__data">{diaMesCurto(d)}</span>
            </button>
          )
        })}
      </div>

      <h3 className="subtitulo">{data && dataPorExtenso(data)}</h3>

      {carregando && <p className="aviso">Buscando horários livres…</p>}
      {erro && (
        <p className="aviso aviso--alerta" role="alert">
          {erro}
        </p>
      )}
      {!carregando && !erro && horarios.length === 0 && (
        <p className="aviso">
          Sem horários livres neste dia para {servico.nome.toLowerCase()}. Escolha outro dia.
        </p>
      )}
      {!carregando && !erro && horarios.length > 0 && (
        <ul className="horarios">
          {horarios.map((h) => (
            <li key={h}>
              <button type="button" className="horario" onClick={() => onEscolher(data, h)}>
                {hora(h)}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
