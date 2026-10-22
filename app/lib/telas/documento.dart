import 'package:flutter/material.dart';

import '../api/api_cliente.dart';
import '../api/modelos.dart';
import '../widgets/comuns.dart';

/// Exibe Termos de Uso, Politica de Privacidade e direitos LGPD.
class TelaDocumento extends StatefulWidget {
  final String slug;
  final String titulo;
  const TelaDocumento({super.key, required this.slug, required this.titulo});

  @override
  State<TelaDocumento> createState() => _TelaDocumentoState();
}

class _TelaDocumentoState extends State<TelaDocumento> {
  late Future<DocumentoLegal> _doc;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  void _carregar() {
    _doc = ApiCliente.instancia
        .get('/api/public/legal/${widget.slug}')
        .then((j) => DocumentoLegal.deJson(j as Map<String, dynamic>));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.titulo)),
      body: FutureBuilder<DocumentoLegal>(
        future: _doc,
        builder: (context, s) {
          if (s.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
          if (s.hasError) {
            return ErroTela(mensagem: s.error.toString(), aoTentar: () => setState(_carregar));
          }
          return Scrollbar(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 40),
              children: Markdown.blocos(context, s.data!.conteudo),
            ),
          );
        },
      ),
    );
  }
}

/// Renderizador simples para os documentos legais (titulos, listas, tabelas, negrito).
class Markdown {
  static List<Widget> blocos(BuildContext context, String texto) {
    final t = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final saida = <Widget>[];
    for (final bloco in texto.trim().split(RegExp(r'\n\s*\n'))) {
      final linhas = bloco.split('\n');
      if (bloco.startsWith('# ')) {
        saida.add(Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Semantics(header: true, child: Text(bloco.substring(2), style: t.headlineSmall)),
        ));
      } else if (bloco.startsWith('## ')) {
        saida.add(Padding(
          padding: const EdgeInsets.only(top: 20, bottom: 8),
          child: Semantics(header: true, child: Text(bloco.substring(3), style: t.titleLarge)),
        ));
      } else if (linhas.every((l) => l.startsWith('- '))) {
        for (final l in linhas) {
          saida.add(Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const ExcludeSemantics(child: Text('•  ')),
              Expanded(child: _rico(l.substring(2), t.bodyLarge)),
            ]),
          ));
        }
      } else if (linhas.first.startsWith('|')) {
        List<String> celulas(String l) => l.split('|').sublist(1, l.split('|').length - 1).map((c) => c.trim()).toList();
        final cab = celulas(linhas.first);
        for (final l in linhas.skip(2)) {
          final c = celulas(l);
          saida.add(Container(
            margin: const EdgeInsets.only(bottom: 8),
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(border: Border.all(color: cs.outlineVariant), borderRadius: BorderRadius.circular(8)),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              for (var i = 0; i < c.length; i++) ...[
                Text(i < cab.length ? cab[i] : '', style: t.labelMedium?.copyWith(color: cs.onSurfaceVariant)),
                _rico(c[i], t.bodyMedium),
                if (i < c.length - 1) const SizedBox(height: 6),
              ],
            ]),
          ));
        }
      } else {
        saida.add(Padding(padding: const EdgeInsets.only(bottom: 12), child: _rico(linhas.join(' '), t.bodyLarge)));
      }
    }
    return saida;
  }

  static Widget _rico(String texto, TextStyle? estilo) {
    final partes = texto.split(RegExp(r'(\*\*[^*]+\*\*)'));
    final spans = <TextSpan>[];
    final negritos = RegExp(r'\*\*([^*]+)\*\*').allMatches(texto).map((m) => m.group(1)!).toList();
    var n = 0;
    for (var i = 0; i < partes.length; i++) {
      spans.add(TextSpan(text: partes[i]));
      if (n < negritos.length && i < partes.length - 1) {
        spans.add(TextSpan(text: negritos[n++], style: const TextStyle(fontWeight: FontWeight.w700)));
      }
    }
    return Text.rich(TextSpan(style: estilo?.copyWith(height: 1.5), children: spans));
  }
}
