import { useEffect, useId, useRef, type ReactNode } from 'react'
import { X } from 'lucide-react'

interface Props {
  titulo: string
  aberto: boolean
  aoFechar: () => void
  children: ReactNode
  rodape?: ReactNode
  largo?: boolean
}

export function Modal({ titulo, aberto, aoFechar, children, rodape, largo }: Props) {
  const id = useId()
  const ref = useRef<HTMLDivElement>(null)
  // guarda a função de fechar sem reexecutar o efeito a cada render
  // (antes o foco voltava para o primeiro campo a cada tecla digitada)
  const fecharRef = useRef(aoFechar)
  useEffect(() => {
    fecharRef.current = aoFechar
  }, [aoFechar])

  // roda só quando a janela abre ou fecha
  useEffect(() => {
    if (!aberto) return
    const anterior = document.activeElement as HTMLElement | null
    const tecla = (e: KeyboardEvent) => {
      if (e.key === 'Escape') fecharRef.current()
    }
    document.addEventListener('keydown', tecla)
    const primeiro = ref.current?.querySelector<HTMLElement>('input, select, textarea, button:not([data-fechar])')
    primeiro?.focus()
    return () => {
      document.removeEventListener('keydown', tecla)
      anterior?.focus()
    }
  }, [aberto])

  if (!aberto) return null
  return (
    <div className="modal-fundo" onMouseDown={(e) => e.target === e.currentTarget && aoFechar()}>
      <div ref={ref} className={largo ? 'modal modal-largo' : 'modal'} role="dialog" aria-modal="true" aria-labelledby={id}>
        <div className="modal-cabeca">
          <h2 id={id}>{titulo}</h2>
          <button className="btn btn-fantasma btn-icone" onClick={aoFechar} aria-label="Fechar" data-fechar>
            <X size={18} />
          </button>
        </div>
        <div className="modal-corpo">{children}</div>
        {rodape && <div className="modal-rodape">{rodape}</div>}
      </div>
    </div>
  )
}
