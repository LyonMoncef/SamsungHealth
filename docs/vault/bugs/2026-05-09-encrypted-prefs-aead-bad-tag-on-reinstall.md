---
title: "AEADBadTagException au démarrage après reinstall APK sans pm clear"
date: 2026-05-09
severity: high
status: open
components: [android-app/app/src/main/java/fr/datasaillance/nightfall/data/auth/TokenDataStore.kt, android-app/app/src/main/java/fr/datasaillance/nightfall/data/network/BackendUrlStore.kt, android-app/app/src/main/java/fr/datasaillance/nightfall/data/settings/SettingsDataStore.kt]
fix_commit: null
---

# Bug — AEADBadTagException au démarrage après reinstall APK sans pm clear

## Symptôme

L'app crash instantanément au démarrage avec AEADBadTagException dans EncryptedSharedPreferences. Aucun écran ne s'affiche.

## Repro

1. Installer une première version du debug APK (pm clear préalable)
2. Se connecter — EncryptedSharedPreferences créées avec clé AndroidKeyStore
3. Installer une nouvelle version du debug APK via adb install -r (sans pm clear)
4. Rouvrir l'app
5. Résultat : crash instantané

## Attendu vs Réel

**Attendu :** L'app démarre normalement. Si la clé est corrompue, les prefs sont recréées silencieusement et l'utilisateur est redirigé vers le login.
**Réel :** FATAL EXCEPTION : javax.crypto.AEADBadTagException dans TokenDataStore.<init> → EncryptedSharedPreferences.create(). App inutilisable sans pm clear.

## Root cause

EncryptedSharedPreferences stocke un keyset Tink chiffré par une clé AndroidKeyStore. Quand le signingConfig ou l'intégrité de la KeyStore change entre deux installations debug (adb install -r sans pm clear), la clé ne peut plus déchiffrer le keyset → AEADBadTagException non catchée → crash.

## Fix appliqué

Aucun — à traiter

## Scénarios de tests à écrire

- TokenDataStore init doit survivre à un AEADBadTagException sans crash (supprimer les prefs corrompues + recreate)
- Après récupération automatique, hasToken() doit retourner false (token perdu)
- BackendUrlStore et SettingsDataStore doivent avoir le même fallback défensif
