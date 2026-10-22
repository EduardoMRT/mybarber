import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../api/api_cliente.dart';
import '../estado/preferencias.dart';
import '../estado/sessao.dart';
import '../servicos/sincronizador.dart';
import '../widgets/comuns.dart';
import 'cadastro.dart';
import 'documento.dart';

class TelaPerfil extends StatelessWidget {
  const TelaPerfil({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    final u = context.watch<Sessao>().usuario!;
    final prefs = context.watch<Preferencias>();

    void abrir(Widget tela) => Navigator.of(context).push(MaterialPageRoute(builder: (_) => tela));
    Widget secao(String titulo) => Padding(
          padding: const EdgeInsets.fromLTRB(20, 24, 20, 8),
          child: Semantics(header: true, child: Text(titulo, style: t.textTheme.titleSmall?.copyWith(color: t.colorScheme.primary))),
        );

    return Scaffold(
      appBar: AppBar(title: const Text('Perfil')),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 32),
        children: [
          ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 20),
            leading: CircleAvatar(
              radius: 26,
              backgroundColor: t.colorScheme.primaryContainer,
              child: Text(u.nome[0].toUpperCase(),
                  style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: t.colorScheme.onPrimaryContainer)),
            ),
            title: Text(u.nome, style: t.textTheme.titleMedium),
            subtitle: Text([u.email, if (u.tipoCabeloNome != null) 'Cabelo ${u.tipoCabeloNome!.toLowerCase()}'].join('\n')),
            isThreeLine: u.tipoCabeloNome != null,
            trailing: IconButton(tooltip: 'Editar perfil', icon: const Icon(Icons.edit_outlined), onPressed: () => abrir(const TelaEditarPerfil())),
          ),
          secao('Lembrete do horário'),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: SegmentedButton<int>(
              segments: const [
                ButtonSegment(value: 15, label: Text('15 min')),
                ButtonSegment(value: 30, label: Text('30 min')),
                ButtonSegment(value: 60, label: Text('1 h')),
                ButtonSegment(value: 120, label: Text('2 h')),
              ],
              selected: {prefs.lembreteMin},
              onSelectionChanged: (v) async {
                await prefs.definirLembrete(v.first);
                if (context.mounted) await context.read<Sincronizador>().definirLembrete(v.first);
              },
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 0),
            child: Text('O celular avisa esse tempo antes de cada horário marcado.', style: t.textTheme.bodySmall),
          ),
          secao('Aparência'),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: SegmentedButton<ThemeMode>(
              segments: const [
                ButtonSegment(value: ThemeMode.system, label: Text('Automático'), icon: Icon(Icons.brightness_auto_outlined)),
                ButtonSegment(value: ThemeMode.light, label: Text('Claro'), icon: Icon(Icons.light_mode_outlined)),
                ButtonSegment(value: ThemeMode.dark, label: Text('Escuro'), icon: Icon(Icons.dark_mode_outlined)),
              ],
              selected: {prefs.tema},
              onSelectionChanged: (v) => prefs.definirTema(v.first),
            ),
          ),
          secao('Privacidade e termos'),
          ListTile(
            leading: const Icon(Icons.description_outlined),
            title: const Text('Termos de Uso'),
            onTap: () => abrir(const TelaDocumento(slug: 'termos-de-uso', titulo: 'Termos de Uso')),
          ),
          ListTile(
            leading: const Icon(Icons.privacy_tip_outlined),
            title: const Text('Política de Privacidade'),
            onTap: () => abrir(const TelaDocumento(slug: 'politica-de-privacidade', titulo: 'Política de Privacidade')),
          ),
          ListTile(
            leading: const Icon(Icons.gavel_outlined),
            title: const Text('Seus direitos (LGPD)'),
            onTap: () => abrir(const TelaDocumento(slug: 'lgpd', titulo: 'Seus direitos (LGPD)')),
          ),
          ListTile(
            leading: const Icon(Icons.download_outlined),
            title: const Text('Baixar meus dados'),
            subtitle: const Text('Tudo o que o MyBarber guarda sobre você'),
            onTap: () => abrir(const TelaMeusDados()),
          ),
          ListTile(
            leading: Icon(Icons.delete_outline, color: t.colorScheme.error),
            title: Text('Excluir minha conta', style: TextStyle(color: t.colorScheme.error)),
            onTap: () => _excluirConta(context),
          ),
          const SizedBox(height: 16),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: BotaoAcao(
              texto: 'Sair',
              contorno: true,
              aoTocar: () async {
                await context.read<Sincronizador>().limparAoSair();
                if (context.mounted) await context.read<Sessao>().sair();
              },
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _excluirConta(BuildContext context) async {
    final senha = TextEditingController();
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Excluir conta?'),
        content: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text(
              'Seus dados pessoais são apagados e suas reservas futuras canceladas. Isso não pode ser desfeito. Digite sua senha para confirmar.'),
          const SizedBox(height: 16),
          TextField(controller: senha, obscureText: true, decoration: const InputDecoration(labelText: 'Senha')),
        ]),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Voltar')),
          TextButton(onPressed: () => Navigator.pop(c, true), child: const Text('Excluir conta')),
        ],
      ),
    );
    if (confirmar != true || !context.mounted) return;
    try {
      await ApiCliente.instancia.post('/api/app/excluir-conta', {'senha': senha.text});
      if (!context.mounted) return;
      await context.read<Sincronizador>().limparAoSair();
      if (context.mounted) await context.read<Sessao>().encerrarLocal();
    } on ApiErro catch (e) {
      if (context.mounted) avisar(context, e.mensagem, erro: true);
    } finally {
      senha.dispose();
    }
  }
}

