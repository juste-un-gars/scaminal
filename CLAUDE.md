# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Règles critiques

1. **Développement incrémental** — Max 150 lignes par itération, STOP pour validation
2. **Pas de hardcoding** — Aucun secret, credential, chemin absolu dans le code
3. **Logging dès le départ** — Via Timber, configurable par BuildConfig
4. **Audit sécurité** — Obligatoire avant toute release
5. **Points d'arrêt** — Attendre "OK" / "validé" après chaque module

---

## Contexte projet

**Nom :** Scaminal
**Type :** Application Android native
**Langage :** Kotlin
**UI :** Jetpack Compose
**Architecture :** MVVM (Model-View-ViewModel)
**Concurrence :** Kotlin Coroutines + Flow
**SSH :** JSch ou Apache MINA sshd
**Stockage :** Room Database
**DI :** Hilt ou Koin
**Min SDK :** 26 | **Target SDK :** 34

---

## Build et commandes

```bash
./gradlew assembleDebug          # Build APK debug
./gradlew assembleRelease        # Build APK release
./gradlew test                   # Tests unitaires
./gradlew testDebugUnitTest      # Tests unitaires debug uniquement
./gradlew connectedAndroidTest   # Tests instrumentés (appareil requis)
./gradlew lint                   # Analyse statique Android Lint
./gradlew dependencyUpdates      # Vérifier les mises à jour de dépendances
```

---

## Architecture

```
app/src/main/java/com/scaminal/
├── ui/          Écrans Compose + ViewModels (scanner, terminal, favoris)
├── network/     Scanner IP (InetAddress.isReachable), scanner ports (Socket)
├── ssh/         Client SSH, gestion sessions, flux terminal, parser ANSI
├── data/        Room Database, DAOs, entités, repositories
├── security/    Chiffrement credentials via Android Keystore
└── di/          Modules d'injection (Hilt/Koin)
```

Voir [ARCHITECTURE.md](ARCHITECTURE.md) pour les flux de données détaillés.

---

## Philosophie de développement

### Développement incrémental (obligatoire)

```
Un module → Test → Validation utilisateur → Module suivant
```

**Limites par itération :**
- 1-3 fichiers liés maximum
- ~50-150 lignes de nouveau code
- Doit être testable indépendamment

### Points d'arrêt obligatoires

Stopper et attendre validation après :
- Schéma Room / migrations de base
- Code lié à la sécurité (Keystore, credentials)
- Chaque écran Compose complet
- Intégrations réseau (scanner IP, scanner ports)
- Connexion SSH / gestion de session

**Format de stop :**
```
[Module] terminé.

**Tester :**
1. [Étape 1]
2. [Étape 2]
Attendu : [Résultat]

En attente de validation avant de continuer.
```

### Ordre de développement

```
Phase 1 : Fondation (valider avant Phase 2)
├── [ ] Structure projet + configuration Gradle
├── [ ] Logging (Timber) configuré
├── [ ] Room Database + entités de base (HostEntity)
├── [ ] WifiHelper (détection IP locale, état Wi-Fi)
└── [ ] REVUE SÉCURITÉ

Phase 2 : Core réseau (valider avant Phase 3)
├── [ ] NetworkScanner (scan IP parallèle via coroutines)
├── [ ] PortScanner (scan ports avec timeout)
├── [ ] ScannerScreen + ScannerViewModel
└── [ ] FavoritesScreen + persistance Room

Phase 3 : SSH
├── [ ] SshClient (connexion/déconnexion)
├── [ ] TerminalStream (flux bidirectionnel)
├── [ ] AnsiParser (couleurs terminal)
└── [ ] TerminalScreen + TerminalViewModel

Phase 4 : Pré-release (obligatoire)
├── [ ] Audit sécurité complet (voir SECURITY.md)
├── [ ] Audit dépendances (./gradlew dependencyUpdates)
├── [ ] ProGuard/R8 activé et testé
├── [ ] Test sur appareil physique
└── [ ] Validation finale
```

---

## Règles de code

### Pas de hardcoding

