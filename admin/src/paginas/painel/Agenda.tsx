import { useMemo, useState, type CSSProperties } from 'react'
import { useQueries, useQuery } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { api, query } from '../../api/client'
import type { Agendamento, Barbeiro, Bloqueio, Janela } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Cabecalho, Carregando, ErroCarga, Selo, Vazio } from '../../componentes/Basicos'
import { useBarbeiros, useServicos, useTiposCabelo } from '../../dados'
import { dataLonga, hora, isoData, minutosParaTexto, somarDias } from '../../util/formato'
import { DetalheAgendamento, LancarManual, type PreManual } from './AgendaModais'

const PX_HORA = 76
const ocupa = (s: Agendamento['status']) => s === 'CONFIRMADO' || s === 'AGUARDANDO_CLIENTE' || s === 'CONCLUIDO'
const min = (hhmm: string) => {
  const [h, m] = hhmm.split(':').map(Number)
  return h * 60 + m
}
const minDoIso = (iso: string) => min(iso.split('T')[1])
const hhmm = (m: number) => `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`

interface Coluna {
  barbeiro: Barbeiro
  janelas: Janela[]
  bloqueios: Bloqueio[]
  agendamentos: Agendamento[]
}

/** Vãos livres dentro do expediente: mostram quanto tempo ainda cabe. */
function vaos(c: Coluna, dia: string) {
  const ocupados = [
    ...c.agendamentos.filter((a) => ocupa(a.status)).map((a) => [minDoIso(a.inicio), minDoIso(a.fim)]),
    ...c.bloqueios
      .filter((b) => b.inicio.slice(0, 10) <= dia && b.fim.slice(0, 10) >= dia)
      .map((b) => [b.inicio.slice(0, 10) < dia ? 0 : minDoIso(b.inicio), b.fim.slice(0, 10) > dia ? 1440 : minDoIso(b.fim)]),
  ].sort((a, b) => a[0] - b[0])
  const lista: { ini: number; fim: number }[] = []
  for (const j of c.janelas) {
    let cursor = min(j.inicio)
    const fimJ = min(j.fim) || 1440
    for (const [oi, of] of ocupados) {
      if (of <= cursor || oi >= fimJ) continue
      if (oi > cursor) lista.push({ ini: cursor, fim: Math.min(oi, fimJ) })
      cursor = Math.max(cursor, of)
    }
    if (cursor < fimJ) lista.push({ ini: cursor, fim: fimJ })
  }
  return lista.filter((v) => v.fim - v.ini >= 10)
}