class TelaEditarPerfil extends StatefulWidget {
  const TelaEditarPerfil({super.key});

  @override
  State<TelaEditarPerfil> createState() => _TelaEditarPerfilState();
}

class _TelaEditarPerfilState extends State<TelaEditarPerfil> {
  late final _u = context.read<Sessao>().usuario!;
  late final _nome = TextEditingController(text: _u.nome);
  late final _telefone = TextEditingController(text: _u.telefone ?? '');
  late final _obs = TextEditingController(text: _u.observacoes ?? '');
  late String? _sexo = _u.sexo;
  late int? _tipo = _u.tipoCabeloId;
  late String? _comprimento = _u.comprimentoCabelo;
  late final _tipos = carregarTiposCabelo();
  bool _ocupado = false;

  @override
  void dispose() {
    _nome.dispose();
    _telefone.dispose();
    _obs.dispose();
    super.dispose();
  }

  Future<void> _salvar() async {
    if (_nome.text.trim().length < 2) {
      avisar(context, 'Informe seu nome.', erro: true);
      return;
    }
    setState(() => _ocupado = true);
    try {
      await context.read<Sessao>().atualizarPerfil({
        'nome': _nome.text.trim(),
        'telefone': _telefone.text.trim().isEmpty ? null : _telefone.text.trim(),
        'sexo': _sexo,
        'tipoCabeloId': _tipo,
        'comprimentoCabelo': _comprimento,
        'observacoes': _obs.text.trim().isEmpty ? null : _obs.text.trim(),
      });
      if (!mounted) return;
      avisar(context, 'Perfil atualizado.');
      Navigator.pop(context);
    } on ApiErro catch (e) {
      if (mounted) avisar(context, e.mensagem, erro: true);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Editar perfil')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
        children: [
          TextField(controller: _nome, textCapitalization: TextCapitalization.words, decoration: const InputDecoration(labelText: 'Nome')),
          const SizedBox(height: 14),
          TextField(controller: _telefone, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: 'Celular')),
          const SizedBox(height: 24),
          FutureBuilder(
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
                : const LinearProgressIndicator(),
          ),
          TextField(
            controller: _obs,
            maxLength: 500,
            maxLines: 3,
            decoration: const InputDecoration(labelText: 'Observações para o barbeiro', hintText: 'Ex.: redemoinho na nuca'),
          ),
          const SizedBox(height: 16),
          BotaoAcao(texto: 'Salvar', ocupado: _ocupado, aoTocar: _salvar),
        ],
      ),
    );
  }
}

/// LGPD: acesso e portabilidade dos dados (JSON copiavel).
class TelaMeusDados extends StatefulWidget {
  const TelaMeusDados({super.key});

  @override
  State<TelaMeusDados> createState() => _TelaMeusDadosState();
}

class _TelaMeusDadosState extends State<TelaMeusDados> {
  late Future<String> _json;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  void _carregar() {
    _json = ApiCliente.instancia
        .get('/api/app/meus-dados')
        .then((j) => const JsonEncoder.withIndent('  ').convert(j));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Meus dados')),
      body: FutureBuilder<String>(
        future: _json,
        builder: (context, s) {
          if (s.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
          if (s.hasError) return ErroTela(mensagem: s.error.toString(), aoTentar: () => setState(_carregar));
          return ListView(
            padding: const EdgeInsets.all(20),
            children: [
              const Text('Estes são todos os dados que o MyBarber guarda sobre você, em formato aberto (JSON).'),
              const SizedBox(height: 12),
              BotaoAcao(
                texto: 'Copiar',
                aoTocar: () async {
                  await Clipboard.setData(ClipboardData(text: s.data!));
                  if (context.mounted) avisar(context, 'Dados copiados. Cole em um e-mail ou arquivo para guardar.');
                },
              ),
              const SizedBox(height: 16),
              SelectableText(s.data!, style: const TextStyle(fontFamily: 'monospace', fontSize: 12.5)),
            ],
          );
        },
      ),
    );
  }
}
