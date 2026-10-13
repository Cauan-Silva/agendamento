import { useState } from 'react'
import { dataPorExtenso, hora, mascaraTelefone, preco } from '../utils/formato.js'

export default function DadosCliente({ servico, profissional, data, horario, enviando, erros, onConfirmar }) {
  const [nome, setNome] = useState('')
  const [telefone, setTelefone] = useState('')

  const numeros = telefone.replace(/\D/g, '')
  const valido = nome.trim().length > 1 && numeros.length >= 10

  function enviar(evento) {
    evento.preventDefault()
    if (valido && !enviando) {
      onConfirmar({ clienteNome: nome.trim(), clienteTelefone: numeros })
    }
  }

  return (
    <section>
      <h2 className="titulo-etapa">Quase lá</h2>

      <dl className="resumo">
        <div>
          <dt>Serviço</dt>
          <dd>
            {servico.nome}, {servico.duracaoMinutos} min, {preco(servico.preco)}
          </dd>
        </div>
        <div>
          <dt>Com</dt>
          <dd>{profissional.nome}</dd>
        </div>
        <div>
          <dt>Quando</dt>
          <dd>
            {dataPorExtenso(data)}, às {hora(horario)}
          </dd>
        </div>
      </dl>

      <form className="formulario" onSubmit={enviar} noValidate>
        <label className="campo">
          <span>Seu nome</span>
          <input
            type="text"
            autoComplete="name"
            value={nome}
            maxLength={100}
            onChange={(e) => setNome(e.target.value)}
            aria-invalid={Boolean(erros.clienteNome)}
          />
          {erros.clienteNome && <small className="campo__erro">{erros.clienteNome}</small>}
        </label>

        <label className="campo">
          <span>Celular com DDD</span>
          <input
            type="tel"
            inputMode="numeric"
            autoComplete="tel-national"
            placeholder="(48) 99999-0000"
            value={telefone}
            onChange={(e) => setTelefone(mascaraTelefone(e.target.value))}
            aria-invalid={Boolean(erros.clienteTelefone)}
          />
          {erros.clienteTelefone && <small className="campo__erro">{erros.clienteTelefone}</small>}
        </label>

        <button type="submit" className="botao-principal" disabled={!valido || enviando}>
          {enviando ? 'Confirmando…' : 'Confirmar agendamento'}
        </button>
      </form>
    </section>
  )
}
