# GlobeSwap im Google Play Store veröffentlichen

Alles, was du zum Hochladen brauchst, liegt in diesem Ordner:

| Datei | Wofür |
|---|---|
| App-Bundle (`.aab`) | baut GitHub bei jedem Push: **Actions** → oberster Lauf → **Artifacts** → `globeswap-release-…` |
| `icon-512.png` | App-Symbol (512 × 512) |
| `feature-graphic.png` | Feature-Grafik (1024 × 500) |
| `screenshots/*.png` | 4 Smartphone-Screenshots (1080 × 1920) |
| `listing-en.txt` | Name, Kurz- und Langbeschreibung (Englisch, optional Deutsch) |

## 0. Vorher erledigen

1. **Server aktualisieren** und in `photoswap.env` die Impressumsangaben eintragen
   (`LEGAL_NAME`, `LEGAL_STREET`, `LEGAL_CITY`, `LEGAL_EMAIL`, empfohlen `LEGAL_PHONE`), Server neu starten.
2. Im Browser prüfen, dass diese Seiten deine Angaben zeigen:
   - https://globeswap.de/privacy
   - https://globeswap.de/terms
   - https://globeswap.de/imprint
   - https://globeswap.de/delete-account
3. Paketname in der Play Console: `tech.eiki.globeswap`.

## 1. Entwicklerkonto

- https://play.google.com/console → Konto erstellen, 25 US-Dollar einmalig, Identitätsprüfung (Ausweis, Adresse, Telefon).
- Bei einem **privaten** Konto gilt Schritt 5 (geschlossener Test mit 12 Personen über 14 Tage).

## 2. App anlegen

Play Console → **App erstellen**:
- App-Name: `GlobeSwap` · Standardsprache: **Englisch (USA) – en-US**
- App oder Spiel: **App** · Kostenlos oder kostenpflichtig: **Kostenlos**
- Erklärungen zu Richtlinien und US-Exportgesetzen bestätigen.

## 3. App-Inhalte („Richtlinie“ → „App-Inhalte“)

**Datenschutzerklärung:** `https://globeswap.de/privacy`

**App-Zugriff:** „Alle Funktionen sind ohne besondere Zugriffsrechte verfügbar“.
Hinweis für die Prüfer (Feld „Anleitung“, auf Englisch):
> No login is required. A swap needs two people: take a photo on a second device (or ask a second person)
> and both photos are exchanged. If nobody else is taking a photo, your photo waits until someone does.

**Werbung:** Nein, die App enthält keine Werbung.

**Einstufung des Inhalts (Fragebogen):**
- E-Mail: deine Kontaktadresse · Kategorie: **Soziale Netzwerke, Foren, Kommunikation** (o. ä.)
- Gewalt, Sexualität, Sprache, Drogen, Glücksspiel: **Nein**
- Können Nutzer miteinander interagieren oder Inhalte austauschen? **Ja**
- Teilt die App nutzergenerierte Inhalte (Bilder)? **Ja**
- Wird der Standort mit anderen Nutzern geteilt? **Nein**
- Digitale Käufe: **Nein**
- Ergebnis meist „ab 12/USK 12“ mit dem Hinweis „Nutzer interagieren“.

**Zielgruppe und Inhalte:**
- Altersgruppen: **16–17** und **18 und älter** (keine Altersgruppe unter 13 auswählen)
- Spricht die App Kinder an? **Nein**

**Datensicherheit:**
- Erhebt oder teilt die App Nutzerdaten? **Ja**
- Werden alle Daten bei der Übertragung verschlüsselt? **Ja** (HTTPS)
- Können Nutzer das Löschen ihrer Daten beantragen? **Ja**
- Kontoerstellung: Die App erstellt ein anonymes Konto →
  Link zum Löschen: `https://globeswap.de/delete-account`

