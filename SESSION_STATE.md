# Scaminal - Session State

## Phase actuelle

**Phase 4 : Release v1.0.0** — Terminée

## Progression

| Phase | Statut |
|-------|--------|
| 0 : Documentation | Terminée |
| 1 : Fondation | Terminée |
| 2 : Core réseau | Terminée |
| 3 : SSH | Terminée |
| 3b : Raccourcis commandes | Terminée |
| 4 : Release v1.0.0 | Terminée |

## Sessions

| # | Objectif | Statut |
|---|----------|--------|
| 000 | Documentation | Terminée |
| 001 | Fondation | Terminée |
| 002 | Core réseau | Terminée |
| 003 | SSH + Raccourcis | Terminée |
| 004 | Bugfixes + Release v1.0.0 | Terminée |

## Phase 4 — Détail (Session 004)

### Bugfixes
1. **Fix credentials password non restauré** — `loadSavedCredentials()` ne déchiffrait pas le password ; ajout `_savedPassword` StateFlow + decrypt dans init + pré-remplissage LoginDialog
2. **Fix déconnexion SSH bouton retour** — ajout `BackHandler` dans TerminalScreen + `SshClient.disconnectSync()` pour nettoyage synchrone + fix `onCleared()` qui n'appelait pas disconnect
3. Suppression du stub `getSavedPassword()` (retournait toujours null)

### Release signing
1. Génération keystore `scaminal-release.jks` (RSA 2048, validité 10000 jours)
2. `keystore.properties` (gitignored) pour les credentials
3. `build.gradle.kts` : signingConfigs release, import Properties, version 1.0.0
4. Build release signé avec R8/ProGuard — APK 1.8 MB

### GitHub Release
- Tag `v1.0.0` poussé sur `origin/main`
- Release à créer manuellement sur github.com/juste-un-gars/scaminal/releases/new
- APK : `scaminal-v1.0.0-release.apk`

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

## Fichiers modifiés (Session 004)

- `app/build.gradle.kts` — signingConfigs release, import Properties, versionName 1.0.0
- `ssh/SshClient.kt` — ajout `disconnectSync()` non-suspend
- `ui/terminal/TerminalViewModel.kt` — fix `_savedPassword`, fix `onCleared()`, simplification `connectWithSaved()`
- `ui/terminal/TerminalScreen.kt` — `BackHandler`, `initialPassword` dans LoginDialog, `LaunchedEffect(initialPassword)`

## Fichiers créés (Session 004)

- `scaminal-release.jks` — Keystore de signature (gitignored)
- `keystore.properties` — Credentials keystore (gitignored)

## Handoff

Release v1.0.0 prête. Prochaines étapes possibles :
- Audit sécurité complet (OWASP, credentials, permissions)
- Tests unitaires / instrumentés
- Nouvelles fonctionnalités (SFTP, multi-sessions, thèmes)
