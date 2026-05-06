---
type: code-source
language: html
file_path: docs/vault/assets/mockups/p4/p4-01-login.html
git_blob: b3bb961e652b52238cac1e4864bc2f1f85c0497b
last_synced: '2026-05-06T10:22:17Z'
loc: 259
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# docs/vault/assets/mockups/p4/p4-01-login.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`docs/vault/assets/mockups/p4/p4-01-login.html`](../../../docs/vault/assets/mockups/p4/p4-01-login.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!doctype html>
<!-- Nightfall · Phase 4 · Screen 1 — Login
     DataSaillance dark mode tokens · OD mobile-app skill · Archetype C (Onboarding adapted) -->
<html lang="fr">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Nightfall · Login</title>
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

      --accent-soft: rgba(211, 124, 4, 0.14);
      --fg-soft:     rgba(245, 245, 245, 0.06);

      --font-display: 'Playfair Display', Georgia, serif;
      --font-body:    'Inter', -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
      --font-mono:    ui-monospace, 'SF Mono', Menlo, monospace;

      --fs-h1: 26px;
      --fs-h2: 20px;
      --fs-h3: 16px;
      --fs-body: 15px;
      --fs-meta: 12px;

      --radius-card: 18px;
      --radius-pill: 999px;
    }

    *, *::before, *::after { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; height: 100%; }
    body {
      background: radial-gradient(60% 80% at 50% 0%, rgba(14, 158, 176, 0.08) 0%, #191E22 60%);
      color: var(--fg);
      font-family: var(--font-body);
      font-size: var(--fs-body);
      line-height: 1.4;
      -webkit-font-smoothing: antialiased;
      display: grid;
      place-items: center;
      padding: 32px;
    }

    .stage { display: flex; flex-direction: column; align-items: center; gap: 24px; }
    .caption {
      font-family: var(--font-mono); font-size: 12px;
      letter-spacing: 0.08em; text-transform: uppercase; color: var(--muted);
    }
    .caption strong { color: var(--fg); font-weight: 500; }

    .device {
      position: relative; width: 390px; height: 844px;
      border-radius: 56px; padding: 12px;
      background: linear-gradient(160deg, #2a2a2c 0%, #1a1a1c 50%, #0e0e10 100%);
      box-shadow:
        0 0 0 1px rgba(255,255,255,0.04) inset,
        0 0 0 2px #000 inset,
        0 28px 60px -12px rgba(0,0,0,0.55),
        0 8px 20px -8px rgba(0,0,0,0.45);
      isolation: isolate;
    }
    .device::before, .device::after {
      content: ''; position: absolute; width: 3px;
      background: linear-gradient(to bottom, transparent 0%, rgba(255,255,255,0.06) 8%, transparent 16%, transparent 84%, rgba(255,255,255,0.04) 92%, transparent 100%);
      top: 100px; bottom: 100px; pointer-events: none;
    }
    .device::before { left: -1px; } .device::after { right: -1px; }
    .island {
      position: absolute; top: 22px; left: 50%; transform: translateX(-50%);
      width: 124px; height: 36px; background: #000; border-radius: 999px; z-index: 5;
    }
    .btn-rail { position: absolute; width: 4px; background: #0a0a0c; border-radius: 2px; }
    .btn-rail.left-1  { left: -3px; top: 174px; height: 32px; }
    .btn-rail.left-2  { left: -3px; top: 220px; height: 60px; }
    .btn-rail.left-3  { left: -3px; top: 290px; height: 60px; }
    .btn-rail.right-1 { right: -3px; top: 250px; height: 100px; }

    .screen {
      position: relative; width: 100%; height: 100%;
      background: var(--bg); border-radius: 44px; overflow: hidden;
      display: flex; flex-direction: column;
    }
    .statusbar {
      flex: 0 0 47px; padding: 18px 26px 0;
      display: flex; align-items: flex-start; justify-content: space-between;
      font-family: var(--font-body); font-size: 15px; font-weight: 600;
      color: var(--fg); letter-spacing: -0.01em;
    }
    .statusbar .right { display: inline-flex; align-items: center; gap: 6px; }
    .statusbar svg { width: 17px; height: 11px; fill: var(--fg); }
    .statusbar .battery { width: 25px; }
    .content {
      flex: 1 1 auto; overflow-y: auto; overflow-x: hidden;
      -webkit-overflow-scrolling: touch; padding: 8px 0 28px;
    }
    .content::-webkit-scrollbar { display: none; }
    .home-indicator { flex: 0 0 28px; position: relative; }
    .home-indicator::after {
      content: ''; position: absolute; left: 50%; bottom: 8px;
      transform: translateX(-50%); width: 134px; height: 5px;
      background: var(--fg); border-radius: 999px; opacity: 0.35;
    }

    .pad { padding-inline: 20px; }
    .stack { display: flex; flex-direction: column; gap: 16px; }
    .row { display: flex; align-items: center; gap: 12px; }

    .btn-primary {
      display: flex; align-items: center; justify-content: center;
      width: 100%; min-height: 48px; padding: 14px 20px;
      background: var(--accent); color: #fff; border: 0; border-radius: 14px;
      font: inherit; font-size: 15px; font-weight: 600; letter-spacing: -0.005em; cursor: pointer;
    }
    .btn-google {
      display: flex; align-items: center; justify-content: center; gap: 10px;
      width: 100%; min-height: 48px; padding: 14px 20px;
      background: transparent; color: var(--fg);
      border: 1px solid rgba(7, 188, 211, 0.3); border-radius: 14px;
      font: inherit; font-size: 15px; font-weight: 500; cursor: pointer;
    }
    .input-group { display: flex; flex-direction: column; gap: 4px; }
    .input-label {
      font-size: 12px; font-weight: 500; letter-spacing: 0.06em;
      text-transform: uppercase; color: var(--muted);
    }
    .input-field {
      background: var(--surface); border: 1px solid rgba(7, 188, 211, 0.2);
      border-radius: 12px; padding: 13px 16px; color: var(--fg);
      font: inherit; font-size: 15px; outline: none;
    }
    .input-field.active { border-color: var(--accent-cool); }
    .divider {
      display: flex; align-items: center; gap: 12px; color: var(--muted);
      font-size: 12px; font-family: var(--font-mono);
    }
    .divider::before, .divider::after {
      content: ''; flex: 1; height: 1px; background: rgba(7, 188, 211, 0.12);
    }

    /* logo placeholder */
    .logo-block {
      display: flex; flex-direction: column; align-items: center; gap: 6px;
      padding: 32px 20px 8px;
    }
    .logo-pill {
      width: 48px; height: 48px; border-radius: 14px;
      background: linear-gradient(135deg, var(--accent-cool), var(--accent));
      display: grid; place-items: center;
    }
    .logo-pill svg { width: 26px; height: 26px; fill: #fff; }
    .wordmark {
      font-family: var(--font-display); font-size: 22px; font-weight: 700;
      letter-spacing: -0.01em; color: var(--fg);
    }
    .tagline { font-size: 13px; color: var(--muted); }
  </style>
</head>
<body>
  <div class="stage">
    <div class="caption"><strong>Nightfall</strong> · Login — Phase 4 · Mockup P4-01</div>

    <div class="device" data-od-id="device">
      <span class="btn-rail left-1"  aria-hidden></span>
      <span class="btn-rail left-2"  aria-hidden></span>
      <span class="btn-rail left-3"  aria-hidden></span>
      <span class="btn-rail right-1" aria-hidden></span>
      <span class="island"           aria-hidden></span>

      <div class="screen">
        <div class="statusbar">
          <span style="font-family: var(--font-mono);">9:41</span>
          <span class="right">
            <svg viewBox="0 0 17 11" aria-hidden>
              <rect x="0" y="7" width="3" height="4" rx="0.6"/>
              <rect x="4" y="5" width="3" height="6" rx="0.6"/>
              <rect x="8" y="3" width="3" height="8" rx="0.6"/>
              <rect x="12" y="0" width="3" height="11" rx="0.6"/>
            </svg>
            <svg viewBox="0 0 17 11" aria-hidden>
              <path d="M8.5 1.5C5.5 1.5 2.7 2.6 0.5 4.6L2 6.1C3.8 4.5 6.1 3.6 8.5 3.6c2.4 0 4.7 0.9 6.5 2.5l1.5-1.5c-2.2-2-5-3.1-8-3.1zM3.5 7.6L5 9.1c1-0.9 2.2-1.4 3.5-1.4 1.3 0 2.5 0.5 3.5 1.4l1.5-1.5c-1.4-1.3-3.1-2-5-2-1.9 0-3.6 0.7-5 2zM6.5 10.6l2 2 2-2c-0.5-0.5-1.2-0.8-2-0.8s-1.5 0.3-2 0.8z"/>
            </svg>
            <svg class="battery" viewBox="0 0 25 11" aria-hidden>
              <rect x="0.5" y="0.5" width="21" height="10" rx="2.5" fill="none" stroke="currentColor" stroke-opacity="0.45"/>
              <rect x="22" y="3.5" width="1.5" height="4" rx="0.4" fill="currentColor" fill-opacity="0.45"/>
              <rect x="2" y="2" width="18" height="7" rx="1.4"/>
            </svg>
          </span>
        </div>

        <main class="content" data-od-id="content">
          <!-- Logo block -->
          <div class="logo-block" data-od-id="logo">
            <div class="logo-pill">
              <svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 14H9V8h2v8zm4 0h-2V8h2v8z"/></svg>
            </div>
            <span class="wordmark">Nightfall</span>
            <span class="tagline">Vos données de santé, chez vous.</span>
          </div>

          <!-- Form -->
          <section class="pad stack" style="margin-top: 24px; gap: 14px;" data-od-id="form">
            <div class="input-group">
              <div class="input-label">Email</div>
              <div class="input-field">ids.mfafi@gmail.com</div>
            </div>
            <div class="input-group">
              <div class="input-label">Mot de passe</div>
              <div class="input-field active" style="color: var(--muted); letter-spacing: 0.2em;">••••••••••</div>
            </div>
            <div style="text-align: right;">
              <span style="font-size: 13px; color: var(--accent-cool); cursor: pointer;">Mot de passe oublié ?</span>
            </div>
          </section>

          <!-- Primary CTA -->
          <section class="pad" style="margin-top: 20px;" data-od-id="cta">
            <button class="btn-primary">Se connecter</button>
          </section>

          <!-- Divider -->
          <section class="pad" style="margin-top: 20px;" data-od-id="divider">
            <div class="divider">ou</div>
          </section>

          <!-- Google OAuth -->
          <section class="pad" style="margin-top: 16px;" data-od-id="oauth">
            <button class="btn-google">
              <!-- G logo simplified -->
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
              </svg>
              Continuer avec Google
            </button>
          </section>

          <!-- Sign up link -->
          <section class="pad" style="margin-top: 28px; text-align: center;" data-od-id="signup-link">
            <span style="font-size: 13px; color: var(--muted);">Pas encore de compte ? </span>
            <span style="font-size: 13px; color: var(--accent); font-weight: 500; cursor: pointer;">Créer un compte</span>
          </section>
        </main>

        <div class="home-indicator" aria-hidden></div>
      </div>
    </div>
  </div>
</body>
</html>
```

---

## Appendix — symbols & navigation *(auto)*
