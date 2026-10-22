import 'dart:async';
import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

import '../config.dart';

class ApiErro implements Exception {
  final int status;
  final String mensagem;
  final Map<String, String> campos;

  ApiErro(this.status, this.mensagem, [this.campos = const {}]);

  @override
  String toString() => mensagem;
}

/// Cliente HTTP da API com JWT: guarda os tokens no armazenamento seguro do
/// aparelho e renova o token de acesso automaticamente quando ele expira.
class ApiCliente {
  ApiCliente._();
  static final instancia = ApiCliente._();

  static const _armazenamento = FlutterSecureStorage();
  static const _chaveAcesso = 'mybarber_acesso';
  static const _chaveRefresh = 'mybarber_refresh';

  final _http = http.Client();
  String? _acesso;
  String? _refresh;
  Future<bool>? _renovando;

  /// Chamado quando a sessao expira de vez (refresh invalido).
  void Function()? aoExpirar;

  bool get temSessao => _refresh != null;

  Future<void> carregar() async {
    _acesso = await _armazenamento.read(key: _chaveAcesso);
    _refresh = await _armazenamento.read(key: _chaveRefresh);
  }

  Future<void> salvarTokens(String acesso, String refresh) async {
    _acesso = acesso;
    _refresh = refresh;
    await _armazenamento.write(key: _chaveAcesso, value: acesso);
    await _armazenamento.write(key: _chaveRefresh, value: refresh);
  }

  Future<void> limpar() async {
    _acesso = null;
    _refresh = null;
    await _armazenamento.delete(key: _chaveAcesso);
    await _armazenamento.delete(key: _chaveRefresh);
  }

  String? get refreshToken => _refresh;

  Future<dynamic> get(String caminho, {Map<String, String?>? params}) => _enviar('GET', caminho, params: params);

  Future<dynamic> post(String caminho, [Object? corpo]) => _enviar('POST', caminho, corpo: corpo ?? const {});

  Future<dynamic> put(String caminho, Object corpo) => _enviar('PUT', caminho, corpo: corpo);

  Future<dynamic> _enviar(String metodo, String caminho,
      {Object? corpo, Map<String, String?>? params, bool podeRenovar = true}) async {
    final query = <String, String>{};
    params?.forEach((k, v) {
      if (v != null && v.isNotEmpty) query[k] = v;
    });
    final uri = Uri.parse('${Config.apiUrl}$caminho').replace(queryParameters: query.isEmpty ? null : query);
    final headers = <String, String>{
      'Accept': 'application/json',
      if (corpo != null) 'Content-Type': 'application/json; charset=utf-8',
      if (_acesso != null) 'Authorization': 'Bearer $_acesso',
    };

    http.Response resp;
    try {
      final req = http.Request(metodo, uri)..headers.addAll(headers);
      if (corpo != null) req.body = jsonEncode(corpo);
      resp = await http.Response.fromStream(await _http.send(req).timeout(const Duration(seconds: 20)));
    } on TimeoutException {
      throw ApiErro(0, 'O servidor demorou para responder. Tente de novo.');
    } catch (_) {
      throw ApiErro(0, 'Sem conexão com a internet ou com o servidor.');
    }

    if (resp.statusCode == 401 && podeRenovar && _refresh != null && !caminho.startsWith('/api/auth/')) {
      if (await _renovar()) {
        return _enviar(metodo, caminho, corpo: corpo, params: params, podeRenovar: false);
      }
    }

    final texto = utf8.decode(resp.bodyBytes);
    if (resp.statusCode >= 400) {
      var mensagem = 'Erro ${resp.statusCode}. Tente de novo.';
      var campos = <String, String>{};
      try {
        final j = jsonDecode(texto) as Map<String, dynamic>;
        mensagem = (j['mensagem'] as String?) ?? mensagem;
        final c = j['campos'];
        if (c is Map) campos = c.map((k, v) => MapEntry(k.toString(), v.toString()));
        if (campos.isNotEmpty) mensagem = '$mensagem ${campos.values.join(' ')}';
      } catch (_) {}
      if (resp.statusCode == 401 && !caminho.startsWith('/api/auth/login')) {
        await limpar();
        aoExpirar?.call();
      }
      throw ApiErro(resp.statusCode, mensagem, campos);
    }
    if (texto.isEmpty) return null;
    return jsonDecode(texto);
  }

  Future<bool> _renovar() {
    return _renovando ??= () async {
      try {
        final resp = await _http.post(
          Uri.parse('${Config.apiUrl}/api/auth/refresh'),
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode({'refreshToken': _refresh}),
        );
        if (resp.statusCode != 200) {
          await limpar();
          aoExpirar?.call();
          return false;
        }
        final j = jsonDecode(utf8.decode(resp.bodyBytes)) as Map<String, dynamic>;
        await salvarTokens(j['accessToken'] as String, j['refreshToken'] as String);
        return true;
      } catch (_) {
        return false;
      } finally {
        _renovando = null;
      }
    }();
  }
}
