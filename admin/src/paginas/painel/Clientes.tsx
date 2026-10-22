import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { ClienteResumo } from '../../api/types'
import { Cabecalho, Carregando, ErroCarga, Vazio } from '../../componentes/Basicos'
import { dataHora, moeda } from '../../util/formato'

export function Clientes() {
  const [busca, setBusca] = useState('')
  const clientes = useQuery({ queryKey: ['clientes'], queryFn: () => api.get<ClienteResumo[]>('/api/painel/clientes') })
  const t = busca.trim().toLowerCase()
  const lista = (clientes.data ?? []).filter((c) => !t || c.nome.toLowerCase().includes(t) || (c.telefone ?? '').includes(t))

  return (
    <>
      <Cabecalho titulo="Clientes" subtitulo="Quem já agendou pelo app com a sua barbearia." />
      <div className="conteudo">
        <input
          className="entrada"
          style={{ maxWidth: 360 }}
          type="search"
          placeholder="Buscar por nome ou telefone"
          aria-label="Buscar cliente"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
        />
        <section className="painel">
          {clientes.isLoading ? (
            <Carregando />
          ) : clientes.error ? (
            <ErroCarga erro={clientes.error} />
          ) : lista.length === 0 ? (
            <Vazio>{t ? 'Nenhum cliente encontrado.' : 'Os clientes aparecem aqui depois do primeiro agendamento pelo app.'}</Vazio>
          ) : (
            <div className="tabela-envelope">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Cliente</th>
                    <th>Tipo de cabelo</th>
                    <th className="direita">Atendimentos</th>
                    <th className="direita">Faltas</th>
                    <th>Último corte</th>
                    <th>Próximo horário</th>
                    <th className="direita">Total gasto</th>
                  </tr>
                </thead>
                <tbody>
                  {lista.map((c) => (
                    <tr key={c.clienteId}>
                      <td>
                        <strong>{c.nome}</strong>
                        {c.telefone && (
                          <div className="pequeno">
                            <a href={`https://wa.me/55${c.telefone.replace(/\D/g, '')}`} target="_blank" rel="noreferrer">
                              {c.telefone}
                            </a>
                          </div>
                        )}
                      </td>
                      <td>{c.tipoCabelo ?? <span className="mudo">—</span>}</td>
                      <td className="direita numero">{c.atendimentos}</td>
                      <td className="direita numero">{c.faltas}</td>
                      <td className="numero">{c.ultimoAtendimento ? dataHora(c.ultimoAtendimento) : <span className="mudo">—</span>}</td>
                      <td className="numero">{c.proximoAgendamento ? dataHora(c.proximoAgendamento) : <span className="mudo">—</span>}</td>
                      <td className="direita numero">{moeda(c.totalGasto)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </>
  )
}
