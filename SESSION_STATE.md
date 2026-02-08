# Scaminal - Session State

> **Claude : Appliquer le protocole de session (CLAUDE.md)**
> - Créer/mettre à jour la session en temps réel
> - Valider après chaque module avec : [Module] terminé. **Tester :** [...] En attente de validation.
> - Ne pas continuer sans validation utilisateur

---

## Projet

| Info | Valeur |
|------|--------|
| Nom | Scaminal |
| Type | Application Android native |
| Stack | Kotlin, Jetpack Compose, Room, Coroutines, JSch/MINA |
| Architecture | MVVM |

## Phase actuelle

**Phase 1 : Fondation** — Pas encore commencée

## Progression globale

| Phase | Statut | Dernière validation |
|-------|--------|---------------------|
| Phase 0 : Documentation | Terminée | 2026-02-08 |
| Phase 1 : Fondation | En attente | - |
| Phase 2 : Core réseau | En attente | - |
| Phase 3 : SSH | En attente | - |
| Phase 4 : Pré-release | En attente | - |

## Index des sessions

| Session | Objectif | Statut | Fichier |
|---------|----------|--------|---------|
| 000 | Documentation projet | Terminée | [SESSION_000](/.claude/sessions/SESSION_000_documentation.md) |
| 001 | Fondation (Gradle, Timber, Room, WifiHelper) | En attente | [SESSION_001](/.claude/sessions/SESSION_001_fondation.md) |

## Décisions techniques prises

- **UI :** Jetpack Compose (déclaratif, idéal pour les mises à jour temps réel du scan)
- **Logging :** Timber (standard Android, désactivé en release via BuildConfig)
- **DI :** Hilt ou Koin (à décider en Session 001)
- **SSH :** JSch ou Apache MINA (à décider en Session 003)
- **Sécurité credentials :** Android Keystore (AES-256-GCM)
- **Config :** BuildConfig fields + local.properties (pas de .env)

## Notes de handoff

La documentation est en place. La prochaine session doit :
1. Créer le projet Android Studio (Empty Compose Activity)
2. Configurer `build.gradle.kts` (dépendances : Compose, Room, Timber, Coroutines)
3. Implémenter le logging Timber
4. Créer les entités Room de base
5. Implémenter WifiHelper
