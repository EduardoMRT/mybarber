import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, query } from '../../api/client'
import type { Barbearia, Cobranca, FormaPagamento, StatusCobranca } from '../../api/types'
import { Cabecalho, Carregando, ErroCarga, Selo, Vazio } from '../../componentes/Basicos'
import { Campo, Selecao } from '../../componentes/Campo'
import { useAvisos } from '../../componentes/Avisos'
import { Modal } from '../../componentes/Modal'
import { data, formasPagamento, isoData, moeda, statusCobranca } from '../../util/formato'

const mesAtual = () => isoData(new Date()).slice(0, 7)

export function Cobrancas() {
  const qc = useQueryClient()
  const avisar = useAvisos()
  const [status, setStatus] = useState<'' | StatusCobranca>('')
  const [competencia, setCompetencia] = useState('')
  const [gerar, setGerar] = useState(false)
  const [mesGerar, setMesGerar] = useState(mesAtual())
  const [nova, setNova] = useState(false)
  const [nf, setNf] = useState({ barbeariaId: '', competencia: mesAtual(), valor: '', vencimento: '', observacao: '' })
  const [pagando, setPagando] = useState<Cobranca | null>(null)
  const [pg, setPg] = useState({ pagoEm: isoData(new Date()), formaPagamento: 'PIX' as FormaPagamento | '' })

  const lista = useQuery({
    queryKey: ['cobrancas', status, competencia],
    queryFn: () => api.get<Cobranca[]>(`/api/admin/cobrancas${query({ status, competencia })}`),
  })
  const barbearias = useQuery({
    queryKey: ['admin-barbearias', '', ''],
    queryFn: () => api.get<Barbearia[]>('/api/admin/barbearias'),
    enabled: nova,
  })

  const atualizar = () => {
    qc.invalidateQueries({ queryKey: ['cobrancas'] })
    qc.invalidateQueries({ queryKey: ['admin-dashboard'] })
    qc.invalidateQueries({ queryKey: ['admin-barbearias'] })
  }
  const erro = (e: Error) => avisar(e.message, true)

  const gerarMes = useMutation({
    mutationFn: () => api.post<{ geradas: number }>('/api/admin/cobrancas/gerar', { competencia: mesGerar }),
    onSuccess: (r) => {
      atualizar()
      avisar(r.geradas ? `${r.geradas} mensalidade(s) gerada(s).` : 'Todas as barbearias já tinham a mensalidade deste mês.')
      setGerar(false)
    },
    onError: erro,
  })
  const criar = useMutation({
    mutationFn: () =>
      api.post('/api/admin/cobrancas', {
        barbeariaId: Number(nf.barbeariaId),
        competencia: nf.competencia,
        valor: Number(nf.valor.replace(',', '.')),
        vencimento: nf.vencimento,
        observacao: nf.observacao || null,
      }),
    onSuccess: () => {
      atualizar()
      avisar('Mensalidade criada.')
      setNova(false)
    },
    onError: erro,
  })
  const pagar = useMutation({
    mutationFn: () => api.post(`/api/admin/cobrancas/${pagando!.id}/pagar`, { pagoEm: pg.pagoEm, formaPagamento: pg.formaPagamento || null }),
    onSuccess: () => {
      atualizar()
      avisar('Pagamento registrado. Se a barbearia estava bloqueada por atraso, ela foi liberada.')
      setPagando(null)
    },
    onError: erro,
  })
  const acao = useMutation({
    mutationFn: ({ id, tipo }: { id: number; tipo: 'reabrir' | 'cancelar' }) => api.post(`/api/admin/cobrancas/${id}/${tipo}`),
    onSuccess: (_d, v) => {
      atualizar()
      avisar(v.tipo === 'reabrir' ? 'Mensalidade reaberta.' : 'Mensalidade cancelada.')
    },
    onError: erro,
  })

  const total = (lista.data ?? []).reduce((s, c) => s + Number(c.valor), 0)

  return (
    <>
      <Cabecalho titulo="Mensalidades" subtitulo="Geradas todo mês automaticamente para as barbearias com valor e vencimento definidos.">
        <button className="btn" onClick={() => setNova(true)}>
          Mensalidade avulsa
        </button>
        <button className="btn btn-primario" onClick={() => setGerar(true)}>
          Gerar mês
        </button>
      </Cabecalho>
      <div className="conteudo">
        <div className="barra-filtros">
          <div className="fichas" role="group" aria-label="Situação">
            {([['', 'Todas'], ...Object.entries(statusCobranca)] as [string, string][]).map(([v, r]) => (
              <button key={v} className="ficha" aria-pressed={status === v} onClick={() => setStatus(v as '' | StatusCobranca)}>
                {r}
              </button>
            ))}
          </div>
          <label className="campo" style={{ minWidth: 0 }}>
            <span className="sr-only">Mês</span>
            <input type="month" aria-label="Mês" value={competencia} onChange={(e) => setCompetencia(e.target.value)} />
          </label>
          {competencia && (
            <button className="btn btn-fantasma btn-pequeno" onClick={() => setCompetencia('')}>
              Todos os meses
            </button>
          )}
        </div>
        <section className="painel">
          <div className="painel-cabeca">
            <h2>{lista.data?.length ?? 0} mensalidades</h2>
            <span className="numero mudo">{moeda(total)}</span>
          </div>
          {lista.isLoading ? (
            <Carregando />
          ) : lista.error ? (
            <ErroCarga erro={lista.error} />
          ) : !lista.data?.length ? (
            <Vazio>Nenhuma mensalidade com esses filtros.</Vazio>
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
                    <th>Pagamento</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {lista.data.map((c) => (
                    <tr key={c.id}>
                      <td>
                        {c.barbeariaNome}
                        {c.observacao && <div className="mudo pequeno">{c.observacao}</div>}
                      </td>
                      <td className="numero">{c.competencia.split('-').reverse().join('/')}</td>
                      <td className="numero">{data(c.vencimento)}</td>
                      <td className="direita numero">{moeda(c.valor)}</td>
                      <td>
                        <Selo status={c.status} />
                      </td>
                      <td className="pequeno">
                        {c.pagoEm ? `${data(c.pagoEm)}${c.formaPagamento ? `, ${formasPagamento[c.formaPagamento]}` : ''}` : '—'}
                      </td>
                      <td className="direita" style={{ whiteSpace: 'nowrap' }}>
                        {(c.status === 'PENDENTE' || c.status === 'ATRASADA') && (
                          <>
                            <button className="btn btn-primario btn-pequeno" onClick={() => setPagando(c)}>
                              Registrar pagamento
                            </button>{' '}
                            <button className="btn btn-pequeno" onClick={() => acao.mutate({ id: c.id, tipo: 'cancelar' })}>
                              Cancelar
                            </button>
                          </>
                        )}
                        {(c.status === 'PAGA' || c.status === 'CANCELADA') && (
                          <button className="btn btn-pequeno" onClick={() => acao.mutate({ id: c.id, tipo: 'reabrir' })}>
                            Reabrir
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      <Modal titulo="Gerar mensalidades do mês" aberto={gerar} aoFechar={() => setGerar(false)}>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            gerarMes.mutate()
          }}
        >
          <p className="mudo">
            Cria a mensalidade de cada barbearia que tem valor e dia de vencimento definidos. Quem já tem a do mês não recebe outra.
            A rotina diária já faz isso sozinha no começo de cada mês.
          </p>
          <Campo rotulo="Mês" type="month" required value={mesGerar} onChange={(e) => setMesGerar(e.target.value)} />
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setGerar(false)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={gerarMes.isPending}>
              Gerar mensalidades
            </button>
          </div>
        </form>
      </Modal>

      <Modal titulo="Mensalidade avulsa" aberto={nova} aoFechar={() => setNova(false)}>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            criar.mutate()
          }}
        >
          <Selecao rotulo="Barbearia" required value={nf.barbeariaId} onChange={(e) => setNf({ ...nf, barbeariaId: e.target.value })}>
            <option value="">Escolha</option>
            {(barbearias.data ?? []).map((b) => (
              <option key={b.id} value={b.id}>
                {b.nome}
              </option>
            ))}
          </Selecao>
          <div className="linha-form">
            <Campo rotulo="Mês" type="month" required value={nf.competencia} onChange={(e) => setNf({ ...nf, competencia: e.target.value })} />
            <Campo rotulo="Valor (R$)" required inputMode="decimal" value={nf.valor} onChange={(e) => setNf({ ...nf, valor: e.target.value })} />
            <Campo rotulo="Vencimento" type="date" required value={nf.vencimento} onChange={(e) => setNf({ ...nf, vencimento: e.target.value })} />
          </div>
          <Campo rotulo="Observação" maxLength={255} value={nf.observacao} onChange={(e) => setNf({ ...nf, observacao: e.target.value })} />
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setNova(false)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={criar.isPending}>
              Criar mensalidade
            </button>
          </div>
        </form>
      </Modal>

      <Modal titulo="Registrar pagamento" aberto={!!pagando} aoFechar={() => setPagando(null)}>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            pagar.mutate()
          }}
        >
          <p>
            {pagando?.barbeariaNome}, mês {pagando?.competencia.split('-').reverse().join('/')}: <strong className="numero">{moeda(pagando?.valor)}</strong>
          </p>
          <div className="linha-form">
            <Campo rotulo="Pago em" type="date" required value={pg.pagoEm} onChange={(e) => setPg({ ...pg, pagoEm: e.target.value })} />
            <Selecao rotulo="Forma de pagamento" value={pg.formaPagamento} onChange={(e) => setPg({ ...pg, formaPagamento: e.target.value as FormaPagamento })}>
              <option value="">Não informar</option>
              {Object.entries(formasPagamento).map(([k, v]) => (
                <option key={k} value={k}>
                  {v}
                </option>
              ))}
            </Selecao>
          </div>
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setPagando(null)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={pagar.isPending}>
              Registrar pagamento
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
