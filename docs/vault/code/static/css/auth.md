---
type: code-source
language: css
file_path: static/css/auth.css
git_blob: f7941f923df8eae4d8ba91247dcaed77ae87c789
last_synced: '2026-04-27T20:51:40Z'
loc: 163
annotations: []
imports: []
exports: []
tags:
- code
- css
---

# static/css/auth.css

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/css/auth.css`](../../../static/css/auth.css).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```css
/* V2.3.3.2 — Auth pages layout (form centré, no inline styles). */

* { box-sizing: border-box; }

html, body {
  margin: 0;
  padding: 0;
  background: var(--ds-bg-primary);
  color: var(--ds-text-primary);
  font-family: var(--ds-font-body);
  font-size: var(--ds-fs-body);
  min-height: 100vh;
}

.auth-shell {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ds-space-6);
}

.auth-card {
  background: var(--ds-bg-secondary);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  padding: var(--ds-space-12);
  max-width: 440px;
  width: 100%;
}

.auth-logo {
  display: flex;
  justify-content: center;
  margin-bottom: var(--ds-space-8);
}

.auth-logo img {
  max-width: 180px;
  height: auto;
}

.auth-title {
  font-family: var(--ds-font-display);
  font-size: var(--ds-fs-h1);
  font-weight: var(--ds-fw-h1);
  margin: 0 0 var(--ds-space-6) 0;
  text-align: center;
  color: var(--ds-text-primary);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-4);
}

.auth-form label {
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-2);
  font-size: var(--ds-fs-body-sm);
  color: var(--ds-text-secondary);
}

.auth-form input {
  background: var(--ds-bg-primary);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-sm);
  color: var(--ds-text-primary);
  font-family: var(--ds-font-body);
  font-size: var(--ds-fs-body);
  padding: var(--ds-space-3);
}

.auth-form input:focus {
  outline: 2px solid var(--ds-bg-accent);
  outline-offset: 2px;
}

.auth-btn-primary {
  background: var(--ds-bg-accent);
  border: none;
  border-radius: var(--ds-radius-sm);
  color: #FFFFFF;
  font-family: var(--ds-font-body);
  font-size: var(--ds-fs-body);
  font-weight: 600;
  padding: var(--ds-space-3) var(--ds-space-6);
  cursor: pointer;
}

.auth-btn-secondary {
  background: transparent;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-sm);
  color: var(--ds-text-primary);
  font-family: var(--ds-font-body);
  font-size: var(--ds-fs-body);
  padding: var(--ds-space-3) var(--ds-space-6);
  cursor: pointer;
}

.auth-message {
  margin-top: var(--ds-space-4);
  padding: var(--ds-space-3);
  border-radius: var(--ds-radius-sm);
  font-size: var(--ds-fs-body-sm);
}

.auth-message-error {
  background: rgba(192, 57, 43, 0.08);
  color: var(--ds-error);
}

.auth-message-success {
  background: rgba(34, 153, 84, 0.08);
  color: var(--ds-success);
}

.auth-links {
  margin-top: var(--ds-space-6);
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-2);
  text-align: center;
  font-size: var(--ds-fs-body-sm);
}

.auth-links a {
  color: var(--ds-bg-accent);
  text-decoration: none;
}

.theme-toggle {
  position: fixed;
  top: var(--ds-space-4);
  right: var(--ds-space-4);
  background: var(--ds-bg-secondary);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-sm);
  color: var(--ds-text-primary);
  cursor: pointer;
  padding: var(--ds-space-2) var(--ds-space-3);
  font-size: var(--ds-fs-body);
}

.auth-divider {
  display: flex;
  align-items: center;
  text-align: center;
  color: var(--ds-text-muted);
  font-size: var(--ds-fs-body-sm);
  margin: var(--ds-space-4) 0;
}

.auth-divider::before,
.auth-divider::after {
  content: "";
  flex: 1;
  border-bottom: 1px solid var(--ds-border);
  margin: 0 var(--ds-space-3);
}
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall]]
