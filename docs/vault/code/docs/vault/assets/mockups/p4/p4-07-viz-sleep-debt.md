---
type: code-source
language: html
file_path: docs/vault/assets/mockups/p4/p4-07-viz-sleep-debt.html
git_blob: 08cd85d73d1da83be2887f4c3d2680a29c6f2fc7
last_synced: '2026-05-06T10:22:17Z'
loc: 298
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# docs/vault/assets/mockups/p4/p4-07-viz-sleep-debt.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`docs/vault/assets/mockups/p4/p4-07-viz-sleep-debt.html`](../../../docs/vault/assets/mockups/p4/p4-07-viz-sleep-debt.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!doctype html>
<!-- Nightfall · Phase 4 · Viz 3 — Sleep Debt
     Dette de sommeil cumulée sur 30 nuits. Cible = 8h/nuit.
     Barre par nuit = écart (positif = surplus rare, négatif = déficit).
     Ligne cumulative en SVG montrant l'accumulation.
     DataSaillance dark tokens · OD mobile-app skill · Archetype F adapted -->
<html lang="fr">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Nightfall · Sleep Debt</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg:#191E22;--surface:#232E32;--fg:#F5F5F5;--muted:#9AA0A5;
      --border:rgba(7,188,211,0.15);--accent:#D37C04;--accent-cool:#07BCD3;--accent-teal:#0E9EB0;
      --font-display:'Playfair Display',Georgia,serif;
      --font-body:'Inter',-apple-system,system-ui,sans-serif;
      --font-mono:ui-monospace,'SF Mono',Menlo,monospace;
      --fs-h1:26px;--fs-body:15px;--fs-meta:12px;--radius-card:18px;
    }
    *,*::before,*::after{box-sizing:border-box;}
    html,body{margin:0;padding:0;height:100%;}
    body{
      background:radial-gradient(60% 80% at 50% 0%,rgba(211,124,4,0.07) 0%,#191E22 60%);
      color:var(--fg);font-family:var(--font-body);font-size:var(--fs-body);
      line-height:1.4;-webkit-font-smoothing:antialiased;
      display:grid;place-items:center;padding:32px;
    }
    .stage-wrap{display:flex;flex-direction:column;align-items:center;gap:24px;}
    .caption{font-family:var(--font-mono);font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:var(--muted);}
    .caption strong{color:var(--fg);font-weight:500;}

    .device{
      position:relative;width:390px;height:844px;border-radius:56px;padding:12px;
      background:linear-gradient(160deg,#2a2a2c 0%,#1a1a1c 50%,#0e0e10 100%);
      box-shadow:0 0 0 1px rgba(255,255,255,.04) inset,0 0 0 2px #000 inset,
        0 28px 60px -12px rgba(0,0,0,.55),0 8px 20px -8px rgba(0,0,0,.45);
      isolation:isolate;
    }
    .device::before,.device::after{content:'';position:absolute;width:3px;
      background:linear-gradient(to bottom,transparent 0%,rgba(255,255,255,.06) 8%,transparent 16%,transparent 84%,rgba(255,255,255,.04) 92%,transparent 100%);
      top:100px;bottom:100px;pointer-events:none;}
    .device::before{left:-1px;}.device::after{right:-1px;}
    .island{position:absolute;top:22px;left:50%;transform:translateX(-50%);width:124px;height:36px;background:#000;border-radius:999px;z-index:5;}
    .btn-rail{position:absolute;width:4px;background:#0a0a0c;border-radius:2px;}
    .btn-rail.left-1{left:-3px;top:174px;height:32px;}
    .btn-rail.left-2{left:-3px;top:220px;height:60px;}
    .btn-rail.left-3{left:-3px;top:290px;height:60px;}
    .btn-rail.right-1{right:-3px;top:250px;height:100px;}

    .screen{position:relative;width:100%;height:100%;background:var(--bg);border-radius:44px;overflow:hidden;display:flex;flex-direction:column;}
    .statusbar{flex:0 0 47px;padding:18px 26px 0;display:flex;align-items:flex-start;justify-content:space-between;font-family:var(--font-body);font-size:15px;font-weight:600;color:var(--fg);letter-spacing:-.01em;}
    .statusbar .right{display:inline-flex;align-items:center;gap:6px;}
    .statusbar svg{width:17px;height:11px;fill:var(--fg);}
    .statusbar .battery{width:25px;}
    .content{flex:1 1 auto;overflow-y:auto;overflow-x:hidden;padding:4px 0 28px;}
    .content::-webkit-scrollbar{display:none;}
    .home-indicator{flex:0 0 28px;position:relative;}
    .home-indicator::after{content:'';position:absolute;left:50%;bottom:8px;transform:translateX(-50%);width:134px;height:5px;background:var(--fg);border-radius:999px;opacity:.35;}

    .pad{padding-inline:16px;}
    .meta{font-family:var(--font-mono);font-size:var(--fs-meta);color:var(--muted);}
    .num{font-family:var(--font-mono);font-variant-numeric:tabular-nums;}
  </style>
</head>
<body>
<div class="stage-wrap">
  <div class="caption"><strong>Nightfall</strong> · Sleep Debt · Viz P4-07 — 30 nuits · cible 8h</div>

  <div class="device" data-od-id="device">
    <span class="btn-rail left-1" aria-hidden></span>
    <span class="btn-rail left-2" aria-hidden></span>
    <span class="btn-rail left-3" aria-hidden></span>
    <span class="btn-rail right-1" aria-hidden></span>
    <span class="island" aria-hidden></span>

    <div class="screen">
      <div class="statusbar">
        <span style="font-family:var(--font-mono)">9:41</span>
        <span class="right">
          <svg viewBox="0 0 17 11" aria-hidden><rect x="0" y="7" width="3" height="4" rx=".6"/><rect x="4" y="5" width="3" height="6" rx=".6"/><rect x="8" y="3" width="3" height="8" rx=".6"/><rect x="12" y="0" width="3" height="11" rx=".6"/></svg>
          <svg viewBox="0 0 17 11" aria-hidden><path d="M8.5 1.5C5.5 1.5 2.7 2.6 0.5 4.6L2 6.1C3.8 4.5 6.1 3.6 8.5 3.6c2.4 0 4.7.9 6.5 2.5l1.5-1.5c-2.2-2-5-3.1-8-3.1zM3.5 7.6L5 9.1c1-.9 2.2-1.4 3.5-1.4 1.3 0 2.5.5 3.5 1.4l1.5-1.5c-1.4-1.3-3.1-2-5-2-1.9 0-3.6.7-5 2zM6.5 10.6l2 2 2-2c-.5-.5-1.2-.8-2-.8s-1.5.3-2 .8z"/></svg>
          <svg class="battery" viewBox="0 0 25 11" aria-hidden><rect x=".5" y=".5" width="21" height="10" rx="2.5" fill="none" stroke="currentColor" stroke-opacity=".45"/><rect x="22" y="3.5" width="1.5" height="4" rx=".4" fill="currentColor" fill-opacity=".45"/><rect x="2" y="2" width="18" height="7" rx="1.4"/></svg>
        </span>
      </div>

      <main class="content" data-od-id="content">

        <!-- Header -->
        <div style="padding:10px 16px 6px;display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="meta" style="margin:0 0 2px;">DETTE DE SOMMEIL</p>
            <h1 style="margin:0;font-family:var(--font-display);font-size:var(--fs-h1);letter-spacing:-.02em;line-height:1.1;">Déficit cumulé</h1>
          </div>
          <div style="display:flex;gap:4px;">
            <span style="padding:4px 9px;background:var(--surface);border:1px solid var(--border);color:var(--muted);border-radius:999px;font-size:11px;font-family:var(--font-mono);">7j</span>
            <span style="padding:4px 9px;background:var(--accent);color:#fff;border-radius:999px;font-size:11px;font-family:var(--font-mono);">30j</span>
          </div>
        </div>

        <!-- Hero KPI -->
        <div style="margin:8px 16px 12px;background:var(--surface);border:1px solid rgba(211,124,4,0.35);border-radius:16px;padding:16px;">
          <div style="display:flex;align-items:flex-start;justify-content:space-between;">
            <div>
              <p class="meta" style="margin:0 0 4px;">DETTE TOTALE 30j</p>
              <div class="num" style="font-size:42px;letter-spacing:-.03em;line-height:1;color:var(--accent);">−66h</div>
              <p style="margin:4px 0 0;font-size:12px;color:var(--muted);">soit 2h12 de déficit / nuit en moy.</p>
            </div>
            <div style="text-align:right;">
              <p class="meta" style="margin:0 0 4px;">CIBLE</p>
              <div class="num" style="font-size:20px;color:var(--muted);">8h</div>
              <p style="margin:2px 0 0;font-size:11px;color:var(--muted);">/ nuit</p>
            </div>
          </div>
          <!-- mini progress toward catastrophe -->
          <div style="margin-top:12px;">
            <div style="display:flex;justify-content:space-between;font-size:10px;font-family:var(--font-mono);color:var(--muted);margin-bottom:4px;">
              <span>Mois précédent −42h</span><span>−66h</span>
            </div>
            <div style="height:5px;background:rgba(245,245,245,0.07);border-radius:999px;overflow:hidden;">
              <div style="height:100%;width:82%;background:linear-gradient(to right,rgba(211,124,4,0.6),#D37C04);border-radius:999px;"></div>
            </div>
          </div>
        </div>

        <!-- Bar chart: écart journalier + courbe cumulative -->
        <div style="background:var(--surface);border:1px solid var(--border);border-radius:16px;padding:14px 12px;margin-inline:16px;" data-od-id="chart">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;">
            <span class="meta">ÉCART / NUIT (cible 8h)</span>
            <div style="display:flex;gap:8px;align-items:center;">
              <span style="display:flex;align-items:center;gap:4px;font-size:10px;color:var(--muted);font-family:var(--font-mono);">
                <span style="width:8px;height:8px;border-radius:2px;background:rgba(211,124,4,0.7);display:inline-block;"></span>déficit</span>
              <span style="display:flex;align-items:center;gap:4px;font-size:10px;color:var(--muted);font-family:var(--font-mono);">
                <span style="width:8px;height:8px;border-radius:2px;background:rgba(14,158,176,0.7);display:inline-block;"></span>surplus</span>
            </div>
          </div>

          <!--
            SVG chart: 30 bars (daily delta) + cumulative line.
            Chart area: 330×110px. Bar width ≈ 9px, gap 2px.
            Y axis: ±4h range. Center = 55px from top (0h line).
            Scale: 4h = 55px → 1h = 13.75px
            Bars go down (deficit = orange) or up (surplus = teal, rare).

            30 nights of data (typical Non-24 + short sleep):
            Durations (h): 7.0, 6.3, 5.5, 6.8, 7.5, 4.0, 6.2, 5.8, 6.5, 7.0,
                           5.5, 6.0, 4.3, 5.8, 6.7, 6.2, 5.0, 7.8, 6.5, 5.5,
                           4.5, 6.3, 5.8, 6.7, 4.0, 6.8, 6.3, 5.5, 5.8, 6.3
            Deltas (8h-dur): 1, 1.7, 2.5, 1.2, 0.5, 4, 1.8, 2.2, 1.5, 1,
                              2.5, 2, 3.7, 2.2, 1.3, 1.8, 3, 0.2, 1.5, 2.5,
                              3.5, 1.7, 2.2, 1.3, 4, 1.2, 1.7, 2.5, 2.2, 1.7
            Most are negative (deficit). Night 18 (7.8h) nearly meets target.

            Bar x positions: 14 + i*(9+2) = 14, 25, 36, 47...
            Bar height (deficit): delta_h * 13.75px, drawn downward from y=55.
            Surplus bar (night 18, delta=0.2): tiny bar upward.
          -->
          <svg viewBox="0 0 330 130" width="100%" aria-label="30-day sleep debt bar chart">
            <!-- Zero line (8h target) -->
            <line x1="0" y1="50" x2="330" y2="50" stroke="rgba(7,188,211,0.25)" stroke-width="1" stroke-dasharray="3,3"/>
            <text x="2" y="48" fill="rgba(7,188,211,0.5)" font-size="7" font-family="ui-monospace,monospace">0</text>
            <!-- -2h line -->
            <line x1="0" y1="77.5" x2="330" y2="77.5" stroke="rgba(245,245,245,0.04)" stroke-width="1"/>
            <text x="2" y="76" fill="rgba(130,133,135,0.4)" font-size="7" font-family="ui-monospace,monospace">-2h</text>
            <!-- -4h line -->
            <line x1="0" y1="105" x2="330" y2="105" stroke="rgba(245,245,245,0.04)" stroke-width="1"/>
            <text x="2" y="104" fill="rgba(130,133,135,0.4)" font-size="7" font-family="ui-monospace,monospace">-4h</text>

            <!-- Bars (x starts at 14, step 11) -->
            <!-- N1  delta=-1h    h=13.75  y=50 → 50+13.75=63.75 -->
            <rect x="14"  y="50" width="9" height="13.75" fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="25"  y="50" width="9" height="23.375" fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="36"  y="50" width="9" height="34.375" fill="rgba(211,124,4,0.85)" rx="1.5"/>
            <rect x="47"  y="50" width="9" height="16.5"   fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="58"  y="50" width="9" height="6.875"  fill="rgba(211,124,4,0.6)"  rx="1.5"/>
            <!-- N6 delta=-4h biggest -->
            <rect x="69"  y="50" width="9" height="55"     fill="rgba(211,124,4,0.95)" rx="1.5"/>
            <rect x="80"  y="50" width="9" height="24.75"  fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="91"  y="50" width="9" height="30.25"  fill="rgba(211,124,4,0.8)"  rx="1.5"/>
            <rect x="102" y="50" width="9" height="20.625" fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="113" y="50" width="9" height="13.75"  fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="124" y="50" width="9" height="34.375" fill="rgba(211,124,4,0.85)" rx="1.5"/>
            <rect x="135" y="50" width="9" height="27.5"   fill="rgba(211,124,4,0.8)"  rx="1.5"/>
            <rect x="146" y="50" width="9" height="50.875" fill="rgba(211,124,4,0.9)"  rx="1.5"/>
            <rect x="157" y="50" width="9" height="30.25"  fill="rgba(211,124,4,0.8)"  rx="1.5"/>
            <rect x="168" y="50" width="9" height="17.875" fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="179" y="50" width="9" height="24.75"  fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="190" y="50" width="9" height="41.25"  fill="rgba(211,124,4,0.85)" rx="1.5"/>
            <!-- N18 delta=-0.2h nearly on target — teal -->
            <rect x="201" y="47.25" width="9" height="2.75" fill="rgba(14,158,176,0.8)" rx="1.5"/>
            <rect x="212" y="50" width="9" height="20.625" fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="223" y="50" width="9" height="34.375" fill="rgba(211,124,4,0.85)" rx="1.5"/>
            <rect x="234" y="50" width="9" height="48.125" fill="rgba(211,124,4,0.9)"  rx="1.5"/>
            <rect x="245" y="50" width="9" height="23.375" fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="256" y="50" width="9" height="30.25"  fill="rgba(211,124,4,0.8)"  rx="1.5"/>
            <rect x="267" y="50" width="9" height="17.875" fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <!-- N25 delta=-4h -->
            <rect x="278" y="50" width="9" height="55"     fill="rgba(211,124,4,0.95)" rx="1.5"/>
            <rect x="289" y="50" width="9" height="16.5"   fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="300" y="50" width="9" height="23.375" fill="rgba(211,124,4,0.75)" rx="1.5"/>
            <rect x="311" y="50" width="9" height="34.375" fill="rgba(211,124,4,0.85)" rx="1.5"/>
            <!-- N29 — hier -->
            <rect x="311" y="50" width="9" height="30.25"  fill="#D37C04" rx="1.5" opacity=".6"/>
            <!-- Last bar highlighted -->
            <rect x="322" y="50" width="9" height="23.375" fill="#D37C04" rx="1.5" stroke="#D37C04" stroke-width="1"/>

            <!-- Cumulative line (polyline approximating running total → steadily decreasing) -->
            <polyline
              fill="none"
              stroke="rgba(7,188,211,0.6)"
              stroke-width="1.5"
              stroke-linejoin="round"
              points="
                18,49   29,47.7  40,45.4  51,43.8  62,43.4  73,39.4  84,37.8
                95,35.8 106,34.9 117,33.9 128,31.5 139,29.8 150,26.8 161,25.1
                172,24.2 183,23.0 194,20.4 205,20.7 216,19.8 227,17.8 238,15.0
                249,13.8 260,12.5 271,11.8 282,8.3  293,7.7  304,6.7  315,5.5
                315,5.5  326,4.2
              "
            />
            <!-- Endpoint dot on cumulative line -->
            <circle cx="326" cy="4.2" r="3" fill="var(--accent-cool)"/>
            <text x="295" y="3" fill="rgba(7,188,211,0.8)" font-size="7" font-family="ui-monospace,monospace">cumul −66h</text>

            <!-- X axis labels -->
            <text x="14"  y="126" fill="rgba(130,133,135,0.5)" font-size="7" font-family="ui-monospace,monospace">1</text>
            <text x="113" y="126" fill="rgba(130,133,135,0.5)" font-size="7" font-family="ui-monospace,monospace">10</text>
            <text x="212" y="126" fill="rgba(130,133,135,0.5)" font-size="7" font-family="ui-monospace,monospace">19</text>
            <text x="311" y="126" fill="var(--accent)" font-size="7" font-family="ui-monospace,monospace">30 ←</text>
          </svg>

          <div style="display:flex;align-items:center;gap:6px;margin-top:4px;">
            <div style="width:16px;height:1.5px;background:rgba(7,188,211,0.6);border-radius:1px;"></div>
            <span style="font-size:10px;color:var(--muted);font-family:var(--font-mono);">dette cumulée (courbe)</span>
          </div>
        </div>

        <!-- Weekly breakdown -->
        <div style="margin:12px 16px 0;background:var(--surface);border:1px solid var(--border);border-radius:16px;overflow:hidden;" data-od-id="weekly">
          <div style="padding:12px 14px;border-bottom:1px solid rgba(7,188,211,0.08);">
            <span class="meta">DETTE PAR SEMAINE</span>
          </div>
          <!-- S4 (dernière semaine) -->
          <div style="padding:11px 14px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid rgba(7,188,211,0.06);">
            <div>
              <div style="font-size:13px;font-weight:500;">Semaine 4 <span style="font-size:11px;color:var(--muted);">(cette sem.)</span></div>
              <div style="font-size:11px;color:var(--muted);font-family:var(--font-mono);margin-top:2px;">moy. 5h54 / nuit</div>
            </div>
            <div class="num" style="color:var(--accent);font-size:18px;">−14h42</div>
          </div>
          <div style="padding:11px 14px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid rgba(7,188,211,0.06);">
            <div>
              <div style="font-size:13px;font-weight:500;">Semaine 3</div>
              <div style="font-size:11px;color:var(--muted);font-family:var(--font-mono);margin-top:2px;">moy. 6h08 / nuit</div>
            </div>
            <div class="num" style="color:var(--accent);font-size:18px;">−13h04</div>
          </div>
          <div style="padding:11px 14px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid rgba(7,188,211,0.06);">
            <div>
              <div style="font-size:13px;font-weight:500;">Semaine 2</div>
              <div style="font-size:11px;color:var(--muted);font-family:var(--font-mono);margin-top:2px;">moy. 5h48 / nuit</div>
            </div>
            <div class="num" style="color:var(--accent);font-size:18px;">−15h24</div>
          </div>
          <div style="padding:11px 14px;display:flex;align-items:center;justify-content:space-between;">
            <div>
              <div style="font-size:13px;font-weight:500;">Semaine 1</div>
              <div style="font-size:11px;color:var(--muted);font-family:var(--font-mono);margin-top:2px;">moy. 6h02 / nuit</div>
            </div>
            <div class="num" style="color:var(--accent);font-size:18px;">−13h58</div>
          </div>
        </div>

        <!-- Computed metrics — arithmetic on sleep_debt value -->
        <div style="margin:12px 16px 0;background:var(--surface);border:1px solid var(--border);border-radius:14px;padding:12px 14px;">
          <span class="meta">REMBOURSEMENT (calcul)</span>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:10px;">
            <div>
              <p style="margin:0 0 3px;font-size:11px;color:var(--muted);">à +1h surplus/nuit</p>
              <div class="num" style="font-size:20px;color:var(--muted);">66 nuits</div>
            </div>
            <div>
              <p style="margin:0 0 3px;font-size:11px;color:var(--muted);">à +2h surplus/nuit</p>
              <div class="num" style="font-size:20px;color:var(--accent-teal);">33 nuits</div>
            </div>
          </div>
          <p style="margin:8px 0 0;font-size:10px;color:var(--muted);font-family:var(--font-mono);">dette ÷ surplus_cible — aucune interprétation</p>
        </div>

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
