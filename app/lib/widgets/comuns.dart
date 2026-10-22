import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../tema/tema_app.dart';

final moeda = NumberFormat.currency(locale: 'pt_BR', symbol: r'R$');
final fmtHora = DateFormat('HH:mm');
final fmtDia = DateFormat("EEE, d 'de' MMM", 'pt_BR');
final fmtDiaLongo = DateFormat("EEEE, d 'de' MMMM", 'pt_BR');

String duracaoTexto(int min) {
  if (min < 60) return '$min min';
  final h = min ~/ 60;
  final m = min % 60;
  return m == 0 ? '$h h' : '$h h $m min';
}

String capitalizar(String s) => s.isEmpty ? s : s[0].toUpperCase() + s.substring(1);

void avisar(BuildContext context, String texto, {bool erro = false}) {
  final cs = Theme.of(context).colorScheme;
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(
      content: Text(texto),
      backgroundColor: erro ? cs.error : null,
    ));
}

/// Selo de situacao com texto (nunca so cor).
class SeloStatus extends StatelessWidget {
  final String status;
  const SeloStatus(this.status, {super.key});

  static const textos = {
    'CONFIRMADO': 'Confirmado',
    'AGUARDANDO_CLIENTE': 'Responda a remarcação',
    'CONCLUIDO': 'Concluído',
    'CANCELADO': 'Cancelado',
    'FALTA': 'Falta',
  };

  @override
  Widget build(BuildContext context) {
    final escuro = Theme.of(context).brightness == Brightness.dark;
    final cor = switch (status) {
      'CONFIRMADO' => Cores.confirmado,
      'AGUARDANDO_CLIENTE' => Cores.aguardando,
      'CONCLUIDO' => Cores.concluido,
      'FALTA' => Cores.falta,
      _ => Cores.cancelado,
    };
    final corTexto = escuro ? Color.lerp(cor, Colors.white, 0.55)! : cor;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: cor.withValues(alpha: escuro ? 0.28 : 0.12),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        textos[status] ?? status,
        style: TextStyle(color: corTexto, fontWeight: FontWeight.w700, fontSize: 12.5),
      ),
    );
  }
}

class Vazio extends StatelessWidget {
  final IconData icone;
  final String titulo;
  final String? texto;
  final Widget? acao;
  const Vazio({super.key, required this.icone, required this.titulo, this.texto, this.acao});

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ExcludeSemantics(child: Icon(icone, size: 48, color: t.colorScheme.onSurfaceVariant)),
            const SizedBox(height: 16),
            Text(titulo, style: t.textTheme.titleMedium, textAlign: TextAlign.center),
            if (texto != null) ...[
              const SizedBox(height: 8),
              Text(texto!, style: t.textTheme.bodyMedium?.copyWith(color: t.colorScheme.onSurfaceVariant), textAlign: TextAlign.center),
            ],
            if (acao != null) ...[const SizedBox(height: 20), acao!],
          ],
        ),
      ),
    );
  }
}

class ErroTela extends StatelessWidget {
  final String mensagem;
  final VoidCallback aoTentar;
  const ErroTela({super.key, required this.mensagem, required this.aoTentar});

  @override
  Widget build(BuildContext context) => Vazio(
        icone: Icons.cloud_off_outlined,
        titulo: 'Não deu para carregar',
        texto: mensagem,
        acao: OutlinedButton(onPressed: aoTentar, child: const Text('Tentar de novo')),
      );
}

/// Botao principal que mostra progresso e evita toque duplo.
class BotaoAcao extends StatelessWidget {
  final String texto;
  final bool ocupado;
  final VoidCallback? aoTocar;
  final bool contorno;
  const BotaoAcao({super.key, required this.texto, required this.aoTocar, this.ocupado = false, this.contorno = false});

  @override
  Widget build(BuildContext context) {
    final filho = ocupado
        ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5))
        : Text(texto);
    final acao = ocupado ? null : aoTocar;
    return SizedBox(
      width: double.infinity,
      child: contorno ? OutlinedButton(onPressed: acao, child: filho) : FilledButton(onPressed: acao, child: filho),
    );
  }
}
