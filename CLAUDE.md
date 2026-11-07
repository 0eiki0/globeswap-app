# Hinweise für Claude – GlobeSwap App

Android-App (Kotlin, Jetpack Compose, CameraX, OkHttp, WorkManager). Backend im privaten Repo `0eiki0/globeswap-server`.
Überblick, Bauen, Release, Store: `README.md`.

## Zusammenarbeit
- Mit dem Inhaber **auf Deutsch** schreiben. Die App, Store-Texte (en) und der Changelog sind **auf Englisch**.
- Code-Kommentare auf Deutsch, im Stil des umgebenden Codes.
- Nach Änderungen: bauen, Tests laufen lassen, committen und auf `main` pushen, dann prüfen, ob GitHub Actions grün ist.
- Keine Release-Läufe starten, außer der Inhaber verlangt es.

## Pflichtregeln
- **Changelog:** Jede für Nutzer sichtbare Änderung bekommt eine kurze englische Zeile `- …` unter `## Next` in
  `app/src/main/assets/changelog.md` (im selben Commit). Nicht für Tests, Interna, Store-Material, Videos, Server.
  Der Release-Lauf benennt `## Next` automatisch um – Versionsüberschriften nie von Hand anlegen.
- **Das Wort S-N-A-P (klein, groß, auch als Wortteil) darf nirgends vorkommen** (Code, Texte, Store-Material, Kommentare) –
  der Inhaber hält es für rechtlich geschützt. Stattdessen: photo, swap, shoot, take a photo.
- **Signierschlüssel, Passwörter, `google-services.json`, Keystores nie ins Repo** (nur als GitHub-Secrets).
  Der alte Signierschlüssel wird nicht mehr verwendet.
- Der Server-Code bleibt privat; nichts daraus in dieses öffentliche Repo (GPL-3.0) kopieren.
- Keine echten Nutzerfotos in Screenshots, Videos oder Tests – nur gezeichnete Beispiele.
- **Mehrsprachig:** Keine Texte für Nutzer direkt im Code. Jeder neue oder geänderte Text kommt in `res/values/strings.xml`
  (Englisch, Standard) **und** in alle Übersetzungen mit gleichem Key: `values-de` (Deutsch, „du“), `values-es`, `values-pt`
  (brasilianisches Portugiesisch), `values-fr`, `values-it`, `values-tr`, `values-in` (Indonesisch), `values-pl` – jeweils
  in der vertrauten Anrede. Fehlt ein Key, bricht Lint in CI ab. Bei Zahlen die Plural-Regeln der Sprache beachten
  (Polnisch one/few/many/other, Französisch/Portugiesisch „one“ gilt auch für 0).
  In Compose `stringResource`/`pluralStringResource`, sonst `context.getString` bzw. `appString` (`util/Strings.kt`).
  Begriffe: „Stranger“ heißt in allen Sprachen „Stranger“ (der Stranger, des Strangers, mit Strangern); Senden-Knopf heißt in allen Sprachen „Swap!“, der Tab „Swap“ (Name der App).
  Lange Texte mit `GermanScreensTest` bzw. `OtherLanguagesTest` prüfen (Screenshots unter `app/build/outputs/roborazzi/`).
  Changelog und Tagesthemen bleiben Englisch. Sprache wählbar in Einstellungen → Sprache (`util/AppLanguage.kt`,
  neue Sprache: Datei `values-xx` anlegen und eine Zeile in `AppLanguage.LANGUAGES`):
  Texte außerhalb von Compose immer über `AppLanguage.wrap(context)` bzw. `appString`, nicht direkt über den App-Kontext.

## Technik
- Domain/Server: `https://globeswap.de` (`globeswap.serverUrl` in `gradle.properties`); alte Domain swap.eiki.tech nur noch Alias.
- Paket `tech.eiki.globeswap`, minSdk 29, targetSdk 36.
- Flavors: `github` (Selbst-Update, `REQUEST_INSTALL_PACKAGES`) und `play` (ohne Installationsrecht; CI prüft das).
- Push über Firebase ohne google-services-Plugin (Werte aus `GOOGLE_SERVICES_JSON` in Gradle gelesen).
- Tests: `./gradlew testGithubDebugUnitTest` (Robolectric + Roborazzi). Bekannt: TextField in einem AlertDialog
  wird unter Robolectric nie idle – Formulare als eigene Composable herauslösen und direkt testen (siehe `RestoreForm`).
- Erfolge (`ui/world/Achievements.kt`, `assets/geo.txt`): Bei Änderungen auch die Regeln im Server anpassen
  (`achievement_rules.go`, `geo.txt`) – sonst stimmt die Erfolgsübersicht im Admin-Bereich nicht.
- Fastlane-Änderungshinweise (`fastlane/metadata/android/*/changelogs/`) max. 500 Zeichen.
- Store-Grafiken, Screenshots für Google Play, Werbevideos und Texte liegen im Repo `0eiki0/globeswap-marketing`
  (nicht hier). Rohe Screenshots dafür erzeugt hier der Test `StoreScreenshots` (`app/build/outputs/store-raw/`).

## Sandbox (Cloud-Umgebung)
- Maven Central drosselt hier: `~/.gradle/init.d/central-mirror.gradle` mit dem Google-Mirror zuerst anlegen, falls fehlt.
- Nie `pkill -f` auf eine Gradle-Befehlszeile – das beendet die eigene Shell. Kotlin-Daemon-Abstürze sind harmlos.
- Playwright/Chromium sind vorinstalliert (`/opt/node-tools/node_modules/playwright`), ffmpeg ebenso.
