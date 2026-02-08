# Session 001 : Fondation

## Meta

- **Date :** -
- **Objectif :** Créer la structure du projet Android et poser les bases (Gradle, Timber, Room, WifiHelper)
- **Statut :** En attente

## Module actuel

**En attente de démarrage**

## Checklist des modules

### Module 1 : Structure projet + Gradle
- [ ] Créer le projet Android (Empty Compose Activity, package `com.scaminal`)
- [ ] Configurer `build.gradle.kts` (app) avec les dépendances :
  - Jetpack Compose (BOM)
  - Room (runtime, compiler, ktx)
  - Timber
  - Kotlin Coroutines (core, android)
  - Hilt ou Koin (à décider)
- [ ] Configurer les `buildConfigField` (ENABLE_LOGGING, DEFAULT_SCAN_TIMEOUT)
- [ ] Créer l'arborescence des packages (`ui/`, `network/`, `ssh/`, `data/`, `security/`, `di/`)
- [ ] Vérifier que le projet compile et se lance sur émulateur
- [ ] **Validation utilisateur**

### Module 2 : Logging (Timber)
- [ ] Créer la classe `ScaminalApp : Application()`
- [ ] Initialiser Timber dans `onCreate()` (DebugTree en debug)
- [ ] Déclarer dans `AndroidManifest.xml`
- [ ] Tester avec un `Timber.d("App started")` au lancement
- [ ] **Validation utilisateur**

### Module 3 : Room Database + entités
- [ ] Créer `HostEntity` (id, ip, hostname, port, label, createdAt)
- [ ] Créer `HostDao` (insert, getAll, getById, delete, update)
- [ ] Créer `AppDatabase` (Room database, version 1)
- [ ] Créer `HostRepository`
- [ ] Tester avec un insert/query en debug
- [ ] **Validation utilisateur**

### Module 4 : WifiHelper
- [ ] Créer `WifiHelper` (détection Wi-Fi, IP locale, masque sous-réseau)
- [ ] Utiliser `ConnectivityManager` et `WifiManager`
- [ ] Retourner le préfixe du sous-réseau (ex: "192.168.1")
- [ ] Gérer le cas "pas de Wi-Fi" proprement
- [ ] Tester sur émulateur / appareil
- [ ] **Validation utilisateur**

### Module 5 : Revue sécurité Phase 1
- [ ] Aucun credential hardcodé
- [ ] Permissions minimales dans le Manifest
- [ ] Timber désactivé en release
- [ ] local.properties et keystore.properties dans .gitignore
- [ ] **Validation utilisateur**

## Modules complétés

| Module | Validé | Date |
|--------|--------|------|
| - | - | - |

## Prochains modules (Phase 2)

1. NetworkScanner (scan IP parallèle)
2. PortScanner (scan ports avec timeout)
3. ScannerScreen + ScannerViewModel
4. FavoritesScreen + persistance Room

## Décisions techniques

- **DI :** À décider (Hilt vs Koin)
- **Min SDK :** 26 (Android 8.0, couvre ~95% des appareils)

## Issues & Solutions

_Aucune pour l'instant_

## Fichiers à créer

```
app/src/main/java/com/scaminal/
├── ScaminalApp.kt
├── data/
│   ├── AppDatabase.kt
│   ├── dao/HostDao.kt
│   ├── entity/HostEntity.kt
│   └── repository/HostRepository.kt
├── network/
│   └── WifiHelper.kt
└── di/
    └── AppModule.kt
```

## Handoff

_À remplir en fin de session_
