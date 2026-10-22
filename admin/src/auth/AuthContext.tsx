import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { aoMudarSessao, api, definirSessao, obterSessao } from '../api/client'
import type { Tokens, Usuario } from '../api/types'

interface Auth {
  usuario: Usuario | null
  entrar: (email: string, senha: string) => Promise<Usuario>
  sair: () => Promise<void>
}

const Ctx = createContext<Auth | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(obterSessao()?.usuario ?? null)

  useEffect(() => aoMudarSessao((s) => setUsuario(s?.usuario ?? null)), [])

  // atualiza dados do usuário (ex.: barbearia foi bloqueada/desbloqueada)
  useEffect(() => {
    if (!obterSessao()) return
    api
      .get<Usuario>('/api/auth/me')
      .then((u) => {
        const s = obterSessao()
        if (s) definirSessao({ ...s, usuario: u })
      })
      .catch(() => undefined)
  }, [])

  async function entrar(email: string, senha: string) {
    const t = await api.post<Tokens>('/api/auth/login', { email, senha })
    if (t.usuario.papel === 'CLIENTE') {
      throw new Error('Esta conta é de cliente. Use o aplicativo MyBarber no celular.')
    }
    definirSessao(t)
    return t.usuario
  }

  async function sair() {
    const s = obterSessao()
    if (s) await api.post('/api/auth/logout', { refreshToken: s.refreshToken }).catch(() => undefined)
    definirSessao(null)
  }

  return <Ctx.Provider value={{ usuario, entrar, sair }}>{children}</Ctx.Provider>
}

export function useAuth() {
  const c = useContext(Ctx)
  if (!c) throw new Error('useAuth fora do AuthProvider')
  return c
}
