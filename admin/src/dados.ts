import { useQuery } from '@tanstack/react-query'
import { api } from './api/client'
import type { Barbearia, Barbeiro, Servico, TipoCabelo } from './api/types'

export const useBarbeiros = () =>
  useQuery({ queryKey: ['barbeiros'], queryFn: () => api.get<Barbeiro[]>('/api/painel/barbeiros') })

export const useServicos = () =>
  useQuery({ queryKey: ['servicos'], queryFn: () => api.get<Servico[]>('/api/painel/servicos') })

export const useTiposCabelo = () =>
  useQuery({
    queryKey: ['tipos-cabelo'],
    queryFn: () => api.get<TipoCabelo[]>('/api/painel/tipos-cabelo'),
    staleTime: 10 * 60_000,
  })

export const useMinhaBarbearia = () =>
  useQuery({ queryKey: ['barbearia'], queryFn: () => api.get<Barbearia>('/api/painel/barbearia') })
