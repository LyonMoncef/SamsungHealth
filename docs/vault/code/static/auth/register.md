---
type: code-source
language: html
file_path: static/auth/register.html
git_blob: 61241864721892591e544ac173ca6bbbe928885a
last_synced: '2026-04-27T20:51:40Z'
loc: 44
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# static/auth/register.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/auth/register.html`](../../../static/auth/register.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Inscription — Data Saillance</title>
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
      <h1 class="auth-title">Inscription</h1>
      <form class="auth-form" id="register-form" novalidate>
        <label>Email
          <input type="email" name="email" autocomplete="email" required maxlength="320">
        </label>
        <label>Mot de passe (12 caractères minimum)
          <input type="password" name="password" autocomplete="new-password" required minlength="12" maxlength="1024">
        </label>
        <label>Code d'accès
          <input type="password" name="reg_token" autocomplete="off" required maxlength="256">
        </label>
        <button type="submit" class="auth-btn-primary">Créer le compte</button>
      </form>
      <div id="register-message" aria-live="polite"></div>
      <div class="auth-links">
        <a href="/auth/login">Déjà inscrit ? Se connecter</a>
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
