import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { api, query } from '../../api/client'
import type { Barbearia, StatusBarbearia } from '../../api/types'
import { Cabecalho, Carregando, ErroCarga, Selo, Vazio } from '../../componentes/Basicos'
import { Campo, Texto } from '../../componentes/Campo'
import { useAvisos } from '../../componentes/Avisos'
import { Modal } from '../../componentes/Modal'
import { CamposBarbearia, dadosIniciais, paraRequisicao, type DadosBarbearia } from '../../componentes/FormBarbearia'
import { moeda } from '../../util/formato'

type Cobranca = { valorMensalidade: string; diaVencimento: string; diasTolerancia: string }
type Dono = { donoNome: string; donoEmail: string; donoSenha: string; donoAtende: boolean }

export function Barbearias() {
  const qc = useQueryClient()
  const avisar = useAvisos()
  const [busca, setBusca] = useState('')
  const [status, setStatus] = useState<'' | StatusBarbearia>('')
  const [editando, setEditando] = useState<Barbearia | 'nova' | null>(null)
  const [d, setD] = useState<DadosBarbearia>(dadosIniciais())
  const [cob, setCob] = useState<Cobranca>({ valorMensalidade: '', diaVencimento: '10', diasTolerancia: '5' })
  const [dono, setDono] = useState<Dono>({ donoNome: '', donoEmail: '', donoSenha: '', donoAtende: true })
  const [bloqueando, setBloqueando] = useState<Barbearia | null>(null)
  const [motivo, setMotivo] = useState('')
  const [senhaPara, setSenhaPara] = useState<Barbearia | null>(null)
  const [novaSenha, setNovaSenha] = useState('')

  const lista = useQuery({
    queryKey: ['admin-barbearias', busca, status],
    queryFn: () => api.get<Barbearia[]>(`/api/admin/barbearias${query({ busca, status })}`),
  })

  const abrir = (b: Barbearia | 'nova') => {
    setEditando(b)
    setD(dadosIniciais(b === 'nova' ? undefined : b))
    setCob(
      b === 'nova'
        ? { valorMensalidade: '', diaVencimento: '10', diasTolerancia: '5' }
        : {
            valorMensalidade: b.valorMensalidade != null ? String(b.valorMensalidade) : '',
            diaVencimento: b.diaVencimento != null ? String(b.diaVencimento) : '',
            diasTolerancia: String(b.diasTolerancia),
          },
    )
    setDono({ donoNome: '', donoEmail: '', donoSenha: '', donoAtende: true })
  }

  const atualizar = () => {
    qc.invalidateQueries({ queryKey: ['admin-barbearias'] })
    qc.invalidateQueries({ queryKey: ['admin-dashboard'] })
  }

  const salvar = useMutation({
    mutationFn: () => {
      const cobranca = {
        valorMensalidade: cob.valorMensalidade ? Number(cob.valorMensalidade.replace(',', '.')) : null,
        diaVencimento: cob.diaVencimento ? Number(cob.diaVencimento) : null,
        diasTolerancia: cob.diasTolerancia ? Number(cob.diasTolerancia) : null,
      }
      return editando === 'nova'
        ? api.post('/api/admin/barbearias', { dados: paraRequisicao(d), cobranca, ...dono })
        : api.put(`/api/admin/barbearias/${(editando as Barbearia).id}`, { dados: paraRequisicao(d), cobranca })
    },
    onSuccess: () => {
      atualizar()
      avisar(editando === 'nova' ? 'Barbearia cadastrada. Envie o e-mail e a senha ao responsável.' : 'Barbearia atualizada.')
      setEditando(null)
    },
    onError: (e) => avisar(e.message, true),
  })

  const bloquear = useMutation({
    mutationFn: () => api.post(`/api/admin/barbearias/${bloqueando!.id}/bloquear`, { motivo }),
    onSuccess: () => {
      atualizar()
      avisar('Barbearia bloqueada. Ela saiu do app.')
      setBloqueando(null)
      setMotivo('')
    },
    onError: (e) => avisar(e.message, true),
  })

  const desbloquear = useMutation({
    mutationFn: (id: number) => api.post(`/api/admin/barbearias/${id}/desbloquear`),
    onSuccess: () => {
      atualizar()
      avisar('Barbearia desbloqueada.')
    },
    onError: (e) => avisar(e.message, true),
  })

  const redefinir = useMutation({
    mutationFn: () => api.post(`/api/admin/barbearias/${senhaPara!.id}/redefinir-senha`, { novaSenha }),
    onSuccess: () => {
      avisar('Senha do responsável redefinida.')
      setSenhaPara(null)
      setNovaSenha('')
    },
    onError: (e) => avisar(e.message, true),
  })

  return (
    <>
      <Cabecalho titulo="Barbearias" subtitulo="Cadastro, mensalidade e bloqueio de cada barbearia assinante.">
        <button className="btn btn-primario" onClick={() => abrir('nova')}>
          <Plus size={18} aria-hidden /> Nova barbearia
        </button>
      </Cabecalho>
      <div className="conteudo">
        <div className="barra-filtros">
          <input
            className="entrada"
            style={{ maxWidth: 320 }}
            type="search"
            placeholder="Buscar por nome ou cidade"
            aria-label="Buscar barbearia"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
          <div className="fichas" role="group" aria-label="Situação">
            {(
              [
                ['', 'Todas'],
                ['ATIVA', 'Ativas'],
                ['BLOQUEADA', 'Bloqueadas'],
              ] as const
            ).map(([v, r]) => (
              <button key={v} className="ficha" aria-pressed={status === v} onClick={() => setStatus(v)}>
                {r}
              </button>
            ))}
          </div>
        </div>
        <section className="painel">
          {lista.isLoading ? (
            <Carregando />
          ) : lista.error ? (
            <ErroCarga erro={lista.error} />
          ) : !lista.data?.length ? (
            <Vazio>Nenhuma barbearia encontrada.</Vazio>
          ) : (
            <div className="tabela-envelope">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Barbearia</th>
                    <th>Responsável</th>
                    <th className="direita">Mensalidade</th>
                    <th>Vencimento</th>
                    <th>Situação</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {lista.data.map((b) => (
                    <tr key={b.id}>
                      <td>
                        <strong>{b.nome}</strong>
                        <div className="mudo pequeno">
                          {[b.cidade, b.uf].filter(Boolean).join('/') || 'Sem endereço'}, {b.barbeirosAtivos} barbeiro(s)
                        </div>
                      </td>
                      <td>
                        {b.donoNome}
                        <div className="mudo pequeno">{b.donoEmail}</div>
                      </td>
                      <td className="direita numero">{b.valorMensalidade != null ? moeda(b.valorMensalidade) : '—'}</td>
                      <td className="numero">{b.diaVencimento ? `Dia ${b.diaVencimento}` : '—'}</td>
                      <td>
                        <Selo status={b.status} />
                        {b.motivoBloqueio && <div className="mudo pequeno">{b.motivoBloqueio}</div>}
                      </td>
                      <td className="direita" style={{ whiteSpace: 'nowrap' }}>
                        <button className="btn btn-pequeno" onClick={() => abrir(b)}>
                          Editar
                        </button>{' '}
                        <button className="btn btn-pequeno" onClick={() => setSenhaPara(b)}>
                          Senha
                        </button>{' '}
                        {b.status === 'ATIVA' ? (
                          <button className="btn btn-perigo btn-pequeno" onClick={() => setBloqueando(b)}>
                            Bloquear
                          </button>
                        ) : (
                          <button className="btn btn-pequeno" onClick={() => desbloquear.mutate(b.id)}>
                            Desbloquear
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

      <Modal titulo={editando === 'nova' ? 'Nova barbearia' : 'Editar barbearia'} aberto={editando !== null} aoFechar={() => setEditando(null)} largo>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            salvar.mutate()
          }}
        >
          <fieldset className="painel painel-corpo form" style={{ margin: 0 }}>
            <legend className="pequeno" style={{ fontWeight: 600, padding: '0 6px' }}>
              Mensalidade
            </legend>
            <div className="linha-form">
              <Campo
                rotulo="Valor (R$)"
                inputMode="decimal"
                value={cob.valorMensalidade}
                onChange={(e) => setCob({ ...cob, valorMensalidade: e.target.value })}
                ajuda="Em branco: sem cobrança."
              />
              <Campo rotulo="Dia do vencimento" type="number" min={1} max={31} value={cob.diaVencimento} onChange={(e) => setCob({ ...cob, diaVencimento: e.target.value })} />
              <Campo
                rotulo="Dias de tolerância"
                type="number"
                min={0}
                max={60}
                value={cob.diasTolerancia}
                onChange={(e) => setCob({ ...cob, diasTolerancia: e.target.value })}
                ajuda="Depois disso, bloqueia sozinho."
              />
            </div>
          </fieldset>
          {editando === 'nova' && (
            <fieldset className="painel painel-corpo form" style={{ margin: 0 }}>
              <legend className="pequeno" style={{ fontWeight: 600, padding: '0 6px' }}>
                Responsável (acesso ao portal)
              </legend>
              <div className="linha-form">
                <Campo rotulo="Nome" required maxLength={120} value={dono.donoNome} onChange={(e) => setDono({ ...dono, donoNome: e.target.value })} />
                <Campo rotulo="E-mail" type="email" required maxLength={160} value={dono.donoEmail} onChange={(e) => setDono({ ...dono, donoEmail: e.target.value })} />
                <Campo
                  rotulo="Senha inicial"
                  type="text"
                  required
                  minLength={8}
                  value={dono.donoSenha}
                  onChange={(e) => setDono({ ...dono, donoSenha: e.target.value })}
                  ajuda="Peça para trocar no primeiro acesso."
                />
              </div>
              <label className="checagem">
                <input type="checkbox" checked={dono.donoAtende} onChange={(e) => setDono({ ...dono, donoAtende: e.target.checked })} />
                O responsável também corta (já cria a agenda dele)
              </label>
            </fieldset>
          )}
          <CamposBarbearia d={d} set={setD} />
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setEditando(null)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={salvar.isPending}>
              {editando === 'nova' ? 'Cadastrar barbearia' : 'Salvar alterações'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal titulo={`Bloquear ${bloqueando?.nome ?? ''}`} aberto={!!bloqueando} aoFechar={() => setBloqueando(null)}>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            bloquear.mutate()
          }}
        >
          <p>A barbearia sai do app, deixa de receber agendamentos e o portal fica só para consulta. Os agendamentos já feitos continuam.</p>
          <Texto rotulo="Motivo (o responsável vê no portal)" required maxLength={255} value={motivo} onChange={(e) => setMotivo(e.target.value)} />
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setBloqueando(null)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-perigo" disabled={!motivo.trim() || bloquear.isPending}>
              Bloquear barbearia
            </button>
          </div>
        </form>
      </Modal>

      <Modal titulo="Redefinir senha do responsável" aberto={!!senhaPara} aoFechar={() => setSenhaPara(null)}>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            redefinir.mutate()
          }}
        >
          <p className="mudo">
            {senhaPara?.donoNome} ({senhaPara?.donoEmail}) vai entrar com a nova senha. As sessões abertas são encerradas.
          </p>
          <Campo rotulo="Nova senha" type="text" minLength={8} required value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} />
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setSenhaPara(null)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={novaSenha.length < 8 || redefinir.isPending}>
              Redefinir senha
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
