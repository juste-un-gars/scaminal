# Sécurité - Scaminal

## Stockage des credentials SSH

### Android Keystore

Les clés privées et mots de passe SSH sont chiffrés via l'API **Android Keystore** avant d'être stockés dans Room.

```
Mot de passe utilisateur
    → Chiffrement AES-256-GCM (clé dans Android Keystore)
    → Stockage du blob chiffré dans Room
    → Déchiffrement uniquement au moment de la connexion SSH
```

### Règles

- Aucun mot de passe ou clé privée en clair dans la base de données
- Aucun credential dans les SharedPreferences
- Aucun credential dans les logs (Logcat)
- Les clés Keystore sont liées à l'appareil (non exportables)

## Permissions réseau

| Permission | Usage | Justification |
|-----------|-------|---------------|
| `INTERNET` | Connexions SSH, scan de ports | Requis pour toute communication réseau |
| `ACCESS_NETWORK_STATE` | Vérifier la connectivité | Éviter les scans sans réseau |
| `ACCESS_WIFI_STATE` | Obtenir l'IP locale et le masque | Calculer la plage du sous-réseau |
| `CHANGE_WIFI_MULTICAST_STATE` | Découverte réseau avancée | Optionnel, pour mDNS/Bonjour |

## Bonnes pratiques de développement

### Ce qui ne doit JAMAIS apparaître dans le code source

- Mots de passe, tokens, clés API
- Adresses IP ou hostnames en dur
- Credentials de test
- Clés de signature (keystore JKS/BKS)

### Configuration sensible

Les informations de signature APK vont dans `keystore.properties` (exclu du git via `.gitignore`) :

```properties
storePassword=***
keyPassword=***
keyAlias=***
storeFile=chemin/vers/keystore.jks
```

Référencé dans `build.gradle.kts` :

```kotlin
val keystoreProps = Properties().apply {
    load(rootProject.file("keystore.properties").inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    }
}
```

## Audit de sécurité pré-release

Avant toute release, vérifier :

- [ ] Aucun credential dans le code source ou les logs
- [ ] Chiffrement Keystore fonctionnel et testé
- [ ] Permissions minimales dans le Manifest
- [ ] Dépendances à jour (`./gradlew dependencyUpdates`)
- [ ] ProGuard/R8 activé pour la release (obfuscation)
- [ ] Network Security Config en place (HTTPS pour les appels externes si applicable)
- [ ] Pas de données sensibles dans les backups Android
