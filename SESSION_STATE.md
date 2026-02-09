# Scaminal - Session State

## Phase actuelle

**Phase 5 : Release v1.1.0** — Terminée

## Progression

| Phase | Statut |
|-------|--------|
| 0 : Documentation | Terminée |
| 1 : Fondation | Terminée |
| 2 : Core réseau | Terminée |
| 3 : SSH | Terminée |
| 3b : Raccourcis commandes | Terminée |
| 4 : Release v1.0.0 | Terminée |
| 5 : Release v1.1.0 | Terminée |

## Sessions

| # | Objectif | Statut |
|---|----------|--------|
| 000 | Documentation | Terminée |
| 001 | Fondation | Terminée |
| 002 | Core réseau | Terminée |
| 003 | SSH + Raccourcis | Terminée |
| 004 | Bugfixes + Release v1.0.0 | Terminée |
| 005 | Scanner amélioré + Release v1.1.0 | Terminée |

## Phase 5 — Détail (Session 005)

### Subnet et range IP configurables
- Le scan IP n'est plus figé sur le /24 auto-détecté
- Champ texte "Subnet" éditable (pré-rempli depuis le réseau actif, modifiable si VPN)
- Champs "Début" et "Fin" pour restreindre le range (défaut 1-254)
- `NetworkScanner.scanSubnet()` accepte `subnetOverride`, `startHost`, `endHost`

### Interaction hôtes repensée
- **Tap** sur un hôte → dialog de détails (IP, hostname, ping, ports) au lieu de forcer SSH
- **Long press** → scan complet des 65535 ports (au lieu des 10 ports communs)
- Bouton "Connexion SSH" dans le dialog uniquement si port 22 détecté ouvert
- Bouton "Scanner ports" dans le dialog si pas encore scanné

### Scan complet de ports (1-65535)
- Nouvelle méthode `PortScanner.scanAllPorts()` avec `Semaphore(500)` pour la concurrence
- Scan par batches de 1000 ports avec progression en temps réel
- Barre de progression visible pendant le scan
- Résultat "Aucun port ouvert" affiché quand le scan est terminé avec 0 résultats

### Ports actionnables
- Ports HTTP (80, 8080, 3000, etc.) → ouvrent le navigateur
- Ports HTTPS (443, 8443, etc.) → ouvrent le navigateur en HTTPS
- Port 22/2222 → connexion SSH (terminal intégré)
- Autres ports → copie `ip:port` dans le presse-papier + toast

### Base de ports connus étendue
- `PORT_NAMES` passe de 10 à ~130 ports référencés (IANA + services courants)
- Inclut NetBIOS (137-139), SMB (445), Redis (6379), MongoDB (27017), Docker (2375), etc.

### Champ `isPortScanned` dans Host
- Nouveau champ pour distinguer "pas encore scanné" de "0 ports ouverts"
- Affiché dans les cartes hôtes et dans le dialog de détails

## Fichiers modifiés (Session 005)

- `app/build.gradle.kts` — versionCode 2, versionName 1.1.0
- `network/NetworkScanner.kt` — params `subnetOverride`, `startHost`, `endHost`
- `network/PortScanner.kt` — `scanAllPorts()`, `PORT_NAMES` étendu (~130 ports)
- `network/model/Host.kt` — ajout `isPortScanned: Boolean`
- `ui/scanner/ScannerViewModel.kt` — states subnet/range, scan complet 65535 ports
- `ui/scanner/ScannerScreen.kt` — dialog détails hôte, ports cliquables, range selector
- `ARCHITECTURE.md` — mise à jour concurrence (semaphore, range configurable)

## Handoff

Release v1.1.0 prête. Prochaines étapes possibles :
- Audit sécurité complet (OWASP, credentials, permissions)
- Tests unitaires / instrumentés
- Nouvelles fonctionnalités (SFTP, multi-sessions, thèmes)
- Configuration des ports à scanner (liste personnalisable)

## Historique des phases précédentes

### Phase 4 (Session 004)
- Fix credentials password non restauré
- Fix déconnexion SSH bouton retour
- Release signing (keystore, ProGuard, APK 1.8 MB)
- Tag v1.0.0 GitHub

### Phase 3 (Session 003)
- SSH : JSch 0.2.20, SshClient, TerminalStream, AnsiParser, TerminalScreen
- UX terminal : edge-to-edge, clavier, padding
- Raccourcis commandes : CRUD, onglet, barre chips terminal
- DB migration v1→v2

### Phase 2 (Session 002)
- NetworkScanner, PortScanner, ScannerScreen, FavoritesScreen, Navigation

### Phase 1 (Session 001)
- Gradle, Hilt, Timber, Room, WifiHelper
