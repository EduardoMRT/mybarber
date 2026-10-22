import 'package:geolocator/geolocator.dart';

class Posicao {
  final double lat;
  final double lng;
  Posicao(this.lat, this.lng);
}

/// Localizacao aproximada, so com permissao do usuario. Nao e enviada
/// para armazenamento: serve apenas para ordenar as barbearias por distancia.
class Localizacao {
  static Future<(Posicao?, String?)> atual() async {
    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        return (null, 'Ative a localização do celular para ver as barbearias mais próximas.');
      }
      var permissao = await Geolocator.checkPermission();
      if (permissao == LocationPermission.denied) {
        permissao = await Geolocator.requestPermission();
      }
      if (permissao == LocationPermission.denied || permissao == LocationPermission.deniedForever) {
        return (null, 'Sem acesso à localização. Busque pela cidade ou pelo bairro.');
      }
      final ultima = await Geolocator.getLastKnownPosition();
      final p = ultima ??
          await Geolocator.getCurrentPosition(
            locationSettings: const LocationSettings(accuracy: LocationAccuracy.medium, timeLimit: Duration(seconds: 12)),
          );
      return (Posicao(p.latitude, p.longitude), null);
    } catch (_) {
      return (null, 'Não foi possível obter sua localização. Busque pela cidade.');
    }
  }
}
