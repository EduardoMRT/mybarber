import { isoData } from '../util/formato'

export interface Periodo {
  de: string
  ate: string
}

export function periodoMes(deslocamento = 0): Periodo {
  const h = new Date()
  const ini = new Date(h.getFullYear(), h.getMonth() + deslocamento, 1)
  const fim = new Date(h.getFullYear(), h.getMonth() + deslocamento + 1, 0)
  return { de: isoData(ini), ate: isoData(fim) }
}

const opcoes: { rotulo: string; p: () => Periodo }[] = [
  { rotulo: 'Este mês', p: () => periodoMes(0) },
  { rotulo: 'Mês passado', p: () => periodoMes(-1) },
  {
    rotulo: 'Últimos 7 dias',
    p: () => {
      const h = new Date()
      const i = new Date(h)
      i.setDate(h.getDate() - 6)
      return { de: isoData(i), ate: isoData(h) }
    },
  },
]

/** Filtro de período: atalhos + datas livres, numa linha acima do conteúdo. */
export function SeletorPeriodo({ valor, aoMudar }: { valor: Periodo; aoMudar: (p: Periodo) => void }) {
  return (
    <div className="barra-filtros">
      <div className="fichas" role="group" aria-label="Período">
        {opcoes.map((o) => {
          const p = o.p()
          const ativo = p.de === valor.de && p.ate === valor.ate
          return (
            <button key={o.rotulo} type="button" className="ficha" aria-pressed={ativo} onClick={() => aoMudar(p)}>
              {o.rotulo}
            </button>
          )
        })}
      </div>
      <label className="campo" style={{ minWidth: 0 }}>
        <span className="sr-only">De</span>
        <input type="date" value={valor.de} onChange={(e) => e.target.value && aoMudar({ ...valor, de: e.target.value })} aria-label="Data inicial" />
      </label>
      <label className="campo" style={{ minWidth: 0 }}>
        <span className="sr-only">Até</span>
        <input type="date" value={valor.ate} onChange={(e) => e.target.value && aoMudar({ ...valor, ate: e.target.value })} aria-label="Data final" />
      </label>
    </div>
  )
}
