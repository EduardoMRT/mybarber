import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_cliente.dart';
import '../api/modelos.dart';
import '../estado/sessao.dart';
import '../servicos/localizacao.dart';
import '../widgets/comuns.dart';
import 'barbearia.dart';

class TelaBarbearias extends StatefulWidget {
  const TelaBarbearias({super.key});

  @override
  State<TelaBarbearias> createState() => _TelaBarbeariasState();
}

class _TelaBarbeariasState extends State<TelaBarbearias> {
  final _busca = TextEditingController();
  Timer? _espera;
  Posicao? _posicao;
  String? _avisoLocal;
  List<BarbeariaResumo>? _lista;
  String? _erro;
  bool _carregando = true;

  @override
  void initState() {
    super.initState();
    _localizarEBuscar();
  }

  @override
  void dispose() {
    _busca.dispose();
    _espera?.cancel();
    super.dispose();
  }

  Future<void> _localizarEBuscar() async {
    setState(() => _carregando = true);
    final (p, aviso) = await Localizacao.atual();
    if (!mounted) return;
    _posicao = p;
    _avisoLocal = aviso;
    await _buscar();
  }

  Future<void> _buscar() async {
    setState(() {
      _carregando = true;
      _erro = null;
    });
    try {
      final lista = await ApiCliente.instancia.get('/api/public/barbearias', params: {
        'lat': _posicao?.lat.toString(),
        'lng': _posicao?.lng.toString(),
        'raioKm': _posicao != null ? '30' : null,
        'busca': _busca.text.trim(),
      }) as List;
      _lista = [for (final j in lista) BarbeariaResumo.deJson(j as Map<String, dynamic>)];
    } on ApiErro catch (e) {
      _erro = e.mensagem;
    } finally {
      if (mounted) setState(() => _carregando = false);
    }
  }

  void _aoDigitar(String _) {
    _espera?.cancel();
    _espera = Timer(const Duration(milliseconds: 450), _buscar);
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final nome = context.watch<Sessao>().usuario?.primeiroNome ?? '';
    return Scaffold(
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _localizarEBuscar,
          child: CustomScrollView(
            slivers: [
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 8),
                sliver: SliverList.list(children: [
                  Semantics(header: true, child: Text('Olá, $nome', style: t.textTheme.headlineSmall)),
                  const SizedBox(height: 4),
                  Text(
                    _posicao != null ? 'Barbearias até 30 km de você' : 'Encontre uma barbearia',
                    style: t.textTheme.bodyLarge?.copyWith(color: t.colorScheme.onSurfaceVariant),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _busca,
                    onChanged: _aoDigitar,
                    textInputAction: TextInputAction.search,
                    onSubmitted: (_) => _buscar(),
                    decoration: InputDecoration(
                      hintText: 'Nome, bairro ou cidade',
                      prefixIcon: const Icon(Icons.search),
                      suffixIcon: _posicao == null
                          ? IconButton(
                              tooltip: 'Usar minha localização',
                              icon: const Icon(Icons.my_location),
                              onPressed: _localizarEBuscar,
                            )
                          : null,
                    ),
                  ),
                  if (_avisoLocal != null) ...[
                    const SizedBox(height: 10),
                    Text(_avisoLocal!, style: t.textTheme.bodySmall?.copyWith(color: t.colorScheme.onSurfaceVariant)),
                  ],
                  const SizedBox(height: 8),
                ]),
              ),
              if (_carregando && _lista == null)
                const SliverFillRemaining(child: Center(child: CircularProgressIndicator()))
              else if (_erro != null)
                SliverFillRemaining(child: ErroTela(mensagem: _erro!, aoTentar: _buscar))
              else if (_lista!.isEmpty)
                SliverFillRemaining(
                  hasScrollBody: false,
                  child: Vazio(
                    icone: Icons.content_cut,
                    titulo: 'Nenhuma barbearia encontrada',
                    texto: _posicao != null
                        ? 'Ainda não há barbearias do MyBarber por perto. Tente buscar pela cidade.'
                        : 'Tente outro nome, bairro ou cidade.',
                  ),
                )
              else
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
                  sliver: SliverList.separated(
                    itemCount: _lista!.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemBuilder: (context, i) => _CartaoBarbearia(b: _lista![i]),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CartaoBarbearia extends StatelessWidget {
  final BarbeariaResumo b;
  const _CartaoBarbearia({required this.b});

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final distancia = b.distanciaKm == null
        ? null
        : b.distanciaKm! < 1
            ? '${(b.distanciaKm! * 1000).round()} m'
            : '${b.distanciaKm!.toStringAsFixed(1).replaceAll('.', ',')} km';
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => TelaBarbearia(id: b.id, nome: b.nome))),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(child: Text(b.nome, style: t.textTheme.titleMedium)),
                  if (distancia != null)
                    Text(distancia, style: t.textTheme.labelLarge?.copyWith(color: t.colorScheme.secondary, fontWeight: FontWeight.w700)),
                ],
              ),
              const SizedBox(height: 4),
              Text(b.endereco, style: t.textTheme.bodyMedium?.copyWith(color: t.colorScheme.onSurfaceVariant)),
              const SizedBox(height: 10),
              Text(
                [
                  if (b.precoAPartirDe != null) 'A partir de ${moeda.format(b.precoAPartirDe)}',
                  '${b.barbeirosAtivos} ${b.barbeirosAtivos == 1 ? 'barbeiro' : 'barbeiros'}',
                ].join(', '),
                style: t.textTheme.bodySmall,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
