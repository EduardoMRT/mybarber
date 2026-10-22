import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { Assinatura as AssinaturaDto } from '../../api/types'
import { Cabecalho, Carregando, ErroCarga, Selo, Vazio } from '../../componentes/Basicos'
import { data, formasPagamento, moeda } from '../../util/formato'

export function Assinatura() {
  const a = useQuery({ queryKey: ['assinatura'], queryFn: () => api.get<AssinaturaDto>('/api/painel/assinatura') })
  if (a.isLoading) return <Carregando />
  if (a.error || !a.data) return <ErroCarga erro={a.error} />
  const d = a.data
  return (
    <>
      <Cabecalho titulo="Assinatura" subtitulo="O app é gratuito para os clientes. A mensalidade é paga pela barbearia." />
      <div className="conteudo">
        <dl className="numeros">
          <div>
            <dt>Situação</dt>
            <dd style={{ fontSize: 18 }}>
              <Selo status={d.status} />
              {d.motivoBloqueio && <small>{d.motivoBloqueio}</small>}
            </dd>
          </div>
          <div>
            <dt>Mensalidade</dt>
            <dd>{d.valorMensalidade != null ? moeda(d.valorMensalidade) : '—'}</dd>
          </div>
          <div>
            <dt>Vencimento</dt>
            <dd>
              {d.diaVencimento ? `Dia ${d.diaVencimento}` : '—'}
              <small>Bloqueio após {d.diasTolerancia} dias de atraso</small>
            </dd>
          </div>
        </dl>
        <section className="painel">
          <div className="painel-cabeca">
            <h2>Mensalidades</h2>
          </div>
          {d.cobrancas.length === 0 ? (
            <Vazio>Nenhuma mensalidade gerada ainda.</Vazio>
          ) : (
            <div className="tabela-envelope">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Mês</th>
                    <th>Vencimento</th>
                    <th className="direita">Valor</th>
                    <th>Situação</th>
                    <th>Pagamento</th>
                  </tr>
                </thead>
                <tbody>
                  {d.cobrancas.map((c) => (
                    <tr key={c.id}>
                      <td className="numero">{c.competencia.split('-').reverse().join('/')}</td>
                      <td className="numero">{data(c.vencimento)}</td>
                      <td className="direita numero">{moeda(c.valor)}</td>
                      <td>
                        <Selo status={c.status} />
                      </td>
                      <td className="pequeno">
                        {c.pagoEm ? `${data(c.pagoEm)}${c.formaPagamento ? `, ${formasPagamento[c.formaPagamento]}` : ''}` : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
        <p className="mudo pequeno">Para pagar ou tirar dúvidas sobre a mensalidade, fale com o suporte do MyBarber.</p>
      </div>
    </>
  )
}
