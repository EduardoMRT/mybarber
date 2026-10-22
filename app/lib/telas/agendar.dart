import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_cliente.dart';
import '../api/modelos.dart';
import '../estado/sessao.dart';
import '../servicos/sincronizador.dart';
import '../widgets/comuns.dart';
import '../widgets/seletor_horario.dart';
import 'inicio.dart';

class TelaAgendar extends StatefulWidget {
  final BarbeariaDetalhe barbearia;
  final ServicoPublico? servicoInicial;
  const TelaAgendar({super.key, required this.barbearia, this.servicoInicial});

  @override
  State<TelaAgendar> createState() => _TelaAgendarState();
}

class _TelaAgendarState extends State<TelaAgendar> {
  ServicoPublico? _servico;
  BarbeiroPublico? _barbeiro;
  DateTime? _horario;
  final _obs = TextEditingController();
  bool _ocupado = false;

  @override
  void initState() {
    super.initState();
    _servico = widget.servicoInicial;
    _autoBarbeiro();
  }

  @override
  void dispose() {
    _obs.dispose();
    super.dispose();
  }

  int? get _tipo => context.read<Sessao>().usuario?.tipoCabeloId;

  List<BarbeiroPublico> get _barbeirosAptos => _servico == null
      ? []
      : widget.barbearia.barbeiros.where((b) => b.faz(_servico!.id) && b.atende(_tipo)).toList();

  void _autoBarbeiro() {
    final aptos = _barbeirosAptos;
    _barbeiro = aptos.length == 1 ? aptos.first : null;
  }

  Future<void> _confirmar() async {
    setState(() => _ocupado = true);
    try {
      await ApiCliente.instancia.post('/api/app/agendamentos', {
        'barbeiroId': _barbeiro!.id,
        'servicoId': _servico!.id,
        'inicio': isoSemFuso(_horario!),
        'observacao': _obs.text.trim().isEmpty ? null : _obs.text.trim(),
      });
      if (!mounted) return;
      await context.read<Sincronizador>().sincronizar();
      if (!mounted) return;
      final inicio = TelaInicioState.de(context);
      final mensageiro = ScaffoldMessenger.of(context);
      final texto = 'Horário confirmado para ${fmtDia.format(_horario!)} às ${fmtHora.format(_horario!)}.';
      Navigator.of(context).popUntil((r) => r.isFirst);
      inicio?.irPara(1);
      mensageiro.showSnackBar(SnackBar(content: Text(texto)));
    } on ApiErro catch (e) {
      if (!mounted) return;
      avisar(context, e.mensagem, erro: true);
      if (e.status == 409) setState(() => _horario = null);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final tipo = context.watch<Sessao>().usuario?.tipoCabeloId;
    final b = widget.barbearia;
    final aptos = _barbeirosAptos;
    final indisponiveis = _servico == null ? <BarbeiroPublico>[] : b.barbeiros.where((x) => !aptos.contains(x)).toList();

    Widget titulo(String texto) => Padding(
          padding: const EdgeInsets.only(top: 24, bottom: 10),
          child: Semantics(header: true, child: Text(texto, style: t.textTheme.titleLarge)),
        );

    return Scaffold(
      appBar: AppBar(title: Text('Agendar em ${b.nome}')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 140),
        children: [
          titulo('Serviço'),
          Wrap(spacing: 8, runSpacing: 8, children: [
            for (final s in b.servicos)
              ChoiceChip(
                label: Text('${s.nome}, ${moeda.format(s.preco)}'),
                selected: _servico?.id == s.id,
                onSelected: (_) => setState(() {
                  _servico = s;
                  _horario = null;
                  _autoBarbeiro();
                }),
              ),
          ]),
          if (_servico != null) ...[
            const SizedBox(height: 10),
            Text(
              tipo == null
                  ? 'Leva cerca de ${duracaoTexto(_servico!.duracaoPara(null))}.'
                  : 'No seu tipo de cabelo leva ${duracaoTexto(_servico!.duracaoPara(tipo))}. Só aparecem horários em que ele cabe inteiro.',
              style: t.textTheme.bodyMedium?.copyWith(color: t.colorScheme.onSurfaceVariant),
            ),
            titulo('Barbeiro'),
            if (aptos.isEmpty)
              Text('Nenhum barbeiro desta barbearia faz este serviço no seu tipo de cabelo.', style: t.textTheme.bodyLarge)
            else
              Wrap(spacing: 8, runSpacing: 8, children: [
                for (final x in aptos)
                  ChoiceChip(
                    label: Text(x.nome),
                    selected: _barbeiro?.id == x.id,
                    onSelected: (_) => setState(() {
                      _barbeiro = x;
                      _horario = null;
                    }),
                  ),
              ]),
            if (indisponiveis.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  '${indisponiveis.map((x) => x.nome).join(', ')} não ${indisponiveis.length == 1 ? 'atende' : 'atendem'} este serviço no seu tipo de cabelo.',
                  style: t.textTheme.bodySmall?.copyWith(color: t.colorScheme.onSurfaceVariant),
                ),
              ),
          ],
          if (_servico != null && _barbeiro != null) ...[
            titulo('Dia e horário'),
            SeletorHorario(
              key: ValueKey('${_barbeiro!.id}-${_servico!.id}'),
              barbeiroId: _barbeiro!.id,
              servicoId: _servico!.id,
              tipoCabeloId: tipo,
              selecionado: _horario,
              aoEscolher: (d) => setState(() => _horario = d),
            ),
          ],
          if (_horario != null) ...[
            titulo('Observação (opcional)'),
            TextField(
              controller: _obs,
              maxLength: 500,
              maxLines: 2,
              decoration: const InputDecoration(hintText: 'Ex.: quero manter o comprimento em cima'),
            ),
          ],
        ],
      ),
      bottomNavigationBar: _horario == null
          ? null
          : SafeArea(
              child: Container(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 12),
                decoration: BoxDecoration(
                  color: t.colorScheme.surface,
                  border: Border(top: BorderSide(color: t.colorScheme.outlineVariant)),
                ),
                child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(
                    '${capitalizar(fmtDia.format(_horario!))} às ${fmtHora.format(_horario!)}',
                    style: t.textTheme.titleMedium,
                  ),
                  Text('${_servico!.nome} com ${_barbeiro!.nome}, ${moeda.format(_servico!.preco)}',
                      style: t.textTheme.bodyMedium?.copyWith(color: t.colorScheme.onSurfaceVariant)),
                  const SizedBox(height: 10),
                  BotaoAcao(texto: 'Confirmar agendamento', ocupado: _ocupado, aoTocar: _confirmar),
                ]),
              ),
            ),
    );
  }
}
