---
title: "Import ping fails — GET /health returns 404"
date: 2026-05-09
severity: high
status: fixed
components: [android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt, android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepositoryImpl.kt]
fix_commit: null
---

# Bug — Import ping fails — GET /health returns 404

## Symptôme

Import screen shows 'Backend inaccessible — Vérifiez l'URL dans les paramètres' despite backend being reachable

## Repro

1. Open app, log in successfully
2. Go to Profile → Importer les données
3. Tap 'Vérifier la connexion'
4. Screen shows connection failed

## Attendu vs Réel

**Attendu :** ImportUiState.Connected — backend responds 200
**Réel :** ImportUiState.ConnectionFailed — NightfallApi.health() calls GET /health which returns 404 Not Found

## Root cause

NightfallApi.health() was annotated @GET("health") but backend exposes /healthz not /health. Fix: change annotation to @GET("healthz")

## Fix appliqué

Changed @GET("health") to @GET("healthz") in NightfallApi.kt

## Scénarios de tests à écrire

- Given backend is running at sh-dev, when Import screen loads and user taps 'Vérifier la connexion', then state transitions to Connected
- Given GET /healthz returns 200, when pingBackend() is called, then returns true
- Given GET /healthz returns 503, when pingBackend() is called, then returns false and error message shown
