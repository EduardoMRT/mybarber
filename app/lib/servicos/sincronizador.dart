import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:intl/intl.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../api/api_cliente.dart';
import '../api/modelos.dart';
import 'notificacoes.dart';

/// Mantem reservas e avisos atualizados e reprograma os lembretes locais.
///
/// Como as notificacoes sao locais (sem servidor de push), os avisos da
/// barbearia chegam quando o app esta aberto ou ao ser reaberto; os
/// lembretes de horario ficam agendados no aparelho e tocam mesmo com o app fechado.
class Sincronizador extends ChangeNotifier with WidgetsBindingObserver {
  final _api = ApiCliente.instancia;
  final _notif = Notificacoes.instancia;
  Timer? _timer;

  List<Agendamento> reservas = [];
  List<Notificacao> avisos = [];
  int naoLidas = 0;
  bool carregando = false;
  String? erro;
  int lembreteMin = 30;

  static const _chaveLembretes = 'lembretes_agendados';
  static const _chaveUltimoAviso = 'ultimo_aviso_mostrado';
  static const _baseIdAviso = 1000000000;

  void iniciar() {
    WidgetsBinding.instance.addObserver(this);
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 60), (_) => sincronizar());
    sincronizar();
  }

  void parar() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    _timer = null;
    reservas = [];
    avisos = [];
    naoLidas = 0;
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) sincronizar();
  }

  Future<void> sincronizar() async {
    if (!_api.temSessao) return;
    carregando = true;
    notifyListeners();
    try {
      final lista = await _api.get('/api/app/agendamentos') as List;
      reservas = [for (final j in lista) Agendamento.deJson(j as Map<String, dynamic>)];
      final n = await _api.get('/api/app/notificacoes') as Map<String, dynamic>;
      avisos = [for (final j in (n['itens'] as List)) Notificacao.deJson(j as Map<String, dynamic>)];
      naoLidas = (n['naoLidas'] as num).toInt();
      erro = null;
      await _reprogramarLembretes();
      await _mostrarNovosAvisos();
    } on ApiErro catch (e) {
      erro = e.mensagem;
    } finally {
      carregando = false;
      notifyListeners();
    }
  }

  Future<void> _reprogramarLembretes() async {
    final prefs = await SharedPreferences.getInstance();
    for (final id in prefs.getStringList(_chaveLembretes) ?? const <String>[]) {
      await _notif.cancelar(int.parse(id));
    }
    final agendados = <String>[];
    final fmt = DateFormat('HH:mm');
    for (final r in reservas.where((r) => r.status == 'CONFIRMADO')) {
      final quando = r.inicio.subtract(Duration(minutes: lembreteMin));
      if (quando.isBefore(DateTime.now())) continue;
      await _notif.agendarLembrete(
        id: r.id,
        quando: quando,
        titulo: 'Seu horário é às ${fmt.format(r.inicio)}',
        mensagem: '${r.servicoNome} com ${r.barbeiroNome} na ${r.barbeariaNome}.',
      );
      agendados.add('${r.id}');
    }
    await prefs.setStringList(_chaveLembretes, agendados);
  }

  Future<void> _mostrarNovosAvisos() async {
    final prefs = await SharedPreferences.getInstance();
    final ultimo = prefs.getInt(_chaveUltimoAviso);
    final maior = avisos.isEmpty ? 0 : avisos.map((a) => a.id).reduce((a, b) => a > b ? a : b);
    if (ultimo == null) {
      // primeira sincronizacao neste aparelho: nao repete avisos antigos
      await prefs.setInt(_chaveUltimoAviso, maior);
      return;
    }
    for (final a in avisos.where((a) => !a.lida && a.id > ultimo)) {
      // o lembrete de confirmacao ja aparece na tela de agendamento
      if (a.tipo == 'AGENDAMENTO_CONFIRMADO') continue;
      await _notif.mostrar(id: _baseIdAviso + (a.id % _baseIdAviso), titulo: a.titulo, mensagem: a.mensagem);
    }
    if (maior > ultimo) await prefs.setInt(_chaveUltimoAviso, maior);
  }

  Future<void> marcarLida(int id) async {
    await _api.post('/api/app/notificacoes/$id/lida');
    await sincronizar();
  }

  Future<void> marcarTodasLidas() async {
    await _api.post('/api/app/notificacoes/lidas');
    await sincronizar();
  }

  Future<void> definirLembrete(int minutos) async {
    lembreteMin = minutos;
    await _reprogramarLembretes();
  }

  Future<void> limparAoSair() async {
    parar();
    await _notif.cancelarTodas();
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_chaveLembretes);
    await prefs.remove(_chaveUltimoAviso);
    notifyListeners();
  }

  @override
  void dispose() {
    parar();
    super.dispose();
  }
}
