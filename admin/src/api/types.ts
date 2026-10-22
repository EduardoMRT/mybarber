export type Papel = 'SUPER_ADMIN' | 'ADMIN_BARBEARIA' | 'BARBEIRO' | 'CLIENTE'
export type StatusAgendamento = 'CONFIRMADO' | 'AGUARDANDO_CLIENTE' | 'CONCLUIDO' | 'CANCELADO' | 'FALTA'
export type FormaPagamento = 'DINHEIRO' | 'PIX' | 'CARTAO_DEBITO' | 'CARTAO_CREDITO' | 'OUTRO'
export type StatusBarbearia = 'ATIVA' | 'BLOQUEADA'
export type StatusCobranca = 'PENDENTE' | 'PAGA' | 'ATRASADA' | 'CANCELADA'
export type TipoLancamento = 'RECEITA' | 'DESPESA'

export interface Usuario {
  id: number
  nome: string
  email: string
  telefone?: string
  papel: Papel
  barbeariaId?: number
  barbeariaNome?: string
  barbeariaBloqueada: boolean
  motivoBloqueio?: string
  barbeiroId?: number
}

export interface Tokens {
  accessToken: string
  refreshToken: string
  expiraEm: string
  usuario: Usuario
}

export interface TipoCabelo {
  id: number
  nome: string
  descricao?: string
}

export interface TipoCabeloAdmin extends TipoCabelo {
  ordem: number
  ativo: boolean
}

export interface Barbearia {
  id: number
  nome: string
  documento?: string
  telefone?: string
  email?: string
  descricao?: string
  cep?: string
  logradouro?: string
  numero?: string
  complemento?: string
  bairro?: string
  cidade?: string
  uf?: string
  latitude?: number
  longitude?: number
  gradeMinutos: number
  antecedenciaMinimaMin: number
  prazoCancelamentoMin: number
  prazoRespostaRemarcacaoHoras: number
  status: StatusBarbearia
  motivoBloqueio?: string
  valorMensalidade?: number
  diaVencimento?: number
  diasTolerancia: number
  criadoEm: string
  donoNome?: string
  donoEmail?: string
  barbeirosAtivos: number
}

export interface Barbeiro {
  id: number
  nome: string
  telefone?: string
  bio?: string
  intervaloMin: number
  ativo: boolean
  temLogin: boolean
  email?: string
  dono: boolean
  tiposCabeloIds: number[]
  servicosIds: number[]
}

export interface Janela {
  id?: number
  diaSemana: number
  inicio: string
  fim: string
  servicosIds: number[]
}

export interface Bloqueio {
  id: number
  inicio: string
  fim: string
  motivo?: string
}

export interface DuracaoTipo {
  tipoCabeloId: number
  duracaoMin: number
}

export interface Servico {
  id: number
  nome: string
  descricao?: string
  preco: number
  duracaoMin: number
  ativo: boolean
  duracoes: DuracaoTipo[]
}

export interface Remarcacao {
  id: number
  inicioAnterior: string
  inicioSugerido: string
  motivo: string
  prazo: string
  status: string
}

export interface Agendamento {
  id: number
  barbeariaId: number
  barbeariaNome: string
  barbeiroId: number
  barbeiroNome: string
  servicoId: number
  servicoNome: string
  clienteId?: number
  clienteNome?: string
  clienteTelefone?: string
  tipoCabeloId?: number
  tipoCabeloNome?: string
  observacoesCliente?: string
  inicio: string
  fim: string
  duracaoMin: number
  valor: number
  status: StatusAgendamento
  origem: 'APP' | 'MANUAL'
  observacao?: string
  motivoCancelamento?: string
  formaPagamento?: FormaPagamento
  remarcacao?: Remarcacao
}

export interface Notificacao {
  id: number
  tipo: string
  titulo: string
  mensagem: string
  agendamentoId?: number
  lida: boolean
  criadoEm: string
}

export interface ResumoNotificacoes {
  naoLidas: number
  itens: Notificacao[]
}

export interface Lancamento {
  id: number
  tipo: TipoLancamento
  categoria: string
  descricao?: string
  valor: number
  data: string
  formaPagamento?: FormaPagamento
  agendamentoId?: number
  barbeiroId?: number
  barbeiroNome?: string
}

export interface Extrato {
  de: string
  ate: string
  receitas: number
  despesas: number
  saldo: number
  lancamentos: Lancamento[]
}

export interface ItemValor {
  nome: string
  quantidade: number
  valor: number
}

export interface Dashboard {
  de: string
  ate: string
  receitas: number
  despesas: number
  lucro: number
  atendimentosConcluidos: number
  faltas: number
  cancelados: number
  agendados: number
  ticketMedio: number
  taxaFaltas: number
  ocupacao: number
  porDia: { data: string; receitas: number; despesas: number }[]
  porServico: ItemValor[]
  porBarbeiro: ItemValor[]
  despesasPorCategoria: ItemValor[]
  formasPagamento: ItemValor[]
  proximosHoje: Agendamento[]
}

export interface ClienteResumo {
  clienteId: number
  nome: string
  telefone?: string
  tipoCabelo?: string
  atendimentos: number
  faltas: number
  ultimoAtendimento?: string
  proximoAgendamento?: string
  totalGasto: number
}

export interface Cobranca {
  id: number
  barbeariaId: number
  barbeariaNome: string
  competencia: string
  valor: number
  vencimento: string
  status: StatusCobranca
  pagoEm?: string
  formaPagamento?: FormaPagamento
  observacao?: string
}

export interface Assinatura {
  status: StatusBarbearia
  motivoBloqueio?: string
  valorMensalidade?: number
  diaVencimento?: number
  diasTolerancia: number
  cobrancas: Cobranca[]
}

export interface AdminDashboard {
  competencia: string
  totalBarbearias: number
  ativas: number
  bloqueadas: number
  receitaMensalPrevista: number
  recebidoNoMes: number
  emAbertoNoMes: number
  cobrancasAtrasadas: number
  valorAtrasado: number
  totalClientes: number
  agendamentosNoMes: number
}

export interface DocumentoLegal {
  slug: string
  titulo: string
  versao: string
  conteudo?: string
}

export interface ErroApi {
  status: number
  erro: string
  mensagem: string
  campos?: Record<string, string>
}
