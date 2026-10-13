import { useCallback, useState } from 'react'

/**
 * Erro das telas do painel: 401 encerra a sessão; o resto vira mensagem na tela.
 */
export function useErroPainel(onSessaoExpirada) {
  const [erro, setErro] = useState(null)

  const tratar = useCallback(
    (e) => {
      if (e.status === 401) onSessaoExpirada()
      else setErro(e.message)
    },
    [onSessaoExpirada],
  )

  return [erro, setErro, tratar]
}
