import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Campo } from '../componentes/Campo'

export function Login() {
  const { entrar } = useAuth()
  const navegar = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      const u = await entrar(email, senha)
      navegar(u.papel === 'SUPER_ADMIN' ? '/admin' : u.papel === 'ADMIN_BARBEARIA' ? '/painel' : '/agenda', { replace: true })
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Não foi possível entrar.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="login">
      <section className="login-arte" aria-hidden="false">
        <strong style={{ fontSize: 18, fontVariationSettings: "'wdth' 125" }}>MyBarber</strong>
        <div>
          <h1>Sua cadeira cheia, sem encaixe errado.</h1>
          <div className="login-filete" />
          <p>
            O cliente só vê os horários em que o corte dele cabe inteiro na sua agenda. Aqui você define expediente,
            serviços e preços, remarca quando precisar e acompanha o caixa do mês.
          </p>
        </div>
        <p className="pequeno" style={{ display: 'flex', gap: 20, flexWrap: 'wrap' }}>
          <Link to="/legal/termos-barbearia" style={{ color: '#c7d6d0' }}>
            Termos para barbearias
          </Link>
          <Link to="/legal/politica-de-privacidade" style={{ color: '#c7d6d0' }}>
            Política de privacidade
          </Link>
        </p>
      </section>

      <section className="login-form">
        <form className="form" onSubmit={enviar} noValidate>
          <div>
            <h2>Entrar no portal</h2>
            <p className="mudo pequeno" style={{ marginTop: 6 }}>
              Use o e-mail cadastrado da barbearia ou do barbeiro.
            </p>
          </div>
          {erro && (
            <div className="erro-form" role="alert">
              {erro}
            </div>
          )}
          <Campo rotulo="E-mail" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          <Campo
            rotulo="Senha"
            type="password"
            autoComplete="current-password"
            required
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
          />
          <button className="btn btn-primario" type="submit" disabled={enviando || !email || !senha}>
            {enviando ? 'Entrando…' : 'Entrar'}
          </button>
          <p className="mudo pequeno">Esqueceu a senha? Fale com o suporte do MyBarber para redefinir.</p>
        </form>
      </section>
    </div>
  )
}
