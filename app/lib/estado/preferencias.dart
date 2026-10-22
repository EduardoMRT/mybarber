import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Preferencias do aparelho: tema e antecedencia do lembrete.
class Preferencias extends ChangeNotifier {
  static const _chaveTema = 'tema';
  static const _chaveLembrete = 'lembrete_min';

  ThemeMode tema = ThemeMode.system;
  int lembreteMin = 30;

  Future<void> carregar() async {
    final p = await SharedPreferences.getInstance();
    tema = switch (p.getString(_chaveTema)) {
      'claro' => ThemeMode.light,
      'escuro' => ThemeMode.dark,
      _ => ThemeMode.system,
    };
    lembreteMin = p.getInt(_chaveLembrete) ?? 30;
    notifyListeners();
  }

  Future<void> definirTema(ThemeMode t) async {
    tema = t;
    notifyListeners();
    final p = await SharedPreferences.getInstance();
    await p.setString(_chaveTema, switch (t) { ThemeMode.light => 'claro', ThemeMode.dark => 'escuro', _ => 'sistema' });
  }

  Future<void> definirLembrete(int minutos) async {
    lembreteMin = minutos;
    notifyListeners();
    final p = await SharedPreferences.getInstance();
    await p.setInt(_chaveLembrete, minutos);
  }
}
