---
title: "Logout ne supprime pas le token — reconnexion impossible"
date: 2026-05-09
severity: high
status: fixed
components: [android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt, android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/auth/AuthViewModel.kt, android-app/app/src/main/java/fr/datasaillance/nightfall/data/auth/TokenDataStore.kt]
fix_commit: null
---

# Bug — Logout ne supprime pas le token — reconnexion impossible

## Symptôme

Après logout, le token JWT reste dans TokenDataStore. À la réouverture (force-kill), l'utilisateur est toujours connecté. Depuis l'écran login, la tentative de connexion retourne une erreur 500 (LoginUiState.Success persistant déclenche LaunchedEffect avant toute saisie).

## Repro

1. Se connecter sur l'app Android native
2. Aller sur l'écran Profil
3. Appuyer sur Se déconnecter
4. L'écran login s'affiche
5. Force-kill l'app et rouvrir
6. Résultat : l'utilisateur est toujours connecté — startDestination = Sleep

## Attendu vs Réel

**Attendu :** Logout vide le token JWT de TokenDataStore et remet AuthViewModel._loginState à Idle. Après force-kill, hasToken() = false → startDestination = Login.
**Réel :** TokenDataStore conserve le token. hasToken() = true après force-kill → l'app démarre directement sur Sleep avec l'ancienne session.

## Root cause

NavGraph.onLogout() naviguait vers Login sans appeler tokenDataStore.clearToken() ni authViewModel.logout(). AuthViewModel._loginState restait LoginUiState.Success → LaunchedEffect dans LoginScreen redirigeait immédiatement vers Sleep.

## Fix appliqué

AuthViewModel.logout() ajouté : clearToken() + reset _loginState à Idle. NavGraph.onLogout() appelle authViewModel?.logout() avant la navigation.

## Scénarios de tests à écrire

- Après logout, tokenDataStore.hasToken() doit retourner false
- Après logout, authViewModel.loginState doit être LoginUiState.Idle
- Après logout + force-kill + reopen, startDestination = NavDestination.Login
- Après logout, LaunchedEffect dans LoginScreen ne doit PAS déclencher onLoginSuccess
- Séquence login → logout → login doit fonctionner sans erreur ni redirection automatique
