---
type: code-source
language: html
file_path: static/admin/login.html
git_blob: 32497b14dd2960aa183f5d314d1851595ce19f98
last_synced: '2026-05-06T08:02:34Z'
loc: 30
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# static/admin/login.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/admin/login.html`](../../../static/admin/login.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Admin — Connexion</title>
  <link rel="icon" type="image/svg+xml" href="/static/assets/logo/favicon.svg">
  <link rel="stylesheet" href="/static/css/ds-tokens.css?v=2.3.3.3">
  <link rel="stylesheet" href="/static/css/auth.css?v=2.3.3.3">
  <link rel="stylesheet" href="/static/css/admin.css?v=2.3.3.3">
  <script src="/static/js/theme.js?v=2.3.3.3"></script>
</head>
<body>
  <button type="button" class="theme-toggle" data-theme-toggle aria-label="Changer le thème">Thème</button>
  <main class="auth-shell">
    <section class="auth-card">
      <h1 class="auth-title">Admin</h1>
      <p class="admin-intro">Accès restreint. Token requis.</p>
      <form class="auth-form" id="admin-login-form" autocomplete="off" novalidate>
        <label>Token admin
          <input type="password" name="admin-token" autocomplete="off" autocorrect="off" spellcheck="false" required>
        </label>
        <button type="submit" class="auth-btn-primary">Se connecter</button>
      </form>
      <div id="admin-login-message" aria-live="polite"></div>
    </section>
  </main>
  <script src="/static/js/admin-auth.js?v=2.3.3.3" defer></script>
</body>
</html>
```

---

## Appendix — symbols & navigation *(auto)*
