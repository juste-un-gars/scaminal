# Session 001 : Fondation

## Meta

- **Date :** 2026-02-08
- **Objectif :** Créer la structure du projet Android et poser les bases
- **Statut :** Terminée

## Modules complétés

| Module | Validé | Date |
|--------|--------|------|
| 1. Structure projet + Gradle | Oui | 2026-02-08 |
| 2. ScaminalApplication + MainActivity | Oui | 2026-02-08 |
| 3. Room Database + HostEntity + HostDao | Oui | 2026-02-08 |
| 4. WifiHelper | Oui | 2026-02-08 |

## Décisions techniques

- **DI :** Hilt
- **Gradle :** 8.14.1, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01
- **Flux scan :** Scan IP (bouton) → Scan Ports tous hôtes (bouton configurable : courants/tous/personnalisé) → Appui long = scan ports 1 hôte

## Fichiers créés

```
settings.gradle.kts
build.gradle.kts
gradle.properties
local.properties
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties + jar
gradlew / gradlew.bat
app/build.gradle.kts
app/proguard-rules.pro
app/.gitignore
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/main/res/drawable/ic_launcher_background.xml
app/src/main/res/drawable/ic_launcher_foreground.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
app/src/main/java/com/scaminal/ScaminalApplication.kt
app/src/main/java/com/scaminal/MainActivity.kt
app/src/main/java/com/scaminal/ui/theme/Theme.kt
app/src/main/java/com/scaminal/data/entity/HostEntity.kt
app/src/main/java/com/scaminal/data/dao/HostDao.kt
app/src/main/java/com/scaminal/data/AppDatabase.kt
app/src/main/java/com/scaminal/di/DatabaseModule.kt
app/src/main/java/com/scaminal/network/WifiHelper.kt
```

## Issues & Solutions

| Problème | Solution |
|----------|----------|
| `buildConfigField("Int", ...)` → erreur Java | Utiliser `"int"` (minuscule) |
| `dependencyResolution {}` dans settings.gradle.kts | C'est `dependencyResolutionManagement {}` |
| `resource mipmap/ic_launcher not found` | Créer adaptive icon en `mipmap-anydpi-v26` + drawables vectoriels |
| Hilt deprecated API warning | Normal (Hilt_ScaminalApplication.java), ne bloque pas |

## Handoff

Phase 1 complète. Build OK. App se lance sur appareil physique.
Phase 2 : NetworkScanner, PortScanner, ScannerScreen + ScannerViewModel, FavoritesScreen.
