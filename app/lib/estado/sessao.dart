import 'package:flutter/foundation.dart';

import '../api/api_cliente.dart';
import '../api/modelos.dart';

/// Usuario logado no app (somente clientes).
class Sessao extends ChangeNotifier {
  final _api = ApiCliente.instancia;
  Usuario? usuario;
  bool carregando = true;

  Sessao() {
    _api.aoExpirar = () {
      usuario = null;
      notifyListeners();
    };
  }

  bool get logado => usuario != null;

  Future<void> iniciar() async {
    await _api.carregar();
    if (_api.temSessao) {
      try {
        usuario = Usuario.deJson(await _api.get('/api/auth/me') as Map<String, dynamic>);
      } on ApiErro catch (e) {
        if (e.status == 401) await _api.limpar();
      }
    }
    carregando = false;
    notifyListeners();
  }

  Future<void> entrar(String email, String senha) async {
    final j = await _api.post('/api/auth/login', {'email': email.trim(), 'senha': senha}) as Map<String, dynamic>;
    await _aplicar(j);
  }

  Future<void> registrar({
    required String nome,
    required String email,
    required String senha,
    String? telefone,
    String? sexo,
    int? tipoCabeloId,
    String? comprimentoCabelo,
  }) async {
    final j = await _api.post('/api/auth/registrar', {
      'nome': nome.trim(),
      'email': email.trim(),
      'senha': senha,
      'telefone': telefone,
      'sexo': sexo,
      'tipoCabeloId': tipoCabeloId,
      'comprimentoCabelo': comprimentoCabelo,
      'aceiteTermos': true,
      'aceitePrivacidade': true,
    }) as Map<String, dynamic>;
    await _aplicar(j);
  }

  Future<void> _aplicar(Map<String, dynamic> j) async {
    final u = Usuario.deJson(j['usuario'] as Map<String, dynamic>);
    if (u.papel != 'CLIENTE') {
      throw ApiErro(403, 'Esta conta é de barbearia. Use o portal MyBarber no computador.');
    }
    await _api.salvarTokens(j['accessToken'] as String, j['refreshToken'] as String);
    usuario = u;
    notifyListeners();
  }

  Future<void> atualizarPerfil(Map<String, dynamic> dados) async {
    usuario = Usuario.deJson(await _api.put('/api/app/perfil', dados) as Map<String, dynamic>);
    notifyListeners();
  }

  Future<void> sair() async {
    final r = _api.refreshToken;
    if (r != null) {
      try {
        await _api.post('/api/auth/logout', {'refreshToken': r});
      } catch (_) {}
    }
    await _api.limpar();
    usuario = null;
    notifyListeners();
  }

  /// Depois de excluir a conta no servidor.
  Future<void> encerrarLocal() async {
    await _api.limpar();
    usuario = null;
    notifyListeners();
  }
}
