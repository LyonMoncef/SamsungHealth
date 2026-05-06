---
type: code-source
language: html
file_path: docs/vault/assets/mockups/p4/p4-05-viz-timeline.html
git_blob: 13149dbc8defb07935e5dd1d8e185f6491a92f0d
last_synced: '2026-05-06T10:22:17Z'
loc: 403
annotations: []
imports: []
exports: []
tags:
- code
- html
---

# docs/vault/assets/mockups/p4/p4-05-viz-timeline.html

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`docs/vault/assets/mockups/p4/p4-05-viz-timeline.html`](../../../docs/vault/assets/mockups/p4/p4-05-viz-timeline.html).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```html
<!doctype html>
<!-- Nightfall · Phase 4 · Viz 1 — Stacked Sleep Timeline
     Montre la dérive circadienne Non-24 sur 14 nuits.
     Axe X = 24h (étendu à 30h pour capturer les débordements minuit).
     Axe Y = nuits (la plus récente en bas, la plus ancienne en haut).
     DataSaillance dark tokens · OD mobile-app skill · Archetype F adapted -->
<html lang="fr">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Nightfall · Stacked Timeline</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg:          #191E22;
      --surface:     #232E32;
      --fg:          #F5F5F5;
      --muted:       #9AA0A5;
      --border:      rgba(7,188,211,0.15);
      --accent:      #D37C04;
      --accent-cool: #07BCD3;
      --accent-teal: #0E9EB0;
      --font-display:'Playfair Display', Georgia, serif;
      --font-body:   'Inter', -apple-system, system-ui, sans-serif;
      --font-mono:   ui-monospace,'SF Mono',Menlo,monospace;
      --fs-h1:26px; --fs-h2:20px; --fs-body:15px; --fs-meta:12px;
      --radius-card:18px;

      /* Sleep stage colors */
      --stage-light: rgba(14,158,176,0.55);
      --stage-deep:  rgba(14,158,176,0.95);
      --stage-rem:   rgba(211,124,4,0.85);
      --stage-awake: rgba(130,133,135,0.35);
    }
    *,*::before,*::after{box-sizing:border-box;}
    html,body{margin:0;padding:0;height:100%;}
    body{
      background:radial-gradient(60% 80% at 50% 0%,rgba(14,158,176,0.07) 0%,#191E22 60%);
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
    .device::before,.device::after{
      content:'';position:absolute;width:3px;
      background:linear-gradient(to bottom,transparent 0%,rgba(255,255,255,.06) 8%,transparent 16%,transparent 84%,rgba(255,255,255,.04) 92%,transparent 100%);
      top:100px;bottom:100px;pointer-events:none;
    }
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

    /* ─── Timeline chart ─────────────────────────────────────────── */
    .timeline-wrap{
      background:var(--surface);border:1px solid var(--border);
      border-radius:16px;padding:14px 12px 12px;margin-inline:16px;
    }
    .tl-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;}
    .tl-axis{
      display:flex;align-items:center;gap:0;
      padding-left:28px; /* room for day labels */
      margin-bottom:6px;
    }
    .tl-axis span{
      flex:1;font-family:var(--font-mono);font-size:9px;color:var(--muted);
      text-align:center;
    }
    .tl-row{
      display:flex;align-items:center;gap:6px;height:18px;margin-bottom:3px;
    }
    .tl-label{
      width:28px;font-family:var(--font-mono);font-size:9px;color:var(--muted);
      text-align:right;flex-shrink:0;
    }
    .tl-label.today{color:var(--accent);}
    .tl-track{
      flex:1;height:100%;position:relative;
      background:rgba(245,245,245,0.03);border-radius:3px;overflow:hidden;
    }
    /* sleep segment: left/width expressed as % of the 30h window (0–30h) */
    .seg{position:absolute;top:0;height:100%;border-radius:2px;}
    .seg.light{background:var(--stage-light);}
    .seg.deep {background:var(--stage-deep);}
    .seg.rem  {background:var(--stage-rem);}
    .seg.awake{background:var(--stage-awake);}
    /* midnight marker */
    .midnight{position:absolute;top:0;left:80%; /* 24/30 = 80% */ height:100%;width:1px;background:rgba(7,188,211,0.25);pointer-events:none;}

    /* ─── Insight card ────────────────────────────────────────────── */
    .insight-card{
      background:rgba(211,124,4,0.08);border:1px solid rgba(211,124,4,0.25);
      border-radius:14px;padding:12px 14px;margin-inline:16px;
      display:flex;gap:10px;align-items:flex-start;
    }
    .insight-icon{flex-shrink:0;margin-top:1px;}

    /* ─── Stats row ───────────────────────────────────────────────── */
    .stat-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-inline:16px;}
    .stat-card{background:var(--surface);border:1px solid var(--border);border-radius:14px;padding:12px;}
  </style>
</head>
<body>
<div class="stage-wrap">
  <div class="caption"><strong>Nightfall</strong> · Stacked Sleep Timeline · Viz P4-05 — dérive Non-24 visible</div>

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
            <p class="meta" style="margin:0 0 2px;">SOMMEIL · 14 NUITS</p>
            <h1 style="margin:0;font-family:var(--font-display);font-size:var(--fs-h1);letter-spacing:-.02em;line-height:1.1;">Timeline</h1>
          </div>
          <!-- Range selector -->
          <div style="display:flex;gap:4px;">
            <span style="padding:4px 9px;background:var(--accent);color:#fff;border-radius:999px;font-size:11px;font-family:var(--font-mono);">14j</span>
            <span style="padding:4px 9px;background:var(--surface);border:1px solid var(--border);color:var(--muted);border-radius:999px;font-size:11px;font-family:var(--font-mono);">30j</span>
            <span style="padding:4px 9px;background:var(--surface);border:1px solid var(--border);color:var(--muted);border-radius:999px;font-size:11px;font-family:var(--font-mono);">90j</span>
          </div>
        </div>

        <!-- Threshold alert — algorithmic detection only, no LLM prose -->
        <div style="margin:6px 16px 10px;">
          <div style="background:rgba(211,124,4,0.08);border:1px solid rgba(211,124,4,0.3);border-radius:12px;padding:10px 12px;display:flex;align-items:center;justify-content:space-between;">
            <div style="display:flex;gap:8px;align-items:center;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.7"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              <span style="font-size:11px;font-family:var(--font-mono);color:var(--accent);">SEUIL DÉRIVE &gt; 30 MIN/NUIT · 13J</span>
            </div>
            <span style="font-size:10px;font-family:var(--font-mono);color:var(--muted);background:rgba(211,124,4,0.15);padding:2px 7px;border-radius:999px;">+43 min/nuit</span>
          </div>
        </div>

        <!-- Timeline chart -->
        <div class="timeline-wrap" data-od-id="timeline">
          <div class="tl-header">
            <span class="meta">HEURE D'ENDORMISSEMENT</span>
            <span class="meta" style="color:var(--accent-cool);">30h fenêtre</span>
          </div>

          <!-- X-axis labels (every 6h over 30h window: 12h, 18h, 0h, 6h, 12h, 18h) -->
          <div class="tl-axis">
            <span>12h</span>
            <span>18h</span>
            <span style="color:rgba(7,188,211,0.6)">0h</span>
            <span>6h</span>
            <span>12h</span>
            <span>18h</span>
          </div>

          <!--
            Fenêtre : 12h00 → 42h00 (= 18h00 lendemain), soit 30h affichées.
            Endormissement typique Non-24 : glisse d'environ +40min/nuit.
            Nuit 1 (il y a 13 nuits) : endorm. 22h00, réveil 05h00 (7h)
            Nuit 14 (hier)          : endorm. 03h20, réveil 09h40 (6h20)
            Toutes les positions en % de la fenêtre 30h (12h00 = 0%).
            Formule : (heure_sur_30h_echelle - 12) / 30 * 100
            Exemples :
              22h00 = (22-12)/30*100 = 33.3%
              05h00 = (5+24-12)/30*100 = 56.7% (lendemain → +24h)
              03h20 = (27.33-12)/30*100 = 51.1%
              09h40 = (33.67-12)/30*100 = 72.2%
          -->

          <!-- Nuit 1 — il y a 13j : 22h00–05h00 -->
          <div class="tl-row">
            <span class="tl-label">-13</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg deep"  style="left:33.3%;width:10%"></div>
              <div class="seg light" style="left:43.3%;width:9%"></div>
              <div class="seg rem"   style="left:52.3%;width:4.4%"></div>
            </div>
          </div>

          <!-- Nuit 2 — il y a 12j : 22h40–05h40 (+40min) -->
          <div class="tl-row">
            <span class="tl-label">-12</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg deep"  style="left:35.5%;width:9%"></div>
              <div class="seg light" style="left:44.5%;width:9.5%"></div>
              <div class="seg rem"   style="left:54%;width:4.4%"></div>
            </div>
          </div>

          <!-- Nuit 3 : 23h20–06h00 -->
          <div class="tl-row">
            <span class="tl-label">-11</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg deep"  style="left:37.8%;width:9%"></div>
              <div class="seg light" style="left:46.8%;width:8.5%"></div>
              <div class="seg rem"   style="left:55.3%;width:4.4%"></div>
            </div>
          </div>

          <!-- Nuit 4 : 00h00–06h40 -->
          <div class="tl-row">
            <span class="tl-label">-10</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg deep"  style="left:40%;width:9%"></div>
              <div class="seg light" style="left:49%;width:8%"></div>
              <div class="seg rem"   style="left:57%;width:5.6%"></div>
            </div>
          </div>

          <!-- Nuit 5 : 00h40–07h00 -->
          <div class="tl-row">
            <span class="tl-label">-9</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg light" style="left:42.2%;width:4%"></div>
              <div class="seg deep"  style="left:46.2%;width:9%"></div>
              <div class="seg light" style="left:55.2%;width:4.4%"></div>
            </div>
          </div>

          <!-- Nuit 6 : 01h30–08h00 -->
          <div class="tl-row">
            <span class="tl-label">-8</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg deep"  style="left:45%;width:9.5%"></div>
              <div class="seg light" style="left:54.5%;width:7%"></div>
              <div class="seg rem"   style="left:61.5%;width:4.5%"></div>
            </div>
          </div>

          <!-- Nuit 7 : 02h10–08h40 -->
          <div class="tl-row">
            <span class="tl-label">-7</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg deep"  style="left:47.2%;width:9.5%"></div>
              <div class="seg light" style="left:56.7%;width:7.5%"></div>
              <div class="seg rem"   style="left:64.2%;width:3.5%"></div>
            </div>
          </div>

          <!-- Nuit 8 : 02h50–09h10 (6h20) -->
          <div class="tl-row">
            <span class="tl-label">-6</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg deep"  style="left:49.4%;width:8.5%"></div>
              <div class="seg light" style="left:57.9%;width:8%"></div>
              <div class="seg rem"   style="left:65.9%;width:4.4%"></div>
            </div>
          </div>

          <!-- Nuit 9 : 03h20 (short — 4h seulement, insomnie) -->
          <div class="tl-row">
            <span class="tl-label">-5</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg awake" style="left:51.1%;width:2%"></div>
              <div class="seg light" style="left:53.1%;width:6%"></div>
              <div class="seg deep"  style="left:59.1%;width:5.5%"></div>
            </div>
          </div>

          <!-- Nuit 10 : 03h40–10h00 -->
          <div class="tl-row">
            <span class="tl-label">-4</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg deep"  style="left:52.2%;width:10%"></div>
              <div class="seg light" style="left:62.2%;width:5.5%"></div>
              <div class="seg rem"   style="left:67.7%;width:5.6%"></div>
            </div>
          </div>

          <!-- Nuit 11 : 03h40–10h20 -->
          <div class="tl-row">
            <span class="tl-label">-3</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg deep"  style="left:52.2%;width:10.5%"></div>
              <div class="seg light" style="left:62.7%;width:7%"></div>
            </div>
          </div>

          <!-- Nuit 12 : 04h10–10h40 -->
          <div class="tl-row">
            <span class="tl-label">-2</span>
            <div class="tl-track">
              <div class="midnight"></div>
              <div class="seg light" style="left:54.4%;width:4%"></div>
              <div class="seg deep"  style="left:58.4%;width:7.5%"></div>
              <div class="seg rem"   style="left:65.9%;width:4.4%"></div>
            </div>
          </div>

          <!-- Nuit 13 (hier) : 04h30–11h00 — selected -->
          <div class="tl-row" style="outline:1px solid rgba(211,124,4,0.4);border-radius:4px;outline-offset:1px;">
            <span class="tl-label today">-1</span>
            <div class="tl-track" style="background:rgba(211,124,4,0.05);">
              <div class="midnight"></div>
              <div class="seg deep"  style="left:55%;width:9.5%;opacity:1"></div>
              <div class="seg light" style="left:64.5%;width:6.5%;opacity:1"></div>
              <div class="seg rem"   style="left:71%;width:5.6%;opacity:1"></div>
            </div>
          </div>

          <!-- Légende -->
          <div style="display:flex;gap:10px;margin-top:10px;flex-wrap:wrap;">
            <span style="display:flex;align-items:center;gap:4px;font-size:10px;color:var(--muted);font-family:var(--font-mono);">
              <span style="width:10px;height:10px;border-radius:2px;background:var(--stage-deep);display:inline-block;"></span>Profond
            </span>
            <span style="display:flex;align-items:center;gap:4px;font-size:10px;color:var(--muted);font-family:var(--font-mono);">
              <span style="width:10px;height:10px;border-radius:2px;background:var(--stage-light);display:inline-block;"></span>Léger
            </span>
            <span style="display:flex;align-items:center;gap:4px;font-size:10px;color:var(--muted);font-family:var(--font-mono);">
              <span style="width:10px;height:10px;border-radius:2px;background:var(--stage-rem);display:inline-block;"></span>REM
            </span>
            <span style="display:flex;align-items:center;gap:4px;font-size:10px;color:var(--muted);font-family:var(--font-mono);">
              <span style="width:1px;height:10px;background:rgba(7,188,211,0.4);display:inline-block;"></span>Minuit
            </span>
          </div>
        </div>

        <!-- Computed metrics — regression linéaire sur timestamps endormissement -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin:12px 16px 0;" data-od-id="drift-metrics">
          <div style="background:var(--surface);border:1px solid rgba(211,124,4,0.3);border-radius:14px;padding:12px;">
            <p class="meta" style="margin:0 0 4px;">DÉRIVE MOY.</p>
            <div class="num" style="font-size:22px;color:var(--accent);">+43 min</div>
            <p style="margin:2px 0 0;font-size:11px;color:var(--muted);">/ nuit · linreg 13j</p>
          </div>
          <div style="background:var(--surface);border:1px solid var(--border);border-radius:14px;padding:12px;">
            <p class="meta" style="margin:0 0 4px;">GLISSEMENT TOTAL</p>
            <div class="num" style="font-size:22px;color:var(--accent-teal);">+9h17</div>
            <p style="margin:2px 0 0;font-size:11px;color:var(--muted);">22h00 → 04h30</p>
          </div>
        </div>

        <!-- Stats row -->
        <div class="stat-grid" style="margin-top:12px;" data-od-id="stats">
          <div class="stat-card">
            <p class="meta" style="margin:0 0 4px;">DURÉE MOY.</p>
            <div class="num" style="font-size:22px;color:var(--accent-teal);">5h47</div>
            <p style="margin:2px 0 0;font-size:11px;color:var(--muted);">−2h13 vs cible 8h</p>
          </div>
          <div class="stat-card">
            <p class="meta" style="margin:0 0 4px;">GLISSEMENT</p>
            <div class="num" style="font-size:22px;color:var(--accent);">+9h17</div>
            <p style="margin:2px 0 0;font-size:11px;color:var(--muted);">en 13 nuits</p>
          </div>
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
