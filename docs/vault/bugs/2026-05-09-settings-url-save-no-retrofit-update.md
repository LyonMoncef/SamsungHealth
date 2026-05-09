---
title: "Settings URL save doesn't update active Retrofit instance"
date: 2026-05-09
severity: medium
status: fixed
components: [android-app/app/src/main/java/fr/datasaillance/nightfall/MainActivity.kt, android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt, android-app/app/src/main/java/fr/datasaillance/nightfall/di/NetworkModule.kt]
fix_commit: null
---

# Bug — Settings URL save doesn't update active Retrofit instance

## Symptôme

User changes backend URL in Settings and taps 'Enregistrer' — URL is saved to EncryptedSharedPreferences but the running API instance still targets the old URL. Button appears to do nothing.

## Repro

1. Open app
2. Go to Profile → Paramètres
3. Change URL field to a new backend URL
4. Tap 'Enregistrer'
5. Go to Import → tap 'Vérifier la connexion'
6. App still connects (or fails) with old URL

## Attendu vs Réel

**Attendu :** After saving, all subsequent API calls use the new URL immediately
**Réel :** api is created via 'by lazy' in MainActivity — Retrofit is built once at first access and never recreated when URL changes

## Root cause

Retrofit is immutable once constructed. BackendUrlStore.saveUrl() updates prefs but the NightfallApi instance already holds a Retrofit pointing to the old base URL. Fix: make api a Compose MutableState, rebuild Retrofit on URL save.

## Fix appliqué

Replaced 'private val api by lazy' with buildApi() function + Compose mutableStateOf in setContent. onSaveUrl now calls buildApi() and reassigns. NavGraph remember blocks use api as key to recreate ImportRepository and ImportViewModel.

## Scénarios de tests à écrire

- Given user saves a new URL in Settings, when Import screen is opened, then pingBackend() uses the new URL
- Given Retrofit is rebuilt after URL change, when Import flow runs, then upload requests target the new base URL
- Given URL is changed to an unreachable server, when Save is tapped and Import is opened, then ConnectionFailed state is shown (not the old server's response)
