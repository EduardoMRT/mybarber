import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Bell, Building2, CalendarDays, Clock, CreditCard, FileText, Gauge, KeyRound, LogOut, Menu, Receipt,
  Scissors, Store, Users, UserRound, Wallet, Waves,
} from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { api } from '../api/client'
import type { ResumoNotificacoes } from '../api/types'
import { BotaoTema } from './Tema'

type Item = { para: string; rotulo: string; icone: typeof Bell; fim?: boolean }

const itensDono: Item[] = [
  { para: '/painel', rotulo: 'Visão geral', icone: Gauge },
  { para: '/agenda', rotulo: 'Agenda', icone: CalendarDays },
  { para: '/horarios', rotulo: 'Horários de trabalho', icone: Clock },
  { para: '/barbeiros', rotulo: 'Barbeiros', icone: Users },
  { para: '/servicos', rotulo: 'Serviços e preços', icone: Scissors },
  { para: '/clientes', rotulo: 'Clientes', icone: UserRound },
  { para: '/financeiro', rotulo: 'Financeiro', icone: Wallet },
  { para: '/barbearia', rotulo: 'Minha barbearia', icone: Store },
  { para: '/assinatura', rotulo: 'Assinatura', icone: CreditCard },
]

const itensBarbeiro: Item[] = [
  { para: '/agenda', rotulo: 'Minha agenda', icone: CalendarDays },
  { para: '/horarios', rotulo: 'Meus horários', icone: Clock },
]

const itensAdmin: Item[] = [
  { para: '/admin', rotulo: 'Visão geral', icone: Gauge, fim: true },
  { para: '/admin/barbearias', rotulo: 'Barbearias', icone: Building2 },
  { para: '/admin/cobrancas', rotulo: 'Mensalidades', icone: Receipt },
  { para: '/admin/tipos-cabelo', rotulo: 'Tipos de cabelo', icone: Waves },
]

export function Layout() {
  const { usuario, sair } = useAuth()
  const [aberto, setAberto] = useState(false)
  const local = useLocation()
  const ehAdmin = usuario?.papel === 'SUPER_ADMIN'

  useEffect(() => setAberto(false), [local.pathname])

  const avisos = useQuery({
    queryKey: ['notificacoes'],
    queryFn: () => api.get<ResumoNotificacoes>('/api/painel/notificacoes'),
    enabled: !ehAdmin,
    refetchInterval: 60_000,
  })

  const itens = ehAdmin ? itensAdmin : usuario?.papel === 'ADMIN_BARBEARIA' ? itensDono : itensBarbeiro
  const naoLidas = avisos.data?.naoLidas ?? 0

  return (
    <div className="layout">
      <div className="menu-movel">
        <button onClick={() => setAberto(true)} aria-label="Abrir menu">
          <Menu size={22} />
        </button>
        <strong>MyBarber</strong>
        <span style={{ width: 38 }} />
      </div>
      {aberto && <div className="veu" onClick={() => setAberto(false)} />}

      <aside className={aberto ? 'lateral aberta' : 'lateral'} aria-label="Menu principal">
        <NavLink to={ehAdmin ? '/admin' : '/'} className="marca">
          <svg width="30" height="30" viewBox="0 0 32 32" aria-hidden>
            <rect width="32" height="32" rx="7" fill="rgb(255 255 255 / 0.08)" />
            <path d="M8 23V10l8 8 8-8v13" fill="none" stroke="#c99a45" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          <strong>MyBarber</strong>
        </NavLink>
        <div className="marca-loja">{ehAdmin ? 'Administração da plataforma' : usuario?.barbeariaNome}</div>

        <nav className="nav">
          {itens.map(({ para, rotulo, icone: Icone, fim }) => (
            <NavLink key={para} to={para} end={fim}>
              <Icone size={18} aria-hidden />
              {rotulo}
            </NavLink>
          ))}
        </nav>

        <div className="lateral-rodape">
          {!ehAdmin && (
            <NavLink to="/notificacoes" style={{ position: 'relative' }}>
              <Bell size={18} aria-hidden />
              Avisos
              {naoLidas > 0 && (
                <span className="contador" aria-label={`${naoLidas} não lidos`}>
                  {naoLidas > 99 ? '99+' : naoLidas}
                </span>
              )}
            </NavLink>
          )}
          <NavLink to="/legal/politica-de-privacidade">
            <FileText size={18} aria-hidden />
            Termos e privacidade
          </NavLink>
          <NavLink to="/conta">
            <KeyRound size={18} aria-hidden />
            Minha conta
          </NavLink>
          <BotaoTema />
          <button onClick={() => sair()}>
            <LogOut size={18} aria-hidden />
            Sair ({usuario?.nome.split(' ')[0]})
          </button>
        </div>
      </aside>

      <main className="principal" id="conteudo">
        {usuario?.barbeariaBloqueada && (
          <div className="aviso-bloqueio" role="alert" style={{ marginTop: 20 }}>
            <strong>Barbearia bloqueada.</strong>
            <span>
              {usuario.motivoBloqueio ?? 'Entre em contato com o suporte.'} Enquanto isso, ela não aparece no app e o portal
              fica só para consulta.
            </span>
          </div>
        )}
        <Outlet />
      </main>
    </div>
  )
}
