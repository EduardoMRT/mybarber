import type { ErroApi, Tokens, Usuario } from './types'

const BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? ''
const CHAVE = 'mybarber-sessao'

export class ApiError extends Error {
  status: number
  campos?: Record<string, string>

  constructor(status: number, mensagem: string, campos?: Record<string, string>) {
    super(mensagem)
    this.status = status
    this.campos = campos
  }
}

let sessao: Tokens | null = carregar()
const ouvintes = new Set<(s: Tokens | null) => void>()

function carregar(): Tokens | null {
  try {
    const bruto = localStorage.getItem(CHAVE)
    return bruto ? (JSON.parse(bruto) as Tokens) : null
  } catch {
    return null
  }
}

export function obterSessao() {
  return sessao
}

export function definirSessao(nova: Tokens | null) {
  sessao = nova
  try {
    if (nova) localStorage.setItem(CHAVE, JSON.stringify(nova))
    else localStorage.removeItem(CHAVE)
  } catch {
    /* armazenamento indisponível: a sessão vale só nesta aba */
  }
  ouvintes.forEach((f) => f(nova))
}

export function atualizarUsuario(usuario: Usuario) {
  if (sessao) definirSessao({ ...sessao, usuario })
}

export function aoMudarSessao(f: (s: Tokens | null) => void) {
  ouvintes.add(f)
  return () => {
    ouvintes.delete(f)
  }
}

let renovando: Promise<boolean> | null = null

async function renovar(): Promise<boolean> {
  if (!sessao?.refreshToken) return false
  if (!renovando) {
    const refreshToken = sessao.refreshToken
    renovando = fetch(`${BASE}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
      .then(async (r) => {
        if (!r.ok) {
          definirSessao(null)
          return false
        }
        definirSessao((await r.json()) as Tokens)
        return true
      })
      .catch(() => false)
      .finally(() => {
        renovando = null
      })
  }
  return renovando
}

async function requisitar<T>(metodo: string, caminho: string, corpo?: unknown, tentarRenovar = true): Promise<T> {
  const headers: Record<string, string> = {}
  if (corpo !== undefined) headers['Content-Type'] = 'application/json'
  if (sessao?.accessToken) headers.Authorization = `Bearer ${sessao.accessToken}`

  let resp: Response
  try {
    resp = await fetch(`${BASE}${caminho}`, {
      method: metodo,
      headers,
      body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Sem conexão com o servidor. Verifique sua internet e tente de novo.')
  }

  if (resp.status === 401 && tentarRenovar && sessao?.refreshToken && !caminho.startsWith('/api/auth/')) {
    if (await renovar()) return requisitar<T>(metodo, caminho, corpo, false)
  }

  if (!resp.ok) {
    let erro: Partial<ErroApi> = {}
    try {
      erro = (await resp.json()) as ErroApi
    } catch {
      /* resposta sem corpo */
    }
    if (resp.status === 401 && !caminho.startsWith('/api/auth/login')) definirSessao(null)
    throw new ApiError(resp.status, erro.mensagem ?? `Erro ${resp.status}`, erro.campos)
  }

  if (resp.status === 204) return undefined as T
  const texto = await resp.text()
  return (texto ? JSON.parse(texto) : undefined) as T
}

export const api = {
  get: <T>(caminho: string) => requisitar<T>('GET', caminho),
  post: <T>(caminho: string, corpo?: unknown) => requisitar<T>('POST', caminho, corpo ?? {}),
  put: <T>(caminho: string, corpo: unknown) => requisitar<T>('PUT', caminho, corpo),
  del: <T = void>(caminho: string) => requisitar<T>('DELETE', caminho),
}

export function query(params: Record<string, string | number | undefined | null>) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') q.set(k, String(v))
  })
  const s = q.toString()
  return s ? `?${s}` : ''
}
