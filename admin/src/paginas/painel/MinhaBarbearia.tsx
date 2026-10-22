import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import { Cabecalho, Carregando, ErroCarga } from '../../componentes/Basicos'
import { useAvisos } from '../../componentes/Avisos'
import { CamposBarbearia, dadosIniciais, paraRequisicao, type DadosBarbearia } from '../../componentes/FormBarbearia'
import { useMinhaBarbearia } from '../../dados'

export function MinhaBarbearia() {
  const barbearia = useMinhaBarbearia()
  const qc = useQueryClient()
  const avisar = useAvisos()
  const [d, setD] = useState<DadosBarbearia>(dadosIniciais())

  useEffect(() => {
    if (barbearia.data) setD(dadosIniciais(barbearia.data))
  }, [barbearia.data])

  const salvar = useMutation({
    mutationFn: () => api.put('/api/painel/barbearia', paraRequisicao(d)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['barbearia'] })
      avisar('Dados da barbearia salvos.')
    },
    onError: (e) => avisar(e.message, true),
  })

  if (barbearia.isLoading) return <Carregando />
  if (barbearia.error) return <ErroCarga erro={barbearia.error} />

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        salvar.mutate()
      }}
    >
      <Cabecalho titulo="Minha barbearia" subtitulo="Como a barbearia aparece no app e as regras da agenda.">
        <button className="btn btn-primario" type="submit" disabled={salvar.isPending}>
          {salvar.isPending ? 'Salvando…' : 'Salvar alterações'}
        </button>
      </Cabecalho>
      <div className="conteudo">
        <CamposBarbearia d={d} set={setD} />
      </div>
    </form>
  )
}