| Datentyp | Erhoben | Geteilt* | Optional? | Zweck |
|---|---|---|---|---|
| Fotos und Videos → **Fotos** | Ja | Nein | Erforderlich | App-Funktionalität |
| Geräte- oder andere IDs (anonyme Kennung/UUID) | Ja | Nein | Erforderlich | App-Funktionalität, Kontoverwaltung, Betrugsprävention/Sicherheit |

\* Fotos gehen nur an die Person, mit der man tauscht – das ist eine vom Nutzer ausgelöste Übertragung
und zählt bei Google nicht als „Teilen mit Dritten“.
Alles andere (Name, E-Mail, Standort, Kontakte, Finanzdaten, Gesundheit, Absturzberichte, Werbe-ID) → **nicht erhoben**.

**Weitere Erklärungen:** Nachrichten-App **Nein** · Gesundheits-App **Nein** · Finanzfunktionen **Keine** ·
Behörden-App **Nein**.

## 4. Store-Eintrag („Wachsen“ → „Store-Präsenz“ → „Haupteintrag im Store“)

- Texte aus `listing-en.txt` kopieren. Für Deutsch: „Übersetzungen verwalten“ → Deutsch hinzufügen.
- App-Symbol: `icon-512.png` · Feature-Grafik: `feature-graphic.png`
- Smartphone-Screenshots: die 4 Dateien aus `screenshots/`
- Kategorie: **Fotografie** · Kontakt-E-Mail: deine Adresse · Website: `https://globeswap.de`

## 5. Geschlossener Test (Pflicht bei neuem privatem Konto)

1. „Testen“ → **Geschlossener Test** → Track erstellen → Tester per E-Mail-Liste oder Google-Gruppe hinzufügen.
2. **Neuen Release erstellen** → das `.aab` aus dem neuesten GitHub-Build (`globeswap-release-…`) hochladen.
   Vorher prüfen: Im Build-Ergebnis steht „Signatur-Zertifikat (SHA-256)“ – das muss dein eigener Schlüssel sein.
   Beim ersten Upload fragt Google nach **Play App Signing** → akzeptieren. Dein Schlüssel
   (`photoswap-release.jks`) wird damit zum *Upload-Schlüssel* – weiterhin sicher aufbewahren,
   jedes Update muss damit signiert sein.
3. Release-Hinweis z. B. „First release“. Zur Überprüfung senden.
4. Mindestens **12 Tester** müssen den Test-Link annehmen und die App installieren, und das
   **14 Tage am Stück**. Bitte sie, wirklich Fotos zu tauschen.

## 6. Produktion

Nach den 14 Tagen erscheint in der Play Console „Zugriff auf Produktion beantragen“. Ein paar Fragen
zum Test beantworten, dann unter **Produktion** einen Release mit demselben `.aab` erstellen und
einreichen. Die Prüfung dauert meist einige Tage.

Nach der Freischaltung: den Play-Store-Link in `photoswap.env` bei `PLAY_STORE_URL` eintragen und den
Server neu starten – dann erscheint auf https://globeswap.de ein „Get it on Google Play“-Knopf.

## Updates später

Code ändern und auf `main` pushen – GitHub baut automatisch und zählt `versionCode` selbst hoch.
Unter **Actions** → oberster Lauf → **Artifacts** → `globeswap-release-…` das `.aab` herunterladen und in der
Play Console hochladen. Die sichtbare Version (`versionName`) bei Bedarf in `app/build.gradle.kts` ändern.
Neue Apps und Updates müssen außerdem die
jeweils aktuelle Android-Version ansprechen (Stand 2026: Android 16 / API 36; Google hebt das jährlich an).

## Wenn Google nachfragt

Bei Apps mit Inhalten von Nutzern achtet Google besonders auf Moderation. GlobeSwap hat:
Regeln vor der ersten Nutzung (16+), Melden und Blockieren in der App, automatische Sperre nach
Meldungen mehrerer Personen und die Admin-Seite zum Prüfen. Wichtig ist, dass du Meldungen
regelmäßig bearbeitest.
