import { useState } from 'react'
import { MapPin } from 'lucide-react'
import type { Barbearia } from '../api/types'
import { Campo, Texto } from './Campo'
import { MapaLocal, buscarCep, geocodificar } from './MapaLocal'
import { useAvisos } from './Avisos'

export type DadosBarbearia = {
  nome: string
  documento: string
  telefone: string
  email: string
  descricao: string
  cep: string
  logradouro: string
  numero: string
  complemento: string
  bairro: string
  cidade: string
  uf: string
  latitude?: number
  longitude?: number
  gradeMinutos: string
  antecedenciaMinimaMin: string
  prazoCancelamentoMin: string
  prazoRespostaRemarcacaoHoras: string
}

export function dadosIniciais(b?: Barbearia): DadosBarbearia {
  return {
    nome: b?.nome ?? '',
    documento: b?.documento ?? '',
    telefone: b?.telefone ?? '',
    email: b?.email ?? '',
    descricao: b?.descricao ?? '',
    cep: b?.cep ?? '',
    logradouro: b?.logradouro ?? '',
    numero: b?.numero ?? '',
    complemento: b?.complemento ?? '',
    bairro: b?.bairro ?? '',
    cidade: b?.cidade ?? '',
    uf: b?.uf ?? '',
    latitude: b?.latitude ?? undefined,
    longitude: b?.longitude ?? undefined,
    gradeMinutos: String(b?.gradeMinutos ?? 15),
    antecedenciaMinimaMin: String(b?.antecedenciaMinimaMin ?? 30),
    prazoCancelamentoMin: String(b?.prazoCancelamentoMin ?? 120),
    prazoRespostaRemarcacaoHoras: String(b?.prazoRespostaRemarcacaoHoras ?? 12),
  }
}

export function paraRequisicao(d: DadosBarbearia) {
  const vazio = (s: string) => (s.trim() ? s.trim() : null)
  return {
    nome: d.nome.trim(),
    documento: vazio(d.documento),
    telefone: vazio(d.telefone),
    email: vazio(d.email),
    descricao: vazio(d.descricao),
    cep: vazio(d.cep),
    logradouro: vazio(d.logradouro),
    numero: vazio(d.numero),
    complemento: vazio(d.complemento),
    bairro: vazio(d.bairro),
    cidade: vazio(d.cidade),
    uf: vazio(d.uf)?.toUpperCase() ?? null,
    latitude: d.latitude ?? null,
    longitude: d.longitude ?? null,
    gradeMinutos: Number(d.gradeMinutos),
    antecedenciaMinimaMin: Number(d.antecedenciaMinimaMin),
    prazoCancelamentoMin: Number(d.prazoCancelamentoMin),
    prazoRespostaRemarcacaoHoras: Number(d.prazoRespostaRemarcacaoHoras),
  }
}