export function Agenda() {
  const { usuario } = useAuth()
  const [dia, setDia] = useState(() => new Date())
  const [selecionado, setSelecionado] = useState<Agendamento | null>(null)
  const [manual, setManual] = useState<PreManual | null>(null)
  const diaIso = isoData(dia)
  const diaSemana = ((dia.getDay() + 6) % 7) + 1

  const barbeiros = useBarbeiros()
  const servicos = useServicos()
  const tipos = useTiposCabelo()

  const visiveis = useMemo(
    () =>
      (barbeiros.data ?? []).filter(
        (b) => b.ativo && (usuario?.papel !== 'BARBEIRO' || b.id === usuario.barbeiroId),
      ),
    [barbeiros.data, usuario],
  )

  const agendamentos = useQuery({
    queryKey: ['agenda', diaIso],
    queryFn: () => api.get<Agendamento[]>(`/api/painel/agendamentos${query({ de: diaIso, ate: diaIso })}`),
    refetchInterval: 60_000,
  })

  const horarios = useQueries({
    queries: visiveis.map((b) => ({
      queryKey: ['horarios', b.id],
      queryFn: () => api.get<Janela[]>(`/api/painel/barbeiros/${b.id}/horarios`),
    })),
  })
  const bloqueios = useQueries({
    queries: visiveis.map((b) => ({
      queryKey: ['bloqueios', b.id],
      queryFn: () => api.get<Bloqueio[]>(`/api/painel/barbeiros/${b.id}/bloqueios`),
    })),
  })

  const colunas: Coluna[] = visiveis.map((b, i) => ({
    barbeiro: b,
    janelas: (horarios[i]?.data ?? []).filter((j) => j.diaSemana === diaSemana),
    bloqueios: bloqueios[i]?.data ?? [],
    agendamentos: (agendamentos.data ?? []).filter((a) => a.barbeiroId === b.id && a.status !== 'CANCELADO'),
  }))
  const cancelados = (agendamentos.data ?? []).filter((a) => a.status === 'CANCELADO')

  // faixa visível do dia: expediente + qualquer atendimento fora dele
  const marcos = colunas.flatMap((c) => [
    ...c.janelas.flatMap((j) => [min(j.inicio), min(j.fim) || 1440]),
    ...c.agendamentos.flatMap((a) => [minDoIso(a.inicio), minDoIso(a.fim)]),
  ])
  const inicioDia = marcos.length ? Math.floor(Math.min(...marcos) / 60) * 60 : 8 * 60
  const fimDia = marcos.length ? Math.ceil(Math.max(...marcos) / 60) * 60 : 20 * 60
  const y = (m: number) => ((m - inicioDia) / 60) * PX_HORA
  const altura = y(fimDia)

  const hoje = isoData(new Date()) === diaIso
  const agora = new Date()
  const minAgora = agora.getHours() * 60 + agora.getMinutes()

  const carregando = barbeiros.isLoading || agendamentos.isLoading
  const erro = barbeiros.error || agendamentos.error

  return (
    <>
      <Cabecalho titulo={usuario?.papel === 'BARBEIRO' ? 'Minha agenda' : 'Agenda'} subtitulo="Os espaços tracejados mostram quanto tempo ainda cabe em cada vão.">
        <button className="btn btn-primario" onClick={() => setManual({ inicio: `${diaIso}T${hhmm(Math.max(inicioDia, 9 * 60))}` })}>
          <Plus size={18} aria-hidden /> Lançar atendimento
        </button>
      </Cabecalho>

      <div className="conteudo">
        <div className="agenda-barra">
          <button className="btn btn-icone" onClick={() => setDia(somarDias(dia, -1))} aria-label="Dia anterior">
            <ChevronLeft size={18} />
          </button>
          <button className="btn" onClick={() => setDia(new Date())} disabled={hoje}>
            Hoje
          </button>
          <button className="btn btn-icone" onClick={() => setDia(somarDias(dia, 1))} aria-label="Próximo dia">
            <ChevronRight size={18} />
          </button>
          <span className="agenda-dia" aria-live="polite">
            {dataLonga(dia)}
          </span>
          <label className="sr-only" htmlFor="ir-para">
            Ir para a data
          </label>
          <input
            id="ir-para"
            className="entrada"
            style={{ width: 170 }}
            type="date"
            value={diaIso}
            onChange={(e) => e.target.value && setDia(new Date(`${e.target.value}T12:00:00`))}
          />
        </div>

        <div className="painel" style={{ overflow: 'hidden' }}>
          {carregando ? (
            <Carregando />
          ) : erro ? (
            <ErroCarga erro={erro} tentar={() => agendamentos.refetch()} />
          ) : visiveis.length === 0 ? (
            <Vazio>Nenhum barbeiro ativo. Cadastre um em Barbeiros para montar a agenda.</Vazio>
          ) : (
            <div className="agenda-colunas" style={{ '--colunas': colunas.length } as CSSProperties}>
              <div className="agenda-cabeca-col" style={{ borderLeft: 0 }} />
              {colunas.map((c) => (
                <div key={c.barbeiro.id} className="agenda-cabeca-col">
                  {c.barbeiro.nome}
                  <div className="mudo pequeno" style={{ fontWeight: 400 }}>
                    {c.janelas.length
                      ? c.janelas.map((j) => `${j.inicio.slice(0, 5)}–${j.fim.slice(0, 5)}`).join(', ')
                      : 'Não atende neste dia'}
                  </div>
                </div>
              ))}

              <div className="agenda-horas" style={{ height: altura }} aria-hidden>
                {Array.from({ length: (fimDia - inicioDia) / 60 + 1 }, (_, i) => (
                  <span key={i} className="agenda-hora" style={{ top: i * PX_HORA }}>
                    {hhmm(inicioDia + i * 60)}
                  </span>
                ))}
              </div>

              {colunas.map((c) => {
                const janelasOrd = [...c.janelas].sort((a, b) => min(a.inicio) - min(b.inicio))
                // faixas fora do expediente (antes, entre e depois das janelas)
                const fora: [number, number][] = []
                let cursor = inicioDia
                for (const j of janelasOrd) {
                  if (min(j.inicio) > cursor) fora.push([cursor, min(j.inicio)])
                  cursor = Math.max(cursor, min(j.fim) || 1440)
                }
                if (cursor < fimDia) fora.push([cursor, fimDia])

                return (
                  <div
                    key={c.barbeiro.id}
                    className="agenda-coluna"
                    style={{ height: altura, '--px-hora': `${PX_HORA}px` } as CSSProperties}
                    aria-label={`Agenda de ${c.barbeiro.nome}`}
                  >
                    {fora.map(([a, b]) => (
                      <div key={`f${a}`} className="agenda-fora" style={{ top: y(a), height: y(b) - y(a) }} />
                    ))}

                    {vaos(c, diaIso)
                      .filter((v) => !hoje || v.fim > minAgora)
                      .map((v) => {
                        const ini = hoje ? Math.max(v.ini, Math.ceil(minAgora / 5) * 5) : v.ini
                        const livre = v.fim - ini
                        if (livre < 10) return null
                        return (
                          <button
                            key={`v${v.ini}`}
                            className="agenda-vao"
                            style={{ top: y(ini) + 2, height: Math.max(y(v.fim) - y(ini) - 4, 18) }}
                            onClick={() => setManual({ barbeiroId: c.barbeiro.id, inicio: `${diaIso}T${hhmm(ini)}` })}
                            aria-label={`Livre das ${hhmm(ini)} às ${hhmm(v.fim)}. Lançar atendimento`}
                          >
                            Livre {hhmm(ini)}–{hhmm(v.fim)}: cabe até {minutosParaTexto(livre)}
                          </button>
                        )
                      })}

                    {c.bloqueios
                      .filter((b) => b.inicio.slice(0, 10) <= diaIso && b.fim.slice(0, 10) >= diaIso)
                      .map((b) => {
                        const a = b.inicio.slice(0, 10) < diaIso ? inicioDia : Math.max(inicioDia, minDoIso(b.inicio))
                        const f = b.fim.slice(0, 10) > diaIso ? fimDia : Math.min(fimDia, minDoIso(b.fim))
                        if (f <= a) return null
                        return (
                          <div key={`b${b.id}`} className="agenda-bloco bloqueio" style={{ top: y(a) + 1, height: y(f) - y(a) - 2 }}>
                            <strong>Bloqueado</strong>
                            {b.motivo && <span>{b.motivo}</span>}
                          </div>
                        )
                      })}

                    {c.agendamentos.map((a) => {
                      const ini = minDoIso(a.inicio)
                      const fim = minDoIso(a.fim)
                      const h = Math.max(y(fim) - y(ini) - 2, 26)
                      return (
                        <button
                          key={a.id}
                          className={`agenda-bloco st-${a.status}`}
                          style={{ top: y(ini) + 1, height: h }}
                          onClick={() => setSelecionado(a)}
                          aria-label={`${hora(a.inicio)} ${a.clienteNome ?? ''}, ${a.servicoNome}`}
                        >
                          <strong>
                            {hora(a.inicio)} {a.clienteNome}
                          </strong>
                          {h > 40 && (
                            <span>
                              {a.servicoNome}
                              {a.tipoCabeloNome ? `, ${a.tipoCabeloNome.toLowerCase()}` : ''}
                            </span>
                          )}
                          {h > 58 && a.status !== 'CONFIRMADO' && (
                            <span>
                              <Selo status={a.status} />
                            </span>
                          )}
                        </button>
                      )
                    })}

                    {hoje && minAgora >= inicioDia && minAgora <= fimDia && (
                      <div className="agenda-agora" style={{ top: y(minAgora) }} aria-hidden />
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>

        <div className="legenda" aria-label="Legenda">
          <Selo status="CONFIRMADO" />
          <Selo status="AGUARDANDO_CLIENTE" />
          <Selo status="CONCLUIDO" />
          <Selo status="FALTA" />
        </div>

        {cancelados.length > 0 && (
          <section className="painel">
            <div className="painel-cabeca">
              <h2>Cancelados neste dia</h2>
            </div>
            <ul className="lista-simples painel-corpo">
              {cancelados.map((a) => (
                <li key={a.id}>
                  <button className="btn btn-fantasma btn-pequeno" onClick={() => setSelecionado(a)}>
                    {hora(a.inicio)} {a.clienteNome}, {a.servicoNome}
                  </button>
                  <span className="mudo pequeno">{a.motivoCancelamento}</span>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>

      <DetalheAgendamento ag={selecionado} aoFechar={() => setSelecionado(null)} />
      <LancarManual
        aberto={!!manual}
        aoFechar={() => setManual(null)}
        inicial={manual ?? {}}
        barbeiros={visiveis}
        servicos={servicos.data ?? []}
        tipos={tipos.data ?? []}
      />
    </>
  )
}
