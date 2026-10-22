import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../estado/sessao.dart';
import '../tema/tema_app.dart';
import '../widgets/comuns.dart';
import 'cadastro.dart';
import 'documento.dart';

class TelaEntrada extends StatefulWidget {
  const TelaEntrada({super.key});

  @override
  State<TelaEntrada> createState() => _TelaEntradaState();
}

class _TelaEntradaState extends State<TelaEntrada> {
  final _form = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _senha = TextEditingController();
  bool _ocupado = false;
  bool _verSenha = false;

  @override
  void dispose() {
    _email.dispose();
    _senha.dispose();
    super.dispose();
  }

  Future<void> _entrar() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _ocupado = true);
    try {
      await context.read<Sessao>().entrar(_email.text, _senha.text);
    } catch (e) {
      if (mounted) avisar(context, e.toString(), erro: true);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Form(
                key: _form,
                child: AutofillGroup(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const _Marca(),
                      const SizedBox(height: 28),
                      Semantics(
                        header: true,
                        child: Text('Seu corte no horário certo', style: t.textTheme.headlineMedium),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Encontre barbearias perto de você e veja só os horários em que o seu corte cabe inteiro.',
                        style: t.textTheme.bodyLarge?.copyWith(color: t.colorScheme.onSurfaceVariant),
                      ),
                      const SizedBox(height: 28),
                      TextFormField(
                        controller: _email,
                        keyboardType: TextInputType.emailAddress,
                        autofillHints: const [AutofillHints.email],
                        textInputAction: TextInputAction.next,
                        decoration: const InputDecoration(labelText: 'E-mail'),
                        validator: (v) => (v == null || !v.contains('@')) ? 'Informe um e-mail válido' : null,
                      ),
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _senha,
                        obscureText: !_verSenha,
                        autofillHints: const [AutofillHints.password],
                        textInputAction: TextInputAction.done,
                        onFieldSubmitted: (_) => _entrar(),
                        decoration: InputDecoration(
                          labelText: 'Senha',
                          suffixIcon: IconButton(
                            tooltip: _verSenha ? 'Esconder senha' : 'Mostrar senha',
                            icon: Icon(_verSenha ? Icons.visibility_off_outlined : Icons.visibility_outlined),
                            onPressed: () => setState(() => _verSenha = !_verSenha),
                          ),
                        ),
                        validator: (v) => (v == null || v.isEmpty) ? 'Informe a senha' : null,
                      ),
                      const SizedBox(height: 24),
                      BotaoAcao(texto: 'Entrar', ocupado: _ocupado, aoTocar: _entrar),
                      const SizedBox(height: 12),
                      BotaoAcao(
                        texto: 'Criar conta grátis',
                        contorno: true,
                        aoTocar: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const TelaCadastro())),
                      ),
                      const SizedBox(height: 28),
                      Wrap(
                        alignment: WrapAlignment.center,
                        children: [
                          TextButton(
                            onPressed: () => Navigator.of(context).push(MaterialPageRoute(
                                builder: (_) => const TelaDocumento(slug: 'termos-de-uso', titulo: 'Termos de Uso'))),
                            child: const Text('Termos de Uso'),
                          ),
                          TextButton(
                            onPressed: () => Navigator.of(context).push(MaterialPageRoute(
                                builder: (_) => const TelaDocumento(slug: 'politica-de-privacidade', titulo: 'Política de Privacidade'))),
                            child: const Text('Privacidade'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Marca extends StatelessWidget {
  const _Marca();

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'MyBarber',
      excludeSemantics: true,
      child: Row(children: [
        Container(
          width: 44,
          height: 44,
          decoration: BoxDecoration(color: Cores.verdeGarrafa, borderRadius: BorderRadius.circular(10)),
          alignment: Alignment.center,
          child: const Text('M', style: TextStyle(color: Cores.latao, fontSize: 24, fontWeight: FontWeight.w900)),
        ),
        const SizedBox(width: 12),
        Text('MyBarber', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900, letterSpacing: -0.3)),
      ]),
    );
  }
}
