# Antrag auf Aufnahme bei IzzyOnDroid

1. Konto auf https://codeberg.org anlegen (kostenlos).
2. https://codeberg.org/IzzyOnDroid/repo/issues → **New issue** → falls angeboten, die Vorlage
   „App inclusion request“ wählen und die Felder mit den Angaben unten füllen, sonst den Text unten einfügen.
3. Voraussetzung: Das Repository `0eiki0/globeswap-app` ist **öffentlich** und es gibt ein GitHub-Release
   mit verkleinerter APK (ab Version 1.4).

---

**Titel:** `Inclusion request: GlobeSwap`

**Text:**

```
App name: GlobeSwap
Package name: tech.eiki.globeswap
Source code: https://github.com/0eiki0/globeswap-app
License: GPL-3.0-only
APK source: GitHub releases (https://github.com/0eiki0/globeswap-app/releases), asset GlobeSwap-<version>.apk
Metadata: fastlane/metadata/android (en-US, de-DE)

Description:
Swap photos with strangers around the world. Take a photo, it goes to someone who just took
a photo somewhere else, and you get theirs. Reply with photos, edit them with text, drawings,
stickers and filters. No sign-up, no ads, no trackers, no Google services.

Anti-features:
- NonFreeNet: the app talks to the GlobeSwap server (https://swap.eiki.tech), whose source code
  is not public.

Notes:
- No proprietary libraries (AndroidX, Jetpack Compose, CameraX, OkHttp, Coil, kotlinx – all Apache-2.0).
- No dependency info block in the APK (dependenciesInfo.includeInApk = false).
- Release APKs are signed by me; signing certificate SHA-256:
  64ed99d65f4712188859fa118cd1510918469c8582b8ad1e2d35af37d8c22eb8
- Target audience: 16+ (user-generated photos from strangers; reporting, blocking and moderation built in).
```
