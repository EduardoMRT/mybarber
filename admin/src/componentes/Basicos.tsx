import type { ReactNode } from 'react'
import { statusAgendamento, statusCobranca } from '../util/formato'
import type { StatusAgendamento, StatusBarbearia, StatusCobranca, TipoLancamento } from '../api/types'

export function Selo({ status }: { status: StatusAgendamento | StatusCobranca | StatusBarbearia | TipoLancamento }) {
  const textos: Record<string, string> = {
    ...statusAgendamento,
    ...statusCobranca,
    ATIVA: 'Ativa',
    BLOQUEADA: 'Bloqueada',
    RECEITA: 'Entrada',
    DESPESA: 'Saída',
  }
  return <span className={`selo selo-${status}`}>{textos[status] ?? status}</span>
}

export function Vazio({ children, acao }: { children: ReactNode; acao?: ReactNode }) {
  return (
    <div className="vazio">
      <p>{children}</p>
      {acao}
    </div>
  )
}

export function Carregando() {
  return (
    <div className="carregando" role="status">
      Carregando…
    </div>
  )
}

export function ErroCarga({ erro, tentar }: { erro: unknown; tentar?: () => void }) {
  return (
    <div className="vazio" role="alert">
      <p>{erro instanceof Error ? erro.message : 'Não foi possível carregar os dados.'}</p>
      {tentar && (
        <button className="btn" onClick={tentar}>
          Tentar de novo
        </button>
      )}
    </div>
  )
}

export function Cabecalho({ titulo, subtitulo, children }: { titulo: string; subtitulo?: ReactNode; children?: ReactNode }) {
  return (
    <header className="topo">
      <div className="topo-titulo">
        <h1>{titulo}</h1>
        {subtitulo && <p className="mudo">{subtitulo}</p>}
      </div>
      {children && <div className="topo-acoes">{children}</div>}
    </header>
  )
}
