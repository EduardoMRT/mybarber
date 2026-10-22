// Modelos espelhando os DTOs da API.

double _num(dynamic v) => v == null ? 0 : (v as num).toDouble();
double? _numOuNulo(dynamic v) => v == null ? null : (v as num).toDouble();

/// A API envia "2026-10-21T15:30:00" no horario de Brasilia (sem fuso).
DateTime dataHora(String s) => DateTime.parse(s);

String isoSemFuso(DateTime d) {
  String p(int n) => n.toString().padLeft(2, '0');
  return '${d.year}-${p(d.month)}-${p(d.day)}T${p(d.hour)}:${p(d.minute)}:00';
}

String isoData(DateTime d) {
  String p(int n) => n.toString().padLeft(2, '0');
  return '${d.year}-${p(d.month)}-${p(d.day)}';
}

class Usuario {
  final int id;
  final String nome;
  final String email;
  final String? telefone;
  final String papel;
  final String? sexo;
  final int? tipoCabeloId;
  final String? tipoCabeloNome;
  final String? comprimentoCabelo;
  final String? observacoes;

  Usuario({
    required this.id,
    required this.nome,
    required this.email,
    required this.papel,
    this.telefone,
    this.sexo,
    this.tipoCabeloId,
    this.tipoCabeloNome,
    this.comprimentoCabelo,
    this.observacoes,
  });

  factory Usuario.deJson(Map<String, dynamic> j) => Usuario(
        id: j['id'] as int,
        nome: j['nome'] as String,
        email: j['email'] as String,
        telefone: j['telefone'] as String?,
        papel: j['papel'] as String,
        sexo: j['sexo'] as String?,
        tipoCabeloId: j['tipoCabeloId'] as int?,
        tipoCabeloNome: j['tipoCabeloNome'] as String?,
        comprimentoCabelo: j['comprimentoCabelo'] as String?,
        observacoes: j['observacoes'] as String?,
      );

  Map<String, dynamic> paraJson() => {
        'id': id,
        'nome': nome,
        'email': email,
        'telefone': telefone,
        'papel': papel,
        'sexo': sexo,
        'tipoCabeloId': tipoCabeloId,
        'tipoCabeloNome': tipoCabeloNome,
        'comprimentoCabelo': comprimentoCabelo,
        'observacoes': observacoes,
      };

  String get primeiroNome => nome.split(' ').first;
}

class TipoCabelo {
  final int id;
  final String nome;
  final String? descricao;

  TipoCabelo(this.id, this.nome, this.descricao);

  factory TipoCabelo.deJson(Map<String, dynamic> j) =>
      TipoCabelo(j['id'] as int, j['nome'] as String, j['descricao'] as String?);
}

class BarbeariaResumo {
  final int id;
  final String nome;
  final String endereco;
  final String? bairro;
  final String? cidade;
  final double? distanciaKm;
  final String? telefone;
  final int barbeirosAtivos;
  final double? precoAPartirDe;

  BarbeariaResumo.deJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        nome = j['nome'] as String,
        endereco = (j['endereco'] as String?) ?? '',
        bairro = j['bairro'] as String?,
        cidade = j['cidade'] as String?,
        distanciaKm = _numOuNulo(j['distanciaKm']),
        telefone = j['telefone'] as String?,
        barbeirosAtivos = (j['barbeirosAtivos'] as num?)?.toInt() ?? 0,
        precoAPartirDe = _numOuNulo(j['precoAPartirDe']);
}

class ServicoPublico {
  final int id;
  final String nome;
  final String? descricao;
  final double preco;
  final int duracaoMin;
  final Map<int, int> duracoes;

  ServicoPublico.deJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        nome = j['nome'] as String,
        descricao = j['descricao'] as String?,
        preco = _num(j['preco']),
        duracaoMin = (j['duracaoMin'] as num).toInt(),
        duracoes = {
          for (final d in (j['duracoes'] as List? ?? []))
            (d['tipoCabeloId'] as num).toInt(): (d['duracaoMin'] as num).toInt(),
        };

  /// Regra do MyBarber: a duracao depende do tipo de cabelo do cliente.
  int duracaoPara(int? tipoCabeloId) => duracoes[tipoCabeloId] ?? duracaoMin;
}

class BarbeiroPublico {
  final int id;
  final String nome;
  final String? bio;
  final Set<int> tiposCabeloIds;
  final Set<int> servicosIds;

  BarbeiroPublico.deJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        nome = j['nome'] as String,
        bio = j['bio'] as String?,
        tiposCabeloIds = {for (final x in (j['tiposCabeloIds'] as List? ?? [])) (x as num).toInt()},
        servicosIds = {for (final x in (j['servicosIds'] as List? ?? [])) (x as num).toInt()};

