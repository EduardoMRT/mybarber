import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/modelos.dart';
import '../servicos/sincronizador.dart';
import '../widgets/comuns.dart';
import 'inicio.dart';
import 'reserva.dart';

class TelaReservas extends StatelessWidget {
  const TelaReservas({super.key});

  @override
  Widget build(BuildContext context) {
    final sinc = context.watch<Sincronizador>();
    final t = Theme.of(context);
    final agora = DateTime.now();
    final proximas = sinc.reservas.where((r) => r.ativo && r.fim.isAfter(agora)).toList()
      ..sort((a, b) => a.inicio.compareTo(b.inicio));
    final historico = sinc.reservas.where((r) => !proximas.contains(r)).toList()
      ..sort((a, b) => b.inicio.compareTo(a.inicio));

    return Scaffold(
      appBar: AppBar(title: const Text('Minhas reservas')),
      body: RefreshIndicator(
        onRefresh: sinc.sincronizar,
        child: sinc.reservas.isEmpty && sinc.carregando
            ? const Center(child: CircularProgressIndicator())
            : sinc.reservas.isEmpty && sinc.erro != null
                ? ListView(children: [ErroTela(mensagem: sinc.erro!, aoTentar: sinc.sincronizar)])
                : sinc.reservas.isEmpty
                    ? ListView(children: [
                        Vazio(
                          icone: Icons.event_available_outlined,
                          titulo: 'Você ainda não tem reservas',
                          texto: 'Escolha uma barbearia e agende em poucos toques.',
                          acao: FilledButton(
                            onPressed: () => TelaInicioState.de(context)?.irPara(0),
                            child: const Text('Ver barbearias'),
                          ),
                        ),
                      ])
                    : ListView(
                        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
                        children: [
                          if (proximas.isNotEmpty) ...[
                            Semantics(header: true, child: Text('Próximas', style: t.textTheme.titleMedium)),
                            const SizedBox(height: 10),
                            for (final r in proximas) Padding(padding: const EdgeInsets.only(bottom: 10), child: CartaoReserva(r: r)),
                          ],
                          if (historico.isNotEmpty) ...[
                            const SizedBox(height: 14),
                            Semantics(header: true, child: Text('Histórico', style: t.textTheme.titleMedium)),
                            const SizedBox(height: 10),
                            for (final r in historico) Padding(padding: const EdgeInsets.only(bottom: 10), child: CartaoReserva(r: r)),
                          ],
                        ],
                      ),
      ),
    );
  }
}

class CartaoReserva extends StatelessWidget {
  final Agendamento r;
  const CartaoReserva({super.key, required this.r});

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final destaque = r.aguardandoResposta;
    return Card(
      clipBehavior: Clip.antiAlias,
      shape: destaque
          ? RoundedRectangleBorder(borderRadius: BorderRadius.circular(12), side: BorderSide(color: t.colorScheme.secondary, width: 2))
          : null,
      child: InkWell(
        onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => TelaReserva(id: r.id))),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                width: 64,
                child: Column(children: [
                  Text(fmtHora.format(r.inicio), style: t.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
                  Text(capitalizar(fmtDia.format(r.inicio).split(',').first), style: t.textTheme.labelMedium),
                  Text('${r.inicio.day}/${r.inicio.month.toString().padLeft(2, '0')}', style: t.textTheme.labelMedium),
                ]),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(r.servicoNome, style: t.textTheme.titleMedium),
                  Text('${r.barbeariaNome}, com ${r.barbeiroNome}',
                      style: t.textTheme.bodyMedium?.copyWith(color: t.colorScheme.onSurfaceVariant)),
                  const SizedBox(height: 8),
                  SeloStatus(r.status),
                ]),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
