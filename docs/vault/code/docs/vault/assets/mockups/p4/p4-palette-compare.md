---
type: code-source
language: html
file_path: docs/vault/assets/mockups/p4/p4-palette-compare.html
git_blob: f0c168e2d1b7dded5c849e2f6de729e9dfedf9c4
last_synced: '2026-05-06T20:34:25Z'
loc: 557
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# docs/vault/assets/mockups/p4/p4-palette-compare.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`docs/vault/assets/mockups/p4/p4-palette-compare.html`](../../../docs/vault/assets/mockups/p4/p4-palette-compare.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Nightfall — Comparatif palettes</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }

  body {
    background: #111;
    font-family: 'Cairo', 'Inter', sans-serif;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40px 20px 60px;
    gap: 16px;
    min-height: 100vh;
  }

  h1 {
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    letter-spacing: 0.05em;
    text-align: center;
  }

  .subtitle {
    color: #888;
    font-size: 13px;
    text-align: center;
    margin-top: 4px;
  }

  .compare-row {
    display: flex;
    gap: 48px;
    align-items: flex-start;
    margin-top: 24px;
  }

  .column {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 14px;
  }

  .label {
    font-size: 13px;
    font-weight: 600;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    padding: 6px 14px;
    border-radius: 6px;
    text-align: center;
    line-height: 1.4;
  }

  .label-a {
    background: #1e2a2f;
    color: #07BCD3;
    border: 1px solid #07BCD3;
  }

  .label-b {
    background: #2a1f1a;
    color: #d97a56;
    border: 1px solid #d97a56;
  }

  .palette-chips {
    display: flex;
    gap: 6px;
    align-items: center;
  }

  .chip {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    border: 2px solid rgba(255,255,255,0.15);
  }

  /* ── iPhone 15 Pro frame ── */
  .phone-frame {
    width: 320px;
    height: 692px;
    border-radius: 50px;
    border: 2px solid rgba(255,255,255,0.12);
    overflow: hidden;
    position: relative;
    box-shadow: 0 32px 64px rgba(0,0,0,0.6), inset 0 0 0 1px rgba(255,255,255,0.05);
  }

  .screen {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    padding: 48px 24px 32px;
    gap: 0;
  }

  /* notch */
  .phone-frame::before {
    content: '';
    position: absolute;
    top: 12px;
    left: 50%;
    transform: translateX(-50%);
    width: 100px;
    height: 30px;
    background: #111;
    border-radius: 20px;
    z-index: 10;
  }

  /* ━━━━━━━━━━━━━━━━━━━━━
     PALETTE A — Tech / Froid
     bg #191E22  surface #232E32
     accent #D37C04  cyan #07BCD3  teal #0E9EB0
  ━━━━━━━━━━━━━━━━━━━━━ */
  .screen-a {
    background: #191E22;
  }

  .screen-a .logo-ring {
    width: 64px; height: 64px;
    border-radius: 50%;
    border: 2px solid #0E9EB0;
    display: flex; align-items: center; justify-content: center;
    margin: 0 auto 16px;
    box-shadow: 0 0 20px rgba(7,188,211,0.25);
  }

  .screen-a .logo-icon {
    font-size: 24px; color: #07BCD3;
  }

  .screen-a .app-name {
    color: #E8EFF2;
    font-size: 26px;
    font-weight: 700;
    text-align: center;
    letter-spacing: 0.02em;
  }

  .screen-a .app-tagline {
    color: #7A9AAA;
    font-size: 12px;
    text-align: center;
    margin-top: 4px;
    letter-spacing: 0.06em;
    text-transform: uppercase;
  }

  .screen-a .field {
    background: #232E32;
    border: 1px solid #2E3D44;
    border-radius: 10px;
    padding: 14px 16px;
    color: #E8EFF2;
    font-size: 14px;
    margin-top: 10px;
  }

  .screen-a .field-label {
    color: #7A9AAA;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.07em;
    text-transform: uppercase;
    margin-top: 16px;
  }

  .screen-a .field-placeholder {
    color: #4A6272;
  }

  .screen-a .forgot {
    color: #07BCD3;
    font-size: 12px;
    text-align: right;
    margin-top: 8px;
  }

  .screen-a .btn-primary {
    background: #D37C04;
    color: #fff;
    font-size: 15px;
    font-weight: 700;
    text-align: center;
    padding: 16px;
    border-radius: 12px;
    margin-top: 24px;
    letter-spacing: 0.04em;
  }

  .screen-a .divider {
    display: flex; align-items: center; gap: 10px;
    margin-top: 20px;
  }

  .screen-a .divider-line {
    flex: 1; height: 1px; background: #2E3D44;
  }

  .screen-a .divider-text {
    color: #4A6272; font-size: 11px;
    letter-spacing: 0.06em; text-transform: uppercase;
  }

  .screen-a .btn-google {
    border: 1px solid #2E3D44;
    background: #232E32;
    color: #E8EFF2;
    font-size: 14px;
    font-weight: 600;
    text-align: center;
    padding: 14px;
    border-radius: 12px;
    margin-top: 12px;
    display: flex; align-items: center; justify-content: center; gap: 8px;
  }

  .screen-a .google-dot {
    width: 16px; height: 16px; border-radius: 50%;
    background: conic-gradient(#4285F4 90deg, #EA4335 90deg 180deg, #FBBC05 180deg 270deg, #34A853 270deg);
  }

  .screen-a .register-link {
    color: #7A9AAA;
    font-size: 12px;
    text-align: center;
    margin-top: auto;
  }

  .screen-a .register-link span {
    color: #07BCD3;
  }

  /* ━━━━━━━━━━━━━━━━━━━━━
     PALETTE B — Rust / Chaud DataSaillance
     bg #1a1917  surface #222120
     accent light #c96442  accent dark #d97a56
  ━━━━━━━━━━━━━━━━━━━━━ */
  .screen-b {
    background: #1a1917;
  }

  .screen-b .logo-ring {
    width: 64px; height: 64px;
    border-radius: 50%;
    border: 2px solid #d97a56;
    display: flex; align-items: center; justify-content: center;
    margin: 0 auto 16px;
    box-shadow: 0 0 20px rgba(217,122,86,0.2);
  }

  .screen-b .logo-icon {
    font-size: 24px; color: #d97a56;
  }

  .screen-b .app-name {
    color: #e8e4dc;
    font-size: 26px;
    font-weight: 700;
    text-align: center;
    letter-spacing: 0.02em;
  }

  .screen-b .app-tagline {
    color: #8a8278;
    font-size: 12px;
    text-align: center;
    margin-top: 4px;
    letter-spacing: 0.06em;
    text-transform: uppercase;
  }

  .screen-b .field {
    background: #222120;
    border: 1px solid #302e2b;
    border-radius: 10px;
    padding: 14px 16px;
    color: #e8e4dc;
    font-size: 14px;
    margin-top: 10px;
  }

  .screen-b .field-label {
    color: #8a8278;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.07em;
    text-transform: uppercase;
    margin-top: 16px;
  }

  .screen-b .field-placeholder {
    color: #5a554f;
  }

  .screen-b .forgot {
    color: #d97a56;
    font-size: 12px;
    text-align: right;
    margin-top: 8px;
  }

  .screen-b .btn-primary {
    background: #c96442;
    color: #fff;
    font-size: 15px;
    font-weight: 700;
    text-align: center;
    padding: 16px;
    border-radius: 12px;
    margin-top: 24px;
    letter-spacing: 0.04em;
  }

  .screen-b .divider {
    display: flex; align-items: center; gap: 10px;
    margin-top: 20px;
  }

  .screen-b .divider-line {
    flex: 1; height: 1px; background: #302e2b;
  }

  .screen-b .divider-text {
    color: #5a554f; font-size: 11px;
    letter-spacing: 0.06em; text-transform: uppercase;
  }

  .screen-b .btn-google {
    border: 1px solid #302e2b;
    background: #222120;
    color: #e8e4dc;
    font-size: 14px;
    font-weight: 600;
    text-align: center;
    padding: 14px;
    border-radius: 12px;
    margin-top: 12px;
    display: flex; align-items: center; justify-content: center; gap: 8px;
  }

  .screen-b .google-dot {
    width: 16px; height: 16px; border-radius: 50%;
    background: conic-gradient(#4285F4 90deg, #EA4335 90deg 180deg, #FBBC05 180deg 270deg, #34A853 270deg);
  }

  .screen-b .register-link {
    color: #8a8278;
    font-size: 12px;
    text-align: center;
    margin-top: auto;
  }

  .screen-b .register-link span {
    color: #d97a56;
  }

  /* ── Token table ── */
  .token-table {
    display: flex;
    gap: 48px;
    margin-top: 32px;
  }

  .token-col {
    width: 320px;
  }

  .token-col table {
    width: 100%;
    border-collapse: collapse;
    font-size: 11px;
  }

  .token-col th {
    color: #666;
    text-align: left;
    padding: 4px 8px;
    border-bottom: 1px solid #222;
    font-weight: 600;
    letter-spacing: 0.06em;
    text-transform: uppercase;
  }

  .token-col td {
    padding: 5px 8px;
    color: #aaa;
    border-bottom: 1px solid #1a1a1a;
  }

  .token-col td:first-child {
    color: #666;
    width: 45%;
  }

  .swatch {
    display: inline-block;
    width: 12px; height: 12px;
    border-radius: 3px;
    vertical-align: middle;
    margin-right: 6px;
    border: 1px solid rgba(255,255,255,0.1);
  }

  @media (max-width: 780px) {
    .compare-row, .token-table { flex-direction: column; gap: 32px; }
  }
</style>
<link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700&display=swap" rel="stylesheet">
</head>
<body>

<h1>Nightfall — Comparatif palettes</h1>
<p class="subtitle">Écran Login · même layout, deux identités visuelles</p>

<div class="compare-row">

  <!-- ── PALETTE A : Tech / Froid ── -->
  <div class="column">
    <div class="label label-a">A — Fork Nightfall<br>Tech / Froid</div>
    <div class="palette-chips">
      <div class="chip" style="background:#191E22" title="#191E22"></div>
      <div class="chip" style="background:#232E32" title="#232E32"></div>
      <div class="chip" style="background:#D37C04" title="#D37C04"></div>
      <div class="chip" style="background:#07BCD3" title="#07BCD3"></div>
      <div class="chip" style="background:#0E9EB0" title="#0E9EB0"></div>
    </div>
    <div class="phone-frame">
      <div class="screen screen-a">
        <!-- Logo -->
        <div class="logo-ring"><span class="logo-icon">◐</span></div>
        <div class="app-name">Nightfall</div>
        <div class="app-tagline">Sleep analytics</div>

        <!-- Form -->
        <div style="margin-top:28px;">
          <div class="field-label">Email</div>
          <div class="field"><span class="field-placeholder">vous@exemple.fr</span></div>

          <div class="field-label">Mot de passe</div>
          <div class="field"><span class="field-placeholder">••••••••••</span></div>

          <div class="forgot">Mot de passe oublié ?</div>

          <div class="btn-primary">Se connecter</div>

          <div class="divider">
            <div class="divider-line"></div>
            <div class="divider-text">ou</div>
            <div class="divider-line"></div>
          </div>

          <div class="btn-google">
            <div class="google-dot"></div>
            Continuer avec Google
          </div>
        </div>

        <div class="register-link">Pas encore de compte ? <span>Créer un compte</span></div>
      </div>
    </div>
  </div>

  <!-- ── PALETTE B : Rust / Chaud DataSaillance ── -->
  <div class="column">
    <div class="label label-b">B — DataSaillance<br>Rust / Chaud</div>
    <div class="palette-chips">
      <div class="chip" style="background:#1a1917" title="#1a1917"></div>
      <div class="chip" style="background:#222120" title="#222120"></div>
      <div class="chip" style="background:#c96442" title="#c96442"></div>
      <div class="chip" style="background:#d97a56" title="#d97a56"></div>
      <div class="chip" style="background:#e8e4dc" title="#e8e4dc"></div>
    </div>
    <div class="phone-frame">
      <div class="screen screen-b">
        <!-- Logo -->
        <div class="logo-ring"><span class="logo-icon">◐</span></div>
        <div class="app-name">Nightfall</div>
        <div class="app-tagline">Sleep analytics</div>

        <!-- Form -->
        <div style="margin-top:28px;">
          <div class="field-label">Email</div>
          <div class="field"><span class="field-placeholder">vous@exemple.fr</span></div>

          <div class="field-label">Mot de passe</div>
          <div class="field"><span class="field-placeholder">••••••••••</span></div>

          <div class="forgot">Mot de passe oublié ?</div>

          <div class="btn-primary">Se connecter</div>

          <div class="divider">
            <div class="divider-line"></div>
            <div class="divider-text">ou</div>
            <div class="divider-line"></div>
          </div>

          <div class="btn-google">
            <div class="google-dot"></div>
            Continuer avec Google
          </div>
        </div>

        <div class="register-link">Pas encore de compte ? <span>Créer un compte</span></div>
      </div>
    </div>
  </div>

</div>

<!-- Token comparison table -->
<div class="token-table">
  <div class="token-col">
    <table>
      <tr>
        <th colspan="2">A — Tokens Nightfall Fork</th>
      </tr>
      <tr><td>Background</td><td><span class="swatch" style="background:#191E22"></span>#191E22</td></tr>
      <tr><td>Surface</td><td><span class="swatch" style="background:#232E32"></span>#232E32</td></tr>
      <tr><td>Accent / CTA</td><td><span class="swatch" style="background:#D37C04"></span>#D37C04</td></tr>
      <tr><td>Accent cool</td><td><span class="swatch" style="background:#07BCD3"></span>#07BCD3</td></tr>
      <tr><td>Accent teal</td><td><span class="swatch" style="background:#0E9EB0"></span>#0E9EB0</td></tr>
      <tr><td>Text primary</td><td><span class="swatch" style="background:#E8EFF2"></span>#E8EFF2</td></tr>
      <tr><td>Text muted</td><td><span class="swatch" style="background:#7A9AAA"></span>#7A9AAA</td></tr>
      <tr><td>Border</td><td><span class="swatch" style="background:#2E3D44"></span>#2E3D44</td></tr>
    </table>
  </div>
  <div class="token-col">
    <table>
      <tr>
        <th colspan="2">B — Tokens DataSaillance (index.css)</th>
      </tr>
      <tr><td>Background</td><td><span class="swatch" style="background:#1a1917"></span>#1a1917</td></tr>
      <tr><td>Surface</td><td><span class="swatch" style="background:#222120"></span>#222120</td></tr>
      <tr><td>Accent / CTA</td><td><span class="swatch" style="background:#c96442"></span>#c96442 (light)</td></tr>
      <tr><td>Accent dark</td><td><span class="swatch" style="background:#d97a56"></span>#d97a56 (dark)</td></tr>
      <tr><td>–</td><td style="color:#444">pas de cyan / teal</td></tr>
      <tr><td>Text primary</td><td><span class="swatch" style="background:#e8e4dc"></span>#e8e4dc</td></tr>
      <tr><td>Text muted</td><td><span class="swatch" style="background:#8a8278"></span>#8a8278</td></tr>
      <tr><td>Border</td><td><span class="swatch" style="background:#302e2b"></span>#302e2b</td></tr>
    </table>
  </div>
</div>

</body>
</html>
```

---

## Appendix — symbols & navigation *(auto)*
