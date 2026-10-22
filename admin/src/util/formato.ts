import type { FormaPagamento, StatusAgendamento, StatusCobranca } from '../api/types'

const moedaFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const dataFmt = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
const dataCurtaFmt = new Intl.DateTimeFormat('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit' })
const dataLongaFmt = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })

export const moeda = (v?: number | null) => moedaFmt.format(Number(v ?? 0))

/** "2026-10-21T15:30:00" -> Date local (o backend usa o horário de Brasília). */
export function parseLocal(iso: string) {
  const [d, t = '00:00'] = iso.split('T')
  const [a, m, dia] = d.split('-').map(Number)
  const [h, min] = t.split(':').map(Number)
  return new Date(a, m - 1, dia, h, min)
}

export const hora = (iso: string) => iso.split('T')[1]?.slice(0, 5) ?? ''
export const data = (iso: string) => dataFmt.format(parseLocal(iso))
export const dataCurta = (iso: string) => dataCurtaFmt.format(parseLocal(iso)).replace('.', '')
export const dataLonga = (d: Date) => dataLongaFmt.format(d)
export const dataHora = (iso: string) => `${dataCurta(iso)}, ${hora(iso)}`

export function isoData(d: Date) {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export function somarDias(d: Date, n: number) {
  const r = new Date(d)
  r.setDate(r.getDate() + n)
  return r
}

export const minutosParaTexto = (min: number) => {
  if (min < 60) return `${min} min`
  const h = Math.floor(min / 60)
  const m = min % 60
  return m ? `${h} h ${m} min` : `${h} h`
}

export const porcentagem = (v: number) => `${Math.round(v * 100)}%`

export const statusAgendamento: Record<StatusAgendamento, string> = {
  CONFIRMADO: 'Confirmado',
  AGUARDANDO_CLIENTE: 'Aguardando cliente',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
  FALTA: 'Faltou',
}

export const statusCobranca: Record<StatusCobranca, string> = {
  PENDENTE: 'Em aberto',
  PAGA: 'Paga',
  ATRASADA: 'Atrasada',
  CANCELADA: 'Cancelada',
}

export const formasPagamento: Record<FormaPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  PIX: 'Pix',
  CARTAO_DEBITO: 'Cartão de débito',
  CARTAO_CREDITO: 'Cartão de crédito',
  OUTRO: 'Outro',
}

export const diasSemana = ['Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado', 'Domingo']
