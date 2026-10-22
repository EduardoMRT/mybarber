import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_cliente.dart';
import '../api/modelos.dart';
import '../estado/sessao.dart';
import '../servicos/sincronizador.dart';
import '../widgets/comuns.dart';
import '../widgets/seletor_horario.dart';
import 'barbearia.dart';

/// Detalhe de uma reserva: cancelar, reagendar e responder remarcacao.
class TelaReserva extends StatefulWidget {
  final int id;
  const TelaReserva({super.key, required this.id});

  @override
  State<TelaReserva> createState() => _TelaReservaState();
}

class _TelaReservaState extends State<TelaReserva> {
  late Future<Agendamento> _reserva;
  bool _ocupado = false;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  void _carregar() {
    _reserva = ApiCliente.instancia
        .get('/api/app/agendamentos/${widget.id}')
        .then((j) => Agendamento.deJson(j as Map<String, dynamic>));
  }

  Future<void> _acao(String caminho, String sucesso, [Object? corpo]) async {
    setState(() => _ocupado = true);
    try {
      await ApiCliente.instancia.post(caminho, corpo);
      if (!mounted) return;
      avisar(context, sucesso);
      context.read<Sincronizador>().sincronizar();
      setState(_carregar);
    } on ApiErro catch (e) {
      if (mounted) avisar(context, e.mensagem, erro: true);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  Future<void> _cancelar(Agendamento r) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Cancelar reserva?'),
        content: Text('${r.servicoNome} em ${fmtDia.format(r.inicio)} às ${fmtHora.format(r.inicio)}. O horário fica livre para outra pessoa.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Manter')),
          TextButton(onPressed: () => Navigator.pop(c, true), child: const Text('Cancelar reserva')),
        ],
      ),
    );
    if (ok == true) await _acao('/api/app/agendamentos/${r.id}/cancelar', 'Reserva cancelada.');
  }

  Future<void> _escolherHorario(Agendamento r, {required bool remarcacao}) async {
    final tipo = context.read<Sessao>().usuario?.tipoCabeloId;
    final escolhido = await showModalBottomSheet<DateTime>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      showDragHandle: true,
      builder: (c) => _EscolherOutro(r: r, tipoCabeloId: tipo),
    );
    if (escolhido == null) return;
    final caminho = remarcacao
        ? '/api/app/agendamentos/${r.id}/remarcacao/outro-horario'
        : '/api/app/agendamentos/${r.id}/reagendar';
    await _acao(caminho, 'Novo horário confirmado: ${fmtDia.format(escolhido)} às ${fmtHora.format(escolhido)}.',
        {'inicio': isoSemFuso(escolhido)});
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Reserva')),
      body: FutureBuilder<Agendamento>(
        future: _reserva,
        builder: (context, s) {
          if (s.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
          if (s.hasError) return ErroTela(mensagem: s.error.toString(), aoTentar: () => setState(_carregar));
          final r = s.data!;
          return AbsorbPointer(
            absorbing: _ocupado,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(20, 4, 20, 32),
              children: [
                if (r.aguardandoResposta && r.remarcacao != null) _PainelRemarcacao(
                  r: r,
                  aoAceitar: () => _acao('/api/app/agendamentos/${r.id}/remarcacao/aceitar', 'Novo horário aceito.'),
                  aoOutro: () => _escolherHorario(r, remarcacao: true),
                  aoCancelar: () => _cancelar(r),
                ),
                Row(children: [Expanded(child: Text(r.servicoNome, style: t.textTheme.headlineSmall)), SeloStatus(r.status)]),
                const SizedBox(height: 8),
                Text(
                  '${capitalizar(fmtDiaLongo.format(r.inicio))}, das ${fmtHora.format(r.inicio)} às ${fmtHora.format(r.fim)}',
                  style: t.textTheme.titleMedium,
                ),
                const SizedBox(height: 4),
                Text('Com ${r.barbeiroNome}, ${moeda.format(r.valor)}',
                    style: t.textTheme.bodyLarge?.copyWith(color: t.colorScheme.onSurfaceVariant)),
                if (r.motivoCancelamento != null) ...[
                  const SizedBox(height: 12),
                  Text('Motivo: ${r.motivoCancelamento}', style: t.textTheme.bodyMedium),
                ],
                const SizedBox(height: 20),
                Card(
                  child: Column(children: [
                    ListTile(
                      leading: const Icon(Icons.storefront_outlined),
                      title: Text(r.barbeariaNome),
                      subtitle: Text(r.barbeariaEndereco ?? ''),
                      trailing: const Icon(Icons.directions_outlined),
                      onTap: () => abrirMapa(r.barbeariaEndereco ?? r.barbeariaNome, null, null),
                    ),
                    if (r.barbeariaTelefone != null) ...[
                      const Divider(height: 1),
                      ListTile(
                        leading: const Icon(Icons.chat_outlined),
                        title: const Text('Falar com a barbearia'),
                        subtitle: Text(r.barbeariaTelefone!),
                        onTap: () => abrirWhatsApp(r.barbeariaTelefone!),
                      ),
                    ],
                  ]),
                ),
                const SizedBox(height: 24),
                if (r.status == 'CONFIRMADO' && r.podeCancelar) ...[
                  BotaoAcao(texto: 'Mudar horário', contorno: true, aoTocar: () => _escolherHorario(r, remarcacao: false)),
                  const SizedBox(height: 10),
                  BotaoAcao(texto: 'Cancelar reserva', contorno: true, aoTocar: () => _cancelar(r)),
                ] else if (r.status == 'CONFIRMADO')
                  Text(
                    'O prazo para mudar ou cancelar pelo app já passou. Fale direto com a barbearia.',
                    style: t.textTheme.bodyMedium?.copyWith(color: t.colorScheme.onSurfaceVariant),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _PainelRemarcacao extends StatelessWidget {
  final Agendamento r;
  final VoidCallback aoAceitar;
  final VoidCallback aoOutro;
  final VoidCallback aoCancelar;
  const _PainelRemarcacao({required this.r, required this.aoAceitar, required this.aoOutro, required this.aoCancelar});

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final m = r.remarcacao!;
    return Container(
      margin: const EdgeInsets.only(bottom: 24),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: t.colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Semantics(header: true, child: Text('A barbearia mudou seu horário', style: t.textTheme.titleMedium)),
        const SizedBox(height: 8),
        Text('Motivo: ${m.motivo}', style: t.textTheme.bodyMedium),
        const SizedBox(height: 12),
        Text('Antes: ${fmtDia.format(m.inicioAnterior)} às ${fmtHora.format(m.inicioAnterior)}',
            style: t.textTheme.bodyMedium?.copyWith(decoration: TextDecoration.lineThrough)),
        Text('Sugerido: ${fmtDia.format(m.inicioSugerido)} às ${fmtHora.format(m.inicioSugerido)}',
            style: t.textTheme.titleMedium),
        const SizedBox(height: 8),
        Text(
          'Responda até ${fmtDia.format(m.prazo)} às ${fmtHora.format(m.prazo)}. Sem resposta, a reserva é cancelada.',
          style: t.textTheme.bodySmall,
        ),
        const SizedBox(height: 14),
        BotaoAcao(texto: 'Aceitar novo horário', aoTocar: aoAceitar),
        const SizedBox(height: 8),
        BotaoAcao(texto: 'Escolher outro horário', contorno: true, aoTocar: aoOutro),
        TextButton(onPressed: aoCancelar, child: const Text('Cancelar a reserva')),
      ]),
    );
  }
}

class _EscolherOutro extends StatefulWidget {
  final Agendamento r;
  final int? tipoCabeloId;
  const _EscolherOutro({required this.r, required this.tipoCabeloId});

  @override
  State<_EscolherOutro> createState() => _EscolherOutroState();
}

class _EscolherOutroState extends State<_EscolherOutro> {
  DateTime? _escolhido;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
      child: SingleChildScrollView(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text('Escolha outro horário', style: t.textTheme.titleLarge),
          const SizedBox(height: 4),
          Text('${widget.r.servicoNome} com ${widget.r.barbeiroNome}', style: t.textTheme.bodyMedium),
          const SizedBox(height: 16),
          SeletorHorario(
            barbeiroId: widget.r.barbeiroId,
            servicoId: widget.r.servicoId,
            tipoCabeloId: widget.tipoCabeloId,
            selecionado: _escolhido,
            aoEscolher: (d) => setState(() => _escolhido = d),
          ),
          const SizedBox(height: 16),
          BotaoAcao(
            texto: _escolhido == null ? 'Escolha um horário' : 'Confirmar ${fmtHora.format(_escolhido!)}',
            aoTocar: _escolhido == null ? null : () => Navigator.pop(context, _escolhido),
          ),
        ]),
      ),
    );
  }
}
