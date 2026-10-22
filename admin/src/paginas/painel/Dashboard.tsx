import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, type TooltipProps } from 'recharts'
import { api, query } from '../../api/client'
import type { Agendamento, Dashboard as DashboardDto, ItemValor } from '../../api/types'
import { Cabecalho, Carregando, ErroCarga, Vazio } from '../../componentes/Basicos'
import { SeletorPeriodo, periodoMes, type Periodo } from '../../componentes/Periodo'
import { formasPagamento, hora, moeda, porcentagem } from '../../util/formato'
import { DetalheAgendamento } from './AgendaModais'

function Dica({ active, payload, label }: TooltipProps<number, string>) {
  if (!active || !payload?.length) return null
  const [a, m, d] = String(label).split('-')
  return (
    <div className="grafico-tooltip">
      <strong>{`${d}/${m}/${a}`}</strong>
      {payload.map((p) => (
        <div key={p.dataKey as string}>
          {p.dataKey === 'receitas' ? 'Entradas' : 'Saídas'}: <span className="numero">{moeda(p.value)}</span>
        </div>
      ))}
    </div>
  )
}

function Ranking({ titulo, itens, rotulo }: { titulo: string; itens: ItemValor[]; rotulo?: (n: string) => string }) {
  const maior = Math.max(...itens.map((i) => i.valor), 1)
  return (
    <section className="painel">
      <div className="painel-cabeca">
        <h2>{titulo}</h2>
      </div>
      <div className="painel-corpo">
        {itens.length === 0 ? (
          <p className="mudo">Nada no período.</p>
        ) : (
          <ul className="lista-simples ranking">
            {itens.slice(0, 6).map((i) => (
              <li key={i.nome}>
                <span>
                  {rotulo ? rotulo(i.nome) : i.nome} <span className="mudo pequeno">({i.quantidade})</span>
                </span>
                <span className="numero">{moeda(i.valor)}</span>
                <span className="ranking-barra" aria-hidden>
                  <i style={{ width: `${(i.valor / maior) * 100}%` }} />
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}

export function Dashboard() {
  const [periodo, setPeriodo] = useState<Periodo>(periodoMes(0))
  const [sel, setSel] = useState<Agendamento | null>(null)
  const dados = useQuery({
    queryKey: ['dashboard', periodo],
    queryFn: () => api.get<DashboardDto>(`/api/painel/dashboard${query({ de: periodo.de, ate: periodo.ate })}`),
  })
  const d = dados.data

  return (
    <>
      <Cabecalho titulo="Visão geral" subtitulo="Caixa, movimento e agenda de hoje." />
      <div className="conteudo">
        <SeletorPeriodo valor={periodo} aoMudar={setPeriodo} />
        {dados.isLoading ? (
          <Carregando />
        ) : dados.error || !d ? (
          <ErroCarga erro={dados.error} tentar={() => dados.refetch()} />
        ) : (
          <>
            <dl className="numeros">
              <div>
                <dt>Lucro</dt>
                <dd>
                  {moeda(d.lucro)}
                  <small>
                    {moeda(d.receitas)} entraram, {moeda(d.despesas)} saíram
                  </small>
                </dd>
              </div>
              <div>
                <dt>Atendimentos</dt>
                <dd>
                  {d.atendimentosConcluidos}
                  <small>{d.agendados} ainda agendados</small>
                </dd>
              </div>
              <div>
                <dt>Ticket médio</dt>
                <dd>{moeda(d.ticketMedio)}</dd>
              </div>
              <div>
                <dt>Agenda ocupada</dt>
                <dd>
                  {porcentagem(d.ocupacao)}
                  <small>do expediente</small>
                </dd>
              </div>
              <div>
                <dt>Faltas</dt>
                <dd>
                  {d.faltas}
                  <small>
                    {porcentagem(d.taxaFaltas)} dos atendimentos, {d.cancelados} cancelados
                  </small>
                </dd>
              </div>
            </dl>

            <div className="grade-3-1">
              <section className="painel">
                <div className="painel-cabeca">
                  <h2>Entradas e saídas por dia</h2>
                  <div className="grafico-legenda" aria-hidden>
                    <span>
                      <i style={{ background: 'var(--serie-1)' }} /> Entradas
                    </span>
                    <span>
                      <i style={{ background: 'var(--serie-2)' }} /> Saídas
                    </span>
                  </div>
                </div>
                <div className="grafico" role="img" aria-label={`Entradas de ${moeda(d.receitas)} e saídas de ${moeda(d.despesas)} no período`}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={d.porDia} barGap={2} barCategoryGap="20%">
                      <CartesianGrid vertical={false} stroke="var(--grade-grafico)" />
                      <XAxis
                        dataKey="data"
                        tickFormatter={(v: string) => v.slice(8, 10)}
                        tick={{ fill: 'var(--tinta-3)', fontSize: 12 }}
                        axisLine={false}
                        tickLine={false}
                        interval="preserveStartEnd"
                        minTickGap={12}
                      />
                      <YAxis
                        tickFormatter={(v: number) => (v >= 1000 ? `${Math.round(v / 100) / 10} mil` : String(v))}
                        tick={{ fill: 'var(--tinta-3)', fontSize: 12 }}
                        axisLine={false}
                        tickLine={false}
                        width={52}
                      />
                      <Tooltip content={<Dica />} cursor={{ fill: 'var(--superficie-2)' }} />
                      <Bar dataKey="receitas" fill="var(--serie-1)" radius={[4, 4, 0, 0]} maxBarSize={18} />
                      <Bar dataKey="despesas" fill="var(--serie-2)" radius={[4, 4, 0, 0]} maxBarSize={18} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </section>

              <section className="painel">
                <div className="painel-cabeca">
                  <h2>Hoje</h2>
                </div>
                {d.proximosHoje.length === 0 ? (
                  <Vazio>Nenhum atendimento pela frente hoje.</Vazio>
                ) : (
                  <ul className="lista-simples painel-corpo">
                    {d.proximosHoje.map((a) => (
                      <li key={a.id}>
                        <button className="btn btn-fantasma btn-pequeno" style={{ padding: 0 }} onClick={() => setSel(a)}>
                          <strong className="numero">{hora(a.inicio)}</strong>&nbsp;{a.clienteNome}
                        </button>
                        <span className="mudo pequeno">{a.servicoNome}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </div>

            <div className="grade-2">
              <Ranking titulo="Serviços mais feitos" itens={d.porServico} />
              <Ranking titulo="Por barbeiro" itens={d.porBarbeiro} />
              <Ranking titulo="Despesas por categoria" itens={d.despesasPorCategoria} />
              <Ranking
                titulo="Como os clientes pagaram"
                itens={d.formasPagamento}
                rotulo={(n) => formasPagamento[n as keyof typeof formasPagamento] ?? 'Não informado'}
              />
            </div>
          </>
        )}
      </div>
      <DetalheAgendamento ag={sel} aoFechar={() => setSel(null)} />
    </>
  )
}
