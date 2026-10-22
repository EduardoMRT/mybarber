import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { api, query } from '../../api/client'
import type { Extrato, FormaPagamento, Lancamento, TipoLancamento } from '../../api/types'
import { Cabecalho, Carregando, ErroCarga, Selo, Vazio } from '../../componentes/Basicos'
import { Campo, Selecao } from '../../componentes/Campo'
import { useAvisos } from '../../componentes/Avisos'
import { Modal } from '../../componentes/Modal'
import { SeletorPeriodo, periodoMes, type Periodo } from '../../componentes/Periodo'
import { useBarbeiros } from '../../dados'
import { data, formasPagamento, isoData, moeda } from '../../util/formato'

const categorias: Record<TipoLancamento, string[]> = {
  RECEITA: ['Serviços', 'Venda de produtos', 'Outras entradas'],
  DESPESA: ['Aluguel', 'Produtos e insumos', 'Energia', 'Água', 'Internet e telefone', 'Comissões', 'Impostos', 'Manutenção', 'Divulgação', 'Outras saídas'],
}

type Form = { tipo: TipoLancamento; categoria: string; descricao: string; valor: string; data: string; formaPagamento: string; barbeiroId: string }

export function Financeiro() {
  const [periodo, setPeriodo] = useState<Periodo>(periodoMes(0))
  const [filtro, setFiltro] = useState<'' | TipoLancamento>('')
  const [editando, setEditando] = useState<Lancamento | 'novo' | null>(null)
  const [f, setF] = useState<Form>({ tipo: 'DESPESA', categoria: '', descricao: '', valor: '', data: isoData(new Date()), formaPagamento: '', barbeiroId: '' })
  const qc = useQueryClient()
  const avisar = useAvisos()
  const barbeiros = useBarbeiros()

  const extrato = useQuery({
    queryKey: ['extrato', periodo],
    queryFn: () => api.get<Extrato>(`/api/painel/financeiro/lancamentos${query({ de: periodo.de, ate: periodo.ate })}`),
  })

  const abrir = (l: Lancamento | 'novo', tipo: TipoLancamento = 'DESPESA') => {
    setEditando(l)
    setF(
      l === 'novo'
        ? { tipo, categoria: '', descricao: '', valor: '', data: isoData(new Date()), formaPagamento: '', barbeiroId: '' }
        : {
            tipo: l.tipo,
            categoria: l.categoria,
            descricao: l.descricao ?? '',
            valor: String(l.valor),
            data: l.data,
            formaPagamento: l.formaPagamento ?? '',
            barbeiroId: l.barbeiroId ? String(l.barbeiroId) : '',
          },
    )
  }

  const invalidar = () => {
    qc.invalidateQueries({ queryKey: ['extrato'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const salvar = useMutation({
    mutationFn: () => {
      const corpo = {
        tipo: f.tipo,
        categoria: f.categoria,
        descricao: f.descricao || null,
        valor: Number(f.valor.replace(',', '.')),
        data: f.data,
        formaPagamento: (f.formaPagamento || null) as FormaPagamento | null,
        barbeiroId: f.barbeiroId ? Number(f.barbeiroId) : null,
      }
      return editando === 'novo'
        ? api.post('/api/painel/financeiro/lancamentos', corpo)
        : api.put(`/api/painel/financeiro/lancamentos/${(editando as Lancamento).id}`, corpo)
    },
    onSuccess: () => {
      invalidar()
      avisar('Lançamento salvo.')
      setEditando(null)
    },
    onError: (e) => avisar(e.message, true),
  })

  const excluir = useMutation({
    mutationFn: (id: number) => api.del(`/api/painel/financeiro/lancamentos/${id}`),
    onSuccess: () => {
      invalidar()
      avisar('Lançamento excluído.')
      setEditando(null)
    },
    onError: (e) => avisar(e.message, true),
  })

  const lista = (extrato.data?.lancamentos ?? []).filter((l) => !filtro || l.tipo === filtro)

  return (
    <>
      <Cabecalho titulo="Financeiro" subtitulo="Atendimentos concluídos entram sozinhos. Lance aqui as outras entradas e as despesas.">
        <button className="btn" onClick={() => abrir('novo', 'RECEITA')}>
          <Plus size={18} aria-hidden /> Entrada
        </button>
        <button className="btn btn-primario" onClick={() => abrir('novo', 'DESPESA')}>
          <Plus size={18} aria-hidden /> Despesa
        </button>
      </Cabecalho>
      <div className="conteudo">
        <SeletorPeriodo valor={periodo} aoMudar={setPeriodo} />
        {extrato.data && (
          <dl className="numeros">
            <div>
              <dt>Entradas</dt>
              <dd>{moeda(extrato.data.receitas)}</dd>
            </div>
            <div>
              <dt>Saídas</dt>
              <dd>{moeda(extrato.data.despesas)}</dd>
            </div>
            <div>
              <dt>Saldo do período</dt>
              <dd style={{ color: extrato.data.saldo < 0 ? 'var(--st-cancelado)' : undefined }}>{moeda(extrato.data.saldo)}</dd>
            </div>
          </dl>
        )}
        <section className="painel">
          <div className="painel-cabeca">
            <h2>Lançamentos</h2>
            <div className="fichas" role="group" aria-label="Filtrar por tipo">
              {(
                [
                  ['', 'Todos'],
                  ['RECEITA', 'Entradas'],
                  ['DESPESA', 'Saídas'],
                ] as const
              ).map(([v, r]) => (
                <button key={v} className="ficha" aria-pressed={filtro === v} onClick={() => setFiltro(v)}>
                  {r}
                </button>
              ))}
            </div>
          </div>
          {extrato.isLoading ? (
            <Carregando />
          ) : extrato.error ? (
            <ErroCarga erro={extrato.error} />
          ) : lista.length === 0 ? (
            <Vazio>Nenhum lançamento no período.</Vazio>
          ) : (
            <div className="tabela-envelope">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Tipo</th>
                    <th>Categoria</th>
                    <th>Descrição</th>
                    <th>Pagamento</th>
                    <th className="direita">Valor</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {lista.map((l) => (
                    <tr key={l.id}>
                      <td className="numero">{data(l.data)}</td>
                      <td>
                        <Selo status={l.tipo} />
                      </td>
                      <td>{l.categoria}</td>
                      <td>
                        {l.descricao}
                        {l.barbeiroNome && <div className="mudo pequeno">{l.barbeiroNome}</div>}
                      </td>
                      <td>{l.formaPagamento ? formasPagamento[l.formaPagamento] : <span className="mudo">—</span>}</td>
                      <td className="direita numero">
                        {l.tipo === 'DESPESA' ? '−' : ''}
                        {moeda(l.valor)}
                      </td>
                      <td className="direita">
                        <button className="btn btn-pequeno" onClick={() => abrir(l)}>
                          Editar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      <Modal
        titulo={editando === 'novo' ? (f.tipo === 'RECEITA' ? 'Nova entrada' : 'Nova despesa') : 'Editar lançamento'}
        aberto={editando !== null}
        aoFechar={() => setEditando(null)}
      >
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            salvar.mutate()
          }}
        >
          <div className="linha-form">
            <Selecao rotulo="Tipo" value={f.tipo} onChange={(e) => setF({ ...f, tipo: e.target.value as TipoLancamento, categoria: '' })}>
              <option value="RECEITA">Entrada</option>
              <option value="DESPESA">Saída</option>
            </Selecao>
            <Campo rotulo="Data" type="date" required value={f.data} onChange={(e) => setF({ ...f, data: e.target.value })} />
          </div>
          <div className="linha-form">
            <Campo
              rotulo="Categoria"
              required
              list="categorias"
              maxLength={60}
              value={f.categoria}
              onChange={(e) => setF({ ...f, categoria: e.target.value })}
            />
            <datalist id="categorias">
              {categorias[f.tipo].map((c) => (
                <option key={c} value={c} />
              ))}
            </datalist>
            <Campo rotulo="Valor (R$)" required inputMode="decimal" value={f.valor} onChange={(e) => setF({ ...f, valor: e.target.value })} />
          </div>
          <Campo rotulo="Descrição" maxLength={255} value={f.descricao} onChange={(e) => setF({ ...f, descricao: e.target.value })} />
          <div className="linha-form">
            <Selecao rotulo="Forma de pagamento" value={f.formaPagamento} onChange={(e) => setF({ ...f, formaPagamento: e.target.value })}>
              <option value="">Não informar</option>
              {Object.entries(formasPagamento).map(([k, v]) => (
                <option key={k} value={k}>
                  {v}
                </option>
              ))}
            </Selecao>
            <Selecao rotulo="Barbeiro (opcional)" value={f.barbeiroId} onChange={(e) => setF({ ...f, barbeiroId: e.target.value })}>
              <option value="">Nenhum</option>
              {(barbeiros.data ?? []).map((b) => (
                <option key={b.id} value={b.id}>
                  {b.nome}
                </option>
              ))}
            </Selecao>
          </div>
          {editando !== 'novo' && editando?.agendamentoId && (
            <p className="mudo pequeno">Este lançamento veio de um atendimento concluído na agenda.</p>
          )}
          <div className="acoes-form">
            {editando !== 'novo' && editando && (
              <button type="button" className="btn btn-perigo" style={{ marginRight: 'auto' }} onClick={() => excluir.mutate(editando.id)}>
                Excluir
              </button>
            )}
            <button type="button" className="btn" onClick={() => setEditando(null)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={salvar.isPending}>
              Salvar lançamento
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
