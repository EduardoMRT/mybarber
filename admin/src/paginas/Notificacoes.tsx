import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { ResumoNotificacoes } from '../api/types'
import { Cabecalho, Carregando, ErroCarga, Vazio } from '../componentes/Basicos'
import { dataHora } from '../util/formato'

export function Notificacoes() {
  const qc = useQueryClient()
  const n = useQuery({ queryKey: ['notificacoes'], queryFn: () => api.get<ResumoNotificacoes>('/api/painel/notificacoes') })
  const todas = useMutation({
    mutationFn: () => api.post('/api/painel/notificacoes/lidas'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notificacoes'] }),
  })
  const uma = useMutation({
    mutationFn: (id: number) => api.post(`/api/painel/notificacoes/${id}/lida`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notificacoes'] }),
  })

  return (
    <>
      <Cabecalho titulo="Avisos" subtitulo="Novos agendamentos, cancelamentos e respostas de remarcação.">
        <button className="btn" disabled={!n.data?.naoLidas || todas.isPending} onClick={() => todas.mutate()}>
          Marcar todos como lidos
        </button>
      </Cabecalho>
      <div className="conteudo">
        <section className="painel">
          {n.isLoading ? (
            <Carregando />
          ) : n.error ? (
            <ErroCarga erro={n.error} />
          ) : !n.data?.itens.length ? (
            <Vazio>Nenhum aviso por enquanto.</Vazio>
          ) : (
            <div className="notificacoes-lista">
              {n.data.itens.map((x) => (
                <div key={x.id} className={x.lida ? 'notificacao' : 'notificacao nao-lida'}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                    <strong>{x.titulo}</strong>
                    <span className="mudo pequeno numero">{dataHora(x.criadoEm)}</span>
                  </div>
                  <p>{x.mensagem}</p>
                  {!x.lida && (
                    <button className="btn btn-fantasma btn-pequeno" style={{ alignSelf: 'flex-start', padding: 0 }} onClick={() => uma.mutate(x.id)}>
                      Marcar como lido
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </>
  )
}