/** Campos da barbearia: dados, endereço com mapa e regras de agendamento. */
export function CamposBarbearia({ d, set, comRegras = true }: { d: DadosBarbearia; set: (d: DadosBarbearia) => void; comRegras?: boolean }) {
  const avisar = useAvisos()
  const [buscando, setBuscando] = useState(false)
  const campo = (k: keyof DadosBarbearia) => (e: { target: { value: string } }) => set({ ...d, [k]: e.target.value })

  async function aoSairCep() {
    const r = await buscarCep(d.cep).catch(() => null)
    if (r) set({ ...d, logradouro: r.logradouro || d.logradouro, bairro: r.bairro || d.bairro, cidade: r.localidade, uf: r.uf })
  }

  async function localizar() {
    setBuscando(true)
    try {
      const p = await geocodificar([d.logradouro, d.numero, d.bairro, d.cidade, d.uf].filter(Boolean).join(', '))
      if (p) set({ ...d, latitude: p.lat, longitude: p.lng })
      else avisar('Endereço não encontrado no mapa. Clique no mapa para marcar o local.', true)
    } catch {
      avisar('Não foi possível consultar o mapa agora. Clique no mapa para marcar o local.', true)
    } finally {
      setBuscando(false)
    }
  }

  return (
    <>
      <fieldset className="painel painel-corpo form" style={{ margin: 0 }}>
        <legend className="pequeno" style={{ fontWeight: 600, padding: '0 6px' }}>
          Dados que aparecem no app
        </legend>
        <div className="linha-form">
          <Campo rotulo="Nome da barbearia" required maxLength={120} value={d.nome} onChange={campo('nome')} />
          <Campo rotulo="Telefone / WhatsApp" inputMode="tel" maxLength={20} value={d.telefone} onChange={campo('telefone')} />
        </div>
        <div className="linha-form">
          <Campo rotulo="E-mail" type="email" maxLength={160} value={d.email} onChange={campo('email')} />
          <Campo rotulo="CPF ou CNPJ" maxLength={20} value={d.documento} onChange={campo('documento')} ajuda="Não aparece no app." />
        </div>
        <Texto rotulo="Descrição" maxLength={1000} value={d.descricao} onChange={campo('descricao')} />
      </fieldset>

      <fieldset className="painel painel-corpo form" style={{ margin: 0 }}>
        <legend className="pequeno" style={{ fontWeight: 600, padding: '0 6px' }}>
          Onde atende
        </legend>
        <div className="linha-form">
          <Campo rotulo="CEP" inputMode="numeric" maxLength={9} value={d.cep} onChange={campo('cep')} onBlur={aoSairCep} ajuda="Preenche o endereço sozinho." />
          <Campo rotulo="Rua" maxLength={160} value={d.logradouro} onChange={campo('logradouro')} />
          <Campo rotulo="Número" maxLength={20} value={d.numero} onChange={campo('numero')} />
        </div>
        <div className="linha-form">
          <Campo rotulo="Complemento" maxLength={80} value={d.complemento} onChange={campo('complemento')} />
          <Campo rotulo="Bairro" maxLength={80} value={d.bairro} onChange={campo('bairro')} />
          <Campo rotulo="Cidade" maxLength={80} value={d.cidade} onChange={campo('cidade')} />
          <Campo rotulo="UF" maxLength={2} value={d.uf} onChange={campo('uf')} />
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <p className="mudo pequeno">
            {d.latitude != null
              ? 'Local marcado. Os clientes próximos já encontram a barbearia no app.'
              : 'Marque o local no mapa para a barbearia aparecer na busca por proximidade.'}
          </p>
          <button type="button" className="btn btn-pequeno" onClick={localizar} disabled={buscando || !d.cidade}>
            <MapPin size={15} aria-hidden /> {buscando ? 'Procurando…' : 'Achar pelo endereço'}
          </button>
        </div>
        <MapaLocal lat={d.latitude} lng={d.longitude} aoMudar={(lat, lng) => set({ ...d, latitude: lat, longitude: lng })} />
      </fieldset>

      {comRegras && (
        <fieldset className="painel painel-corpo form" style={{ margin: 0 }}>
          <legend className="pequeno" style={{ fontWeight: 600, padding: '0 6px' }}>
            Regras de agendamento
          </legend>
          <div className="linha-form">
            <Campo
              rotulo="Intervalo entre horários (min)"
              type="number"
              min={5}
              max={60}
              value={d.gradeMinutos}
              onChange={campo('gradeMinutos')}
              ajuda="De quanto em quanto tempo os horários aparecem no app."
            />
            <Campo
              rotulo="Antecedência mínima (min)"
              type="number"
              min={0}
              max={1440}
              value={d.antecedenciaMinimaMin}
              onChange={campo('antecedenciaMinimaMin')}
              ajuda="Quanto antes o cliente precisa agendar."
            />
          </div>
          <div className="linha-form">
            <Campo
              rotulo="Prazo para o cliente cancelar (min)"
              type="number"
              min={0}
              max={4320}
              value={d.prazoCancelamentoMin}
              onChange={campo('prazoCancelamentoMin')}
              ajuda="Depois disso, só falando com a barbearia."
            />
            <Campo
              rotulo="Prazo para responder remarcação (h)"
              type="number"
              min={1}
              max={72}
              value={d.prazoRespostaRemarcacaoHoras}
              onChange={campo('prazoRespostaRemarcacaoHoras')}
              ajuda="Sem resposta, a reserva é cancelada."
            />
          </div>
        </fieldset>
      )}
    </>
  )
}