  bool atende(int? tipoCabeloId) => tipoCabeloId == null || tiposCabeloIds.isEmpty || tiposCabeloIds.contains(tipoCabeloId);

  bool faz(int servicoId) => servicosIds.isEmpty || servicosIds.contains(servicoId);
}

class BarbeariaDetalhe {
  final int id;
  final String nome;
  final String? descricao;
  final String? telefone;
  final String endereco;
  final double? latitude;
  final double? longitude;
  final int prazoCancelamentoMin;
  final List<ServicoPublico> servicos;
  final List<BarbeiroPublico> barbeiros;

  BarbeariaDetalhe.deJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        nome = j['nome'] as String,
        descricao = j['descricao'] as String?,
        telefone = j['telefone'] as String?,
        endereco = (j['endereco'] as String?) ?? '',
        latitude = _numOuNulo(j['latitude']),
        longitude = _numOuNulo(j['longitude']),
        prazoCancelamentoMin = (j['prazoCancelamentoMin'] as num?)?.toInt() ?? 120,
        servicos = [for (final s in (j['servicos'] as List? ?? [])) ServicoPublico.deJson(s as Map<String, dynamic>)],
        barbeiros = [for (final b in (j['barbeiros'] as List? ?? [])) BarbeiroPublico.deJson(b as Map<String, dynamic>)];
}

class Horarios {
  final int duracaoMin;
  final List<String> horarios; // "09:00:00"

  Horarios.deJson(Map<String, dynamic> j)
      : duracaoMin = (j['duracaoMin'] as num).toInt(),
        horarios = [for (final h in (j['horarios'] as List? ?? [])) h as String];
}

class Remarcacao {
  final DateTime inicioAnterior;
  final DateTime inicioSugerido;
  final String motivo;
  final DateTime prazo;

  Remarcacao.deJson(Map<String, dynamic> j)
      : inicioAnterior = dataHora(j['inicioAnterior'] as String),
        inicioSugerido = dataHora(j['inicioSugerido'] as String),
        motivo = (j['motivo'] as String?) ?? '',
        prazo = dataHora(j['prazo'] as String);
}

class Agendamento {
  final int id;
  final int barbeariaId;
  final String barbeariaNome;
  final String? barbeariaEndereco;
  final String? barbeariaTelefone;
  final int barbeiroId;
  final String barbeiroNome;
  final int servicoId;
  final String servicoNome;
  final DateTime inicio;
  final DateTime fim;
  final int duracaoMin;
  final double valor;
  final String status;
  final String? motivoCancelamento;
  final Remarcacao? remarcacao;
  final bool podeCancelar;

  Agendamento.deJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        barbeariaId = j['barbeariaId'] as int,
        barbeariaNome = j['barbeariaNome'] as String,
        barbeariaEndereco = j['barbeariaEndereco'] as String?,
        barbeariaTelefone = j['barbeariaTelefone'] as String?,
        barbeiroId = j['barbeiroId'] as int,
        barbeiroNome = j['barbeiroNome'] as String,
        servicoId = j['servicoId'] as int,
        servicoNome = j['servicoNome'] as String,
        inicio = dataHora(j['inicio'] as String),
        fim = dataHora(j['fim'] as String),
        duracaoMin = (j['duracaoMin'] as num).toInt(),
        valor = _num(j['valor']),
        status = j['status'] as String,
        motivoCancelamento = j['motivoCancelamento'] as String?,
        remarcacao = j['remarcacao'] == null ? null : Remarcacao.deJson(j['remarcacao'] as Map<String, dynamic>),
        podeCancelar = (j['podeCancelar'] as bool?) ?? false;

  bool get ativo => status == 'CONFIRMADO' || status == 'AGUARDANDO_CLIENTE';
  bool get aguardandoResposta => status == 'AGUARDANDO_CLIENTE';
}

class Notificacao {
  final int id;
  final String tipo;
  final String titulo;
  final String mensagem;
  final int? agendamentoId;
  final bool lida;
  final DateTime criadoEm;

  Notificacao.deJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        tipo = j['tipo'] as String,
        titulo = j['titulo'] as String,
        mensagem = j['mensagem'] as String,
        agendamentoId = (j['agendamentoId'] as num?)?.toInt(),
        lida = (j['lida'] as bool?) ?? false,
        criadoEm = dataHora(j['criadoEm'] as String);
}

class DocumentoLegal {
  final String slug;
  final String titulo;
  final String versao;
  final String conteudo;

  DocumentoLegal.deJson(Map<String, dynamic> j)
      : slug = j['slug'] as String,
        titulo = j['titulo'] as String,
        versao = j['versao'] as String,
        conteudo = (j['conteudo'] as String?) ?? '';
}
