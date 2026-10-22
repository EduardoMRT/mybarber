import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/api_cliente.dart';
import '../api/modelos.dart';
import '../estado/sessao.dart';
import '../widgets/comuns.dart';
import 'agendar.dart';

Future<void> abrirMapa(String endereco, double? lat, double? lng) async {
  final destino = lat != null && lng != null ? '$lat,$lng' : Uri.encodeComponent(endereco);
  await launchUrl(Uri.parse('https://www.google.com/maps/search/?api=1&query=$destino'), mode: LaunchMode.externalApplication);
}

Future<void> abrirWhatsApp(String telefone) async {
  final numero = telefone.replaceAll(RegExp(r'\D'), '');
  final completo = numero.startsWith('55') ? numero : '55$numero';
  await launchUrl(Uri.parse('https://wa.me/$completo'), mode: LaunchMode.externalApplication);
}

class TelaBarbearia extends StatefulWidget {
  final int id;
  final String nome;
  const TelaBarbearia({super.key, required this.id, required this.nome});

  @override
  State<TelaBarbearia> createState() => _TelaBarbeariaState();
}

class _TelaBarbeariaState extends State<TelaBarbearia> {
  late Future<BarbeariaDetalhe> _detalhe;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  void _carregar() {
    _detalhe = ApiCliente.instancia
        .get('/api/public/barbearias/${widget.id}')
        .then((j) => BarbeariaDetalhe.deJson(j as Map<String, dynamic>));
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final usuario = context.watch<Sessao>().usuario;
    return Scaffold(
      appBar: AppBar(title: Text(widget.nome)),
      body: FutureBuilder<BarbeariaDetalhe>(
        future: _detalhe,
        builder: (context, s) {
          if (s.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
          if (s.hasError) return ErroTela(mensagem: s.error.toString(), aoTentar: () => setState(_carregar));
          final b = s.data!;
          final tipo = usuario?.tipoCabeloId;
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 4, 20, 120),
            children: [
              if (b.descricao != null) ...[
                Text(b.descricao!, style: t.textTheme.bodyLarge),
                const SizedBox(height: 16),
              ],
              Card(
                child: Column(children: [
                  ListTile(
                    leading: const Icon(Icons.place_outlined),
                    title: Text(b.endereco.isEmpty ? 'Endereço não informado' : b.endereco),
                    subtitle: const Text('Como chegar'),
                    onTap: b.endereco.isEmpty && b.latitude == null ? null : () => abrirMapa(b.endereco, b.latitude, b.longitude),
                  ),
                  if (b.telefone != null) ...[
                    const Divider(height: 1),
                    ListTile(
                      leading: const Icon(Icons.chat_outlined),
                      title: Text(b.telefone!),
                      subtitle: const Text('Conversar no WhatsApp'),
                      onTap: () => abrirWhatsApp(b.telefone!),
                    ),
                  ],
                ]),
              ),
              const SizedBox(height: 24),
              Semantics(header: true, child: Text('Serviços', style: t.textTheme.titleLarge)),
              if (tipo == null)
                Padding(
                  padding: const EdgeInsets.only(top: 6),
                  child: Text('Informe seu tipo de cabelo no Perfil para ver a duração exata.',
                      style: t.textTheme.bodySmall?.copyWith(color: t.colorScheme.onSurfaceVariant)),
                ),
              const SizedBox(height: 10),
              if (b.servicos.isEmpty)
                const Text('Esta barbearia ainda não cadastrou serviços.')
              else
                Card(
                  child: Column(children: [
                    for (var i = 0; i < b.servicos.length; i++) ...[
                      if (i > 0) const Divider(height: 1),
                      ListTile(
                        title: Text(b.servicos[i].nome, style: t.textTheme.titleMedium),
                        subtitle: Text([
                          if (b.servicos[i].descricao != null) b.servicos[i].descricao!,
                          'Cerca de ${duracaoTexto(b.servicos[i].duracaoPara(tipo))}',
                        ].join('\n')),
                        isThreeLine: b.servicos[i].descricao != null,
                        trailing: Text(moeda.format(b.servicos[i].preco), style: t.textTheme.titleMedium),
                        onTap: () => _agendar(b, b.servicos[i]),
                      ),
                    ],
                  ]),
                ),
              const SizedBox(height: 24),
              Semantics(header: true, child: Text('Barbeiros', style: t.textTheme.titleLarge)),
              const SizedBox(height: 10),
              for (final barbeiro in b.barbeiros)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Card(
                    child: ListTile(
                      leading: CircleAvatar(
                        backgroundColor: t.colorScheme.primaryContainer,
                        child: Text(barbeiro.nome[0], style: TextStyle(color: t.colorScheme.onPrimaryContainer)),
                      ),
                      title: Text(barbeiro.nome),
                      subtitle: barbeiro.bio != null ? Text(barbeiro.bio!) : null,
                      trailing: barbeiro.atende(tipo) ? null : const Text('Não atende\nseu tipo', textAlign: TextAlign.end),
                    ),
                  ),
                ),
            ],
          );
        },
      ),
      bottomNavigationBar: FutureBuilder<BarbeariaDetalhe>(
        future: _detalhe,
        builder: (context, s) => s.hasData && s.data!.servicos.isNotEmpty
            ? SafeArea(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
                  child: BotaoAcao(texto: 'Agendar horário', aoTocar: () => _agendar(s.data!, null)),
                ),
              )
            : const SizedBox.shrink(),
      ),
    );
  }

  void _agendar(BarbeariaDetalhe b, ServicoPublico? servico) {
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => TelaAgendar(barbearia: b, servicoInicial: servico)));
  }
}
