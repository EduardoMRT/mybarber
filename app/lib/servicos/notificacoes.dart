import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;

import '../config.dart';

/// Notificacoes locais do aparelho: lembrete antes do horario e avisos
/// vindos da barbearia (remarcacao, cancelamento).
class Notificacoes {
  Notificacoes._();
  static final instancia = Notificacoes._();

  final _plugin = FlutterLocalNotificationsPlugin();
  late tz.Location _fuso;
  bool _pronto = false;

  static const _canalLembretes = AndroidNotificationDetails(
    'lembretes',
    'Lembretes de horário',
    channelDescription: 'Aviso alguns minutos antes do horário marcado',
    importance: Importance.high,
    priority: Priority.high,
  );

  static const _canalAvisos = AndroidNotificationDetails(
    'avisos',
    'Avisos da barbearia',
    channelDescription: 'Remarcações, cancelamentos e confirmações',
    importance: Importance.high,
    priority: Priority.high,
  );

  Future<void> iniciar() async {
    if (_pronto || kIsWeb) return;
    tzdata.initializeTimeZones();
    _fuso = tz.getLocation(Config.fuso);
    const config = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
      iOS: DarwinInitializationSettings(
        requestAlertPermission: false,
        requestBadgePermission: false,
        requestSoundPermission: false,
      ),
    );
    await _plugin.initialize(config);
    _pronto = true;
  }

  /// Pede permissao (Android 13+ e iOS). Chamado depois do login.
  Future<void> pedirPermissao() async {
    if (!_pronto) return;
    await _plugin
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
    await _plugin
        .resolvePlatformSpecificImplementation<IOSFlutterLocalNotificationsPlugin>()
        ?.requestPermissions(alert: true, badge: true, sound: true);
  }

  /// Agenda o lembrete no fuso de Brasilia (mesmo fuso dos horarios da API).
  Future<void> agendarLembrete({
    required int id,
    required DateTime quando,
    required String titulo,
    required String mensagem,
  }) async {
    if (!_pronto) return;
    final data = tz.TZDateTime(_fuso, quando.year, quando.month, quando.day, quando.hour, quando.minute);
    if (data.isBefore(tz.TZDateTime.now(_fuso))) return;
    await _plugin.zonedSchedule(
      id,
      titulo,
      mensagem,
      data,
      const NotificationDetails(android: _canalLembretes, iOS: DarwinNotificationDetails()),
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
    );
  }

  Future<void> cancelar(int id) async {
    if (_pronto) await _plugin.cancel(id);
  }

  Future<void> mostrar({required int id, required String titulo, required String mensagem}) async {
    if (!_pronto) return;
    await _plugin.show(
      id,
      titulo,
      mensagem,
      const NotificationDetails(android: _canalAvisos, iOS: DarwinNotificationDetails()),
    );
  }

  Future<void> cancelarTodas() async {
    if (_pronto) await _plugin.cancelAll();
  }
}
