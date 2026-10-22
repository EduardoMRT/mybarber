import 'package:flutter/material.dart';

/// Identidade visual do MyBarber: verde de azulejo, latao e porcelana.
class Cores {
  static const verdeGarrafa = Color(0xFF1D3A33);
  static const verdeAzulejo = Color(0xFF2F6B5C);
  static const latao = Color(0xFFB8862F);
  static const porcelana = Color(0xFFF3F6F4);

  static const confirmado = Color(0xFF2F6B5C);
  static const aguardando = Color(0xFF9A6412);
  static const concluido = Color(0xFF3F5375);
  static const cancelado = Color(0xFFA03D34);
  static const falta = Color(0xFF6F4A85);
}

ThemeData temaClaro() {
  const esquema = ColorScheme(
    brightness: Brightness.light,
    primary: Cores.verdeAzulejo,
    onPrimary: Colors.white,
    primaryContainer: Color(0xFFD4E8E0),
    onPrimaryContainer: Color(0xFF0E2A22),
    secondary: Cores.latao,
    onSecondary: Color(0xFF1B1408),
    secondaryContainer: Color(0xFFF3E6C9),
    onSecondaryContainer: Color(0xFF3A2A0A),
    tertiary: Cores.verdeGarrafa,
    onTertiary: Colors.white,
    error: Cores.cancelado,
    onError: Colors.white,
    surface: Colors.white,
    onSurface: Color(0xFF17211E),
    onSurfaceVariant: Color(0xFF55625E),
    surfaceContainerLowest: Colors.white,
    surfaceContainerLow: Color(0xFFF7F9F8),
    surfaceContainer: Color(0xFFF0F4F2),
    surfaceContainerHigh: Color(0xFFEAF0ED),
    surfaceContainerHighest: Color(0xFFE2E9E5),
    outline: Color(0xFFB9C5C0),
    outlineVariant: Color(0xFFD9E1DD),
  );
  return _base(esquema, Cores.porcelana);
}

ThemeData temaEscuro() {
  const esquema = ColorScheme(
    brightness: Brightness.dark,
    primary: Color(0xFF7CC1AC),
    onPrimary: Color(0xFF0D1A16),
    primaryContainer: Color(0xFF1C3530),
    onPrimaryContainer: Color(0xFFD4E8E0),
    secondary: Color(0xFFD9AD5E),
    onSecondary: Color(0xFF1B1408),
    secondaryContainer: Color(0xFF3A3120),
    onSecondaryContainer: Color(0xFFF3E6C9),
    tertiary: Color(0xFFA3C9BC),
    onTertiary: Color(0xFF0D1A16),
    error: Color(0xFFEC9A90),
    onError: Color(0xFF3D1210),
    surface: Color(0xFF16211E),
    onSurface: Color(0xFFE4ECE8),
    onSurfaceVariant: Color(0xFFA3B4AE),
    surfaceContainerLowest: Color(0xFF0B1311),
    surfaceContainerLow: Color(0xFF131C1A),
    surfaceContainer: Color(0xFF1A2522),
    surfaceContainerHigh: Color(0xFF1D2A26),
    surfaceContainerHighest: Color(0xFF26332F),
    outline: Color(0xFF4A5B56),
    outlineVariant: Color(0xFF26332F),
  );
  return _base(esquema, const Color(0xFF0F1715));
}

ThemeData _base(ColorScheme e, Color fundo) {
  final texto = Typography.material2021(platform: TargetPlatform.android).black.apply(
        bodyColor: e.onSurface,
        displayColor: e.onSurface,
      );
  return ThemeData(
    useMaterial3: true,
    colorScheme: e,
    scaffoldBackgroundColor: fundo,
    textTheme: texto.copyWith(
      headlineMedium: texto.headlineMedium?.copyWith(fontWeight: FontWeight.w800, letterSpacing: -0.5),
      headlineSmall: texto.headlineSmall?.copyWith(fontWeight: FontWeight.w800, letterSpacing: -0.3),
      titleLarge: texto.titleLarge?.copyWith(fontWeight: FontWeight.w700),
      titleMedium: texto.titleMedium?.copyWith(fontWeight: FontWeight.w600),
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: fundo,
      foregroundColor: e.onSurface,
      elevation: 0,
      scrolledUnderElevation: 1,
      centerTitle: false,
      titleTextStyle: TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: e.onSurface),
    ),
    cardTheme: CardThemeData(
      color: e.surface,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: e.outlineVariant),
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size(64, 52),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(64, 52),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(minimumSize: const Size(48, 48)),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: e.surface,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: BorderSide(color: e.outline),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(10),
        borderSide: BorderSide(color: e.secondary, width: 2),
      ),
    ),
    chipTheme: ChipThemeData(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      side: BorderSide(color: e.outline),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: e.surface,
      indicatorColor: e.primaryContainer,
      labelTextStyle: WidgetStatePropertyAll(TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: e.onSurface)),
    ),
    dividerTheme: DividerThemeData(color: e.outlineVariant, space: 1),
    snackBarTheme: const SnackBarThemeData(behavior: SnackBarBehavior.floating),
  );
}
