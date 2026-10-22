import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2 } from 'lucide-react'
import { api } from '../../api/client'
import type { Bloqueio, Janela } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Cabecalho, Carregando, ErroCarga, Vazio } from '../../componentes/Basicos'
import { Campo, Fichas } from '../../componentes/Campo'
import { useAvisos } from '../../componentes/Avisos'
import { Modal } from '../../componentes/Modal'
import { useBarbeiros, useServicos } from '../../dados'
import { dataHora, diasSemana } from '../../util/formato'

type Faixa = { inicio: string; fim: string; servicosIds: number[]; restrita: boolean }
type Semana = Record<number, Faixa[]>

function paraSemana(janelas: Janela[]): Semana {
  const s: Semana = { 1: [], 2: [], 3: [], 4: [], 5: [], 6: [], 7: [] }
  for (const j of janelas) {
    s[j.diaSemana].push({
      inicio: j.inicio.slice(0, 5),
      fim: j.fim.slice(0, 5),
      servicosIds: j.servicosIds ?? [],
      restrita: (j.servicosIds ?? []).length > 0,
    })
  }
  return s
}

export function Horarios() {
  const { usuario } = useAuth()
  const barbeiros = useBarbeiros()
  const servicos = useServicos()
  const qc = useQueryClient()
  const avisar = useAvisos()

  const meus = (barbeiros.data ?? []).filter((b) => usuario?.papel !== 'BARBEIRO' || b.id === usuario.barbeiroId)
  const [barbeiroId, setBarbeiroId] = useState<number | null>(null)
  const atual = barbeiroId ?? meus[0]?.id ?? null

  const horarios = useQuery({
    queryKey: ['horarios', atual],
    queryFn: () => api.get<Janela[]>(`/api/painel/barbeiros/${atual}/horarios`),
    enabled: atual != null,
  })
  const bloqueios = useQuery({
    queryKey: ['bloqueios', atual],
    queryFn: () => api.get<Bloqueio[]>(`/api/painel/barbeiros/${atual}/bloqueios`),
    enabled: atual != null,
  })

  const [semana, setSemana] = useState<Semana>(paraSemana([]))
  const [alterado, setAlterado] = useState(false)
  useEffect(() => {
    if (horarios.data) {
      setSemana(paraSemana(horarios.data))
      setAlterado(false)
    }
  }, [horarios.data])

  const salvar = useMutation({
    mutationFn: () =>
      api.put<Janela[]>(`/api/painel/barbeiros/${atual}/horarios`, {
        janelas: Object.entries(semana).flatMap(([dia, faixas]) =>
          faixas.map((f) => ({
            diaSemana: Number(dia),
            inicio: `${f.inicio}:00`,
            fim: `${f.fim}:00`,
            servicosIds: f.restrita ? f.servicosIds : [],
          })),
        ),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['horarios', atual] })
      avisar('Horários salvos. A agenda do app já usa os novos horários.')
    },
    onError: (e) => avisar(e.message, true),
  })

  const mudar = (dia: number, faixas: Faixa[]) => {
    setSemana({ ...semana, [dia]: faixas })
    setAlterado(true)
  }

  const copiarSegunda = () => {
    const base = semana[1]
    const nova: Semana = { ...semana }
    for (let d = 2; d <= 5; d++) nova[d] = base.map((f) => ({ ...f, servicosIds: [...f.servicosIds] }))
    setSemana(nova)
    setAlterado(true)
  }

  const ativosServ = (servicos.data ?? []).filter((s) => s.ativo)

  // bloqueios
  const [novoBloqueio, setNovoBloqueio] = useState(false)
  const [bq, setBq] = useState({ inicio: '', fim: '', motivo: '' })
  const criarBloqueio = useMutation({
    mutationFn: () =>
      api.post(`/api/painel/barbeiros/${atual}/bloqueios`, {
        inicio: `${bq.inicio}:00`,
        fim: `${bq.fim}:00`,
        motivo: bq.motivo || null,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bloqueios', atual] })
      setNovoBloqueio(false)
      setBq({ inicio: '', fim: '', motivo: '' })
      avisar('Horário bloqueado.')
    },
    onError: (e) => avisar(e.message, true),
  })
  const removerBloqueio = useMutation({
    mutationFn: (id: number) => api.del(`/api/painel/barbeiros/${atual}/bloqueios/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bloqueios', atual] })
      avisar('Bloqueio removido.')
    },
    onError: (e) => avisar(e.message, true),
  })

  if (barbeiros.isLoading) return <Carregando />
  if (barbeiros.error) return <ErroCarga erro={barbeiros.error} />

  return (
    <>
      <Cabecalho
        titulo={usuario?.papel === 'BARBEIRO' ? 'Meus horários' : 'Horários de trabalho'}
        subtitulo="Monte o expediente de cada dia. Pausas são o intervalo entre duas faixas."
      >
        {meus.length > 1 && (
          <select
            className="entrada"
            style={{ width: 220 }}
            aria-label="Barbeiro"
            value={atual ?? ''}
            onChange={(e) => setBarbeiroId(Number(e.target.value))}
          >
            {meus.map((b) => (
              <option key={b.id} value={b.id}>
                {b.nome}
              </option>
            ))}
          </select>
        )}
        <button className="btn btn-primario" disabled={!alterado || salvar.isPending} onClick={() => salvar.mutate()}>
          {salvar.isPending ? 'Salvando…' : 'Salvar horários'}
        </button>
      </Cabecalho>

      <div className="conteudo">
        {meus.length === 0 ? (
          <Vazio>Nenhum barbeiro cadastrado.</Vazio>
        ) : (
          <section className="painel">
            <div className="painel-cabeca">
              <div>
                <h2>Semana</h2>
                <p className="mudo pequeno">
                  Restrinja uma faixa para aceitar só alguns serviços, por exemplo “das 18h às 19h só barba”.
                </p>
              </div>
              <button className="btn btn-pequeno" onClick={copiarSegunda} disabled={semana[1].length === 0}>
                Copiar segunda para terça a sexta
              </button>
            </div>
            {horarios.isLoading ? (
              <Carregando />
            ) : (
              <div className="semana">
                {diasSemana.map((nome, i) => {
                  const dia = i + 1
                  const faixas = semana[dia]
                  return (
                    <div key={dia} className="semana-dia">
                      <div>
                        <h3>{nome}</h3>
                        {faixas.length === 0 && <p className="mudo pequeno">Folga</p>}
                      </div>
                      <div>
                        {faixas.map((f, k) => (
                          <div key={k}>
                            <div className="faixa">
                              <Campo
                                rotulo="Início"
                                type="time"
                                value={f.inicio}
                                onChange={(e) => mudar(dia, faixas.map((x, n) => (n === k ? { ...x, inicio: e.target.value } : x)))}
                              />
                              <Campo
                                rotulo="Fim"
                                type="time"
                                value={f.fim}
                                onChange={(e) => mudar(dia, faixas.map((x, n) => (n === k ? { ...x, fim: e.target.value } : x)))}
                              />
                              <label className="checagem" style={{ marginTop: 30 }}>
                                <input
                                  type="checkbox"
                                  checked={f.restrita}
                                  onChange={(e) =>
                                    mudar(dia, faixas.map((x, n) => (n === k ? { ...x, restrita: e.target.checked } : x)))
                                  }
                                />
                                Só alguns serviços
                              </label>
                              <button
                                className="btn btn-fantasma btn-icone"
                                style={{ marginTop: 24 }}
                                aria-label={`Remover faixa ${f.inicio}–${f.fim} de ${nome}`}
                                onClick={() => mudar(dia, faixas.filter((_, n) => n !== k))}
                              >
                                <Trash2 size={17} />
                              </button>
                            </div>
                            {f.restrita && (
                              <div style={{ marginBottom: 14 }}>
                                <Fichas
                                  rotulo={`Serviços aceitos de ${f.inicio} às ${f.fim}`}
                                  opcoes={ativosServ.map((s) => ({ id: s.id, nome: s.nome }))}
                                  valor={f.servicosIds}
                                  aoMudar={(v) => mudar(dia, faixas.map((x, n) => (n === k ? { ...x, servicosIds: v } : x)))}
                                />
                              </div>
                            )}
                          </div>
                        ))}
                        <button
                          className="btn btn-pequeno"
                          onClick={() => {
                            const ultimo = faixas[faixas.length - 1]
                            mudar(dia, [
                              ...faixas,
                              ultimo
                                ? { inicio: ultimo.fim, fim: ultimo.fim < '18:00' ? '18:00' : '22:00', servicosIds: [], restrita: false }
                                : { inicio: '09:00', fim: '12:00', servicosIds: [], restrita: false },
                            ])
                          }}
                        >
                          <Plus size={15} aria-hidden /> {faixas.length ? 'Adicionar faixa' : 'Atender neste dia'}
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </section>
        )}

        {atual != null && (
          <section className="painel">
            <div className="painel-cabeca">
              <div>
                <h2>Bloqueios</h2>
                <p className="mudo pequeno">Compromissos, feriados ou folgas pontuais. Ninguém consegue agendar nesses horários.</p>
              </div>
              <button className="btn btn-pequeno" onClick={() => setNovoBloqueio(true)}>
                <Plus size={15} aria-hidden /> Bloquear horário
              </button>
            </div>
            {bloqueios.data?.length ? (
              <ul className="lista-simples painel-corpo">
                {bloqueios.data.map((b) => (
                  <li key={b.id}>
                    <span>
                      <strong className="numero">
                        {dataHora(b.inicio)} até {dataHora(b.fim)}
                      </strong>
                      {b.motivo && <span className="mudo"> ({b.motivo})</span>}
                    </span>
                    <button className="btn btn-perigo btn-pequeno" onClick={() => removerBloqueio.mutate(b.id)}>
                      Remover
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <Vazio>Nenhum bloqueio programado.</Vazio>
            )}
          </section>
        )}
      </div>

      <Modal titulo="Bloquear horário" aberto={novoBloqueio} aoFechar={() => setNovoBloqueio(false)}>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            criarBloqueio.mutate()
          }}
        >
          <div className="linha-form">
            <Campo rotulo="De" type="datetime-local" required value={bq.inicio} onChange={(e) => setBq({ ...bq, inicio: e.target.value })} />
            <Campo rotulo="Até" type="datetime-local" required value={bq.fim} onChange={(e) => setBq({ ...bq, fim: e.target.value })} />
          </div>
          <Campo rotulo="Motivo (opcional)" maxLength={255} value={bq.motivo} onChange={(e) => setBq({ ...bq, motivo: e.target.value })} />
          <p className="mudo pequeno">Atendimentos já marcados nesse período não são cancelados: remarque-os pela agenda.</p>
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setNovoBloqueio(false)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={criarBloqueio.isPending}>
              Bloquear
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}

