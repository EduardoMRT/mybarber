import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { api } from '../../api/client'
import type { Barbeiro } from '../../api/types'
import { Cabecalho, Carregando, ErroCarga, Vazio } from '../../componentes/Basicos'
import { Campo, Fichas, Texto } from '../../componentes/Campo'
import { useAvisos } from '../../componentes/Avisos'
import { Modal } from '../../componentes/Modal'
import { useBarbeiros, useServicos, useTiposCabelo } from '../../dados'

type Form = {
  nome: string
  telefone: string
  bio: string
  intervaloMin: string
  ativo: boolean
  tiposCabeloIds: number[]
  servicosIds: number[]
  email: string
  senha: string
}

const vazio: Form = { nome: '', telefone: '', bio: '', intervaloMin: '0', ativo: true, tiposCabeloIds: [], servicosIds: [], email: '', senha: '' }

export function Barbeiros() {
  const barbeiros = useBarbeiros()
  const servicos = useServicos()
  const tipos = useTiposCabelo()
  const qc = useQueryClient()
  const avisar = useAvisos()
  const [editando, setEditando] = useState<Barbeiro | 'novo' | null>(null)
  const [f, setF] = useState<Form>(vazio)

  const abrir = (b: Barbeiro | 'novo') => {
    setEditando(b)
    setF(
      b === 'novo'
        ? vazio
        : {
            nome: b.nome,
            telefone: b.telefone ?? '',
            bio: b.bio ?? '',
            intervaloMin: String(b.intervaloMin),
            ativo: b.ativo,
            tiposCabeloIds: b.tiposCabeloIds,
            servicosIds: b.servicosIds,
            email: b.email ?? '',
            senha: '',
          },
    )
  }

  const salvar = useMutation({
    mutationFn: () => {
      const corpo = {
        nome: f.nome,
        telefone: f.telefone || null,
        bio: f.bio || null,
        intervaloMin: Number(f.intervaloMin) || 0,
        ativo: f.ativo,
        tiposCabeloIds: f.tiposCabeloIds,
        servicosIds: f.servicosIds,
        email: f.email || null,
        senha: f.senha || null,
      }
      return editando === 'novo' ? api.post('/api/painel/barbeiros', corpo) : api.put(`/api/painel/barbeiros/${(editando as Barbeiro).id}`, corpo)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['barbeiros'] })
      avisar(editando === 'novo' ? 'Barbeiro cadastrado.' : 'Barbeiro atualizado.')
      setEditando(null)
    },
    onError: (e) => avisar(e.message, true),
  })

  const nomesTipos = (ids: number[]) =>
    ids.length === 0 ? 'Todos' : (tipos.data ?? []).filter((t) => ids.includes(t.id)).map((t) => t.nome).join(', ')
  const nomesServ = (ids: number[]) =>
    ids.length === 0 ? 'Todos' : (servicos.data ?? []).filter((s) => ids.includes(s.id)).map((s) => s.nome).join(', ')

  const ehNovo = editando === 'novo'
  const temLogin = editando !== null && editando !== 'novo' && editando.temLogin

  return (
    <>
      <Cabecalho titulo="Barbeiros" subtitulo="Quem atende, quais tipos de cabelo e quais serviços cada um faz.">
        <button className="btn btn-primario" onClick={() => abrir('novo')}>
          <Plus size={18} aria-hidden /> Novo barbeiro
        </button>
      </Cabecalho>
      <div className="conteudo">
        <section className="painel">
          {barbeiros.isLoading ? (
            <Carregando />
          ) : barbeiros.error ? (
            <ErroCarga erro={barbeiros.error} />
          ) : !barbeiros.data?.length ? (
            <Vazio acao={<button className="btn btn-primario" onClick={() => abrir('novo')}>Cadastrar o primeiro</button>}>
              Nenhum barbeiro cadastrado ainda.
            </Vazio>
          ) : (
            <div className="tabela-envelope">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Nome</th>
                    <th>Tipos de cabelo</th>
                    <th>Serviços</th>
                    <th>Intervalo</th>
                    <th>Acesso ao portal</th>
                    <th>Situação</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {barbeiros.data.map((b) => (
                    <tr key={b.id}>
                      <td>
                        <strong>{b.nome}</strong>
                        {b.dono && <div className="mudo pequeno">Responsável pela barbearia</div>}
                      </td>
                      <td>{nomesTipos(b.tiposCabeloIds)}</td>
                      <td>{nomesServ(b.servicosIds)}</td>
                      <td className="numero">{b.intervaloMin ? `${b.intervaloMin} min` : 'Sem intervalo'}</td>
                      <td>{b.temLogin ? b.email : <span className="mudo">Sem acesso</span>}</td>
                      <td>{b.ativo ? 'Atendendo' : <span className="mudo">Inativo</span>}</td>
                      <td className="direita">
                        <button className="btn btn-pequeno" onClick={() => abrir(b)}>
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

      <Modal titulo={ehNovo ? 'Novo barbeiro' : 'Editar barbeiro'} aberto={editando !== null} aoFechar={() => setEditando(null)} largo>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            salvar.mutate()
          }}
        >
          <div className="linha-form">
            <Campo rotulo="Nome" required maxLength={120} value={f.nome} onChange={(e) => setF({ ...f, nome: e.target.value })} />
            <Campo rotulo="Telefone" inputMode="tel" maxLength={20} value={f.telefone} onChange={(e) => setF({ ...f, telefone: e.target.value })} />
            <Campo
              rotulo="Intervalo entre clientes (min)"
              type="number"
              min={0}
              max={120}
              value={f.intervaloMin}
              onChange={(e) => setF({ ...f, intervaloMin: e.target.value })}
              ajuda="Tempo para limpeza entre um atendimento e outro."
            />
          </div>
          <Texto rotulo="Apresentação (aparece no app)" maxLength={500} value={f.bio} onChange={(e) => setF({ ...f, bio: e.target.value })} />
          <Fichas
            rotulo="Tipos de cabelo que atende"
            ajuda="Nenhum marcado = atende todos."
            opcoes={tipos.data ?? []}
            valor={f.tiposCabeloIds}
            aoMudar={(v) => setF({ ...f, tiposCabeloIds: v })}
          />
          <Fichas
            rotulo="Serviços que realiza"
            ajuda="Nenhum marcado = realiza todos os serviços da barbearia."
            opcoes={(servicos.data ?? []).filter((s) => s.ativo).map((s) => ({ id: s.id, nome: s.nome }))}
            valor={f.servicosIds}
            aoMudar={(v) => setF({ ...f, servicosIds: v })}
          />
          <fieldset className="painel painel-corpo" style={{ margin: 0 }}>
            <legend className="pequeno" style={{ fontWeight: 600, padding: '0 6px' }}>
              Acesso ao portal (opcional)
            </legend>
            <p className="mudo pequeno" style={{ marginBottom: 12 }}>
              Com acesso, o barbeiro vê a própria agenda, ajusta os próprios horários e conclui atendimentos. Ele não vê o
              financeiro.
            </p>
            <div className="linha-form">
              <Campo rotulo="E-mail de acesso" type="email" maxLength={160} value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} />
              <Campo
                rotulo={temLogin ? 'Nova senha (deixe em branco para manter)' : 'Senha inicial'}
                type="password"
                minLength={8}
                autoComplete="new-password"
                value={f.senha}
                onChange={(e) => setF({ ...f, senha: e.target.value })}
              />
            </div>
          </fieldset>
          {!ehNovo && (
            <label className="checagem">
              <input type="checkbox" checked={f.ativo} onChange={(e) => setF({ ...f, ativo: e.target.checked })} />
              Atendendo (desmarque para tirar da agenda e do app)
            </label>
          )}
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setEditando(null)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={salvar.isPending}>
              {ehNovo ? 'Cadastrar barbeiro' : 'Salvar alterações'}
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
