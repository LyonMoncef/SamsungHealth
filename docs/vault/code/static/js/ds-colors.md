---
type: code-source
language: javascript
file_path: static/js/ds-colors.js
git_blob: f8754ec35317d1c74bf3b90a2ce996e9ceefd4bb
last_synced: '2026-05-06T08:02:34Z'
loc: 24
annotations: []
imports: []
exports: []
tags:
- code
- javascript
---

# static/js/ds-colors.js

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/js/ds-colors.js`](../../../static/js/ds-colors.js).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```javascript
// V2.3.3.3 — dsColor helper: lit --ds-* depuis CSS computed avec sanitizer.
(function (global) {
  'use strict';

  var COLOR_RE = /^(#[0-9a-fA-F]{3,8}|rgba?\([^)]+\)|hsla?\([^)]+\)|[a-z]+)$/;

  function dsColor(token) {
    var raw = '';
    try {
      raw = getComputedStyle(document.documentElement)
        .getPropertyValue('--ds-' + token)
        .trim();
    } catch (e) { /* ignore */ }
    if (!COLOR_RE.test(raw)) {
      if (typeof console !== 'undefined' && console.warn) {
        console.warn('[ds-colors] invalid value for --ds-' + token + ', falling back: ' + raw);
      }
      return '#000000';
    }
    return raw;
  }

  global.dsColor = dsColor;
})(typeof window !== 'undefined' ? window : this);
```

---

## Appendix — symbols & navigation *(auto)*
