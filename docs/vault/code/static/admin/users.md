---
type: code-source
language: html
file_path: static/admin/users.html
git_blob: af405231cd0704b584c45af1776bdafef594d936
last_synced: '2026-04-28T14:04:54Z'
loc: 48
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# static/admin/users.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/admin/users.html`](../../../static/admin/users.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Admin — Users</title>
  <link rel="icon" type="image/svg+xml" href="/static/assets/logo/favicon.svg">
  <link rel="stylesheet" href="/static/css/ds-tokens.css?v=2.3.3.3">
  <link rel="stylesheet" href="/static/css/auth.css?v=2.3.3.3">
  <link rel="stylesheet" href="/static/css/admin.css?v=2.3.3.3">
  <script src="/static/js/theme.js?v=2.3.3.3"></script>
</head>
<body>
  <header class="admin-header">
    <h1 class="admin-title">Users</h1>
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
    <section class="admin-toolbar">
      <input type="search" id="admin-users-search" placeholder="Filtrer par email" autocomplete="off">
    </section>
    <section class="admin-card">
      <table class="admin-table" id="admin-users-table">
        <thead>
          <tr>
            <th>Email</th>
            <th>Statut</th>
            <th>Échecs</th>
            <th>Dernier login</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody id="admin-users-tbody"></tbody>
      </table>
      <div id="admin-users-empty" class="admin-empty" hidden>Aucun utilisateur.</div>
    </section>
    <div id="admin-users-message" aria-live="polite"></div>
  </main>
  <script src="/static/js/admin.js?v=2.3.3.3" defer></script>
</body>
</html>
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]]
