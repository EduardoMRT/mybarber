import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

interface Base {
  rotulo: string
  ajuda?: ReactNode
  erro?: string
}

export function Campo({ rotulo, ajuda, erro, ...props }: Base & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="campo">
      <span>{rotulo}</span>
      <input aria-invalid={!!erro} {...props} />
      {ajuda && !erro && <small>{ajuda}</small>}
      {erro && <small className="erro">{erro}</small>}
    </label>
  )
}

export function Selecao({ rotulo, ajuda, erro, children, ...props }: Base & SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <label className="campo">
      <span>{rotulo}</span>
      <select aria-invalid={!!erro} {...props}>
        {children}
      </select>
      {ajuda && !erro && <small>{ajuda}</small>}
      {erro && <small className="erro">{erro}</small>}
    </label>
  )
}

export function Texto({ rotulo, ajuda, erro, ...props }: Base & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <label className="campo">
      <span>{rotulo}</span>
      <textarea aria-invalid={!!erro} {...props} />
      {ajuda && !erro && <small>{ajuda}</small>}
      {erro && <small className="erro">{erro}</small>}
    </label>
  )
}

/** Seleção múltipla em fichas (tipos de cabelo, serviços aceitos...). */
export function Fichas<T extends number>({
  opcoes,
  valor,
  aoMudar,
  rotulo,
  ajuda,
}: {
  opcoes: { id: T; nome: string }[]
  valor: T[]
  aoMudar: (v: T[]) => void
  rotulo: string
  ajuda?: string
}) {
  const alternar = (id: T) => aoMudar(valor.includes(id) ? valor.filter((x) => x !== id) : [...valor, id])
  return (
    <div className="campo" role="group" aria-label={rotulo}>
      <span>{rotulo}</span>
      <div className="fichas">
        {opcoes.map((o) => (
          <button type="button" key={o.id} className="ficha" aria-pressed={valor.includes(o.id)} onClick={() => alternar(o.id)}>
            {o.nome}
          </button>
        ))}
      </div>
      {ajuda && <small>{ajuda}</small>}
    </div>
  )
}
