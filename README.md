# MyBarber

Agendamento para barbearias em que o cliente **só vê os horários em que o serviço cabe inteiro** na agenda do barbeiro. A duração do serviço depende do tipo de cabelo de cada cliente.

| Parte | Pasta | Tecnologia | Quem usa |
| --- | --- | --- | --- |
| API | `backend/` | Spring Boot 3.5, JDK 25, MySQL, Flyway, Spring Security + JWT | todos |
| Portal | `admin/` | React 19 + Vite + TypeScript | barbearias (dono e barbeiros) e você (super admin) |
| App | `app/` | Flutter | clientes (gratuito) |

O GitHub Actions (`.github/workflows/ci.yml`) compila e testa as três partes a cada push. Os testes do backend incluem o fluxo completo contra um MySQL real.

## Regras de negócio

- **Encaixe:** um horário só aparece se `início + duração do serviço` couber dentro de uma faixa de expediente do barbeiro, sem invadir outro atendimento, bloqueio ou o intervalo de limpeza entre clientes. Exemplo: livre às 15:30 e cliente marcado às 16:00, então corte em cabelo liso (45 min) não aparece às 15:30 e corte em cabelo crespo (30 min) aparece.
- **Duração por tipo de cabelo:** cada serviço tem uma duração padrão e, se quiser, uma duração diferente para cada tipo de cabelo (Liso, Ondulado, Cacheado, Crespo; a lista é editável pelo super admin).
- **Faixas restritas:** o barbeiro pode limitar uma faixa a alguns serviços (ex.: "das 18h às 19h só barba").
- **Barbeiros:** uma barbearia pode ter vários barbeiros. Cada um tem a própria agenda, os tipos de cabelo que atende e os serviços que faz.
- **Concorrência:** a reserva trava a agenda do barbeiro no banco (`SELECT ... FOR UPDATE`), então dois clientes não conseguem pegar o mesmo horário ao mesmo tempo.
- **Remarcação pelo barbeiro:** o horário antigo é liberado na hora e a reserva fica "aguardando cliente". O cliente escolhe entre aceitar, escolher outro horário ou cancelar. Sem resposta no prazo (padrão de 12 h), a reserva é cancelada automaticamente.
- **Cancelamento pelo cliente:** só até o prazo definido pela barbearia (padrão de 2 h antes).
- **Lançamento manual:** o barbeiro lança cortes de balcão ou telefone. Se marcar "já foi atendido", o valor entra direto no caixa.
- **Financeiro:** concluir um atendimento gera a receita automaticamente. Despesas e outras entradas são lançadas à mão. O dashboard mostra lucro, ticket médio, ocupação da agenda e faltas.
- **Assinatura:** só a barbearia paga, e o app é gratuito para o cliente. A rotina diária (00:10) gera a mensalidade do mês, marca atrasos e **bloqueia** a barbearia depois da tolerância (padrão de 5 dias). Bloqueada, ela some do app e o portal fica só para consulta. O desbloqueio é automático quando você registra o pagamento.
- **Lembretes:** o app agenda no próprio celular um lembrete de 15 min a 2 h antes do horário, escolhido pelo cliente. Esse lembrete funciona mesmo com o app fechado.

## Perfis de acesso

| Papel | Onde | Pode |
| --- | --- | --- |
| `SUPER_ADMIN` | portal, `/admin` | barbearias, mensalidades, bloqueio, tipos de cabelo, redefinir senha do dono |
| `ADMIN_BARBEARIA` | portal | tudo da própria barbearia: agenda, barbeiros, horários, serviços, financeiro, clientes, assinatura |
| `BARBEIRO` | portal | a própria agenda e os próprios horários (sem financeiro) |
| `CLIENTE` | app | perfil, buscar barbearias, agendar, remarcar, cancelar, LGPD |

O super admin é criado na primeira execução da API, a partir de `ADMIN_EMAIL` e `ADMIN_PASSWORD`.

## Rodar localmente

### 1. API

```bash
cd backend
cp .env.example .env        # preencha DB_*, JWT_SECRET, ADMIN_EMAIL, ADMIN_PASSWORD
mvn spring-boot:run
```

- Documentação interativa dos endpoints: http://localhost:8080/docs
- Saúde: http://localhost:8080/actuator/health
- Sem banco à mão, rode `docker compose up mysql` na raiz e use `DB_HOST=localhost`, `DB_USER=mybarber`, `DB_PASSWORD=mybarber`.
- O Flyway cria as tabelas sozinho na primeira subida (`backend/src/main/resources/db/migration`).

