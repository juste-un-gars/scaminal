# Architecture - Scaminal

## Vue d'ensemble

Scaminal suit une architecture **MVVM (Model-View-ViewModel)** avec séparation claire des responsabilités. L'UI est entièrement en Jetpack Compose, la logique métier est exposée via des ViewModels, et les opérations réseau/SSH tournent dans des coroutines.

## Structure des packages

```
app/src/main/java/com/scaminal/
│
├── ui/                          # Couche présentation (Compose + ViewModels)
│   ├── scanner/
│   │   ├── ScannerScreen.kt        Écran principal de scan réseau
│   │   └── ScannerViewModel.kt     État du scan, lancement, résultats
│   ├── terminal/
│   │   ├── TerminalScreen.kt       Console SSH interactive
│   │   └── TerminalViewModel.kt    Gestion session SSH, I/O
│   ├── favorites/
│   │   ├── FavoritesScreen.kt      Liste des machines sauvegardées
│   │   └── FavoritesViewModel.kt   CRUD favoris via Room
│   ├── components/                  Composables réutilisables
│   ├── navigation/
│   │   └── NavGraph.kt             Navigation entre écrans
│   └── theme/                       Thème Material 3, couleurs, typo
│
├── network/                     # Couche réseau bas niveau
│   ├── NetworkScanner.kt           Scan IP du sous-réseau (InetAddress.isReachable)
│   ├── PortScanner.kt              Scan de ports (Socket avec timeout)
│   ├── WifiHelper.kt               Détection Wi-Fi, IP locale, masque sous-réseau
│   └── model/
│       ├── Host.kt                  Représentation d'un appareil découvert
│       └── ScanResult.kt           Résultat agrégé d'un scan
│
├── ssh/                         # Couche SSH
│   ├── SshClient.kt                Wrapper autour de JSch/MINA
│   ├── SshSession.kt               Gestion connexion/déconnexion
│   ├── TerminalStream.kt           Flux entrée/sortie du terminal
│   └── AnsiParser.kt               Parsing des codes couleur ANSI
│
├── data/                        # Couche persistance (Room)
│   ├── AppDatabase.kt              Définition de la base Room
│   ├── dao/
│   │   ├── HostDao.kt              Requêtes CRUD pour les hôtes
│   │   └── CommandHistoryDao.kt    Historique des commandes SSH
│   ├── entity/
│   │   ├── HostEntity.kt           Table des machines sauvegardées
│   │   └── CommandEntity.kt        Table historique commandes
│   └── repository/
│       ├── HostRepository.kt       Abstraction d'accès aux hôtes
│       └── CommandRepository.kt    Abstraction d'accès à l'historique
│
├── security/                    # Sécurité
│   └── KeystoreManager.kt          Chiffrement clés SSH via Android Keystore
│
└── di/                          # Injection de dépendances
    └── AppModule.kt                Modules Hilt ou Koin
```

## Flux de données

### Scan réseau

```
[ScannerScreen] → [ScannerViewModel] → [NetworkScanner]
                                      → [PortScanner]
     ↑ Flow<List<Host>>                    ↑ Coroutines (Dispatchers.IO)
     └────────────────────────────────────┘
```

1. L'utilisateur lance un scan depuis `ScannerScreen`
2. `ScannerViewModel` appelle `NetworkScanner.scanSubnet()` qui retourne un `Flow<Host>`
3. Pour chaque hôte trouvé, `PortScanner.scanPorts()` vérifie les services
4. Les résultats remontent via Flow vers l'UI en temps réel

### Connexion SSH

```
[TerminalScreen] → [TerminalViewModel] → [SshClient]
       ↑ Flow<String> (output)              ↓ connect(host, port, credentials)
       └────── input (commandes) ──────→ [TerminalStream]
```

1. L'utilisateur sélectionne un hôte et entre ses identifiants
2. `SshClient` établit la connexion via JSch/MINA
3. `TerminalStream` gère les flux stdin/stdout bidirectionnels
4. `AnsiParser` convertit les codes ANSI pour l'affichage Compose

### Persistance

```
[FavoritesViewModel] → [HostRepository] → [HostDao] → [Room/SQLite]
```

Les favoris et l'historique de commandes passent par des Repository qui abstraient les DAOs Room.

## Concurrence

- **Dispatchers.IO** pour toutes les opérations réseau et SSH
- **Dispatchers.Main** pour les mises à jour UI
- Le scan IP utilise `async/awaitAll` pour paralléliser les 254 requêtes `isReachable`
- Les timeouts Socket sont à ~200ms pour éviter les blocages

## Sécurité

- Les credentials SSH sont chiffrés via `KeystoreManager` (Android Keystore API)
- Aucun mot de passe en clair dans Room ou SharedPreferences
- La vérification Wi-Fi via `ConnectivityManager` est faite avant chaque scan
