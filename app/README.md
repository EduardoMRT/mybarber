# MyBarber — app do cliente (Flutter)

App gratuito para o cliente encontrar barbearias próximas, agendar e responder remarcações.

## Rodar

```bash
cd app
flutter pub get
# API local no computador (emulador Android enxerga o PC em 10.0.2.2):
flutter run --dart-define=API_URL=http://10.0.2.2:8080
# API publicada:
flutter run --dart-define=API_URL=https://api.seudominio.com.br
```

As pastas `android/` e `ios/` são geradas pelo workflow **Plataformas do app**
(`flutter create` + `tool/configurar_plataformas.py`, que adiciona permissões de
notificação e localização, receptores dos lembretes e o desugaring do Android).

## Build de produção

```bash
flutter build apk --release --dart-define=API_URL=https://api.seudominio.com.br
flutter build appbundle --release --dart-define=API_URL=https://api.seudominio.com.br   # Play Store
flutter build ipa --release --dart-define=API_URL=https://api.seudominio.com.br         # App Store (macOS)
```
