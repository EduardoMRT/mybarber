import 'package:flutter_test/flutter_test.dart';
import 'package:mybarber/api/modelos.dart';

void main() {
  test('duração do serviço depende do tipo de cabelo', () {
    final s = ServicoPublico.deJson({
      'id': 1,
      'nome': 'Corte',
      'preco': 40,
      'duracaoMin': 30,
      'duracoes': [
        {'tipoCabeloId': 1, 'duracaoMin': 45},
      ],
    });
    expect(s.duracaoPara(1), 45);
    expect(s.duracaoPara(4), 30);
    expect(s.duracaoPara(null), 30);
  });

  test('barbeiro sem restrição atende todos os tipos', () {
    final b = BarbeiroPublico.deJson({'id': 1, 'nome': 'João', 'tiposCabeloIds': [], 'servicosIds': [2]});
    expect(b.atende(3), isTrue);
    expect(b.faz(2), isTrue);
    expect(b.faz(5), isFalse);
  });

  test('data sem fuso no formato da API', () {
    expect(isoSemFuso(DateTime(2026, 10, 13, 9, 5)), '2026-10-13T09:05:00');
  });
}
