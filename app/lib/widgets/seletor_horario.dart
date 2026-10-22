import 'package:flutter/material.dart';

import '../api/api_cliente.dart';
import '../api/modelos.dart';
import 'comuns.dart';

/// Escolha de dia e horario. So aparecem horarios em que o servico cabe
/// inteiro na agenda do barbeiro (a API ja aplica a regra de encaixe).
class SeletorHorario extends StatefulWidget {
  final int barbeiroId;
  final int servicoId;
  final int? tipoCabeloId;
  final DateTime? selecionado;
  final ValueChanged<DateTime> aoEscolher;

  const SeletorHorario({
    super.key,
    required this.barbeiroId,
    required this.servicoId,
    required this.tipoCabeloId,
    required this.aoEscolher,
    this.selecionado,
  });

  @override
  State<SeletorHorario> createState() => _SeletorHorarioState();
}

class _SeletorHorarioState extends State<SeletorHorario> {
  static const _dias = 30;
  late DateTime _dia;
  Future<Horarios>? _horarios;

  @override
  void initState() {
    super.initState();
    final hoje = DateTime.now();
    _dia = widget.selecionado != null
        ? DateTime(widget.selecionado!.year, widget.selecionado!.month, widget.selecionado!.day)
        : DateTime(hoje.year, hoje.month, hoje.day);
    _carregar();
  }

  @override
  void didUpdateWidget(SeletorHorario antigo) {
    super.didUpdateWidget(antigo);
    if (antigo.barbeiroId != widget.barbeiroId || antigo.servicoId != widget.servicoId) _carregar();
  }

  void _carregar() {
    setState(() {
      _horarios = ApiCliente.instancia.get('/api/public/horarios', params: {
        'barbeiroId': '${widget.barbeiroId}',
        'servicoId': '${widget.servicoId}',
        'tipoCabeloId': widget.tipoCabeloId?.toString(),
        'data': isoData(_dia),
      }).then((j) => Horarios.deJson(j as Map<String, dynamic>));
    });
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final hoje = DateTime.now();
    final dias = [for (var i = 0; i < _dias; i++) DateTime(hoje.year, hoje.month, hoje.day + i)];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          height: 76,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: dias.length,
            separatorBuilder: (_, __) => const SizedBox(width: 8),
            itemBuilder: (context, i) {
              final d = dias[i];
              final ativo = d == _dia;
              final semana = i == 0 ? 'Hoje' : i == 1 ? 'Amanhã' : capitalizar(fmtDia.format(d).split(',').first);
              return Semantics(
                selected: ativo,
                button: true,
                label: fmtDiaLongo.format(d),
                excludeSemantics: true,
                child: InkWell(
                  borderRadius: BorderRadius.circular(12),
                  onTap: () {
                    _dia = d;
                    _carregar();
                  },
                  child: Container(
                    width: 64,
                    decoration: BoxDecoration(
                      color: ativo ? t.colorScheme.primary : t.colorScheme.surface,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: ativo ? t.colorScheme.primary : t.colorScheme.outlineVariant),
                    ),
                    alignment: Alignment.center,
                    child: Column(mainAxisSize: MainAxisSize.min, children: [
                      Text(semana,
                          style: t.textTheme.labelSmall?.copyWith(color: ativo ? t.colorScheme.onPrimary : t.colorScheme.onSurfaceVariant)),
                      const SizedBox(height: 2),
                      Text('${d.day}',
                          style: t.textTheme.titleLarge?.copyWith(
                              color: ativo ? t.colorScheme.onPrimary : t.colorScheme.onSurface, fontWeight: FontWeight.w800)),
                    ]),
                  ),
                ),
              );
            },
          ),
        ),
        const SizedBox(height: 16),
        FutureBuilder<Horarios>(
          future: _horarios,
          builder: (context, s) {
            if (s.connectionState != ConnectionState.done) {
              return const Padding(padding: EdgeInsets.all(24), child: Center(child: CircularProgressIndicator()));
            }
            if (s.hasError) {
              return ErroTela(mensagem: s.error.toString(), aoTentar: _carregar);
            }
            final h = s.data!;
            if (h.horarios.isEmpty) {
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 20),
                child: Text(
                  'Nenhum horário livre em ${fmtDiaLongo.format(_dia)} para este serviço. Escolha outro dia.',
                  style: t.textTheme.bodyLarge?.copyWith(color: t.colorScheme.onSurfaceVariant),
                ),
              );
            }
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('${capitalizar(fmtDiaLongo.format(_dia))}, ${duracaoTexto(h.duracaoMin)} de atendimento',
                    style: t.textTheme.titleSmall),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    for (final hora in h.horarios)
                      () {
                        final partes = hora.split(':');
                        final dt = DateTime(_dia.year, _dia.month, _dia.day, int.parse(partes[0]), int.parse(partes[1]));
                        final marcado = widget.selecionado == dt;
                        return ChoiceChip(
                          label: Text('${partes[0]}:${partes[1]}'),
                          selected: marcado,
                          showCheckmark: false,
                          labelStyle: TextStyle(
                            fontWeight: FontWeight.w700,
                            fontSize: 15,
                            color: marcado ? t.colorScheme.onPrimary : t.colorScheme.onSurface,
                          ),
                          selectedColor: t.colorScheme.primary,
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
                          onSelected: (_) => widget.aoEscolher(dt),
                        );
                      }(),
                  ],
                ),
              ],
            );
          },
        ),
      ],
    );
  }
}