### 2. Portal

```bash
cd admin
npm install
npm run dev                 # http://localhost:5173 (proxy de /api para :8080)
```

### 3. App

```bash
cd app
flutter pub get
flutter run --dart-define=API_URL=http://10.0.2.2:8080   # emulador Android -> API no PC
```

## Banco de dados (Locaweb)

A API lê a conexão das variáveis `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD`. **A senha nunca vai para o git**: ela fica no `backend/.env`, que está no `.gitignore`, ou nas variáveis de ambiente do servidor.

- Libere o IP do servidor da API no painel do DBaaS da Locaweb (acesso remoto).
- Se o MySQL contratado for 5.7, o Flyway Community pode recusar a versão. Nesse caso, prefira um banco MySQL 8.
- Troque a senha temporária do banco antes de entrar em produção.

## Publicar

**API.** O jeito mais simples é Docker, com o `Dockerfile` que já está em `backend/`. Serve qualquer VPS, Railway, Render ou Fly.io.

```bash
docker build -t mybarber-api backend
docker run -d -p 8080:8080 --env-file backend/.env --name mybarber-api mybarber-api
```

- Coloque HTTPS na frente da API (Nginx, Caddy ou o próprio provedor).
- Ajuste `CORS_ORIGINS` para o domínio do portal (ex.: `https://portal.mybarber.com.br`).
- Gere um `JWT_SECRET` forte com `openssl rand -base64 48`.

**Portal.** É um site estático. Gere com `VITE_API_URL=https://api.seudominio.com.br npm run build` e publique a pasta `admin/dist` em Netlify, Vercel, Cloudflare Pages ou na própria VPS. Configure o servidor para responder `index.html` em qualquer rota.

**App.** Gere o pacote apontando para a API publicada:

```bash
flutter build appbundle --release --dart-define=API_URL=https://api.seudominio.com.br   # Google Play
flutter build ipa --release --dart-define=API_URL=https://api.seudominio.com.br         # App Store (precisa de macOS)
```

Antes de publicar, troque o ícone (`flutter_launcher_icons`) e configure a assinatura de release do Android (`android/key.properties`).

## Textos legais (LGPD)

Os documentos ficam em `backend/src/main/resources/legal/` e aparecem no app e no portal:

- `termos-de-uso.md`
- `politica-de-privacidade.md`
- `lgpd.md`
- `termos-barbearia.md`

**Antes de publicar, preencha:**

- `[CNPJ]`
- `[e-mail de contato]`
- `[nome do encarregado]` e `[e-mail do encarregado]`

O ideal é que um advogado revise os textos. Ao mudar um texto de forma relevante, suba `mybarber.legal.versao` em `application.yml`: os novos aceites ficam registrados com a versão nova.

O que já está implementado:

- aceite obrigatório no cadastro, registrado com data, versão e IP;
- exportação dos dados em JSON (app: Perfil, Baixar meus dados);
- exclusão de conta com anonimização (os atendimentos ficam sem identificação, para a contabilidade da barbearia);
- localização usada só no aparelho, sem armazenamento.

## Acessibilidade

**Portal:**

- foco visível em todos os controles;
- navegação por teclado, com Esc fechando os diálogos;
- rótulos em todos os campos e botões de ícone;
- status sempre com texto, nunca só por cor;
- cores dos gráficos validadas para daltonismo;
- tema claro e escuro.

**App:**

- rótulos de leitor de tela nos seletores de dia e horário;
- áreas de toque de 48 px ou mais;
- respeita o tamanho de fonte do sistema;
- tema claro, escuro ou automático.

## Limitações conhecidas e próximos passos

- **Avisos com o app fechado:** as notificações são locais, sem servidor de push. Os lembretes de horário tocam mesmo com o app fechado, mas os avisos da barbearia (remarcação, cancelamento) aparecem quando o cliente abre o app. Para avisos instantâneos com o app fechado, o próximo passo é adicionar Firebase Cloud Messaging.
- **Recuperação de senha:** ainda não há recuperação por e-mail. Hoje o super admin redefine a senha do dono da barbearia, e o dono redefine a dos barbeiros.
- **Pagamento online:** não há pagamento no app nem gateway para as mensalidades, que têm controle manual.
- **Programa de pontos e compensação ao cliente:** fase 2 do projeto.
