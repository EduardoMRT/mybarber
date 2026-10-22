import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../servicos/sincronizador.dart';
import '../widgets/comuns.dart';
import 'reserva.dart';

class TelaAvisos extends StatelessWidget {
  const TelaAvisos({super.key});

  @override
  Widget build(BuildContext context) {
    final sinc = context.watch<Sincronizador>();
    final t = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Avisos'),
        actions: [
          if (sinc.naoLidas > 0)
            TextButton(onPressed: sinc.marcarTodasLidas, child: const Text('Marcar todos como lidos')),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: sinc.sincronizar,
        child: sinc.avisos.isEmpty
            ? ListView(children: const [
                Vazio(
                  icone: Icons.notifications_none,
                  titulo: 'Nenhum aviso',
                  texto: 'Confirmações, remarcações e cancelamentos aparecem aqui.',
                ),
              ])
            : ListView.separated(
                itemCount: sinc.avisos.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (context, i) {
                  final a = sinc.avisos[i];
                  return Semantics(
                    label: a.lida ? null : 'Não lido.',
                    child: ListTile(
                      tileColor: a.lida ? null : t.colorScheme.secondaryContainer.withValues(alpha: 0.45),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                      title: Text(a.titulo, style: TextStyle(fontWeight: a.lida ? FontWeight.w500 : FontWeight.w800)),
                      subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        const SizedBox(height: 4),
                        Text(a.mensagem),
                        const SizedBox(height: 4),
                        Text('${fmtDia.format(a.criadoEm)}, ${fmtHora.format(a.criadoEm)}', style: t.textTheme.bodySmall),
                      ]),
                      onTap: () {
                        if (!a.lida) sinc.marcarLida(a.id);
                        if (a.agendamentoId != null) {
                          Navigator.of(context).push(MaterialPageRoute(builder: (_) => TelaReserva(id: a.agendamentoId!)));
                        }
                      },
                    ),
                  );
                },
              ),
      ),
    );
  }
}
