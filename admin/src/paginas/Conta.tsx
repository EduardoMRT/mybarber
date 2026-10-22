import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Cabecalho } from '../componentes/Basicos'
import { Campo } from '../componentes/Campo'
import { useAvisos } from '../componentes/Avisos'

export function Conta() {
  const { usuario, sair } = useAuth()
  const avisar = useAvisos()
  const [atual, setAtual] = useState('')
  const [nova, setNova] = useState('')
  const [confirma, setConfirma] = useState('')

  const trocar = useMutation({
    mutationFn: () => api.put('/api/auth/senha', { senhaAtual: atual, novaSenha: nova }),
    onSuccess: async () => {
      avisar('Senha alterada. Entre de novo com a nova senha.')
      await sair()
    },
    onError: (e) => avisar(e.message, true),
  })

  const diferente = confirma.length > 0 && confirma !== nova

  return (
    <>
      <Cabecalho titulo="Minha conta" subtitulo={`${usuario?.nome}, ${usuario?.email}`} />
      <div className="conteudo">
        <section className="painel" style={{ maxWidth: 520 }}>
          <div className="painel-cabeca">
            <h2>Trocar senha</h2>
          </div>
          <form
            className="form painel-corpo"
            onSubmit={(e) => {
              e.preventDefault()
              trocar.mutate()
            }}
          >
            <Campo rotulo="Senha atual" type="password" autoComplete="current-password" required value={atual} onChange={(e) => setAtual(e.target.value)} />
            <Campo
              rotulo="Nova senha"
              type="password"
              autoComplete="new-password"
              minLength={8}
              required
              value={nova}
              onChange={(e) => setNova(e.target.value)}
              ajuda="Pelo menos 8 caracteres."
            />
            <Campo
              rotulo="Repita a nova senha"
              type="password"
              autoComplete="new-password"
              required
              value={confirma}
              onChange={(e) => setConfirma(e.target.value)}
              erro={diferente ? 'As senhas não conferem.' : undefined}
            />
            <div className="acoes-form">
              <button className="btn btn-primario" type="submit" disabled={trocar.isPending || diferente || nova.length < 8}>
                Trocar senha
              </button>
            </div>
          </form>
        </section>
      </div>
    </>
  )
}
