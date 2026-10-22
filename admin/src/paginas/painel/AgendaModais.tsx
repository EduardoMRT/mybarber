import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { Agendamento, Barbeiro, FormaPagamento, Servico, TipoCabelo } from '../../api/types'
import { Modal } from '../../componentes/Modal'
import { Campo, Selecao, Texto } from '../../componentes/Campo'
import { Selo } from '../../componentes/Basicos'
import { useAvisos } from '../../componentes/Avisos'
import { dataHora, formasPagamento, hora, minutosParaTexto, moeda } from '../../util/formato'

function useAcao(aoConcluir: () => void) {
  const qc = useQueryClient()
  const avisar = useAvisos()
  return useMutation({
    mutationFn: ({ caminho, corpo }: { caminho: string; corpo?: unknown; msg: string }) => api.post<Agendamento>(caminho, corpo),
    onSuccess: (_d, v) => {
      qc.invalidateQueries({ queryKey: ['agenda'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      avisar(v.msg)
      aoConcluir()
    },
    onError: (e) => avisar(e.message, true),
  })
}

type Modo = 'ver' | 'concluir' | 'remarcar' | 'cancelar'

/** Detalhe de um atendimento com as ações do barbeiro. */
export function DetalheAgendamento({ ag, aoFechar }: { ag: Agendamento | null; aoFechar: () => void }) {
  const [modo, setModo] = useState<Modo>('ver')
  const [forma, setForma] = useState<FormaPagamento | ''>('')
  const [valor, setValor] = useState('')
  const [novoInicio, setNovoInicio] = useState('')
  const [motivo, setMotivo] = useState('')
  const acao = useAcao(() => fechar())

  function fechar() {
    setModo('ver')
    setForma('')
    setValor('')
    setNovoInicio('')
    setMotivo('')
    aoFechar()
  }

  if (!ag) return null
  const ativo = ag.status === 'CONFIRMADO' || ag.status === 'AGUARDANDO_CLIENTE'
  const passou = new Date(ag.inicio) <= new Date()

  const executar = (e: FormEvent) => {
    e.preventDefault()
    if (modo === 'concluir') {
      acao.mutate({
        caminho: `/api/painel/agendamentos/${ag.id}/concluir`,
        corpo: { formaPagamento: forma || null, valor: valor ? Number(valor.replace(',', '.')) : null },
        msg: 'Atendimento concluído e lançado no caixa.',
      })
    } else if (modo === 'remarcar') {
      acao.mutate({
        caminho: `/api/painel/agendamentos/${ag.id}/remarcar`,
        corpo: { novoInicio: novoInicio.length === 16 ? `${novoInicio}:00` : novoInicio, motivo },
        msg: ag.clienteId ? 'Remarcação enviada. O cliente foi avisado no app.' : 'Horário alterado.',
      })
    } else if (modo === 'cancelar') {
      acao.mutate({
        caminho: `/api/painel/agendamentos/${ag.id}/cancelar`,
        corpo: { motivo },
        msg: 'Atendimento cancelado.',
      })
    }
  }

  const titulos: Record<Modo, string> = {
    ver: ag.servicoNome,
    concluir: 'Concluir atendimento',
    remarcar: 'Remarcar atendimento',
    cancelar: 'Cancelar atendimento',
  }

  return (
    <Modal titulo={titulos[modo]} aberto={!!ag} aoFechar={fechar}>
      {modo === 'ver' && (
        <div className="form">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
            <div>
              <p style={{ fontSize: 20, fontWeight: 700 }} className="numero">
                {hora(ag.inicio)}–{hora(ag.fim)}
              </p>
              <p className="mudo pequeno">
                {dataHora(ag.inicio)}, {minutosParaTexto(ag.duracaoMin)} com {ag.barbeiroNome}
              </p>
            </div>
            <Selo status={ag.status} />
          </div>
          <ul className="lista-simples">
            <li>
              <span className="mudo">Cliente</span>
              <span>
                {ag.clienteNome ?? '—'}
                {ag.origem === 'MANUAL' && <span className="mudo pequeno"> (lançado no portal)</span>}
              </span>
            </li>
            {ag.clienteTelefone && (
              <li>
                <span className="mudo">Telefone</span>
                <a href={`https://wa.me/55${ag.clienteTelefone.replace(/\D/g, '')}`} target="_blank" rel="noreferrer">
                  {ag.clienteTelefone}
                </a>
              </li>
            )}
            <li>
              <span className="mudo">Tipo de cabelo</span>
              <span>{ag.tipoCabeloNome ?? 'Não informado'}</span>
            </li>
            <li>
              <span className="mudo">Valor</span>
              <span className="numero">{moeda(ag.valor)}</span>
            </li>
            {ag.formaPagamento && (
              <li>
                <span className="mudo">Pagamento</span>
                <span>{formasPagamento[ag.formaPagamento]}</span>
              </li>
            )}
          </ul>
          {(ag.observacao || ag.observacoesCliente) && (
            <p className="pequeno">
              <strong>Observações: </strong>
              {[ag.observacao, ag.observacoesCliente].filter(Boolean).join(' / ')}
            </p>
          )}
          {ag.remarcacao && (
            <p className="erro-form" style={{ background: 'var(--st-aguardando-fundo)', color: 'var(--st-aguardando)' }}>
              Remarcado de {dataHora(ag.remarcacao.inicioAnterior)} por “{ag.remarcacao.motivo}”. O cliente tem até{' '}
              {dataHora(ag.remarcacao.prazo)} para responder; sem resposta, a reserva é cancelada.
            </p>
          )}
          {ag.motivoCancelamento && <p className="mudo pequeno">Motivo do cancelamento: {ag.motivoCancelamento}</p>}

          {ativo && (
            <div className="acoes-form" style={{ justifyContent: 'flex-start' }}>
              {ag.status === 'CONFIRMADO' && (
                <button className="btn btn-primario" onClick={() => { setValor(String(ag.valor)); setModo('concluir') }}>
                  Concluir
                </button>
              )}
              <button className="btn" onClick={() => setModo('remarcar')}>
                Remarcar
              </button>
              {ag.status === 'CONFIRMADO' && passou && (
                <button
                  className="btn"
                  disabled={acao.isPending}
                  onClick={() => acao.mutate({ caminho: `/api/painel/agendamentos/${ag.id}/falta`, msg: 'Falta registrada.' })}
                >
                  Cliente faltou
                </button>
              )}
              <button className="btn btn-perigo" onClick={() => setModo('cancelar')}>
                Cancelar
              </button>
            </div>
          )}
        </div>
      )}

      {modo !== 'ver' && (
        <form className="form" onSubmit={executar}>
          {modo === 'concluir' && (
            <>
              <div className="linha-form">
                <Selecao rotulo="Forma de pagamento" value={forma} onChange={(e) => setForma(e.target.value as FormaPagamento)}>
                  <option value="">Não informar</option>
                  {Object.entries(formasPagamento).map(([k, v]) => (
                    <option key={k} value={k}>
                      {v}
                    </option>
                  ))}
                </Selecao>
                <Campo rotulo="Valor cobrado (R$)" inputMode="decimal" value={valor} onChange={(e) => setValor(e.target.value)} />
              </div>
              <p className="mudo pequeno">O valor entra automaticamente como receita no financeiro.</p>
            </>
          )}
          {modo === 'remarcar' && (
            <>
              <Campo
                rotulo="Novo horário"
                type="datetime-local"
                required
                value={novoInicio}
                onChange={(e) => setNovoInicio(e.target.value)}
                ajuda={`Duração deste atendimento: ${minutosParaTexto(ag.duracaoMin)}.`}
              />
              <Texto
                rotulo="Motivo (aparece para o cliente)"
                required
                maxLength={255}
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
              />
              {ag.clienteId && (
                <p className="mudo pequeno">
                  O cliente recebe um aviso no app e escolhe entre aceitar este horário, escolher outro ou cancelar. O horário
                  atual fica livre na hora.
                </p>
              )}
            </>
          )}
          {modo === 'cancelar' && (
            <Texto
              rotulo="Motivo (opcional, aparece para o cliente)"
              maxLength={255}
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
            />
          )}
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setModo('ver')}>
              Voltar
            </button>
            <button
              type="submit"
              className={modo === 'cancelar' ? 'btn btn-perigo' : 'btn btn-primario'}
              disabled={acao.isPending || (modo === 'remarcar' && (!novoInicio || !motivo.trim()))}
            >
              {modo === 'concluir' ? 'Concluir atendimento' : modo === 'remarcar' ? 'Enviar remarcação' : 'Cancelar atendimento'}
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}

export interface PreManual {
  barbeiroId?: number
  inicio?: string
}

/** Lançar corte manualmente (cliente de balcão, telefone, WhatsApp). */
export function LancarManual({
  aberto,
  aoFechar,
  inicial,
  barbeiros,
  servicos,
  tipos,
}: {
  aberto: boolean
  aoFechar: () => void
  inicial: PreManual
  barbeiros: Barbeiro[]
  servicos: Servico[]
  tipos: TipoCabelo[]
}) {
  const qc = useQueryClient()
  const avisar = useAvisos()
  const [f, setF] = useState({
    barbeiroId: '',
    servicoId: '',
    tipoCabeloId: '',
    clienteNome: '',
    clienteTelefone: '',
    inicio: '',
    valor: '',
    observacao: '',
    concluido: false,
    formaPagamento: '',
  })
  const [chave, setChave] = useState('')

  // reinicia o formulário sempre que abrir com outro ponto de partida
  const chaveAtual = `${aberto}-${inicial.barbeiroId}-${inicial.inicio}`
  if (aberto && chave !== chaveAtual) {
    setChave(chaveAtual)
    setF((x) => ({
      ...x,
      barbeiroId: String(inicial.barbeiroId ?? barbeiros[0]?.id ?? ''),
      inicio: inicial.inicio ?? '',
      clienteNome: '',
      clienteTelefone: '',
      observacao: '',
      valor: '',
      concluido: false,
    }))
  }

  const ativos = servicos.filter((s) => s.ativo)
  const servico = ativos.find((s) => String(s.id) === f.servicoId)
  const duracao = servico
    ? (servico.duracoes.find((d) => String(d.tipoCabeloId) === f.tipoCabeloId)?.duracaoMin ?? servico.duracaoMin)
    : null

  const salvar = useMutation({
    mutationFn: () =>
      api.post('/api/painel/agendamentos', {
        barbeiroId: Number(f.barbeiroId),
        servicoId: Number(f.servicoId),
        tipoCabeloId: f.tipoCabeloId ? Number(f.tipoCabeloId) : null,
        clienteNome: f.clienteNome,
        clienteTelefone: f.clienteTelefone || null,
        inicio: f.inicio.length === 16 ? `${f.inicio}:00` : f.inicio,
        valor: f.valor ? Number(f.valor.replace(',', '.')) : null,
        observacao: f.observacao || null,
        concluido: f.concluido,
        formaPagamento: f.concluido && f.formaPagamento ? f.formaPagamento : null,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['agenda'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      avisar(f.concluido ? 'Atendimento lançado no caixa.' : 'Horário reservado na agenda.')
      setChave('')
      aoFechar()
    },
    onError: (e) => avisar(e.message, true),
  })

  const set = (k: keyof typeof f) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value })

  return (
    <Modal titulo="Lançar atendimento" aberto={aberto} aoFechar={aoFechar}>
      <form
        className="form"
        onSubmit={(e) => {
          e.preventDefault()
          salvar.mutate()
        }}
      >
        <div className="linha-form">
          <Selecao rotulo="Barbeiro" required value={f.barbeiroId} onChange={set('barbeiroId')}>
            {barbeiros.map((b) => (
              <option key={b.id} value={b.id}>
                {b.nome}
              </option>
            ))}
          </Selecao>
          <Selecao rotulo="Serviço" required value={f.servicoId} onChange={set('servicoId')}>
            <option value="">Escolha</option>
            {ativos.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nome} ({moeda(s.preco)})
              </option>
            ))}
          </Selecao>
        </div>
        <div className="linha-form">
          <Campo rotulo="Nome do cliente" required maxLength={120} value={f.clienteNome} onChange={set('clienteNome')} />
          <Campo rotulo="Telefone" inputMode="tel" maxLength={20} value={f.clienteTelefone} onChange={set('clienteTelefone')} />
        </div>
        <div className="linha-form">
          <Selecao rotulo="Tipo de cabelo" value={f.tipoCabeloId} onChange={set('tipoCabeloId')}>
            <option value="">Não informar</option>
            {tipos.map((t) => (
              <option key={t.id} value={t.id}>
                {t.nome}
              </option>
            ))}
          </Selecao>
          <Campo
            rotulo="Data e hora"
            type="datetime-local"
            required
            value={f.inicio}
            onChange={set('inicio')}
            ajuda={duracao ? `Ocupa ${minutosParaTexto(duracao)} da agenda.` : undefined}
          />
        </div>
        <div className="linha-form">
          <Campo
            rotulo="Valor (R$)"
            inputMode="decimal"
            placeholder={servico ? String(servico.preco) : ''}
            value={f.valor}
            onChange={set('valor')}
            ajuda="Em branco usa o preço do serviço."
          />
          <Campo rotulo="Observação" maxLength={500} value={f.observacao} onChange={set('observacao')} />
        </div>
        <label className="checagem">
          <input type="checkbox" checked={f.concluido} onChange={(e) => setF({ ...f, concluido: e.target.checked })} />
          Já foi atendido: lançar direto no caixa
        </label>
        {f.concluido && (
          <Selecao rotulo="Forma de pagamento" value={f.formaPagamento} onChange={set('formaPagamento')}>
            <option value="">Não informar</option>
            {Object.entries(formasPagamento).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </Selecao>
        )}
        <div className="acoes-form">
          <button type="button" className="btn" onClick={aoFechar}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primario" disabled={salvar.isPending}>
            {f.concluido ? 'Lançar no caixa' : 'Reservar horário'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
