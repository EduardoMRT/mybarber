import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import 'estado/preferencias.dart';
import 'estado/sessao.dart';
import 'servicos/notificacoes.dart';
import 'servicos/sincronizador.dart';
import 'tema/tema_app.dart';
import 'telas/entrada.dart';
import 'telas/inicio.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  Intl.defaultLocale = 'pt_BR';
  await initializeDateFormatting('pt_BR');
  await Notificacoes.instancia.iniciar();
  final prefs = Preferencias();
  await prefs.carregar();
  final sessao = Sessao();
  sessao.iniciar();
  runApp(MyBarberApp(sessao: sessao, preferencias: prefs));
}

class MyBarberApp extends StatelessWidget {
  final Sessao sessao;
  final Preferencias preferencias;
  const MyBarberApp({super.key, required this.sessao, required this.preferencias});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: sessao),
        ChangeNotifierProvider.value(value: preferencias),
        ChangeNotifierProvider(create: (_) => Sincronizador()),
      ],
      child: Consumer<Preferencias>(
        builder: (context, prefs, _) => MaterialApp(
          title: 'MyBarber',
          debugShowCheckedModeBanner: false,
          theme: temaClaro(),
          darkTheme: temaEscuro(),
          themeMode: prefs.tema,
          locale: const Locale('pt', 'BR'),
          supportedLocales: const [Locale('pt', 'BR')],
          localizationsDelegates: const [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          home: const Portao(),
        ),
      ),
    );
  }
}

/// Decide entre a tela de entrada e o app, conforme a sessao.
class Portao extends StatelessWidget {
  const Portao({super.key});

  @override
  Widget build(BuildContext context) {
    final sessao = context.watch<Sessao>();
    if (sessao.carregando) {
      return const Scaffold(body: Center(child: CircularProgressIndicator(semanticsLabel: 'Carregando')));
    }
    return sessao.logado ? const TelaInicio() : const TelaEntrada();
  }
}
