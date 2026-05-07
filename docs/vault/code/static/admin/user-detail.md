---
type: code-source
language: html
file_path: static/admin/user-detail.html
git_blob: 8674119d3210f09a6ca2c14879e612a91889de37
last_synced: '2026-05-06T08:02:34Z'
loc: 49
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# static/admin/user-detail.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/admin/user-detail.html`](../../../static/admin/user-detail.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Admin — User Detail</title>
  <link rel="icon" type="image/svg+xml" href="/static/assets/logo/favicon.svg">
  <link rel="stylesheet" href="/static/css/ds-tokens.css?v=2.3.3.3">
  <link rel="stylesheet" href="/static/css/auth.css?v=2.3.3.3">
  <link rel="stylesheet" href="/static/css/admin.css?v=2.3.3.3">
  <script src="/static/js/theme.js?v=2.3.3.3"></script>
</head>
<body>
  <header class="admin-header">
    <h1 class="admin-title">User Detail</h1>
    <nav class="admin-nav">
      <a href="/admin/users">Retour aux users</a>
    </nav>
    <div class="admin-actions">
      <button type="button" class="theme-toggle" data-theme-toggle aria-label="Changer le thème">Thème</button>
      <button type="button" id="admin-logout" class="auth-btn-secondary">Déconnexion</button>
    </div>
  </header>
  <main class="admin-main">
    <section class="admin-card" id="admin-user-card">
      <h2>Identifiants</h2>
      <dl class="admin-user-info" id="admin-user-info"></dl>
    </section>
    <section class="admin-card">
      <h2>Providers</h2>
      <ul class="admin-list" id="admin-user-providers"></ul>
    </section>
    <section class="admin-card">
      <h2>Événements récents</h2>
      <ul class="admin-list" id="admin-user-events"></ul>
    </section>
    <section class="admin-card">
      <h2>Actions</h2>
      <div class="admin-action-row">
        <button type="button" data-admin-action="lock-15">Lock 15 min</button>
        <button type="button" data-admin-action="lock-60">Lock 1 h</button>
        <button type="button" data-admin-action="unlock">Unlock</button>
      </div>
    </section>
    <div id="admin-user-message" aria-live="polite"></div>
  </main>
  <script src="/static/js/admin.js?v=2.3.3.3" defer></script>
</body>
</html>
```

---

## Appendix — symbols & navigation *(auto)*
