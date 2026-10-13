import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import Painel from './painel/Painel.jsx'
import './styles.css'
import './painel/painel.css'

// Rotas por "#": #/painel abre a área da equipe; o resto é a página de agendamento.
// Usar o hash evita configurar redirecionamentos no servidor ao publicar.
function Raiz() {
  const [hash, setHash] = useState(window.location.hash)

  useEffect(() => {
    const aoMudar = () => setHash(window.location.hash)
    window.addEventListener('hashchange', aoMudar)
    return () => window.removeEventListener('hashchange', aoMudar)
  }, [])

  return hash.startsWith('#/painel') ? <Painel /> : <App />
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Raiz />
  </StrictMode>,
)
