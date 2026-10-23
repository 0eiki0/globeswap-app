# GlobeSwap – Android-App

Fotos tauschen mit Fremden auf der ganzen Welt: Du machst ein Foto, es geht an eine fremde Person, die etwa
zur gleichen Zeit irgendwo ein Foto gemacht hat, und du bekommst deren Foto zurück. Auf jedes Foto kannst du
mit einem Foto antworten. Keine Registrierung, keine Werbung, kein Google/Firebase.

Das Backend liegt in einem eigenen Repository: **globeswap-server**.

```
app/      Android-App (Kotlin, Jetpack Compose, CameraX, OkHttp, WorkManager)
store/    Play-Store-Material: Icon, Feature-Grafik, Screenshots, Texte, PLAY-STORE-ANLEITUNG.md
```

## Funktionen

* Foto aufnehmen (Front-/Rückkamera, Blitz) und mit einer fremden Person tauschen
* Vor dem Senden bearbeiten wie bei Snapchat: Text, Zeichnen, Emoji-Sticker, Filter;
  Sticker per Anfasser oder mit zwei Fingern drehen und skalieren
* Unterhaltungen nur aus Fotos, live per WebSocket, Liste mit Badge für neue Fotos
* Melden, Blockieren, Foto in der Galerie speichern, alle eigenen Daten löschen
* Benachrichtigungen ohne Google-Dienste (Abfrage etwa alle 15 Minuten)
* Jede Person hat eine eindeutige öffentliche UUID („Stranger“ + UUID)
* Links zu Datenschutz, Nutzungsbedingungen, Impressum und Datenlöschung (Chats → ⋮ → About & privacy)

Technik: minSdk 29 (Android 10), targetSdk/compileSdk 36 (Android 16), Android Gradle Plugin 8.13, Gradle 8.14.

## Bauen

Adresse des Servers in `gradle.properties`:

```properties
globeswap.serverUrl=https://swap.eiki.tech
```

```bash
./gradlew assembleDebug        # Test-APK: app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # Tests ohne Gerät (Robolectric), Screenshots in app/build/outputs/roborazzi/
./gradlew bundleRelease        # App-Bundle für den Play Store
```

Lokal mit eigenem Testserver (`go run .` im Server-Repository) im Emulator:
`globeswap.serverUrl=http://10.0.2.2:8080`. `http://` ist nur im Debug-Build erlaubt.

## Versionsnummer

`versionCode` zählt GitHub Actions automatisch hoch (100 + Nummer des Builds), damit jedes App-Bundle bei
Google hochgeladen werden kann. Die für Nutzer sichtbare Version (`versionName` in `app/build.gradle.kts`)
änderst du bei Bedarf von Hand. In der App steht beides unter **About** („Version 1.3 (build 107)“).
Lokale Builds haben `versionCode` 4.

## Signieren

Release-Builds werden signiert, wenn `signing/keystore.properties` und `signing/photoswap-release.jks`
vorhanden sind. Beide sind **absichtlich nicht im Repository** (`.gitignore`) – sicher aufbewahren,
jedes Update im Play Store muss mit demselben Schlüssel signiert sein.

`signing/keystore.properties`:

```properties
storeFile=../signing/photoswap-release.jks
storePassword=…
keyAlias=photoswap
keyPassword=…
```

## Play Store

Paketname: `tech.eiki.globeswap` (`applicationId` in `app/build.gradle.kts`).
`versionCode` wird beim Build auf GitHub automatisch erhöht (siehe „Versionsnummer“).
Schritt-für-Schritt-Anleitung: [`store/PLAY-STORE-ANLEITUNG.md`](store/PLAY-STORE-ANLEITUNG.md).

## Automatische Builds (GitHub Actions)

Bei jedem Push auf `main` und bei Pull Requests laufen Tests und Lint, und eine Debug-APK wird gebaut.
Ergebnisse: Reiter **Actions** → Lauf auswählen → **Artifacts** (`globeswap-debug-apk`, `test-reports`).

Signierte Release-APK und App-Bundle (`globeswap-release`) entstehen zusätzlich, wenn im Repository unter
**Settings → Secrets and variables → Actions** zwei Secrets hinterlegt sind:

| Secret | Inhalt |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | der Signaturschlüssel als Base64: `base64 -w0 photoswap-release.jks` |
| `SIGNING_PASSWORD` | das Passwort aus `keystore.properties` (Store- und Key-Passwort sind gleich) |
