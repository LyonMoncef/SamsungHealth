---
type: code-source
language: javascript
file_path: static/js/theme.js
git_blob: 920faffa4c95ccfe9c8d72c8f80a6a6ef70d21ea
last_synced: '2026-05-06T08:02:34Z'
loc: 46
annotations: []
imports: []
exports: []
tags:
- code
- javascript
---

# static/js/theme.js

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/js/theme.js`](../../../static/js/theme.js).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```javascript
// V2.3.3.2 — Theme switcher (light/dark) with FOUC prevention.
// Set data-theme SYNC before body render.
(function () {
  var STORAGE_KEY = 'ds-theme';

  function getEffectiveTheme() {
    try {
      var stored = window.localStorage.getItem(STORAGE_KEY);
      if (stored === 'light' || stored === 'dark') return stored;
    } catch (e) { /* localStorage may be unavailable */ }
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
  }

  function toggleTheme() {
    var next = getEffectiveTheme() === 'dark' ? 'light' : 'dark';
    try {
      window.localStorage.setItem(STORAGE_KEY, next);
    } catch (e) { /* ignore */ }
    applyTheme(next);
  }

  applyTheme(getEffectiveTheme());

  document.addEventListener('DOMContentLoaded', function () {
    var btn = document.querySelector('[data-theme-toggle]');
    if (btn) btn.addEventListener('click', toggleTheme);
  });

  if (window.matchMedia) {
    var mq = window.matchMedia('(prefers-color-scheme: dark)');
    var handler = function () {
      try {
        if (!window.localStorage.getItem(STORAGE_KEY)) applyTheme(getEffectiveTheme());
      } catch (e) { applyTheme(getEffectiveTheme()); }
    };
    if (mq.addEventListener) mq.addEventListener('change', handler);
    else if (mq.addListener) mq.addListener(handler);
  }
})();
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall]]
