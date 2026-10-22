import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import type { Papel } from './api/types'
import { Layout } from './componentes/Layout'
import { Login } from './paginas/Login'
import { Legal } from './paginas/Legal'
import { Conta } from './paginas/Conta'
import { Notificacoes } from './paginas/Notificacoes'
import { Dashboard } from './paginas/painel/Dashboard'
import { Agenda } from './paginas/painel/Agenda'
import { Horarios } from './paginas/painel/Horarios'
import { Barbeiros } from './paginas/painel/Barbeiros'
import { Servicos } from './paginas/painel/Servicos'
import { Clientes } from './paginas/painel/Clientes'
import { Financeiro } from './paginas/painel/Financeiro'
import { MinhaBarbearia } from './paginas/painel/MinhaBarbearia'
import { Assinatura } from './paginas/painel/Assinatura'
import { AdminDashboard } from './paginas/admin/AdminDashboard'
import { Barbearias } from './paginas/admin/Barbearias'
import { Cobrancas } from './paginas/admin/Cobrancas'
import { TiposCabelo } from './paginas/admin/TiposCabelo'

function Exige({ papeis, children }: { papeis: Papel[]; children: ReactNode }) {
  const { usuario } = useAuth()
  if (!usuario) return <Navigate to="/login" replace />
  if (!papeis.includes(usuario.papel)) return <Navigate to="/" replace />
  return <>{children}</>
}

function Inicio() {
  const { usuario } = useAuth()
  if (!usuario) return <Navigate to="/login" replace />
  if (usuario.papel === 'SUPER_ADMIN') return <Navigate to="/admin" replace />
  if (usuario.papel === 'ADMIN_BARBEARIA') return <Navigate to="/painel" replace />
  return <Navigate to="/agenda" replace />
}

const DONO: Papel[] = ['ADMIN_BARBEARIA']
const EQUIPE: Papel[] = ['ADMIN_BARBEARIA', 'BARBEIRO']
const ADMIN: Papel[] = ['SUPER_ADMIN']
const TODOS: Papel[] = ['ADMIN_BARBEARIA', 'BARBEIRO', 'SUPER_ADMIN']

export function App() {
  const { usuario } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={usuario ? <Navigate to="/" replace /> : <Login />} />
      {!usuario && <Route path="/legal/:slug" element={<Legal />} />}
      <Route path="/" element={<Inicio />} />
      <Route
        element={
          <Exige papeis={TODOS}>
            <Layout />
          </Exige>
        }
      >
        <Route path="/painel" element={<Exige papeis={DONO}><Dashboard /></Exige>} />
        <Route path="/agenda" element={<Exige papeis={EQUIPE}><Agenda /></Exige>} />
        <Route path="/horarios" element={<Exige papeis={EQUIPE}><Horarios /></Exige>} />
        <Route path="/barbeiros" element={<Exige papeis={DONO}><Barbeiros /></Exige>} />
        <Route path="/servicos" element={<Exige papeis={DONO}><Servicos /></Exige>} />
        <Route path="/clientes" element={<Exige papeis={DONO}><Clientes /></Exige>} />
        <Route path="/financeiro" element={<Exige papeis={DONO}><Financeiro /></Exige>} />
        <Route path="/barbearia" element={<Exige papeis={DONO}><MinhaBarbearia /></Exige>} />
        <Route path="/assinatura" element={<Exige papeis={DONO}><Assinatura /></Exige>} />
        <Route path="/notificacoes" element={<Exige papeis={EQUIPE}><Notificacoes /></Exige>} />
        <Route path="/admin" element={<Exige papeis={ADMIN}><AdminDashboard /></Exige>} />
        <Route path="/admin/barbearias" element={<Exige papeis={ADMIN}><Barbearias /></Exige>} />
        <Route path="/admin/cobrancas" element={<Exige papeis={ADMIN}><Cobrancas /></Exige>} />
        <Route path="/admin/tipos-cabelo" element={<Exige papeis={ADMIN}><TiposCabelo /></Exige>} />
        <Route path="/conta" element={<Conta />} />
        <Route path="/legal/:slug" element={<Legal />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
