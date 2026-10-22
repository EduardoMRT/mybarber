import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'

type Aviso = { id: number; texto: string; erro?: boolean }
type Avisar = (texto: string, erro?: boolean) => void

const Ctx = createContext<Avisar>(() => undefined)

export function AvisosProvider({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([])

  const avisar = useCallback<Avisar>((texto, erro) => {
    const id = Date.now() + Math.random()
    setAvisos((a) => [...a, { id, texto, erro }])
    setTimeout(() => setAvisos((a) => a.filter((x) => x.id !== id)), erro ? 6000 : 3500)
  }, [])

  return (
    <Ctx.Provider value={avisar}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} className={a.erro ? 'aviso aviso-erro' : 'aviso'}>
            {a.texto}
          </div>
        ))}
      </div>
    </Ctx.Provider>
  )
}

export const useAvisos = () => useContext(Ctx)
