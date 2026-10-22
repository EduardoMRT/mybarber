import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'
import type { AdminDashboard as Dto, Cobranca } from '../../api/types'
import { Cabecalho, Carregando, ErroCarga, Selo, Vazio } from '../../componentes/Basicos'
import { data, moeda } from '../../util/formato'

export function AdminDashboard() {
  const d = useQuery({ queryKey: ['admin-dashboard'], queryFn: () => api.get<Dto>('/api/admin/dashboard') })
  const atrasadas = useQuery({ queryKey: ['cobrancas', 'ATRASADA'], queryFn: () => api.get<Cobranca[]>('/api/admin/cobrancas?status=ATRASADA') })

  if (d.isLoading) return <Carregando />
  if (d.error || !d.data) return <ErroCarga erro={d.error} />
  const x = d.data
  const [ano, mes] = x.competencia.split('-')

  return (
    <>
      <Cabecalho titulo="Visão geral" subtitulo={`Plataforma MyBarber em ${mes}/${ano}.`} />
      <div className="conteudo">
        <dl className="numeros">
          <div>
            <dt>Receita mensal prevista</dt>
            <dd>
              {moeda(x.receitaMensalPrevista)}
              <small>{x.ativas} barbearias ativas</small>
            </dd>
          </div>
          <div>
            <dt>Recebido no mês</dt>
            <dd>
              {moeda(x.recebidoNoMes)}
              <small>{moeda(x.emAbertoNoMes)} ainda em aberto</small>
            </dd>
          </div>
          <div>
            <dt>Em atraso</dt>
            <dd style={{ color: x.cobrancasAtrasadas ? 'var(--st-cancelado)' : undefined }}>
              {moeda(x.valorAtrasado)}
              <small>{x.cobrancasAtrasadas} mensalidades</small>
            </dd>
          </div>
          <div>
            <dt>Barbearias</dt>
            <dd>
              {x.totalBarbearias}
              <small>{x.bloqueadas} bloqueadas</small>
            </dd>
          </div>
          <div>
            <dt>Clientes no app</dt>
            <dd>
              {x.totalClientes}
              <small>{x.agendamentosNoMes} agendamentos no mês</small>
            </dd>
          </div>
        </dl>

        <section className="painel">
          <div className="painel-cabeca">
            <h2>Mensalidades atrasadas</h2>
            <Link className="btn btn-pequeno" to="/admin/cobrancas">
              Ver todas as mensalidades
            </Link>
          </div>
          {!atrasadas.data?.length ? (
            <Vazio>Nenhuma mensalidade atrasada.</Vazio>
          ) : (
            <div className="tabela-envelope">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Barbearia</th>
                    <th>Mês</th>
                    <th>Vencimento</th>
                    <th className="direita">Valor</th>
                    <th>Situação</th>
                  </tr>
                </thead>
                <tbody>
                  {atrasadas.data.map((c) => (
                    <tr key={c.id}>
                      <td>{c.barbeariaNome}</td>
                      <td className="numero">{c.competencia.split('-').reverse().join('/')}</td>
                      <td className="numero">{data(c.vencimento)}</td>
                      <td className="direita numero">{moeda(c.valor)}</td>
                      <td>
                        <Selo status={c.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </>
  )
}
