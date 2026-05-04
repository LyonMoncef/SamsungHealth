---
type: code-source
language: javascript
file_path: static/js/admin-auth.js
git_blob: e57f6c7ff05f1dde3a28a08e43a2cf656745f166
last_synced: '2026-04-28T14:04:54Z'
loc: 62
annotations: []
imports: []
exports: []
tags:
- code
- javascript
---

# static/js/admin-auth.js

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/js/admin-auth.js`](../../../static/js/admin-auth.js).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```javascript
// V2.3.3.3 — Admin login flow. Sessionstorage token, probe endpoint validation.
(function () {
  'use strict';

  var STORAGE_KEY = 'admin_token';
  var TOKEN_HEADER = 'X-' + 'Registration-Token';

  function showMessage(text, isError) {
    var el = document.getElementById('admin-login-message');
    if (!el) return;
    el.textContent = text;
    el.dataset.kind = isError ? 'error' : 'ok';
  }

  function validateReturnTo(raw) {
    if (!raw) return null;
    if (/^\/admin\/[a-z0-9/-]+$/.test(raw)) return raw;
    return null;
  }

  function nextUrl() {
    var params = new URLSearchParams(window.location.search);
    return validateReturnTo(params.get('return_to')) || '/admin/users';
  }

  document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('admin-login-form');
    if (!form) return;
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var input = form.querySelector('input[name="admin-token"]');
      var token = input ? input.value.trim() : '';
      if (!token) {
        showMessage('Token requis.', true);
        return;
      }
      var headers = { 'Content-Type': 'application/json' };
      headers[TOKEN_HEADER] = token;
      fetch('/admin/probe', {
        method: 'POST',
        headers: headers,
        credentials: 'same-origin',
        body: '{}'
      }).then(function (r) {
        if (r.status === 200) {
          try {
            window.sessionStorage.setItem(STORAGE_KEY, token);
          } catch (e) { /* ignore */ }
          window.location.href = nextUrl();
        } else if (r.status === 401) {
          showMessage('Token invalide.', true);
        } else if (r.status === 429) {
          showMessage('Trop de tentatives. Réessayez plus tard.', true);
        } else {
          showMessage('Erreur (' + r.status + ').', true);
        }
      }).catch(function () {
        showMessage('Erreur réseau.', true);
      });
    });
  });
})();
```

---

## Appendix — symbols & navigation *(auto)*
