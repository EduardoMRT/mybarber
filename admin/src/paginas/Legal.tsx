import { Fragment, type ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { DocumentoLegal } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Carregando, ErroCarga } from '../componentes/Basicos'

/** Markdown simples (títulos, listas, tabelas e negrito) dos documentos legais. */
function inline(texto: string): ReactNode[] {
  return texto.split(/(\*\*[^*]+\*\*)/g).map((p, i) =>
    p.startsWith('**') && p.endsWith('**') ? <strong key={i}>{p.slice(2, -2)}</strong> : <Fragment key={i}>{p}</Fragment>,
  )
}

function Markdown({ texto }: { texto: string }) {
  const blocos = texto.trim().split(/\n\s*\n/)
  return (
    <>
      {blocos.map((b, i) => {
        const linhas = b.split('\n')
        if (b.startsWith('# ')) return <h1 key={i}>{b.slice(2)}</h1>
        if (b.startsWith('## ')) return <h2 key={i}>{b.slice(3)}</h2>
        if (linhas.every((l) => l.startsWith('- '))) {
          return (
            <ul key={i}>
              {linhas.map((l, k) => (
                <li key={k}>{inline(l.slice(2))}</li>
              ))}
            </ul>
          )
        }
        if (linhas[0].startsWith('|')) {
          const celulas = (l: string) => l.split('|').slice(1, -1).map((c) => c.trim())
          const [cab, , ...corpo] = linhas
          return (
            <table key={i}>
              <thead>
                <tr>
                  {celulas(cab).map((c, k) => (
                    <th key={k}>{inline(c)}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {corpo.map((l, k) => (
                  <tr key={k}>
                    {celulas(l).map((c, j) => (
                      <td key={j}>{inline(c)}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          )
        }
        return <p key={i}>{inline(linhas.join(' '))}</p>
      })}
    </>
  )
}

const documentos = [
  ['termos-barbearia', 'Termos para barbearias'],
  ['politica-de-privacidade', 'Política de privacidade'],
  ['termos-de-uso', 'Termos de uso do app'],
  ['lgpd', 'Direitos do titular (LGPD)'],
]

export function Legal() {
  const { slug = 'politica-de-privacidade' } = useParams()
  const { usuario } = useAuth()
  const doc = useQuery({ queryKey: ['legal', slug], queryFn: () => api.get<DocumentoLegal>(`/api/public/legal/${slug}`) })

  return (
    <div className="conteudo" style={{ paddingTop: 28 }}>
      {!usuario && (
        <p>
          <Link to="/login">Voltar para o login</Link>
        </p>
      )}
      <nav className="fichas" aria-label="Documentos">
        {documentos.map(([s, t]) => (
          <Link key={s} to={`/legal/${s}`} className="ficha" aria-current={s === slug ? 'page' : undefined} style={{ textDecoration: 'none' }}>
            {t}
          </Link>
        ))}
      </nav>
      <article className="painel painel-corpo documento">
        {doc.isLoading ? <Carregando /> : doc.error || !doc.data?.conteudo ? <ErroCarga erro={doc.error} /> : <Markdown texto={doc.data.conteudo} />}
      </article>
    </div>
  )
}
