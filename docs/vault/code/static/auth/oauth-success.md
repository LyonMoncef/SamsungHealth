---
type: code-source
language: html
file_path: static/auth/oauth-success.html
git_blob: 7c5422623a1daf039fe0badfadd06ec7017f34c4
last_synced: '2026-05-06T08:02:34Z'
loc: 31
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# static/auth/oauth-success.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/auth/oauth-success.html`](../../../static/auth/oauth-success.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="referrer" content="no-referrer">
  <title>Connexion réussie — Data Saillance</title>
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
      <h1 class="auth-title">Connexion réussie</h1>
      <p>Redirection en cours...</p>
      <div id="oauth-success-message" aria-live="polite"></div>
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
