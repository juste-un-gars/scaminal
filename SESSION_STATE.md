# Scaminal - Session State

## Phase actuelle

**Phase 3 : SSH + Raccourcis** — Terminée

## Progression

| Phase | Statut |
|-------|--------|
| 0 : Documentation | Terminée |
| 1 : Fondation | Terminée |
| 2 : Core réseau | Terminée |
| 3 : SSH | Terminée |
| 3b : Raccourcis commandes | Terminée |
| 4 : Pré-release | En attente |

## Sessions

| # | Objectif | Statut |
|---|----------|--------|
| 000 | Documentation | Terminée |
| 001 | Fondation | Terminée |
| 002 | Core réseau | Terminée |
| 003 | SSH + Raccourcis | Terminée |

## Phase 3 — Détail des itérations

### SSH (8 itérations)
1. Dépendance JSch 0.2.20 + KeystoreManager AES-GCM
2. SshClient (sealed class SshConnectionState, connect/disconnect, PTY xterm-256color)
3. TerminalStream (attach/write/detach, Flow<String>)
4. AnsiParser (machine à états NORMAL→ESCAPE→CSI, TerminalSpan, toAnnotatedString)
5. HostRepository credential methods (saveCredentials, getCredentials)
6. TerminalViewModel (injection SSH + credentials + stream)
7. TerminalScreen (LoginDialog avec oeil mot de passe, terminal coloré, input bar)
8. Navigation wiring (route terminal/{hostIp}, tap hôte → terminal)

### Améliorations UX terminal
- Fix clavier : `adjustNothing` + `imePadding()` (pas de décalage écran)
- Fix double padding : `innerPadding` par route (pas sur NavHost global)
- Icone oeil visibilité mot de passe dans LoginDialog
- Dépendance `material-icons-extended`

### Raccourcis commandes (4 itérations)
1. Entity CommandShortcut + DAO + Migration DB v1→v2 + ShortcutRepository + 8 commandes par défaut
2. ShortcutsViewModel + ShortcutsScreen (CRUD complet)
3. Navigation 3ème onglet "Raccourcis" dans bottom nav
4. Barre de chips raccourcis dans TerminalScreen (tap = remplit, + = ajouter)

## Fichiers créés/modifiés (Phase 3)

### Nouveaux fichiers
- `security/KeystoreManager.kt` — Chiffrement AES-256-GCM via Android Keystore
- `ssh/SshClient.kt` — Client SSH JSch
- `ssh/TerminalStream.kt` — Flux bidirectionnel SSH
- `ssh/AnsiParser.kt` — Parser ANSI escape codes
- `ui/terminal/TerminalViewModel.kt` — ViewModel terminal SSH
- `ui/terminal/TerminalScreen.kt` — Écran terminal complet
- `data/entity/CommandShortcut.kt` — Entité raccourcis
- `data/dao/CommandShortcutDao.kt` — DAO raccourcis
- `data/repository/ShortcutRepository.kt` — Repository + défauts
- `ui/shortcuts/ShortcutsViewModel.kt` — ViewModel raccourcis
- `ui/shortcuts/ShortcutsScreen.kt` — Écran gestion raccourcis

### Fichiers modifiés
- `gradle/libs.versions.toml` — JSch 0.2.20, material-icons-extended
- `app/build.gradle.kts` — Dépendances JSch + icons, BuildConfig SSH_TIMEOUT, packaging exclusion
- `app/proguard-rules.pro` — Keep JSch classes
- `data/AppDatabase.kt` — Version 2, migration, CommandShortcutDao
- `data/repository/HostRepository.kt` — saveCredentials/getCredentials
- `di/DatabaseModule.kt` — Migration + provider CommandShortcutDao
- `ScaminalApplication.kt` — ensureDefaults() au démarrage
- `MainActivity.kt` — Route terminal, onglet raccourcis, padding par route
- `ui/scanner/ScannerScreen.kt` — onNavigateToTerminal, tap hôte
- `ui/favorites/FavoritesScreen.kt` — onNavigateToTerminal, tap favori
- `AndroidManifest.xml` — windowSoftInputMode=adjustNothing

## Handoff

Phase 3 complète. Prochaine étape : Phase 4 (Pré-release)
- Audit sécurité complet (credentials, Keystore, permissions)
- Audit dépendances (./gradlew dependencyUpdates)
- ProGuard/R8 activé et testé (build release)
- Test sur appareil physique
- Validation finale
