---
type: code-source
language: html
file_path: static/auth/login.html
git_blob: 343ad9bdfa16ca85454b90bd153e7b360b311c27
last_synced: '2026-04-27T20:51:40Z'
loc: 43
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# static/auth/login.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/auth/login.html`](../../../static/auth/login.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Connexion — Data Saillance</title>
  <link rel="icon" type="image/svg+xml" href="/static/assets/logo/favicon.svg">
  <link rel="stylesheet" href="/static/css/ds-tokens.css?v=2.3.3.2">
  <link rel="stylesheet" href="/static/css/auth.css?v=2.3.3.2">
  <script src="/static/js/theme.js?v=2.3.3.2"></script>
</head>
<body>
  <button type="button" class="theme-toggle" data-theme-toggle aria-label="Changer le thème">Thème</button>
  <main class="auth-shell">
    <section class="auth-card">
      <div class="auth-logo">
        <picture>
          <source srcset="/static/assets/logo/logo-dark.svg" media="(prefers-color-scheme: dark)">
          <img src="/static/assets/logo/logo-white-fond-blanc.svg" alt="Data Saillance" width="180">
        </picture>
      </div>
      <h1 class="auth-title">Connexion</h1>
      <form class="auth-form" id="login-form" novalidate>
        <label>Email
          <input type="email" name="email" autocomplete="email" required maxlength="320">
        </label>
        <label>Mot de passe
          <input type="password" name="password" autocomplete="current-password" required minlength="1" maxlength="1024">
        </label>
        <button type="submit" class="auth-btn-primary">Se connecter</button>
      </form>
      <div class="auth-divider">ou</div>
      <button type="button" class="auth-btn-secondary" id="google-btn">Continuer avec Google</button>
      <div id="login-message" aria-live="polite"></div>
      <div class="auth-links">
        <a href="/auth/reset-request">Mot de passe oublié ?</a>
      </div>
    </section>
  </main>
  <script src="/static/js/api-client.js?v=2.3.3.2" defer></script>
  <script src="/static/js/auth-form.js?v=2.3.3.2" defer></script>
</body>
</html>
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall]]
