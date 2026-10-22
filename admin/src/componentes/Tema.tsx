import { useEffect, useState } from 'react'
import { Moon, Sun, SunMoon } from 'lucide-react'

type Tema = 'sistema' | 'claro' | 'escuro'
const CHAVE = 'mybarber-tema'

function ler(): Tema {
  try {
    const t = localStorage.getItem(CHAVE)
    return t === 'claro' || t === 'escuro' ? t : 'sistema'
  } catch {
    return 'sistema'
  }
}

export function BotaoTema() {
  const [tema, setTema] = useState<Tema>(ler)

  useEffect(() => {
    const raiz = document.documentElement
    if (tema === 'sistema') delete raiz.dataset.theme
    else raiz.dataset.theme = tema === 'claro' ? 'light' : 'dark'
    try {
      localStorage.setItem(CHAVE, tema)
    } catch {
      /* sem armazenamento */
    }
  }, [tema])

  const proximo: Record<Tema, Tema> = { sistema: 'claro', claro: 'escuro', escuro: 'sistema' }
  const rotulo: Record<Tema, string> = { sistema: 'Tema do sistema', claro: 'Tema claro', escuro: 'Tema escuro' }
  const Icone = tema === 'claro' ? Sun : tema === 'escuro' ? Moon : SunMoon

  return (
    <button onClick={() => setTema(proximo[tema])} aria-label={`${rotulo[tema]}. Trocar tema`}>
      <Icone size={18} aria-hidden />
      {rotulo[tema]}
    </button>
  )
}