**Jamais dans le code source :**
- Mots de passe, clés API, tokens
- Credentials de base de données
- Chemins absolus
- Adresses IP, hostnames, ports en dur

**Utiliser à la place :**
- `BuildConfig` fields dans `build.gradle.kts` pour la config par variante
- `local.properties` pour les chemins locaux (SDK, keystore) — jamais commité
- `keystore.properties` pour les credentials de signature — jamais commité
- `gradle.properties` pour les propriétés partagées du projet
- Android Keystore pour les secrets runtime (credentials SSH)

**Pattern de configuration (build.gradle.kts) :**
```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            buildConfigField("Int", "DEFAULT_SCAN_TIMEOUT", "200")
        }
        release {
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
            buildConfigField("Int", "DEFAULT_SCAN_TIMEOUT", "200")
        }
    }
}
```

### Logging (Timber)

Utiliser **Timber** au lieu de `Log.d/e/i` directement.

**Initialisation (Application.onCreate) :**
```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}
// En release : pas de log ou CrashReportingTree
```

**Ce qu'on log :**
- Résultats de scan (nombre d'hôtes, durée)
- Événements SSH (connexion, déconnexion, erreurs)
- Opérations Room (si debug)
- Erreurs avec stack traces

**Ce qu'on ne log JAMAIS :**
- Mots de passe, clés privées SSH
- Credentials en clair
- Tokens de session

### Taille des fichiers

| Lignes | Statut |
|--------|--------|
| < 300 | Idéal |
| 300-500 | Acceptable |
| 500-800 | Envisager un split |
| > 800 | Doit être splitté |

**Convention de nommage pour les splits :**
```
SshClient.kt           → Connexion, déconnexion, config
SshClientSession.kt    → Gestion de session active
SshClientAuth.kt       → Authentification (password, key)
```

---

## Documentation Kotlin

### En-tête de fichier

```kotlin
/**
 * @file NomDuFichier.kt
 * @description Brève description du rôle
 */
```

### Documentation de fonction (KDoc)

```kotlin
/**
 * Scanne le sous-réseau pour découvrir les hôtes actifs.
 *
 * @param subnet Le préfixe du sous-réseau (ex: "192.168.1")
 * @param timeout Timeout par hôte en millisecondes
 * @return Flow émettant chaque [Host] découvert
 */
fun scanSubnet(subnet: String, timeout: Int = 200): Flow<Host>
```

---

## Permissions Android requises

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
```

---

## Contraintes techniques clés

- Le scan IP doit utiliser `async/awaitAll` sur `Dispatchers.IO` pour paralléliser 254 requêtes sans bloquer l'UI
- Les connexions Socket pour le scan de ports nécessitent un timeout court (~200ms)
- Les clés et mots de passe SSH stockés localement doivent être chiffrés via **Android Keystore**
- La connectivité Wi-Fi doit être vérifiée via `ConnectivityManager` avant chaque scan
- Le terminal SSH doit supporter le parsing des codes **ANSI** pour les couleurs

---

## Git

### Branches

```
feature/<nom-court>        # Nouvelle fonctionnalité
fix/<nom-court>            # Correction de bug
refactor/<nom-court>       # Refactoring
```

### Messages de commit

```
[Type] Résumé court

- Détail 1
- Détail 2
```

Types : `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

### Ne jamais commiter

- `local.properties`, `keystore.properties`
- Fichiers `.jks` / `.keystore`
- APK/AAB signés
- Fichiers de base de données `.db`
- Logs
- Répertoires `.idea/`, `.gradle/`, `build/`

---

## Session de travail

**Continuer :** `"continue"` ou `"on continue"`
**Nouvelle session :** `"nouvelle session: Nom de Feature"`

Toujours lire CLAUDE.md en début de session.
Toujours attendre la validation utilisateur entre les modules.
Corriger les bugs avant d'ajouter de nouvelles fonctionnalités.

---

## Standards

- **Encodage :** UTF-8 avec fins de ligne LF
- **Timestamps :** ISO 8601 (YYYY-MM-DD HH:mm)
- **Format horaire :** 24h
