#!/usr/bin/env python3
"""Ajusta as pastas android/ e ios/ geradas pelo `flutter create` para o MyBarber.

Uso (dentro de app/):
    flutter create --platforms=android,ios --org br.com.mybarber --project-name mybarber .
    python3 tool/configurar_plataformas.py

Idempotente: pode rodar mais de uma vez.
"""
import pathlib
import re
import sys

raiz = pathlib.Path(__file__).resolve().parent.parent
avisos = []


def ler(p):
    return p.read_text(encoding='utf-8')


def gravar(p, s):
    p.write_text(s, encoding='utf-8')


# ---------------------------------------------------------------- Android: manifesto principal
manifesto = raiz / 'android/app/src/main/AndroidManifest.xml'
if manifesto.exists():
    s = ler(manifesto)
    permissoes = [
        'android.permission.INTERNET',
        'android.permission.POST_NOTIFICATIONS',
        'android.permission.RECEIVE_BOOT_COMPLETED',
        'android.permission.VIBRATE',
        'android.permission.ACCESS_COARSE_LOCATION',
        'android.permission.ACCESS_FINE_LOCATION',
    ]
    faltam = [p for p in permissoes if p not in s]
    if faltam:
        bloco = ''.join(f'    <uses-permission android:name="{p}"/>\n' for p in faltam)
        s = s.replace('    <application', bloco + '    <application', 1)
    s = re.sub(r'android:label="[^"]*"', 'android:label="MyBarber"', s, count=1)
    if 'ScheduledNotificationReceiver' not in s:
        receptores = '''        <receiver android:exported="false" android:name="com.dexterous.flutterlocalnotifications.ScheduledNotificationReceiver" />
        <receiver android:exported="false" android:name="com.dexterous.flutterlocalnotifications.ScheduledNotificationBootReceiver">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED"/>
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
                <action android:name="com.htc.intent.action.QUICKBOOT_POWERON"/>
            </intent-filter>
        </receiver>
'''
        s = s.replace('    </application>', receptores + '    </application>', 1)
    if '<data android:scheme="https"' not in s:
        consulta = '''        <intent>
            <action android:name="android.intent.action.VIEW" />
            <data android:scheme="https" />
        </intent>
'''
        if '<queries>' in s:
            s = s.replace('<queries>\n', '<queries>\n' + consulta, 1)
        else:
            s = s.replace('</manifest>', '    <queries>\n' + consulta + '    </queries>\n</manifest>', 1)
    gravar(manifesto, s)
else:
    avisos.append('android/app/src/main/AndroidManifest.xml nao encontrado')

# ---------------------------------------------------------------- Android: http liberado so em debug (API local)
debug = raiz / 'android/app/src/debug/AndroidManifest.xml'
if debug.exists():
    s = ler(debug)
    if 'usesCleartextTraffic' not in s:
        s = s.replace('</manifest>', '    <application android:usesCleartextTraffic="true" />\n</manifest>', 1)
        gravar(debug, s)

# ---------------------------------------------------------------- Android: desugaring (flutter_local_notifications)
kts = raiz / 'android/app/build.gradle.kts'
groovy = raiz / 'android/app/build.gradle'
if kts.exists():
    s = ler(kts)
    if 'isCoreLibraryDesugaringEnabled' not in s:
        s = re.sub(r'compileOptions\s*\{', 'compileOptions {\n        isCoreLibraryDesugaringEnabled = true', s, count=1)
    if 'desugar_jdk_libs' not in s:
        s += '\ndependencies {\n    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")\n}\n'
    gravar(kts, s)
elif groovy.exists():
    s = ler(groovy)
    if 'coreLibraryDesugaringEnabled' not in s:
        s = re.sub(r'compileOptions\s*\{', 'compileOptions {\n        coreLibraryDesugaringEnabled true', s, count=1)
    if 'desugar_jdk_libs' not in s:
        s += "\ndependencies {\n    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.4'\n}\n"
    gravar(groovy, s)
else:
    avisos.append('build.gradle do app Android nao encontrado')

# ---------------------------------------------------------------- iOS
plist = raiz / 'ios/Runner/Info.plist'
if plist.exists():
    s = ler(plist)
    extras = {
        'NSLocationWhenInUseUsageDescription': 'O MyBarber usa sua localização para mostrar as barbearias mais próximas. Ela não fica guardada.',
    }
    for chave, valor in extras.items():
        if chave not in s:
            s = s.replace('<dict>', f'<dict>\n\t<key>{chave}</key>\n\t<string>{valor}</string>', 1)
    s = re.sub(r'(<key>CFBundleDisplayName</key>\s*<string>)[^<]*(</string>)', r'\1MyBarber\2', s)
    gravar(plist, s)
else:
    avisos.append('ios/Runner/Info.plist nao encontrado')

delegate = raiz / 'ios/Runner/AppDelegate.swift'
if delegate.exists():
    s = ler(delegate)
    if 'UNUserNotificationCenter' not in s:
        trecho = 'if #available(iOS 10.0, *) {\n      UNUserNotificationCenter.current().delegate = self as? UNUserNotificationCenterDelegate\n    }\n    '
        # template novo (motor implicito) e antigo: insere antes do retorno do didFinishLaunching
        alvo = 'return super.application(application, didFinishLaunchingWithOptions: launchOptions)'
        if alvo in s:
            s = s.replace(alvo, trecho + alvo, 1)
            gravar(delegate, s)
        else:
            avisos.append('AppDelegate.swift com formato diferente: configure o delegate de notificacoes manualmente')

for a in avisos:
    print('AVISO:', a)
print('Plataformas configuradas.')
sys.exit(0)
