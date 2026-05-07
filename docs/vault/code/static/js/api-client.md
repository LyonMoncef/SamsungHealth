---
type: code-source
language: javascript
file_path: static/js/api-client.js
git_blob: 5d8d6560c47632eaa45a5b0848d528584d5ed60c
last_synced: '2026-05-06T08:02:34Z'
loc: 49
annotations: []
imports: []
exports: []
tags:
- code
- javascript
---

# static/js/api-client.js

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/js/api-client.js`](../../../static/js/api-client.js).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```javascript
// V2.3.3.2 — Fetch wrapper with unified error handling (429/423/401).
// Uses credentials: 'include' so the httpOnly refresh cookie travels on /auth/refresh.
(function () {
  var API_BASE = '';

  async function _handleResponse(r) {
    var body = null;
    try { body = await r.json(); } catch (e) { /* not JSON */ }
    if (r.status === 429) {
      return {
        ok: false,
        status: 429,
        code: 'rate_limit_exceeded',
        retryAfter: r.headers.get('Retry-After'),
      };
    }
    if (r.status === 423) {
      return { ok: false, status: 423, code: 'account_locked' };
    }
    if (r.status >= 400) {
      var detail = (body && body.detail) ? body.detail : 'unknown_error';
      return { ok: false, status: r.status, code: detail };
    }
    return { ok: true, status: r.status, body: body };
  }

  async function apiPost(path, payload, opts) {
    opts = opts || {};
    var headers = { 'Content-Type': 'application/json' };
    if (opts.headers) {
      for (var k in opts.headers) {
        if (Object.prototype.hasOwnProperty.call(opts.headers, k)) {
          headers[k] = opts.headers[k];
        }
      }
    }
    if (opts.bearer) headers['Authorization'] = 'Bearer ' + opts.bearer;
    if (opts.regToken) headers['X-Registration-Token'] = opts.regToken;
    var r = await fetch(API_BASE + path, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload || {}),
      credentials: 'include',
    });
    return _handleResponse(r);
  }

  window.dsApiClient = { apiPost: apiPost };
})();
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall]]
