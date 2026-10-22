import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_cliente.dart';
import '../api/modelos.dart';
import '../estado/sessao.dart';
import '../widgets/comuns.dart';
import 'documento.dart';

const sexos = {'MASCULINO': 'Masculino', 'FEMININO': 'Feminino', 'OUTRO': 'Outro', 'NAO_INFORMADO': 'Prefiro não dizer'};
const comprimentos = {'CURTO': 'Curto', 'MEDIO': 'Médio', 'LONGO': 'Longo'};

Future<List<TipoCabelo>> carregarTiposCabelo() async {
  final lista = await ApiCliente.instancia.get('/api/public/tipos-cabelo') as List;
  return [for (final j in lista) TipoCabelo.deJson(j as Map<String, dynamic>)];
}

/// Escolha do perfil de cabelo, usada no cadastro e na edicao do perfil.
class PerfilCabeloCampos extends StatelessWidget {
  final List<TipoCabelo> tipos;
  final String? sexo;
  final int? tipoCabeloId;
  final String? comprimento;
  final ValueChanged<String?> aoSexo;
  final ValueChanged<int?> aoTipo;
  final ValueChanged<String?> aoComprimento;

  const PerfilCabeloCampos({
    super.key,
    required this.tipos,
    required this.sexo,
    required this.tipoCabeloId,
    required this.comprimento,
    required this.aoSexo,
    required this.aoTipo,
    required this.aoComprimento,
  });

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    Widget grupo(String titulo, List<Widget> chips) => Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(titulo, style: t.titleSmall),
            const SizedBox(height: 8),
            Wrap(spacing: 8, runSpacing: 8, children: chips),
            const SizedBox(height: 18),
          ],
        );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        grupo('Tipo de cabelo', [
          for (final tipo in tipos)
            ChoiceChip(
              label: Text(tipo.nome),
              selected: tipoCabeloId == tipo.id,
              onSelected: (s) => aoTipo(s ? tipo.id : null),
              tooltip: tipo.descricao,
            ),
        ]),
        grupo('Comprimento', [
          for (final e in comprimentos.entries)
            ChoiceChip(label: Text(e.value), selected: comprimento == e.key, onSelected: (s) => aoComprimento(s ? e.key : null)),
        ]),
        grupo('Sexo', [
          for (final e in sexos.entries)
            ChoiceChip(label: Text(e.value), selected: sexo == e.key, onSelected: (s) => aoSexo(s ? e.key : null)),
        ]),
      ],
    );
  }
}

class TelaCadastro extends StatefulWidget {
  const TelaCadastro({super.key});

  @override
  State<TelaCadastro> createState() => _TelaCadastroState();
}

class _TelaCadastroState extends State<TelaCadastro> {
  final _form = GlobalKey<FormState>();
  final _nome = TextEditingController();
  final _email = TextEditingController();
  final _telefone = TextEditingController();
  final _senha = TextEditingController();
  late final Future<List<TipoCabelo>> _tipos = carregarTiposCabelo();
  String? _sexo;
  int? _tipo;
  String? _comprimento;
  bool _aceite = false;
  bool _ocupado = false;

  @override
  void dispose() {
    for (final c in [_nome, _email, _telefone, _senha]) {
      c.dispose();
    }
    super.dispose();
  }

  void _abrir(String slug, String titulo) =>
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => TelaDocumento(slug: slug, titulo: titulo)));

  Future<void> _criar() async {
    if (!_form.currentState!.validate()) return;
    if (!_aceite) {
      avisar(context, 'Para criar a conta, aceite os Termos de Uso e a Política de Privacidade.', erro: true);
      return;
    }
    setState(() => _ocupado = true);
    try {
      await context.read<Sessao>().registrar(
            nome: _nome.text,
            email: _email.text,
            senha: _senha.text,
            telefone: _telefone.text.trim().isEmpty ? null : _telefone.text.trim(),
            sexo: _sexo,
            tipoCabeloId: _tipo,
            comprimentoCabelo: _comprimento,
          );
      if (mounted) Navigator.of(context).popUntil((r) => r.isFirst);
    } catch (e) {
      if (mounted) avisar(context, e.toString(), erro: true);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final link = TextStyle(color: t.colorScheme.primary, decoration: TextDecoration.underline, fontWeight: FontWeight.w600);
    return Scaffold(
      appBar: AppBar(title: const Text('Criar conta')),
      body: SafeArea(
        child: Form(
          key: _form,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
            children: [
              TextFormField(
                controller: _nome,
                textCapitalization: TextCapitalization.words,
                autofillHints: const [AutofillHints.name],
                decoration: const InputDecoration(labelText: 'Nome'),
                validator: (v) => (v == null || v.trim().length < 2) ? 'Informe seu nome' : null,
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _email,
                keyboardType: TextInputType.emailAddress,
                autofillHints: const [AutofillHints.email],
                decoration: const InputDecoration(labelText: 'E-mail'),
                validator: (v) => (v == null || !v.contains('@')) ? 'Informe um e-mail válido' : null,
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _telefone,
                keyboardType: TextInputType.phone,
                autofillHints: const [AutofillHints.telephoneNumber],
                decoration: const InputDecoration(labelText: 'Celular (opcional)', helperText: 'A barbearia usa para falar com você.'),
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _senha,
                obscureText: true,
                autofillHints: const [AutofillHints.newPassword],
                decoration: const InputDecoration(labelText: 'Senha', helperText: 'Pelo menos 8 caracteres.'),
                validator: (v) => (v == null || v.length < 8) ? 'A senha precisa ter 8 caracteres ou mais' : null,
              ),
              const SizedBox(height: 28),
              Semantics(header: true, child: Text('Seu cabelo', style: t.textTheme.titleLarge)),
              const SizedBox(height: 4),
              Text(
                'O tipo de cabelo define quanto tempo o corte leva. Assim você só vê horários que cabem.',
                style: t.textTheme.bodyMedium?.copyWith(color: t.colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 16),
              FutureBuilder<List<TipoCabelo>>(
                future: _tipos,
                builder: (context, s) => s.hasData
                    ? PerfilCabeloCampos(
                        tipos: s.data!,
                        sexo: _sexo,
                        tipoCabeloId: _tipo,
                        comprimento: _comprimento,
                        aoSexo: (v) => setState(() => _sexo = v),
                        aoTipo: (v) => setState(() => _tipo = v),
                        aoComprimento: (v) => setState(() => _comprimento = v),
                      )
                    : const Padding(padding: EdgeInsets.all(16), child: LinearProgressIndicator()),
              ),
              CheckboxListTile(
                value: _aceite,
                onChanged: (v) => setState(() => _aceite = v ?? false),
                contentPadding: EdgeInsets.zero,
                controlAffinity: ListTileControlAffinity.leading,
                title: Text.rich(TextSpan(
                  text: 'Li e aceito os ',
                  children: [
                    TextSpan(
                      text: 'Termos de Uso',
                      style: link,
                      recognizer: TapGestureRecognizer()..onTap = () => _abrir('termos-de-uso', 'Termos de Uso'),
                    ),
                    const TextSpan(text: ' e a '),
                    TextSpan(
                      text: 'Política de Privacidade',
                      style: link,
                      recognizer: TapGestureRecognizer()
                        ..onTap = () => _abrir('politica-de-privacidade', 'Política de Privacidade'),
                    ),
                    const TextSpan(text: '.'),
                  ],
                )),
              ),
              const SizedBox(height: 16),
              BotaoAcao(texto: 'Criar conta', ocupado: _ocupado, aoTocar: _criar),
            ],
          ),
        ),
      ),
    );
  }
}
