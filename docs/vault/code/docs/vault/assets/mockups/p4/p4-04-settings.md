---
type: code-source
language: html
file_path: docs/vault/assets/mockups/p4/p4-04-settings.html
git_blob: 4e6d12cab39a7fffd607489ea1fbae6c422073ed
last_synced: '2026-05-06T10:22:17Z'
loc: 288
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# docs/vault/assets/mockups/p4/p4-04-settings.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`docs/vault/assets/mockups/p4/p4-04-settings.html`](../../../docs/vault/assets/mockups/p4/p4-04-settings.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!doctype html>
<!-- Nightfall · Phase 4 · Screen 4 — Settings
     DataSaillance dark mode tokens · OD mobile-app skill · Archetype D (Profile adapted) -->
<html lang="fr">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Nightfall · Settings</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg:      #191E22;
      --surface: #232E32;
      --fg:      #F5F5F5;
      --muted:   #9AA0A5;
      --border:  rgba(7, 188, 211, 0.15);
      --accent:  #D37C04;
      --accent-cool: #07BCD3;
      --accent-teal: #0E9EB0;
      --danger:  #C0392B;

      --font-display: 'Playfair Display', Georgia, serif;
      --font-body:    'Inter', -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
      --font-mono:    ui-monospace, 'SF Mono', Menlo, monospace;

      --fs-h1: 26px; --fs-h2: 20px; --fs-h3: 16px; --fs-body: 15px; --fs-meta: 12px;
      --radius-card: 18px;
    }
    *, *::before, *::after { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; height: 100%; }
    body {
      background: radial-gradient(60% 80% at 50% 0%, rgba(7, 188, 211, 0.05) 0%, #191E22 60%);
      color: var(--fg); font-family: var(--font-body); font-size: var(--fs-body);
      line-height: 1.4; -webkit-font-smoothing: antialiased;
      display: grid; place-items: center; padding: 32px;
    }
    .stage { display: flex; flex-direction: column; align-items: center; gap: 24px; }
    .caption { font-family: var(--font-mono); font-size: 12px; letter-spacing: 0.08em; text-transform: uppercase; color: var(--muted); }
    .caption strong { color: var(--fg); font-weight: 500; }

    .device {
      position: relative; width: 390px; height: 844px; border-radius: 56px; padding: 12px;
      background: linear-gradient(160deg, #2a2a2c 0%, #1a1a1c 50%, #0e0e10 100%);
      box-shadow: 0 0 0 1px rgba(255,255,255,0.04) inset, 0 0 0 2px #000 inset, 0 28px 60px -12px rgba(0,0,0,0.55), 0 8px 20px -8px rgba(0,0,0,0.45);
      isolation: isolate;
    }
    .device::before, .device::after {
      content: ''; position: absolute; width: 3px;
      background: linear-gradient(to bottom, transparent 0%, rgba(255,255,255,0.06) 8%, transparent 16%, transparent 84%, rgba(255,255,255,0.04) 92%, transparent 100%);
      top: 100px; bottom: 100px; pointer-events: none;
    }
    .device::before { left: -1px; } .device::after { right: -1px; }
    .island { position: absolute; top: 22px; left: 50%; transform: translateX(-50%); width: 124px; height: 36px; background: #000; border-radius: 999px; z-index: 5; }
    .btn-rail { position: absolute; width: 4px; background: #0a0a0c; border-radius: 2px; }
    .btn-rail.left-1  { left: -3px; top: 174px; height: 32px; }
    .btn-rail.left-2  { left: -3px; top: 220px; height: 60px; }
    .btn-rail.left-3  { left: -3px; top: 290px; height: 60px; }
    .btn-rail.right-1 { right: -3px; top: 250px; height: 100px; }

    .screen { position: relative; width: 100%; height: 100%; background: var(--bg); border-radius: 44px; overflow: hidden; display: flex; flex-direction: column; }
    .statusbar { flex: 0 0 47px; padding: 18px 26px 0; display: flex; align-items: flex-start; justify-content: space-between; font-family: var(--font-body); font-size: 15px; font-weight: 600; color: var(--fg); letter-spacing: -0.01em; }
    .statusbar .right { display: inline-flex; align-items: center; gap: 6px; }
    .statusbar svg { width: 17px; height: 11px; fill: var(--fg); }
    .statusbar .battery { width: 25px; }
    .content { flex: 1 1 auto; overflow-y: auto; overflow-x: hidden; padding: 8px 0 4px; }
    .content::-webkit-scrollbar { display: none; }
    .tabbar {
      flex: 0 0 auto; display: grid; grid-template-columns: repeat(4, 1fr);
      padding: 8px 8px 4px; border-top: 1px solid rgba(7,188,211,0.12);
      background: rgba(35,46,50,0.95); backdrop-filter: blur(20px);
    }
    .tab { display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 8px 0; color: var(--muted); font-size: 10px; letter-spacing: 0.02em; }
    .tab.active { color: var(--accent); }
    .tab svg { width: 22px; height: 22px; stroke: currentColor; fill: none; stroke-width: 1.7; }
    .tab.active svg { stroke-width: 2.2; }
    .home-indicator { flex: 0 0 26px; position: relative; }
    .home-indicator::after { content: ''; position: absolute; left: 50%; bottom: 6px; transform: translateX(-50%); width: 134px; height: 5px; background: var(--fg); border-radius: 999px; opacity: 0.35; }

    .pad { padding-inline: 20px; }
    .meta { font-family: var(--font-mono); font-size: var(--fs-meta); color: var(--muted); }
    .num { font-family: var(--font-mono); font-variant-numeric: tabular-nums; }
    .card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-card); overflow: hidden; }

    /* Settings row */
    .setting-row {
      display: flex; align-items: center; justify-content: space-between;
      padding: 14px 16px; border-top: 1px solid rgba(7,188,211,0.08);
      min-height: 52px;
    }
    .setting-row:first-child { border-top: 0; }
    .setting-left { display: flex; align-items: center; gap: 12px; }
    .setting-icon {
      width: 32px; height: 32px; border-radius: 9px;
      display: grid; place-items: center; flex-shrink: 0;
    }
    .setting-icon svg { width: 16px; height: 16px; stroke: currentColor; fill: none; stroke-width: 1.7; }
    .setting-label { font-size: 14px; font-weight: 500; }
    .setting-value { font-size: 13px; color: var(--muted); font-family: var(--font-mono); }
    .chevron { width: 16px; height: 16px; stroke: var(--muted); fill: none; stroke-width: 1.7; }

    /* Avatar initials */
    .avatar-ring {
      width: 56px; height: 56px; border-radius: 50%;
      background: linear-gradient(135deg, var(--accent-teal), var(--accent));
      display: grid; place-items: center;
      font-family: var(--font-display); font-size: 20px; font-weight: 700; color: #fff;
    }

    /* Toggle pill */
    .toggle {
      width: 44px; height: 26px; border-radius: 999px; background: var(--accent-teal);
      position: relative; flex-shrink: 0;
    }
    .toggle::after {
      content: ''; position: absolute; width: 20px; height: 20px;
      border-radius: 50%; background: #fff; top: 3px; right: 4px;
    }
    .toggle.off { background: rgba(130,133,135,0.3); }
    .toggle.off::after { right: auto; left: 4px; }
  </style>
</head>
<body>
  <div class="stage">
    <div class="caption"><strong>Nightfall</strong> · Settings — Phase 4 · Mockup P4-04</div>

    <div class="device" data-od-id="device">
      <span class="btn-rail left-1" aria-hidden></span>
      <span class="btn-rail left-2" aria-hidden></span>
      <span class="btn-rail left-3" aria-hidden></span>
      <span class="btn-rail right-1" aria-hidden></span>
      <span class="island" aria-hidden></span>

      <div class="screen">
        <div class="statusbar">
          <span style="font-family: var(--font-mono);">9:41</span>
          <span class="right">
            <svg viewBox="0 0 17 11" aria-hidden><rect x="0" y="7" width="3" height="4" rx="0.6"/><rect x="4" y="5" width="3" height="6" rx="0.6"/><rect x="8" y="3" width="3" height="8" rx="0.6"/><rect x="12" y="0" width="3" height="11" rx="0.6"/></svg>
            <svg viewBox="0 0 17 11" aria-hidden><path d="M8.5 1.5C5.5 1.5 2.7 2.6 0.5 4.6L2 6.1C3.8 4.5 6.1 3.6 8.5 3.6c2.4 0 4.7 0.9 6.5 2.5l1.5-1.5c-2.2-2-5-3.1-8-3.1zM3.5 7.6L5 9.1c1-0.9 2.2-1.4 3.5-1.4 1.3 0 2.5 0.5 3.5 1.4l1.5-1.5c-1.4-1.3-3.1-2-5-2-1.9 0-3.6 0.7-5 2zM6.5 10.6l2 2 2-2c-0.5-0.5-1.2-0.8-2-0.8s-1.5 0.3-2 0.8z"/></svg>
            <svg class="battery" viewBox="0 0 25 11" aria-hidden><rect x="0.5" y="0.5" width="21" height="10" rx="2.5" fill="none" stroke="currentColor" stroke-opacity="0.45"/><rect x="22" y="3.5" width="1.5" height="4" rx="0.4" fill="currentColor" fill-opacity="0.45"/><rect x="2" y="2" width="18" height="7" rx="1.4"/></svg>
          </span>
        </div>

        <main class="content" data-od-id="content">
          <!-- Header -->
          <div style="padding: 10px 20px 8px;" data-od-id="header">
            <h1 style="margin: 0; font-family: var(--font-display); font-size: var(--fs-h1); letter-spacing: -0.02em;">Paramètres</h1>
          </div>

          <!-- User profile card -->
          <section class="pad" style="margin-top: 8px;" data-od-id="profile">
            <div class="card" style="padding: 16px;">
              <div style="display: flex; align-items: center; gap: 14px;">
                <div class="avatar-ring">M</div>
                <div>
                  <div style="font-weight: 600; font-size: 16px;">Lyon Moncef</div>
                  <div class="meta" style="margin-top: 3px;">ids.mfafi@gmail.com</div>
                  <div style="margin-top: 6px; display: inline-flex; align-items: center; gap: 4px; padding: 2px 8px; background: rgba(14,158,176,0.1); border-radius: 999px;">
                    <span style="width: 6px; height: 6px; border-radius: 50%; background: var(--accent-teal);"></span>
                    <span style="font-size: 11px; color: var(--accent-teal); font-family: var(--font-mono);">Connecté via Google</span>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- Connection settings -->
          <section class="pad" style="margin-top: 16px;" data-od-id="connection">
            <div class="meta" style="margin: 0 0 8px; padding-left: 4px;">CONNEXION</div>
            <div class="card">
              <div class="setting-row">
                <div class="setting-left">
                  <div class="setting-icon" style="background: rgba(14,158,176,0.12); color: var(--accent-teal);">
                    <svg viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
                  </div>
                  <div>
                    <div class="setting-label">Serveur backend</div>
                    <div class="setting-value">sh-prod.datasaillance.fr</div>
                  </div>
                </div>
                <svg class="chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"/></svg>
              </div>
              <div class="setting-row">
                <div class="setting-left">
                  <div class="setting-icon" style="background: rgba(211,124,4,0.12); color: var(--accent);">
                    <svg viewBox="0 0 24 24"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>
                  </div>
                  <div>
                    <div class="setting-label">Sync Health Connect</div>
                    <div class="setting-value">Dernière sync : 04:32</div>
                  </div>
                </div>
                <div class="toggle"></div>
              </div>
            </div>
          </section>

          <!-- Data & Privacy -->
          <section class="pad" style="margin-top: 16px;" data-od-id="data-privacy">
            <div class="meta" style="margin: 0 0 8px; padding-left: 4px;">DONNÉES & CONFIDENTIALITÉ</div>
            <div class="card">
              <div class="setting-row">
                <div class="setting-left">
                  <div class="setting-icon" style="background: rgba(7,188,211,0.1); color: var(--accent-cool);">
                    <svg viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                  </div>
                  <div class="setting-label">Exporter mes données</div>
                </div>
                <svg class="chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"/></svg>
              </div>
              <div class="setting-row">
                <div class="setting-left">
                  <div class="setting-icon" style="background: rgba(7,188,211,0.1); color: var(--accent-cool);">
                    <svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
                  </div>
                  <div>
                    <div class="setting-label">Journal d'audit</div>
                    <div class="setting-value">12 mois · RGPD Art. 15</div>
                  </div>
                </div>
                <svg class="chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"/></svg>
              </div>
              <div class="setting-row">
                <div class="setting-left">
                  <div class="setting-icon" style="background: rgba(192,57,43,0.1); color: #E74C3C;">
                    <svg viewBox="0 0 24 24"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
                  </div>
                  <div class="setting-label" style="color: #E74C3C;">Effacer mes données</div>
                </div>
                <svg class="chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"/></svg>
              </div>
            </div>
          </section>

          <!-- App info -->
          <section class="pad" style="margin-top: 16px; margin-bottom: 8px;" data-od-id="app-info">
            <div class="meta" style="margin: 0 0 8px; padding-left: 4px;">APPLICATION</div>
            <div class="card">
              <div class="setting-row">
                <div class="setting-left">
                  <div class="setting-icon" style="background: rgba(245,245,245,0.06); color: var(--muted);">
                    <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                  </div>
                  <div>
                    <div class="setting-label">Version</div>
                    <div class="setting-value">2.0.0-beta (Phase 4)</div>
                  </div>
                </div>
              </div>
              <div class="setting-row" style="cursor: pointer;">
                <div class="setting-left">
                  <div class="setting-icon" style="background: rgba(245,245,245,0.06); color: var(--muted);">
                    <svg viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                  </div>
                  <div class="setting-label">Se déconnecter</div>
                </div>
                <svg class="chevron" viewBox="0 0 24 24"><polyline points="9 18 15 12 9 6"/></svg>
              </div>
            </div>
          </section>
        </main>

        <!-- Tab bar -->
        <nav class="tabbar" data-od-id="tabbar">
          <a class="tab">
            <svg viewBox="0 0 24 24"><path d="M3 12 12 3l9 9"/><path d="M5 10v10h14V10"/></svg>
            Sommeil
          </a>
          <a class="tab">
            <svg viewBox="0 0 24 24"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
            Tendances
          </a>
          <a class="tab">
            <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            Activité
          </a>
          <a class="tab active">
            <svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-7 8-7s8 3 8 7"/></svg>
            Profil
          </a>
        </nav>

        <div class="home-indicator" aria-hidden></div>
      </div>
    </div>
  </div>
</body>
</html>
```

---

## Appendix — symbols & navigation *(auto)*
