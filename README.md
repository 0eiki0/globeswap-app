# GlobeSwap – Android-App

Fotos tauschen mit Fremden auf der ganzen Welt: Du machst ein Foto, es geht an eine fremde Person, die etwa
zur gleichen Zeit irgendwo ein Foto gemacht hat, und du bekommst deren Foto zurück. Auf jedes Foto kannst du
mit einem Foto antworten. Keine Registrierung, keine Werbung, kein Google/Firebase.

Das Backend liegt in einem eigenen Repository: **globeswap-server**.

```
app/       Android-App (Kotlin, Jetpack Compose, CameraX, OkHttp, WorkManager)
store/     Play-Store-Material: Icon, Feature-Grafik, Screenshots, Texte, Anleitungen
fastlane/  Store-Daten für F-Droid/IzzyOnDroid (Titel, Beschreibungen, Icon, Screenshots, Änderungen)
```

## Funktionen

* Foto aufnehmen (Front-/Rückkamera, Blitz) und mit einer fremden Person tauschen
* Vor dem Senden bearbeiten wie bei Snapchat: Text, Zeichnen, Emoji-Sticker, Filter;
  Sticker per Anfasser oder mit zwei Fingern drehen und skalieren
* Unterhaltungen nur aus Fotos, immer im Wechsel (erst antworten, wenn das Gegenüber ein Foto geschickt hat) – es bleiben nur die neuesten 10 Fotos (einstellbar am Server), live per WebSocket, Liste mit Badge für neue Fotos
* Ganze Chats (Chat → ⋮ → Delete chat) und eigene Fotos (lange drücken oder im Vollbild 🗑) löschen – auch bei der anderen Person
* Herkunftsland unter jedem Foto (Server ermittelt es aus der IP-Adresse, abschaltbar unter Chats → ⋮ → Settings)
  und „Your world“: Weltkarte der Länder, aus denen man Fotos bekam (Natural Earth, `assets/world.txt`)
* Tausch-Wunsch egal / nah / fern (Kontinent), Tagesthema über der Kamera
* Reaktionen auf Fotos (doppelt tippen = ❤️, lange drücken = Auswahl), Chat beenden ohne Blockieren
* Altersabfrage (Tag, Monat, Jahr, ab 16) mit Pflicht-Häkchen für die Datenschutzerklärung vor dem ersten Start;
  gespeichert wird nur das Ergebnis
* Melden, Blockieren, alle eigenen Daten löschen
* Fotos lassen sich nicht speichern; Screenshots und Bildschirmaufnahmen sind in der ganzen App gesperrt (`FLAG_SECURE`)
* Updates: Der Server kann im Admin-Bereich eine Mindestversion festlegen – ältere Versionen zeigen dann
  „Update required“ und sind gesperrt. Bei Installation über F-Droid/IzzyOnDroid oder GitHub zeigt die App
  einen Hinweis, sobald ein neues GitHub-Release erschienen ist (der Server fragt GitHub, nicht die App;
  bei Play-Store-Installationen übernimmt Google die Updates)
* Benachrichtigungen: sofort per Push (Firebase Cloud Messaging), wenn App und Server dafür eingerichtet sind;
  sonst und auf Geräten ohne Google-Dienste Abfrage etwa alle 15 Minuten
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

## Release auf GitHub (Download ohne Play Store)

Ein Release enthält die signierte APK (`GlobeSwap-1.4.apk`), Prüfsumme und eine Installationsanleitung.
Die sichtbare Version kommt dabei aus der Eingabe bzw. dem Tag. Zwei Wege:

* **Im Browser:** **Actions → Build → Run workflow** → bei „Version für ein GitHub-Release“ z. B. `1.4`
  eintragen → **Run workflow**. Der Build legt Tag `v1.4` und das Release selbst an.
* **Per git:** `git tag v1.4 && git push origin v1.4`

Damit andere die APK herunterladen können, muss das Repository öffentlich sein.

## Versionsnummer

`versionCode` zählt GitHub Actions automatisch hoch (100 + Nummer des Builds), damit jedes App-Bundle bei
Google hochgeladen werden kann. Die für Nutzer sichtbare Version (`versionName` in `app/build.gradle.kts`)
änderst du bei Bedarf von Hand; bei einem Release-Tag (`v1.4`) kommt sie automatisch aus dem Tag. In der App steht beides unter **About** („Version 1.3 (build 107)“).
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
| `GOOGLE_SERVICES_JSON` | optional, für Push: der komplette Inhalt der `google-services.json` aus der Firebase-Konsole |

## Push-Mitteilungen

Die App nutzt Firebase Cloud Messaging, ohne das Google-Services-Gradle-Plugin: `app/build.gradle.kts` liest
die Werte aus `google-services.json` (lokal unter `app/google-services.json`, in GitHub Actions aus dem Secret
`GOOGLE_SERVICES_JSON`). Ohne die Datei baut die App ohne Push.

Die App meldet sich nur bei Google an, wenn der Server Push verschickt (`/api/config` → `push`, auf dem Server
`FCM_CREDENTIALS_FILE`), und erst nachdem sie beim Server angemeldet ist. Ein Push enthält nur „neues Foto in
Unterhaltung X“; die App lädt den Rest von deinem Server. Die Abfrage alle 15 Minuten bleibt als Reserve.

## F-Droid / IzzyOnDroid

Die App wird über [IzzyOnDroid](https://izzyondroid.org) verteilt: Das F-Droid-kompatible Repository übernimmt die
signierte APK aus den GitHub-Releases. Titel, Beschreibungen und Bilder kommen aus `fastlane/metadata/android/`
(Englisch und Deutsch), die Änderungshinweise aus `fastlane/metadata/android/*/changelogs/default.txt` –
vor einem neuen Release dort kurz eintragen, was neu ist. Antrag: [`store/IZZYONDROID-ANTRAG.md`](store/IZZYONDROID-ANTRAG.md).

Für F-Droid/IzzyOnDroid relevant:
* keine proprietären Bibliotheken, keine Google-Dienste, keine Tracker oder Werbung
* Release-Builds mit R8 verkleinert (ca. 4 MB statt 45 MB); kein verschlüsselter Abhängigkeits-Block in der APK
* Anti-Feature **NonFreeNet**: Die App nutzt den GlobeSwap-Server, dessen Code nicht öffentlich ist.

## Lizenz

Copyright (C) 2026 GlobeSwap

Dieses Programm ist freie Software: Du kannst es unter den Bedingungen der **GNU General Public License,
Version 3** weitergeben und/oder verändern, wie von der Free Software Foundation veröffentlicht.
Es wird OHNE JEDE GEWÄHRLEISTUNG bereitgestellt. Den vollständigen Text findest du in [`LICENSE`](LICENSE).

SPDX-License-Identifier: GPL-3.0-only
