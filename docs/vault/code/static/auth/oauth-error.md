---
type: code-source
language: html
file_path: static/auth/oauth-error.html
git_blob: 6098d6795774c5245b9b6f300cd0ba315777990c
last_synced: '2026-05-06T08:02:34Z'
loc: 32
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# static/auth/oauth-error.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/auth/oauth-error.html`](../../../static/auth/oauth-error.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Erreur de connexion Google — Data Saillance</title>
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
      <h1 class="auth-title">Erreur de connexion</h1>
      <p id="oauth-error-message">Erreur de connexion Google</p>
      <div class="auth-links">
        <a href="/auth/login">Retour à la connexion</a>
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
