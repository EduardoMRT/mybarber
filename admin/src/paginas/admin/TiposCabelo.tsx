import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { api } from '../../api/client'
import type { TipoCabeloAdmin } from '../../api/types'
import { Cabecalho, Carregando, ErroCarga } from '../../componentes/Basicos'
import { Campo } from '../../componentes/Campo'
import { useAvisos } from '../../componentes/Avisos'
import { Modal } from '../../componentes/Modal'

export function TiposCabelo() {
  const qc = useQueryClient()
  const avisar = useAvisos()
  const tipos = useQuery({ queryKey: ['admin-tipos'], queryFn: () => api.get<TipoCabeloAdmin[]>('/api/admin/tipos-cabelo') })
  const [editando, setEditando] = useState<TipoCabeloAdmin | 'novo' | null>(null)
  const [f, setF] = useState({ nome: '', descricao: '', ordem: '0', ativo: true })

  const abrir = (t: TipoCabeloAdmin | 'novo') => {
    setEditando(t)
    setF(t === 'novo' ? { nome: '', descricao: '', ordem: String((tipos.data?.length ?? 0) + 1), ativo: true } : { nome: t.nome, descricao: t.descricao ?? '', ordem: String(t.ordem), ativo: t.ativo })
  }

  const salvar = useMutation({
    mutationFn: () => {
      const corpo = { nome: f.nome, descricao: f.descricao || null, ordem: Number(f.ordem) || 0, ativo: f.ativo }
      return editando === 'novo' ? api.post('/api/admin/tipos-cabelo', corpo) : api.put(`/api/admin/tipos-cabelo/${(editando as TipoCabeloAdmin).id}`, corpo)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-tipos'] })
      avisar('Tipo de cabelo salvo.')
      setEditando(null)
    },
    onError: (e) => avisar(e.message, true),
  })

  return (
    <>
      <Cabecalho titulo="Tipos de cabelo" subtitulo="Lista usada no cadastro do cliente e na duração dos serviços de todas as barbearias.">
        <button className="btn btn-primario" onClick={() => abrir('novo')}>
          <Plus size={18} aria-hidden /> Novo tipo
        </button>
      </Cabecalho>
      <div className="conteudo">
        <section className="painel">
          {tipos.isLoading ? (
            <Carregando />
          ) : tipos.error ? (
            <ErroCarga erro={tipos.error} />
          ) : (
            <table className="tabela">
              <thead>
                <tr>
                  <th>Ordem</th>
                  <th>Nome</th>
                  <th>Descrição</th>
                  <th>Situação</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {tipos.data?.map((t) => (
                  <tr key={t.id}>
                    <td className="numero">{t.ordem}</td>
                    <td>
                      <strong>{t.nome}</strong>
                    </td>
                    <td className="mudo">{t.descricao}</td>
                    <td>{t.ativo ? 'Em uso' : <span className="mudo">Oculto</span>}</td>
                    <td className="direita">
                      <button className="btn btn-pequeno" onClick={() => abrir(t)}>
                        Editar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>
      <Modal titulo={editando === 'novo' ? 'Novo tipo de cabelo' : 'Editar tipo de cabelo'} aberto={editando !== null} aoFechar={() => setEditando(null)}>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault()
            salvar.mutate()
          }}
        >
          <div className="linha-form">
            <Campo rotulo="Nome" required maxLength={60} value={f.nome} onChange={(e) => setF({ ...f, nome: e.target.value })} />
            <Campo rotulo="Ordem na lista" type="number" value={f.ordem} onChange={(e) => setF({ ...f, ordem: e.target.value })} />
          </div>
          <Campo rotulo="Descrição" maxLength={255} value={f.descricao} onChange={(e) => setF({ ...f, descricao: e.target.value })} />
          <label className="checagem">
            <input type="checkbox" checked={f.ativo} onChange={(e) => setF({ ...f, ativo: e.target.checked })} />
            Mostrar no app e no portal
          </label>
          <div className="acoes-form">
            <button type="button" className="btn" onClick={() => setEditando(null)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primario" disabled={salvar.isPending}>
              Salvar
            </button>
          </div>
        </form>
      </Modal>
    </>
  )
}
