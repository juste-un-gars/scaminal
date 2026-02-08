# Session 002 : Core réseau

## Meta

- **Date :** 2026-02-08
- **Objectif :** Implémenter les scanners réseau, UI Compose et favoris
- **Statut :** En cours

## Modules complétés

| Module | Validé | Date |
|--------|--------|------|
| 1. Modèles réseau (Host, ScanState) | Oui | 2026-02-08 |
| 2. NetworkScanner | Oui | 2026-02-08 |
| 3. PortScanner | Oui | 2026-02-08 |
| 4. HostRepository | Oui | 2026-02-08 |
| 5. ScannerViewModel | Oui | 2026-02-08 |
| 6. ScannerScreen | Oui | 2026-02-08 |
| 7. FavoritesViewModel + FavoritesScreen | En attente | — |
| 8. Navigation + MainActivity | En attente | — |

## Décisions techniques

- **Scan IP :** async/awaitAll sur Dispatchers.IO, 254 pings parallèles, Flow<Host>
- **Scan Ports :** Socket.connect avec timeout, COMMON_PORTS (10 ports), parallèle par hôte
- **Flux scan :** Scan IP (bouton) → Scan Ports tous hôtes (bouton) → Appui long = scan 1 hôte
- **HostRepository :** upsert intelligent (update si IP existe, sinon insert)
- **ScannerViewModel :** deux StateFlow séparés pour scanState et portScanState
- **Icône app :** scaminal.png déclinée en mipmap (mdpi→xxxhdpi) + adaptive icon + drawable-nodpi

## Fichiers créés

```
app/src/main/java/com/scaminal/network/model/Host.kt
app/src/main/java/com/scaminal/network/model/ScanState.kt
app/src/main/java/com/scaminal/network/NetworkScanner.kt
app/src/main/java/com/scaminal/network/PortScanner.kt
app/src/main/java/com/scaminal/data/repository/HostRepository.kt
app/src/main/java/com/scaminal/ui/scanner/ScannerViewModel.kt
app/src/main/java/com/scaminal/ui/scanner/ScannerScreen.kt
app/src/main/res/mipmap-mdpi/ic_launcher.png
app/src/main/res/mipmap-mdpi/ic_launcher_round.png
app/src/main/res/mipmap-mdpi/ic_launcher_foreground.png
app/src/main/res/mipmap-hdpi/ic_launcher.png
app/src/main/res/mipmap-hdpi/ic_launcher_round.png
app/src/main/res/mipmap-hdpi/ic_launcher_foreground.png
app/src/main/res/mipmap-xhdpi/ic_launcher.png
app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
app/src/main/res/mipmap-xhdpi/ic_launcher_foreground.png
app/src/main/res/mipmap-xxhdpi/ic_launcher.png
app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
app/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.png
app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png
app/src/main/res/drawable-nodpi/scaminal_logo.png
```

## Fichiers modifiés

```
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml (foreground → @mipmap)
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml (foreground → @mipmap)
app/src/main/res/drawable/ic_launcher_background.xml (couleur → #0D1B2A)
```

## Handoff

Itérations 1-6 terminées. Reste :
- Itération 7 : FavoritesViewModel + FavoritesScreen
- Itération 8 : Navigation + MainActivity (BottomNavigationBar + NavGraph)
