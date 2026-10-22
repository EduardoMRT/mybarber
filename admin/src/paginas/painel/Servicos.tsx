import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { api } from '../../api/client'
import type { Servico } from '../../api/types'
import { Cabecalho, Carregando, ErroCarga, Vazio } from '../../componentes/Basicos'
import { Campo, Texto } from '../../componentes/Campo'
import { useAvisos } from '../../componentes/Avisos'
import { Modal } from '../../componentes/Modal'
import { useServicos, useTiposCabelo } from '../../dados'
import { minutosParaTexto, moeda } from '../../util/formato'

type Form = { nome: string; descricao: string; preco: string; duracaoMin: string; ativo: boolean; duracoes: Record<number, string> }

export function Servicos() {
  const servicos = useServicos()
  const tipos = useTiposCabelo()
  const qc = useQueryClient()
  const avisar = useAvisos()
  const [editando, setEditando] = useState<Servico | 'novo' | null>(null)
  const [f, setF] = useState<Form>({ nome: '', descricao: '', preco: '', duracaoMin: '30', ativo: true, duracoes: {} })

  const abrir = (s: Servico | 'novo') => {
    setEditando(s)
    setF(
      s === 'novo'
        ? { nome: '', descricao: '', preco: '', duracaoMin: '30', ativo: true, duracoes: {} }
        : {
            nome: s.nome,
            descricao: s.descricao ?? '',
            preco: String(s.preco),
            duracaoMin: String(s.duracaoMin),
            ativo: s.ativo,
            duracoes: Object.fromEntries(s.duracoes.map((d) => [d.tipoCabeloId, String(d.duracaoMin)])),
          },
    )
  }

  const salvar = useMutation({
    mutationFn: () => {
      const corpo = {
        nome: f.nome,
        descricao: f.descricao || null,
        preco: Number(f.preco.replace(',', '.')),
        duracaoMin: Number(f.duracaoMin),
        ativo: f.ativo,
        duracoes: Object.entries(f.duracoes)
          .filter(([, v]) => v && Number(v) > 0)
          .map(([k, v]) => ({ tipoCabeloId: Number(k), duracaoMin: Number(v) })),
      }
      return editando === 'novo' ? api.post('/api/painel/servicos', corpo) : api.put(`/api/painel/servicos/${(editando as Servico).id}`, corpo)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['servicos'] })
      avisar(editando === 'novo' ? 'Serviço cadastrado.' : 'Serviço atualizado.')
      setEditando(null)
    },
    onError: (e) => avisar(e.message, true),
  })

  const nomeTipo = (id: number) => tipos.data?.find((t) => t.id === id)?.nome ?? `Tipo ${id}`

  return (
    <>
      <Cabecalho titulo="Serviços e preços" subtitulo="A duração por tipo de cabelo decide em quais horários cada serviço aparece no app.">
        <button className="btn btn-primario" onClick={() => abrir('novo')}>
          <Plus size={18} aria-hidden /> Novo serviço
        </button>
      </Cabecalho>
      <div className="conteudo">
        <section className="painel">
          {servicos.isLoading ? (
            <Carregando />
          ) : servicos.error ? (
            <ErroCarga erro={servicos.error} />
          ) : !servicos.data?.length ? (
            <Vazio acao={<button className="btn btn-primario" onClick={() => abrir('novo')}>Cadastrar o primeiro serviço</button>}>
              Cadastre os serviços para os clientes conseguirem agendar.
            </Vazio>
          ) : (
            <div className="tabela-envelope">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Serviço</th>
                    <th className="direita">Preço</th>
                    <th>Duração</th>
                    <th>Por tipo de cabelo</th>
                    <th>Situação</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {servicos.data.map((s) => (
                    <tr key={s.id}>
                      <td>
                        <strong>{s.nome}</strong>
                        {s.descricao && <div className="mudo pequeno">{s.descricao}</div>}
                      </td>
                      <td className="direita numero">{moeda(s.preco)}</td>
                      <td className="numero">{minutosParaTexto(s.duracaoMin)}</td>
                      <td className="pequeno">
                        {s.duracoes.length === 0 ? (
                          <span className="mudo">Igual para todos</span>
                        ) : (
                          s.duracoes.map((d) => `${nomeTipo(d.tipoCabeloId)}: ${d.duracaoMin} min`).join('; ')
                        )}
                      </td>
                      <td>{s.ativo ? 'No app' : <span className="mudo">Desativado</span>}</td>
                      <td className="direita">
                        <button className="btn btn-pequeno" onClick={() => abrir(s)}>
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

      <Modal titulo={editando === 'novo' ? 'Novo serviço' : 'Editar serviço'} aberto={editando !== null} aoFechar={() => setEditando(null)}>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            salvar.mutate()
          }}
        >
          <Campo rotulo="Nome" required maxLength={120} value={f.nome} onChange={(e) => setF({ ...f, nome: e.target.value })} />
          <Texto rotulo="Descrição (aparece no app)" maxLength={500} value={f.descricao} onChange={(e) => setF({ ...f, descricao: e.target.value })} />
          <div className="linha-form">
            <Campo rotulo="Preço (R$)" required inputMode="decimal" value={f.preco} onChange={(e) => setF({ ...f, preco: e.target.value })} />
            <Campo
              rotulo="Duração padrão (min)"
              required
              type="number"
              min={5}
              max={600}
              value={f.duracaoMin}
              onChange={(e) => setF({ ...f, duracaoMin: e.target.value })}
            />
          </div>
          <div className="campo">
            <span>Duração por tipo de cabelo</span>
            <small style={{ marginBottom: 6 }}>Deixe em branco para usar a duração padrão.</small>
            <div className="linha-form">
              {(tipos.data ?? []).map((t) => (
                <Campo
                  key={t.id}
                  rotulo={`${t.nome} (min)`}
                  type="number"
                  min={5}
                  max={600}
                  placeholder={f.duracaoMin}
                  value={f.duracoes[t.id] ?? ''}
                  onChange={(e) => setF({ ...f, duracoes: { ...f.duracoes, [t.id]: e.target.value } })}
                />
              ))}
            </div>
          </div>
          {editando !== 'novo' && (
            <label className="checagem">
              <input type="checkbox" checked={f.ativo} onChange={(e) => setF({ ...f, ativo: e.target.checked })} />
              Disponível no app
            </label>
          )}
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setEditando(null)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={salvar.isPending}>
              {editando === 'novo' ? 'Cadastrar serviço' : 'Salvar alterações'}
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
