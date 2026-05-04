---
type: code-source
language: html
file_path: static/admin/pending-verifications.html
git_blob: dddf701ac2a38dcb30f5cab9d6fa51c4da529a21
last_synced: '2026-04-28T14:04:54Z'
loc: 35
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# static/admin/pending-verifications.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/admin/pending-verifications.html`](../../../static/admin/pending-verifications.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Admin — Pending Verifications</title>
  <link rel="icon" type="image/svg+xml" href="/static/assets/logo/favicon.svg">
  <link rel="stylesheet" href="/static/css/ds-tokens.css?v=2.3.3.3">
  <link rel="stylesheet" href="/static/css/auth.css?v=2.3.3.3">
  <link rel="stylesheet" href="/static/css/admin.css?v=2.3.3.3">
  <script src="/static/js/theme.js?v=2.3.3.3"></script>
</head>
<body>
  <header class="admin-header">
    <h1 class="admin-title">Pending Verifications</h1>
    <nav class="admin-nav">
      <a href="/admin/users">Users</a>
      <a href="/admin/pending-verifications">Verifications</a>
    </nav>
    <div class="admin-actions">
      <button type="button" class="theme-toggle" data-theme-toggle aria-label="Changer le thème">Thème</button>
      <button type="button" id="admin-logout" class="auth-btn-secondary">Déconnexion</button>
    </div>
  </header>
  <main class="admin-main">
    <section class="admin-card">
      <p class="admin-intro">Liens valides 60 secondes (cache outbound). Auto-refresh 30 s.</p>
      <ul class="admin-cards" id="admin-pending-list"></ul>
      <div id="admin-pending-empty" class="admin-empty" hidden>Aucune vérification en attente.</div>
    </section>
    <div id="admin-pending-message" aria-live="polite"></div>
  </main>
  <script src="/static/js/admin.js?v=2.3.3.3" defer></script>
</body>
</html>
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]]
