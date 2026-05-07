---
type: code-source
language: css
file_path: static/css/admin.css
git_blob: aaf37b21fbe4869fb5c3f1d4a8f5a2751c8bb861
last_synced: '2026-05-06T08:02:34Z'
loc: 224
annotations: []
imports: []
exports: []
tags:
- code
- css
---

# static/css/admin.css

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`static/css/admin.css`](../../../static/css/admin.css).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```css
/* V2.3.3.3 — Admin layout (table + cards + header). Reuses ds-tokens + auth base. */

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ds-space-4);
  padding: var(--ds-space-4) var(--ds-space-6);
  background: var(--ds-bg-secondary);
  border-bottom: 1px solid var(--ds-border);
  flex-wrap: wrap;
}

.admin-title {
  font-family: var(--ds-font-display);
  font-size: var(--ds-fs-h2);
  margin: 0;
  color: var(--ds-text-primary);
}

.admin-nav {
  display: flex;
  gap: var(--ds-space-4);
}

.admin-nav a {
  color: var(--ds-text-secondary);
  text-decoration: none;
  font-size: var(--ds-fs-body-sm);
}

.admin-nav a:hover {
  color: var(--ds-accent-warm);
  text-decoration: underline;
}

.admin-actions {
  display: flex;
  gap: var(--ds-space-2);
  align-items: center;
}

.admin-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--ds-space-6);
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-6);
}

.admin-toolbar {
  display: flex;
  gap: var(--ds-space-2);
}

.admin-toolbar input[type="search"] {
  flex: 1;
  padding: var(--ds-space-2) var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-sm);
  background: var(--ds-bg-secondary);
  color: var(--ds-text-primary);
  font-family: var(--ds-font-body);
}

.admin-card {
  background: var(--ds-bg-secondary);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  padding: var(--ds-space-6);
}

.admin-card h2 {
  font-family: var(--ds-font-display);
  font-size: var(--ds-fs-h3);
  font-weight: var(--ds-fw-h3);
  margin: 0 0 var(--ds-space-4) 0;
  color: var(--ds-text-primary);
}

.admin-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--ds-fs-body-sm);
}

.admin-table th,
.admin-table td {
  padding: var(--ds-space-2) var(--ds-space-3);
  text-align: left;
  border-bottom: 1px solid var(--ds-border);
}

.admin-table th {
  color: var(--ds-text-secondary);
  font-weight: var(--ds-fw-eyebrow);
  text-transform: uppercase;
  font-size: var(--ds-fs-eyebrow);
  letter-spacing: 0.06em;
}

.admin-table td {
  color: var(--ds-text-primary);
}

.admin-table .status-active { color: var(--ds-success); }
.admin-table .status-locked { color: var(--ds-error); }
.admin-table .status-disabled { color: var(--ds-text-muted); }

.admin-table button {
  margin-right: var(--ds-space-1);
  padding: var(--ds-space-1) var(--ds-space-2);
  font-size: var(--ds-fs-caption);
  background: var(--ds-bg-primary);
  color: var(--ds-text-primary);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-sm);
  cursor: pointer;
}

.admin-table button:hover {
  border-color: var(--ds-accent-warm);
}

.admin-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-2);
}

.admin-list li {
  padding: var(--ds-space-2) var(--ds-space-3);
  background: var(--ds-bg-primary);
  border-radius: var(--ds-radius-sm);
  font-size: var(--ds-fs-body-sm);
  color: var(--ds-text-primary);
}

.admin-cards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--ds-space-4);
}

.admin-cards li {
  background: var(--ds-bg-primary);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  padding: var(--ds-space-4);
}

.admin-cards .verify-link {
  word-break: break-all;
  font-family: var(--ds-font-body);
  font-size: var(--ds-fs-caption);
  color: var(--ds-text-secondary);
  margin-bottom: var(--ds-space-2);
  display: block;
}

.admin-cards button {
  padding: var(--ds-space-1) var(--ds-space-3);
  background: var(--ds-bg-secondary);
  color: var(--ds-text-primary);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-sm);
  cursor: pointer;
}

.admin-empty {
  color: var(--ds-text-muted);
  font-style: italic;
  padding: var(--ds-space-4) 0;
  text-align: center;
}

.admin-user-info {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: var(--ds-space-2) var(--ds-space-4);
}

.admin-user-info dt {
  color: var(--ds-text-secondary);
  font-size: var(--ds-fs-body-sm);
}

.admin-user-info dd {
  color: var(--ds-text-primary);
  margin: 0;
  font-size: var(--ds-fs-body-sm);
}

.admin-action-row {
  display: flex;
  gap: var(--ds-space-2);
  flex-wrap: wrap;
}

.admin-action-row button {
  padding: var(--ds-space-2) var(--ds-space-4);
  background: var(--ds-bg-primary);
  color: var(--ds-text-primary);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-sm);
  cursor: pointer;
}

.admin-action-row button:hover {
  border-color: var(--ds-accent-warm);
}

.admin-intro {
  color: var(--ds-text-secondary);
  margin: 0 0 var(--ds-space-4) 0;
  font-size: var(--ds-fs-body-sm);
}
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]]
