import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../estado/preferencias.dart';
import '../servicos/notificacoes.dart';
import '../servicos/sincronizador.dart';
import 'avisos.dart';
import 'barbearias.dart';
import 'perfil.dart';
import 'reservas.dart';

class TelaInicio extends StatefulWidget {
  const TelaInicio({super.key});

  @override
  State<TelaInicio> createState() => TelaInicioState();
}

class TelaInicioState extends State<TelaInicio> {
  int _aba = 0;

  /// Permite que outras telas mudem a aba (ex.: depois de agendar, ir para Reservas).
  static TelaInicioState? de(BuildContext context) => context.findAncestorStateOfType<TelaInicioState>();

  void irPara(int aba) => setState(() => _aba = aba);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final sinc = context.read<Sincronizador>();
      sinc.lembreteMin = context.read<Preferencias>().lembreteMin;
      sinc.iniciar();
      await Notificacoes.instancia.pedirPermissao();
    });
  }

  @override
  Widget build(BuildContext context) {
    final sinc = context.watch<Sincronizador>();
    final aguardando = sinc.reservas.where((r) => r.aguardandoResposta).length;
    return Scaffold(
      body: IndexedStack(
        index: _aba,
        children: const [TelaBarbearias(), TelaReservas(), TelaAvisos(), TelaPerfil()],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _aba,
        onDestinationSelected: irPara,
        destinations: [
          const NavigationDestination(icon: Icon(Icons.storefront_outlined), selectedIcon: Icon(Icons.storefront), label: 'Barbearias'),
          NavigationDestination(
            icon: Badge(isLabelVisible: aguardando > 0, label: Text('$aguardando'), child: const Icon(Icons.event_note_outlined)),
            selectedIcon: Badge(isLabelVisible: aguardando > 0, label: Text('$aguardando'), child: const Icon(Icons.event_note)),
            label: 'Reservas',
            tooltip: aguardando > 0 ? 'Reservas, $aguardando aguardando sua resposta' : 'Reservas',
          ),
          NavigationDestination(
            icon: Badge(isLabelVisible: sinc.naoLidas > 0, label: Text('${sinc.naoLidas}'), child: const Icon(Icons.notifications_none)),
            selectedIcon: Badge(isLabelVisible: sinc.naoLidas > 0, label: Text('${sinc.naoLidas}'), child: const Icon(Icons.notifications)),
            label: 'Avisos',
            tooltip: sinc.naoLidas > 0 ? 'Avisos, ${sinc.naoLidas} não lidos' : 'Avisos',
          ),
          const NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Perfil'),
        ],
      ),
    );
  }
}
