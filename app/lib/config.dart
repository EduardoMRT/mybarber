/// Configuracao de ambiente.
///
/// A URL da API vem de --dart-define=API_URL=https://api.seudominio.com.br
/// Sem ela, usa o endereco do computador visto pelo emulador Android.
class Config {
  static const apiUrl = String.fromEnvironment('API_URL', defaultValue: 'http://10.0.2.2:8080');

  /// Todos os horarios do MyBarber sao no fuso de Brasilia.
  static const fuso = 'America/Sao_Paulo';
}
