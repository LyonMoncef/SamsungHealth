(function () {
  let D = window.SleepData;
  const STAGE_COLORS = {
    deep: "oklch(0.48 0.14 275)",
    light: "oklch(0.68 0.14 295)",
    rem: "oklch(0.82 0.13 220)",
    awake: "oklch(0.78 0.15 60)",
  };

  const PREFS = /*EDITMODE-BEGIN*/{
    "focusNightIndex": 29,
    "accent": "violet",
    "showGrain": true
  }/*EDITMODE-END*/;

  try {
    const saved = JSON.parse(localStorage.getItem("sleep-prefs") || "{}");
    Object.assign(PREFS, saved);
  } catch (e) {}

  const savePrefs = () => {
    try { localStorage.setItem("sleep-prefs", JSON.stringify(PREFS)); } catch (e) {}
  };

  const pad = (n) => String(n).padStart(2, "0");
  const fmtHM = (d) => `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  const fmtDateLong = (d) => d.toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" });
  const fmtDay = (d) => d.toLocaleDateString("en-US", { weekday: "short" });
  const hoursMinutes = (ms) => {
    const m = Math.round(ms / 60000);
    return `${Math.floor(m / 60)}h ${pad(m % 60)}m`;
  };

  const app = document.getElementById("app");

  function topbar() {
    return `
      <div class="topbar">
        <div class="brand">
          <div class="brand-moon"></div>
          <div class="brand-text">Nightfall <em>/</em> Samsung Sleep</div>
        </div>
        <div class="top-meta">
          <span><span class="dot"></span>Watch synced · ${fmtHM(new Date())}</span>
          <span>${D.sessions.length} nights observed</span>
        </div>
      </div>
    `;
  }

  function nightArcSVG(session) {
    const w = 520, h = 360, cx = w / 2, by = h - 40, radius = w / 2 - 30;
    const start = session.sleep_start, end = session.sleep_end;
    const total = end - start;
    const pt = (t) => {
      const angle = Math.PI - t * Math.PI;
      return [cx + Math.cos(angle) * radius, by - Math.sin(angle) * radius * 0.75];
    };
    let pathLayers = "";
    for (const st of session.stages) {
      const t0 = (st.stage_start - start) / total;
      const t1 = (st.stage_end - start) / total;
      const steps = Math.max(2, Math.ceil((t1 - t0) * 80));
      const pts = [];
      for (let i = 0; i <= steps; i++) pts.push(pt(t0 + (i / steps) * (t1 - t0)));
      const d = pts.map((p, i) => (i === 0 ? `M${p[0]},${p[1]}` : `L${p[0]},${p[1]}`)).join(" ");
      pathLayers += `<path d="${d}" fill="none" stroke="${STAGE_COLORS[st.stage_type]}" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>`;
    }
    const horizon = `<line x1="20" x2="${w - 20}" y1="${by}" y2="${by}" stroke="rgba(255,255,255,0.12)" stroke-width="1"/>`;
    const startLabel = `<text x="30" y="${by + 24}" class="arc-label">Bedtime</text><text x="30" y="${by + 44}" class="arc-time">${fmtHM(start)}</text>`;
    const endLabel = `<text x="${w - 30}" y="${by + 24}" class="arc-label" text-anchor="end">Wake</text><text x="${w - 30}" y="${by + 44}" class="arc-time" text-anchor="end">${fmtHM(end)}</text>`;
    let stars = "";
    for (let i = 0; i < 18; i++) {
      const x = (i * 37) % (w - 40) + 20;
      const y = Math.max(20, (i * 53) % (by - 80));
      const r = (i % 3 === 0) ? 1.5 : 0.8;
      stars += `<circle cx="${x}" cy="${y}" r="${r}" fill="oklch(0.9 0.02 270)" opacity="${0.3 + (i % 4) * 0.15}"/>`;
    }
    const moon = `<circle cx="${w - 60}" cy="40" r="14" fill="url(#moonGrad)"/>`;
    return `
      <svg viewBox="0 0 ${w} ${h}" preserveAspectRatio="xMidYMid meet">
        <defs>
          <radialGradient id="moonGrad" cx="0.35" cy="0.35">
            <stop offset="0%" stop-color="oklch(0.96 0.02 85)"/>
            <stop offset="70%" stop-color="oklch(0.8 0.04 85)"/>
            <stop offset="100%" stop-color="oklch(0.5 0.03 85)"/>
          </radialGradient>
        </defs>
        ${stars}${moon}${horizon}${pathLayers}${startLabel}${endLabel}
      </svg>
    `;
  }

  function hero() {
    const last = D.sessions[D.sessions.length - 1];
    const dur = hoursMinutes(last.duration_ms);
    const remPct = Math.round((last.totals.rem / last.duration_ms) * 100);
    return `
      <section class="hero">
        <div class="hero-text">
          <span class="eyebrow">${fmtDateLong(last.sleep_end)} · Last night</span>
          <h1>You traced an <em>arc</em><br/>across the night.</h1>
          <p class="lede">${dur} of sleep, through ${last.stages.filter((s)=>s.stage_type==="rem").length} REM cycles. Your body cooled, dreamt, and surfaced — in that order.</p>
          <div class="hero-stats">
            <div class="hero-stat"><div class="k">Score</div><div class="v">${last.score}<small>/100</small></div></div>
            <div class="hero-stat"><div class="k">In bed</div><div class="v">${last.duration_hours.toFixed(1)}<small>h</small></div></div>
            <div class="hero-stat"><div class="k">REM</div><div class="v">${remPct}<small>%</small></div></div>
          </div>
        </div>
        <div class="night-arc">${nightArcSVG(last)}</div>
      </section>
    `;
  }

  const TSTART = 18, TSPAN = 24, TSPAN_MS = TSPAN * 3600000;

  function aggregateByDay(sessions) {
    const map = new Map();
    for (const s of sessions) {
      if (!map.has(s.date_key)) map.set(s.date_key, []);
      map.get(s.date_key).push(s);
    }
    return Array.from(map.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([key, ss]) => ({ date_key: key, date: ss[0].sleep_end, sessions: ss, isEmpty: false }));
  }

  function localKey(d) {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  }

  function buildCalendarDays(sessions) {
    if (!sessions.length) return [];
    const map = new Map();
    for (const s of sessions) {
      if (!map.has(s.date_key)) map.set(s.date_key, []);
      map.get(s.date_key).push(s);
    }
    const days = [];
    const first = new Date(sessions[0].sleep_end); first.setHours(0, 0, 0, 0);
    const last = new Date(); last.setHours(0, 0, 0, 0);
    let cur = new Date(first);
    while (cur <= last) {
      const key = localKey(cur);
      days.push({ date_key: key, date: new Date(cur), sessions: map.get(key) || [], isEmpty: !map.has(key) });
      cur = new Date(cur.getTime() + 86400000);
    }
    return days;
  }

  function buildTimelineRow(day) {
    const wake = new Date(day.date); wake.setHours(0, 0, 0, 0);
    const anchorMs = wake.getTime() - (24 - TSTART) * 3600000;
    let segs = "";
    for (const s of day.sessions) {
      for (const st of s.stages) {
        const left = ((st.stage_start.getTime() - anchorMs) / TSPAN_MS) * 100;
        const width = ((st.stage_end - st.stage_start) / TSPAN_MS) * 100;
        segs += `<div class="stage-seg ${st.stage_type}" style="left:${left.toFixed(2)}%;width:${width.toFixed(2)}%"></div>`;
      }
    }
    const dayNum = wake.getDay();
    const wknd = dayNum === 0 || dayNum === 6;
    const label = `${fmtDay(wake)} ${wake.getDate()}`;
    const totalDur = day.sessions.reduce((a, s) => a + s.duration_ms, 0);
    let tip = fmtDateLong(wake);
    if (day.sessions.length > 1) tip += `\n${day.sessions.length} sessions · ${hoursMinutes(totalDur)} total`;
    else if (day.sessions.length === 1) { const s = day.sessions[0]; tip += `\n${fmtHM(s.sleep_start)} → ${fmtHM(s.sleep_end)} · ${hoursMinutes(s.duration_ms)}`; }
    const cls = ["timeline-row", wknd ? "weekend" : "", day.isEmpty ? "empty" : ""].filter(Boolean).join(" ");
    return `<div class="${cls}"${day.isEmpty ? "" : ` data-tip="${tip}"`}><div class="label">${label}</div><div class="track">${segs}</div></div>`;
  }

  function timelineTicks() {
    return Array.from({ length: TSPAN / 3 + 1 }, (_, i) => {
      const hour = (TSTART + i * 3) % 24;
      return `<div class="tick" style="left:${(i * 3 / TSPAN) * 100}%">${pad(hour)}</div>`;
    }).join("");
  }

  function bedtimeDriftLabel() {
    const std = D.summary.bedtimeStdDev;
    if (!std || std < 20) return `Your bedtime is <em>remarkably stable</em>.`;
    if (std < 35) return `Your bedtime drifts by <em>~${Math.round(std)} min</em> on average.`;
    if (std < 60) return `Your bedtime <em>drifts noticeably</em> — ±${Math.round(std)} min variance.`;
    return `Your bedtime <em>drifts significantly</em> — over an hour of variance.`;
  }

  function chapterTimeline() {
    const days = aggregateByDay(D.sessions);
    const n = (D.sessionsFull || D.sessions).length;
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">02</span><span class="eyebrow">Stacked timeline</span></div>
        <h2>${bedtimeDriftLabel()}</h2>
        <p class="chapter-desc">Each bar is one day, 24h from 6pm. Weekends in amber. <button class="history-toggle" id="timeline-to-history">Full history (${n}) →</button></p>
      </div>
      <div class="panel">
        <div class="timeline"><div class="timeline-axis">${timelineTicks()}</div>${days.map(buildTimelineRow).join("")}</div>
      </div>
    `;
  }

  function renderHistoryTimeline() {
    const src = D.sessionsFull || D.sessions;
    const calDays = buildCalendarDays(src);
    const groups = [];
    let curKey = null, curLabel = "", curRows = [], curNights = 0;
    for (const day of calDays) {
      const key = `${day.date.getFullYear()}-${day.date.getMonth()}`;
      const label = day.date.toLocaleDateString("en-US", { month: "long", year: "numeric" });
      if (key !== curKey) {
        if (curKey !== null) groups.push({ label: curLabel, rows: curRows, nights: curNights });
        curKey = key; curLabel = label; curRows = []; curNights = 0;
      }
      curRows.push(buildTimelineRow(day));
      if (!day.isEmpty) curNights++;
    }
    if (curKey !== null) groups.push({ label: curLabel, rows: curRows, nights: curNights });

    const groupsHtml = groups.map((g) =>
      `<div class="timeline-month-sep"><span class="timeline-month-sep-label">${g.label}</span><span class="timeline-month-count">${g.nights} nights</span></div><div class="timeline-axis timeline-axis-month">${timelineTicks()}</div>${g.rows.join("")}`
    ).join("");

    app.innerHTML = `
      <div class="history-view-header">
        <button class="history-back-btn" id="history-back">← Nightfall</button>
        <div>
          <span class="eyebrow">Stacked timeline — full history</span>
          <h2 class="history-view-title">All <em>${src.length} nights</em></h2>
        </div>
      </div>
      <div class="panel history-panel">
        <div class="timeline">
          <div class="timeline-axis">${timelineTicks()}</div>
          ${groupsHtml}
        </div>
      </div>
      <div id="hover-tip"></div>
    `;

    document.getElementById("history-back")?.addEventListener("click", () => {
      navigateTo("dashboard");
    });
    const tip = document.getElementById("hover-tip");
    document.querySelectorAll("[data-tip]").forEach((el) => {
      el.addEventListener("mouseenter", () => { tip.textContent = el.dataset.tip; tip.classList.add("show"); });
      el.addEventListener("mousemove", (e) => { tip.style.left = e.clientX + 14 + "px"; tip.style.top = e.clientY + 14 + "px"; });
      el.addEventListener("mouseleave", () => tip.classList.remove("show"));
    });
  }

  function navigateTo(view) {
    currentView = view;
    window.location.hash = view === "dashboard" ? "" : view;
    window.scrollTo(0, 0);
    renderApp();
  }

  async function renderHistoryTimelineAsync() {
    if (!D.sessionsFull) {
      app.innerHTML = `<div style="display:flex;align-items:center;justify-content:center;height:60vh;font-family:'Geist Mono',monospace;font-size:12px;color:#6a6488;letter-spacing:0.12em;">Loading full history…</div>`;
      await (window.loadFullSessions ? window.loadFullSessions() : Promise.resolve());
      D = window.SleepData;
    }
    renderHistoryTimeline();
  }

  const LOADING_HTML = `<div style="display:flex;align-items:center;justify-content:center;height:60vh;font-family:'Geist Mono',monospace;font-size:12px;color:#6a6488;letter-spacing:0.12em;">Loading full history…</div>`;

  async function loadFull() {
    if (!D.sessionsFull) {
      app.innerHTML = LOADING_HTML;
      await (window.loadFullSessions?.() || Promise.resolve());
      D = window.SleepData;
    }
  }

  function historyViewShell(eyebrow, title, content, backId) {
    return `
      <div class="history-view-header">
        <button class="history-back-btn" id="${backId}">← Nightfall</button>
        <div><span class="eyebrow">${eyebrow}</span><h2 class="history-view-title">${title}</h2></div>
      </div>
      <div class="panel history-panel">${content}</div>
    `;
  }

  function renderHistoryScatter() {
    const src = D.sessionsFull || D.sessions;
    const nights = groupByNight(src);
    const w = 1000, h = 400, padL = 50, padR = 20, padT = 20, padB = 40;
    const innerW = w - padL - padR, innerH = h - padT - padB;
    const yMin = 18 * 60, yRange = 24 * 60;
    const first = nights[0].mainSeg.sleep_end.getTime(), last = nights[nights.length - 1].mainSeg.sleep_end.getTime();
    const timeRange = last - first || 1;
    let dots = "";
    for (const n of nights) {
      let m = n.mainSeg.sleep_start.getHours() * 60 + n.mainSeg.sleep_start.getMinutes();
      if (m < 12 * 60) m += 24 * 60;
      const x = padL + ((n.mainSeg.sleep_end.getTime() - first) / timeRange) * innerW;
      const y = padT + ((m - yMin) / yRange) * innerH;
      dots += `<circle cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="2" fill="oklch(0.68 0.14 295)" opacity="0.45"/>`;
    }
    let yTicks = "";
    for (let step = 0; step <= 8; step++) {
      const m = yMin + step * 3 * 60;
      const hh = Math.floor((m / 60) % 24);
      const y = padT + (step / 8) * innerH;
      const isMaj = step % 2 === 0;
      yTicks += `<text x="${padL - 8}" y="${y}" dominant-baseline="middle" text-anchor="end" fill="#6a6488" font-family="Geist Mono,monospace" font-size="9" opacity="${isMaj ? 1 : 0.45}">${pad(hh)}h</text>`;
      yTicks += `<line x1="${padL}" x2="${w - padR}" y1="${y}" y2="${y}" stroke="rgba(255,255,255,${isMaj ? 0.06 : 0.03})"/>`;
    }
    let xTicks = "";
    let cur = new Date(nights[0].mainSeg.sleep_end.getFullYear(), nights[0].mainSeg.sleep_end.getMonth(), 1);
    let mCount = 0;
    while (cur.getTime() <= last) {
      const x = padL + ((cur.getTime() - first) / timeRange) * innerW;
      xTicks += `<line x1="${x}" x2="${x}" y1="${padT}" y2="${padT + innerH}" stroke="rgba(255,255,255,0.05)"/>`;
      if (mCount % 3 === 0) {
        const label = cur.toLocaleDateString("en-US", { month: "short", year: "2-digit" });
        xTicks += `<text x="${x}" y="${padT + innerH + 16}" text-anchor="middle" fill="#6a6488" font-family="Geist Mono,monospace" font-size="8">${label}</text>`;
      }
      cur = new Date(cur.getFullYear(), cur.getMonth() + 1, 1);
      mCount++;
    }
    const vals = nights.map((n) => { let m = n.mainSeg.sleep_start.getHours() * 60 + n.mainSeg.sleep_start.getMinutes(); if (m < 12 * 60) m += 24 * 60; return m; });
    const mean = vals.reduce((a, b) => a + b, 0) / vals.length;
    const meanY = padT + ((mean - yMin) / yRange) * innerH;
    const meanLine = `<line x1="${padL}" x2="${w - padR}" y1="${meanY}" y2="${meanY}" stroke="rgba(255,255,255,0.18)" stroke-dasharray="3 4"/>`;
    const svg = `<svg viewBox="0 0 ${w} ${h}" style="width:100%;height:auto;display:block">${yTicks}${xTicks}${meanLine}${dots}</svg>`;
    app.innerHTML = historyViewShell("Bedtime regularity", `<em>${nights.length} nights</em> — bedtime across time`, svg, "history-back");
    document.getElementById("history-back")?.addEventListener("click", () => navigateTo("dashboard"));
  }

  async function renderHistoryScatterAsync() {
    await loadFull();
    renderHistoryScatter();
  }

  function renderHistoryDebt() {
    const src = D.sessionsFull || D.sessions;
    const nights = groupByNight(src);
    const rows = nights.map((n) => {
      const diff = n.totalHours - 8;
      const pct = Math.min(48, Math.abs(diff) * 12);
      const side = diff < 0 ? "deficit" : "surplus";
      const style = diff < 0 ? `right:50%;width:${pct}%` : `left:50%;width:${pct}%`;
      return `<div class="debt-row debt-row-full"><span class="debt-date">${n.mainSeg.sleep_end.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" })}</span><div class="debt-bar-track"><div class="debt-bar ${side}" style="${style}"></div></div><span class="debt-val" style="color:${diff<0?"oklch(0.7 0.17 25)":"oklch(0.78 0.14 155)"}">${diff>=0?"+":""}${diff.toFixed(1)}h</span></div>`;
    }).join("");
    const target = naturalSleepTarget();
    const totalDebt = nights.reduce((a, n) => a + (target - n.totalHours), 0);
    const debtSign = totalDebt <= 0 ? "+" : "";
    const content = `
      <div class="debt-summary-row">
        <span class="debt-summary-label">Cumulative debt (all time)</span>
        <span class="debt-summary-val" style="color:${totalDebt>0?"oklch(0.7 0.17 25)":"oklch(0.78 0.14 155)"}">${debtSign}${(-totalDebt).toFixed(1)}h vs ${target.toFixed(1)}h median target</span>
      </div>
      <div class="debt-full-list">${rows}</div>
    `;
    app.innerHTML = historyViewShell("Sleep debt", `<em>${nights.length} nights</em> — all history`, content, "history-back");
    document.getElementById("history-back")?.addEventListener("click", () => navigateTo("dashboard"));
  }

  async function renderHistoryDebtAsync() {
    await loadFull();
    renderHistoryDebt();
  }

  function renderHistoryStages() {
    const src = D.sessionsFull || D.sessions;
    const W = 28;
    const pts = { deep: [], rem: [], light: [], awake: [] };
    for (let i = 0; i < src.length; i++) {
      const win = src.slice(Math.max(0, i - W + 1), i + 1);
      const total = win.reduce((a, s) => a + s.duration_ms, 0) || 1;
      for (const k of Object.keys(pts)) {
        pts[k].push(win.reduce((a, s) => a + s.totals[k], 0) / total);
      }
    }
    const w = 1000, h = 360, padL = 50, padR = 20, padT = 20, padB = 40;
    const innerW = w - padL - padR, innerH = h - padT - padB;
    const n = src.length;
    const colors = { deep: "oklch(0.48 0.14 275)", light: "oklch(0.68 0.14 295)", rem: "oklch(0.82 0.13 220)", awake: "oklch(0.78 0.15 60)" };
    const targets = { deep: 0.15, rem: 0.22, light: 0.55, awake: 0.08 };
    let lines = "";
    for (const k of ["awake", "light", "deep", "rem"]) {
      const d = pts[k].map((v, i) => {
        const x = padL + (i / (n - 1)) * innerW;
        const y = padT + (1 - v) * innerH;
        return `${i === 0 ? "M" : "L"}${x.toFixed(1)},${y.toFixed(1)}`;
      }).join(" ");
      lines += `<path d="${d}" fill="none" stroke="${colors[k]}" stroke-width="1.5" opacity="0.85"/>`;
      const tY = padT + (1 - targets[k]) * innerH;
      lines += `<line x1="${padL}" x2="${w - padR}" y1="${tY}" y2="${tY}" stroke="${colors[k]}" stroke-dasharray="2 6" opacity="0.3"/>`;
    }
    let yTicks = "";
    for (let p = 0; p <= 4; p++) {
      const v = p * 0.25;
      const y = padT + (1 - v) * innerH;
      yTicks += `<text x="${padL - 6}" y="${y}" dominant-baseline="middle" text-anchor="end" fill="#6a6488" font-family="Geist Mono,monospace" font-size="9">${Math.round(v * 100)}%</text>`;
      yTicks += `<line x1="${padL}" x2="${w - padR}" y1="${y}" y2="${y}" stroke="rgba(255,255,255,0.05)"/>`;
    }
    let xTicks = "";
    const first = src[0].sleep_end.getTime(), last = src[src.length - 1].sleep_end.getTime();
    const timeRange = last - first || 1;
    let cur = new Date(src[0].sleep_end.getFullYear(), src[0].sleep_end.getMonth(), 1);
    let mCount = 0;
    while (cur.getTime() <= last) {
      const x = padL + ((cur.getTime() - first) / timeRange) * innerW;
      if (mCount % 3 === 0) xTicks += `<text x="${x}" y="${padT + innerH + 16}" text-anchor="middle" fill="#6a6488" font-family="Geist Mono,monospace" font-size="8">${cur.toLocaleDateString("en-US", { month: "short", year: "2-digit" })}</text>`;
      xTicks += `<line x1="${x}" x2="${x}" y1="${padT}" y2="${padT + innerH}" stroke="rgba(255,255,255,0.04)"/>`;
      cur = new Date(cur.getFullYear(), cur.getMonth() + 1, 1); mCount++;
    }
    const legend = Object.entries(colors).map(([k, c]) => `<span style="display:inline-flex;align-items:center;gap:5px;margin-right:16px"><span style="width:12px;height:2px;background:${c};display:inline-block"></span>${k}</span>`).join("");
    const content = `
      <div class="stages-legend">${legend}</div>
      <svg viewBox="0 0 ${w} ${h}" style="width:100%;height:auto;display:block">${yTicks}${xTicks}${lines}</svg>
      <p style="font-size:11px;color:#6a6488;margin-top:8px;">28-night rolling average. Dashed lines = healthy targets.</p>
    `;
    app.innerHTML = historyViewShell("Stage mix", `<em>${src.length} nights</em> — rolling 28-night average`, content, "history-back");
    document.getElementById("history-back")?.addEventListener("click", () => navigateTo("dashboard"));
  }

  async function renderHistoryStagesAsync() {
    await loadFull();
    renderHistoryStages();
  }

  function renderHistoryHR() {
    const allEntries = Object.entries(D.hr).sort(([a], [b]) => a.localeCompare(b));
    if (allEntries.length < 2) {
      app.innerHTML = historyViewShell("Heart rate", "No HR data available", `<div class="metric-no-data" style="padding:40px">Sync more data from the Android app to see HR history.</div>`, "history-back");
      document.getElementById("history-back")?.addEventListener("click", () => navigateTo("dashboard"));
      return;
    }
    const vals = allEntries.map(([, v]) => v);
    const w = 1000, h = 280, padL = 50, padR = 20, padT = 20, padB = 40;
    const innerW = w - padL - padR, innerH = h - padT - padB;
    const minV = Math.min(...vals) - 2, maxV = Math.max(...vals) + 2, range = maxV - minV;
    const first = new Date(allEntries[0][0]).getTime(), last = new Date(allEntries[allEntries.length - 1][0]).getTime();
    const timeRange = last - first || 1;
    const pts = allEntries.map(([d, v]) => {
      const x = padL + ((new Date(d).getTime() - first) / timeRange) * innerW;
      const y = padT + innerH - ((v - minV) / range) * innerH;
      return [x, y];
    });
    const line = pts.map((p, i) => (i === 0 ? `M${p[0]},${p[1]}` : `L${p[0]},${p[1]}`)).join(" ");
    const area = `${line} L${pts[pts.length-1][0]},${padT+innerH} L${pts[0][0]},${padT+innerH} Z`;
    let yTicks = "";
    for (let i = 0; i <= 4; i++) {
      const v = minV + (range * i) / 4;
      const y = padT + innerH - (i / 4) * innerH;
      yTicks += `<text x="${padL - 6}" y="${y}" dominant-baseline="middle" text-anchor="end" fill="#6a6488" font-family="Geist Mono,monospace" font-size="9">${Math.round(v)}</text>`;
      yTicks += `<line x1="${padL}" x2="${w - padR}" y1="${y}" y2="${y}" stroke="rgba(255,255,255,0.05)"/>`;
    }
    let xTicks = "";
    let cur = new Date(new Date(allEntries[0][0]).getFullYear(), new Date(allEntries[0][0]).getMonth(), 1);
    let mCount = 0;
    while (cur.getTime() <= last) {
      const x = padL + ((cur.getTime() - first) / timeRange) * innerW;
      if (mCount % 3 === 0) xTicks += `<text x="${x}" y="${padT + innerH + 16}" text-anchor="middle" fill="#6a6488" font-family="Geist Mono,monospace" font-size="8">${cur.toLocaleDateString("en-US", { month: "short", year: "2-digit" })}</text>`;
      xTicks += `<line x1="${x}" x2="${x}" y1="${padT}" y2="${padT + innerH}" stroke="rgba(255,255,255,0.04)"/>`;
      cur = new Date(cur.getFullYear(), cur.getMonth() + 1, 1); mCount++;
    }
    const svg = `<svg viewBox="0 0 ${w} ${h}" style="width:100%;height:auto;display:block"><defs><linearGradient id="hrGradFull" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stop-color="oklch(0.82 0.13 220)" stop-opacity="0.35"/><stop offset="100%" stop-color="oklch(0.82 0.13 220)" stop-opacity="0"/></linearGradient></defs>${yTicks}${xTicks}<path d="${area}" fill="url(#hrGradFull)"/><path d="${line}" fill="none" stroke="oklch(0.82 0.13 220)" stroke-width="1.5"/></svg>`;
    app.innerHTML = historyViewShell("Nighttime heart rate", `<em>${allEntries.length} nights</em> — resting bpm history`, svg, "history-back");
    document.getElementById("history-back")?.addEventListener("click", () => navigateTo("dashboard"));
  }

  async function renderHistoryHRAsync() {
    await loadFull();
    renderHistoryHR();
  }

  function renderApp() {
    if (currentView === "history/timeline") {
      renderHistoryTimelineAsync();
    } else if (currentView === "history/scatter") {
      renderHistoryScatterAsync();
    } else if (currentView === "history/debt") {
      renderHistoryDebtAsync();
    } else if (currentView === "history/stages") {
      renderHistoryStagesAsync();
    } else if (currentView === "history/hr") {
      renderHistoryHRAsync();
    } else {
      render();
    }
  }

  let focusIdx = 0;
  let currentView = "dashboard";
  let driftDemoMode = false;
  let driftPlayback = { playing: false, frameFrac: 0, windowSize: 30, speed: 1, rafId: null, frames: null, _src: null, _loading: false };
  let metricsMonth = null;
  let metricsFullLoading = false;

  function hypnogramSVG(session) {
    const w = 1000, h = 260, padL = 70, padR = 20, padT = 16, padB = 40;
    const innerW = w - padL - padR, innerH = h - padT - padB;
    const yForStage = { awake: padT + innerH * 0.08, rem: padT + innerH * 0.32, light: padT + innerH * 0.58, deep: padT + innerH * 0.88 };
    const start = session.sleep_start.getTime(), end = session.sleep_end.getTime(), total = end - start;
    let segs = "";
    for (const st of session.stages) {
      const x0 = padL + ((st.stage_start.getTime() - start) / total) * innerW;
      const x1 = padL + ((st.stage_end.getTime() - start) / total) * innerW;
      const y = yForStage[st.stage_type];
      const baseY = padT + innerH;
      segs += `<rect x="${x0}" y="${y}" width="${x1 - x0}" height="${baseY - y}" fill="${STAGE_COLORS[st.stage_type]}" opacity="0.2"/>`;
      segs += `<line x1="${x0}" x2="${x1}" y1="${y}" y2="${y}" stroke="${STAGE_COLORS[st.stage_type]}" stroke-width="2.5" stroke-linecap="round"/>`;
    }
    const yLabels = Object.entries(yForStage).map(([k, y]) => `<line class="hypno-grid-line" x1="${padL}" x2="${w - padR}" y1="${y}" y2="${y}"/><text class="hypno-y-label" x="${padL - 12}" y="${y + 4}" text-anchor="end">${k}</text>`).join("");
    const hours = total / 3600000;
    let xTicks = "";
    for (let i = 0; i <= hours; i++) {
      const t = session.sleep_start.getTime() + i * 3600000;
      const x = padL + (i / hours) * innerW;
      xTicks += `<text class="hypno-x-label" x="${x}" y="${h - 14}" text-anchor="middle">${pad(new Date(t).getHours())}:00</text>`;
    }
    return `<svg class="hypno-svg" viewBox="0 0 ${w} ${h}" preserveAspectRatio="none">${yLabels}${segs}${xTicks}</svg>`;
  }

  function chapterHypnogram() {
    const s = D.sessions[focusIdx];
    const totals = s.totals;
    const stats = [
      { k: "Deep", v: hoursMinutes(totals.deep), pct: totals.deep / s.duration_ms, stage: "deep" },
      { k: "REM", v: hoursMinutes(totals.rem), pct: totals.rem / s.duration_ms, stage: "rem" },
      { k: "Light", v: hoursMinutes(totals.light), pct: totals.light / s.duration_ms, stage: "light" },
      { k: "Awake", v: hoursMinutes(totals.awake), pct: totals.awake / s.duration_ms, stage: "awake" },
    ];
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">03</span><span class="eyebrow">Hypnogram</span></div>
        <h2>One night, <em>zoomed in</em>.</h2>
        <p class="chapter-desc">The classic sleep-lab staircase. Sharp climbs back to REM are where you were dreaming.</p>
      </div>
      <div class="panel">
        <div class="hypno-header">
          <div class="hypno-date">${fmtDateLong(s.sleep_end)}</div>
          <div class="hypno-controls"><button id="hypno-prev">‹</button><button id="hypno-next">›</button></div>
        </div>
        <div class="hypno-stats">
          ${stats.map((st) => `<div class="hypno-stat"><div class="k"><span class="sw" style="background:${STAGE_COLORS[st.stage]}"></span>${st.k}</div><div class="v">${st.v}</div><div class="pct">${Math.round(st.pct * 100)}% of night</div></div>`).join("")}
        </div>
        <div class="hypno-svg-wrap">${hypnogramSVG(s)}</div>
      </div>
    `;
  }

  function radialSVG(session) {
    const w = 520, cx = w / 2, cy = w / 2, rOuter = w * 0.46, rInner = w * 0.32;
    let stageArcs = "";
    for (const st of session.stages) {
      const a0 = angleForTime(st.stage_start);
      const a1 = angleForTime(st.stage_end);
      let d = a1 - a0; if (d < 0) d += 2 * Math.PI;
      const largeArc = d > Math.PI ? 1 : 0;
      const x0 = cx + Math.cos(a0) * rInner, y0 = cy + Math.sin(a0) * rInner;
      const x1 = cx + Math.cos(a1) * rInner, y1 = cy + Math.sin(a1) * rInner;
      const x2 = cx + Math.cos(a1) * rOuter, y2 = cy + Math.sin(a1) * rOuter;
      const x3 = cx + Math.cos(a0) * rOuter, y3 = cy + Math.sin(a0) * rOuter;
      const p = `M${x0},${y0} A${rInner},${rInner} 0 ${largeArc} 1 ${x1},${y1} L${x2},${y2} A${rOuter},${rOuter} 0 ${largeArc} 0 ${x3},${y3} Z`;
      stageArcs += `<path d="${p}" fill="${STAGE_COLORS[st.stage_type]}" opacity="0.85"/>`;
    }
    let ticks = "";
    for (let h = 0; h < 24; h++) {
      const a = -Math.PI / 2 + (h / 24) * Math.PI * 2;
      const isMajor = h % 6 === 0, isMid = h % 3 === 0;
      const r1 = rOuter + 4, r2 = rOuter + (isMajor ? 16 : isMid ? 10 : 6);
      ticks += `<line x1="${cx + Math.cos(a) * r1}" y1="${cy + Math.sin(a) * r1}" x2="${cx + Math.cos(a) * r2}" y2="${cy + Math.sin(a) * r2}" stroke="rgba(255,255,255,${isMajor ? 0.4 : isMid ? 0.2 : 0.1})" stroke-width="1"/>`;
      if (isMid) {
        const rt = rOuter + (isMajor ? 32 : 27);
        ticks += `<text x="${(cx + Math.cos(a) * rt).toFixed(1)}" y="${(cy + Math.sin(a) * rt).toFixed(1)}" text-anchor="middle" dominant-baseline="middle" font-family="Geist Mono" font-size="${isMajor ? 11 : 9}" fill="${isMajor ? "rgba(232,228,245,0.65)" : "rgba(232,228,245,0.32)"}">${pad(h)}</text>`;
      }
    }
    const durHrs = session.duration_hours.toFixed(1);
    const center = `<text x="${cx}" y="${cy - 8}" text-anchor="middle" font-family="Instrument Serif" font-size="48" fill="#e8e4f5">${durHrs}</text><text x="${cx}" y="${cy + 18}" text-anchor="middle" font-family="Geist Mono" font-size="10" fill="#6a6488" letter-spacing="0.15em">HOURS</text>`;
    return `<svg viewBox="0 0 ${w} ${w}" class="radial-svg"><circle cx="${cx}" cy="${cy}" r="${(rOuter + rInner) / 2}" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="${rOuter - rInner}"/>${stageArcs}${ticks}${center}</svg>`;
  }

  function chapterRadial() {
    const s = D.sessions[focusIdx];
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">04</span><span class="eyebrow">Radial clock</span></div>
        <h2>A night shaped like a <em>clock face</em>.</h2>
        <p class="chapter-desc">Twelve at the top, noon at the bottom. The ring's colour shifts with every stage.</p>
      </div>
      <div class="panel">
        <div class="radial-wrap">
          <div>${radialSVG(s)}</div>
          <div class="radial-info">
            <h3>${fmtDateLong(s.sleep_end)}</h3>
            <p>Read clockwise from midnight. The ring is your night, with each colour marking a sleep stage in real time.</p>
            <dl class="radial-kv">
              <dt>Bedtime</dt><dd>${fmtHM(s.sleep_start)}</dd>
              <dt>Wake</dt><dd>${fmtHM(s.sleep_end)}</dd>
              <dt>Duration</dt><dd>${hoursMinutes(s.duration_ms)}</dd>
              <dt>Efficiency</dt><dd>${Math.round(s.efficiency * 100)}%</dd>
              <dt>Score</dt><dd>${s.score} / 100</dd>
            </dl>
          </div>
        </div>
      </div>
    `;
  }

  function angleForTime(d) {
    return -Math.PI / 2 + ((d.getHours() + d.getMinutes() / 60) / 24) * Math.PI * 2;
  }

  function clockTicks(cx, cy, rOuter) {
    let t = "";
    for (let h = 0; h < 24; h++) {
      const a = -Math.PI / 2 + (h / 24) * Math.PI * 2;
      const isMajor = h % 6 === 0;
      const isMid3 = h % 3 === 0;
      const tickLen = isMajor ? 12 : isMid3 ? 7 : 4;
      const tickOp = isMajor ? 0.45 : isMid3 ? 0.22 : 0.1;
      const r1 = rOuter + 3, r2 = rOuter + 3 + tickLen;
      t += `<line x1="${(cx + Math.cos(a) * r1).toFixed(1)}" y1="${(cy + Math.sin(a) * r1).toFixed(1)}" x2="${(cx + Math.cos(a) * r2).toFixed(1)}" y2="${(cy + Math.sin(a) * r2).toFixed(1)}" stroke="rgba(255,255,255,${tickOp})" stroke-width="${isMajor ? 1.5 : 1}"/>`;
      if (isMid3) {
        const rt = rOuter + 22;
        const label = `${h}h`;
        const isMidnight = h === 0;
        const fill = isMidnight ? "rgba(180,150,255,0.9)" : isMajor ? "rgba(232,228,245,0.6)" : "rgba(232,228,245,0.32)";
        const fs = isMajor ? 11 : 9;
        t += `<text x="${(cx + Math.cos(a) * rt).toFixed(1)}" y="${(cy + Math.sin(a) * rt).toFixed(1)}" text-anchor="middle" dominant-baseline="middle" font-family="Geist Mono" font-size="${fs}" fill="${fill}">${label}</text>`;
      }
    }
    return t;
  }

  function driftDensitySVG(sessions) {
    const SLOTS = 96;
    const counts = new Array(SLOTS).fill(0);
    const STEP = 15 * 60 * 1000;
    for (const s of sessions) {
      let cur = s.sleep_start.getTime();
      while (cur < s.sleep_end.getTime()) {
        const d = new Date(cur);
        const slot = Math.floor((d.getHours() * 60 + d.getMinutes()) / 15) % SLOTS;
        counts[slot]++;
        cur += STEP;
      }
    }
    const maxC = Math.max(...counts, 1);
    const w = 400, cx = w / 2, cy = w / 2, rO = w * 0.43, rI = w * 0.29;
    let arcs = "";
    for (let i = 0; i < SLOTS; i++) {
      const op = counts[i] / maxC;
      if (op < 0.01) continue;
      const a0 = -Math.PI / 2 + (i / SLOTS) * Math.PI * 2;
      const a1 = -Math.PI / 2 + ((i + 1) / SLOTS) * Math.PI * 2;
      const xi0 = cx + Math.cos(a0) * rI, yi0 = cy + Math.sin(a0) * rI;
      const xi1 = cx + Math.cos(a1) * rI, yi1 = cy + Math.sin(a1) * rI;
      const xo1 = cx + Math.cos(a1) * rO, yo1 = cy + Math.sin(a1) * rO;
      const xo0 = cx + Math.cos(a0) * rO, yo0 = cy + Math.sin(a0) * rO;
      arcs += `<path d="M${xi0},${yi0} A${rI},${rI} 0 0 1 ${xi1},${yi1} L${xo1},${yo1} A${rO},${rO} 0 0 0 ${xo0},${yo0} Z" fill="oklch(0.68 0.14 295)" opacity="${(op * 0.88 + 0.05).toFixed(3)}"/>`;
    }
    const center = `<text x="${cx}" y="${cy - 4}" text-anchor="middle" font-family="Instrument Serif" font-size="40" fill="#e8e4f5">${sessions.length}</text><text x="${cx}" y="${cy + 16}" text-anchor="middle" font-family="Geist Mono" font-size="9" fill="#6a6488" letter-spacing="0.15em">NIGHTS</text>`;
    return `<svg viewBox="-30 -30 460 460" class="drift-svg"><circle cx="${cx}" cy="${cy}" r="${(rO + rI) / 2}" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="${rO - rI}"/>${arcs}${clockTicks(cx, cy, rO)}${center}</svg>`;
  }

  function driftSpaghettiSVG(sessions) {
    const w = 400, cx = w / 2, cy = w / 2, rO = w * 0.43, rI = w * 0.29;
    const n = sessions.length;
    let arcs = "";
    for (let i = 0; i < n; i++) {
      const s = sessions[i];
      const t = n > 1 ? i / (n - 1) : 0;
      const hue = 275 - t * 215;
      const lum = (0.52 + t * 0.26).toFixed(2);
      const color = `oklch(${lum} 0.14 ${hue.toFixed(0)})`;
      const a0 = angleForTime(s.sleep_start);
      const a1 = angleForTime(s.sleep_end);
      let span = a1 - a0; if (span <= 0) span += Math.PI * 2;
      const largeArc = span > Math.PI ? 1 : 0;
      const xi0 = cx + Math.cos(a0) * rI, yi0 = cy + Math.sin(a0) * rI;
      const xi1 = cx + Math.cos(a1) * rI, yi1 = cy + Math.sin(a1) * rI;
      const xo1 = cx + Math.cos(a1) * rO, yo1 = cy + Math.sin(a1) * rO;
      const xo0 = cx + Math.cos(a0) * rO, yo0 = cy + Math.sin(a0) * rO;
      arcs += `<path d="M${xi0},${yi0} A${rI},${rI} 0 ${largeArc} 1 ${xi1},${yi1} L${xo1},${yo1} A${rO},${rO} 0 ${largeArc} 0 ${xo0},${yo0} Z" fill="${color}" opacity="${(0.04 + t * 0.08).toFixed(3)}"/>`;
    }
    const yr0 = new Date(sessions[0].sleep_start).getFullYear();
    const yr1 = new Date(sessions[n - 1].sleep_start).getFullYear();
    const ly = w - 22, lx1 = cx - 56, lx2 = cx + 56;
    const legend = `<defs><linearGradient id="dsg"><stop offset="0%" stop-color="oklch(0.52 0.14 275)"/><stop offset="100%" stop-color="oklch(0.78 0.14 60)"/></linearGradient></defs><rect x="${lx1}" y="${ly}" width="${lx2 - lx1}" height="3" fill="url(#dsg)" rx="1.5"/><text x="${lx1}" y="${ly + 13}" font-family="Geist Mono" font-size="9" fill="rgba(232,228,245,0.45)" text-anchor="start">${yr0}</text><text x="${lx2}" y="${ly + 13}" font-family="Geist Mono" font-size="9" fill="rgba(232,228,245,0.45)" text-anchor="end">${yr1}</text>`;
    const center = `<text x="${cx}" y="${cy - 4}" text-anchor="middle" font-family="Instrument Serif" font-size="40" fill="#e8e4f5">${n}</text><text x="${cx}" y="${cy + 16}" text-anchor="middle" font-family="Geist Mono" font-size="9" fill="#6a6488" letter-spacing="0.15em">NIGHTS</text>`;
    return `<svg viewBox="-30 -30 460 460" class="drift-svg"><circle cx="${cx}" cy="${cy}" r="${(rO + rI) / 2}" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="${rO - rI}"/>${arcs}${clockTicks(cx, cy, rO)}${legend}${center}</svg>`;
  }

  function circularMeanMinutes(minsList) {
    const TAU = 2 * Math.PI, MINS_DAY = 24 * 60;
    const sinSum = minsList.reduce((a, m) => a + Math.sin((m / MINS_DAY) * TAU), 0);
    const cosSum = minsList.reduce((a, m) => a + Math.cos((m / MINS_DAY) * TAU), 0);
    let angle = Math.atan2(sinSum / minsList.length, cosSum / minsList.length);
    let mins = (angle / TAU) * MINS_DAY;
    if (mins < 0) mins += MINS_DAY;
    return mins;
  }

  function lerpCircular(a, b, alpha) {
    const MINS = 24 * 60;
    let diff = b - a;
    if (diff > MINS / 2) diff -= MINS;
    if (diff < -MINS / 2) diff += MINS;
    return ((a + diff * alpha) % MINS + MINS) % MINS;
  }

  function interpolatedFrame(frameFrac) {
    const frames = driftPlayback.frames;
    if (!frames || !frames.length) return null;
    const i0 = Math.min(Math.floor(frameFrac), frames.length - 1);
    const i1 = Math.min(i0 + 1, frames.length - 1);
    const alpha = frameFrac - Math.floor(frameFrac);
    const f0 = frames[i0], f1 = frames[i1];
    return {
      avgBed: lerpCircular(f0.avgBed, f1.avgBed, alpha),
      avgWake: lerpCircular(f0.avgWake, f1.avgWake, alpha),
      t: f0.t + (f1.t - f0.t) * alpha,
      date: f0.date,
    };
  }

  function computeDriftFrames(sessions, scope) {
    const src = scope > 0 ? sessions.slice(-scope) : sessions;
    const n = src.length;
    return src.map((s, i) => ({
      avgBed: s.sleep_start.getHours() * 60 + s.sleep_start.getMinutes(),
      avgWake: s.sleep_end.getHours() * 60 + s.sleep_end.getMinutes(),
      date: s.sleep_end,
      t: n > 1 ? i / (n - 1) : 0,
    }));
  }

  function driftPlaybackSVG(frames, windowSize) {
    const w = 400, cx = w / 2, cy = w / 2, rO = w * 0.43, rI = w * 0.29, rMid = (rO + rI) / 2;
    const step = Math.max(1, Math.floor(frames.length / 60));
    let ghosts = "";
    for (let i = 0; i < frames.length; i += step) {
      const f = frames[i];
      const a0 = -Math.PI / 2 + (f.avgBed / (24 * 60)) * Math.PI * 2;
      const a1 = -Math.PI / 2 + (f.avgWake / (24 * 60)) * Math.PI * 2;
      let span = a1 - a0; if (span <= 0) span += Math.PI * 2;
      const la = span > Math.PI ? 1 : 0;
      const x0 = cx + Math.cos(a0) * rMid, y0 = cy + Math.sin(a0) * rMid;
      const x1 = cx + Math.cos(a1) * rMid, y1 = cy + Math.sin(a1) * rMid;
      const hue = 275 - f.t * 215;
      ghosts += `<path d="M${x0.toFixed(2)},${y0.toFixed(2)} A${rMid},${rMid} 0 ${la} 1 ${x1.toFixed(2)},${y1.toFixed(2)}" fill="none" stroke="oklch(0.6 0.08 ${hue.toFixed(0)})" stroke-width="1" opacity="0.09" stroke-linecap="round"/>`;
    }
    const f0 = frames[Math.round(driftPlayback.frameFrac)] || frames[0];
    const a0i = -Math.PI / 2 + (f0.avgBed / (24 * 60)) * Math.PI * 2;
    const a1i = -Math.PI / 2 + (f0.avgWake / (24 * 60)) * Math.PI * 2;
    let spani = a1i - a0i; if (spani <= 0) spani += Math.PI * 2;
    const lai = spani > Math.PI ? 1 : 0;
    const x0i = cx + Math.cos(a0i) * rMid, y0i = cy + Math.sin(a0i) * rMid;
    const x1i = cx + Math.cos(a1i) * rMid, y1i = cy + Math.sin(a1i) * rMid;
    const di = `M${x0i.toFixed(2)},${y0i.toFixed(2)} A${rMid},${rMid} 0 ${lai} 1 ${x1i.toFixed(2)},${y1i.toFixed(2)}`;
    const hue0 = 275 - f0.t * 215, lum0 = (0.55 + f0.t * 0.23).toFixed(2);
    const col0 = `oklch(${lum0} 0.18 ${hue0.toFixed(0)})`;
    const bedH = Math.floor(f0.avgBed / 60), bedM = Math.round(f0.avgBed % 60);
    const wkH = Math.floor(f0.avgWake / 60), wkM = Math.round(f0.avgWake % 60);
    const center = `
      <text id="drift-pb-bed" x="${cx}" y="${cy - 12}" text-anchor="middle" font-family="Geist Mono" font-size="17" fill="#e8e4f5" letter-spacing="0.04em">${pad(bedH)}:${pad(bedM)}</text>
      <text x="${cx}" y="${cy + 3}" text-anchor="middle" font-family="Geist Mono" font-size="8" fill="#6a6488" letter-spacing="0.14em">BED · WAKE</text>
      <text id="drift-pb-wake" x="${cx}" y="${cy + 20}" text-anchor="middle" font-family="Geist Mono" font-size="17" fill="#e8e4f5" letter-spacing="0.04em">${pad(wkH)}:${pad(wkM)}</text>`;
    return `<svg viewBox="-30 -30 460 460" class="drift-svg">
      <circle cx="${cx}" cy="${cy}" r="${rMid}" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="${rO - rI}"/>
      ${ghosts}
      <path id="drift-pb-arc-glow" d="${di}" fill="none" stroke="${col0}" stroke-width="14" opacity="0.18" stroke-linecap="round"/>
      <path id="drift-pb-arc" d="${di}" fill="none" stroke="${col0}" stroke-width="5" opacity="1" stroke-linecap="round"/>
      <circle id="drift-pb-dot-bed" cx="${x0i.toFixed(2)}" cy="${y0i.toFixed(2)}" r="4.5" fill="${col0}"/>
      <circle id="drift-pb-dot-wake" cx="${x1i.toFixed(2)}" cy="${y1i.toFixed(2)}" r="4.5" fill="${col0}"/>
      ${clockTicks(cx, cy, rO)}
      ${center}
    </svg>`;
  }

  function updateDriftArc(frameObj) {
    const cx = 200, cy = 200, rMid = 144;
    const a0 = -Math.PI / 2 + (frameObj.avgBed / (24 * 60)) * Math.PI * 2;
    const a1 = -Math.PI / 2 + (frameObj.avgWake / (24 * 60)) * Math.PI * 2;
    let span = a1 - a0; if (span <= 0) span += Math.PI * 2;
    const la = span > Math.PI ? 1 : 0;
    const x0 = cx + Math.cos(a0) * rMid, y0 = cy + Math.sin(a0) * rMid;
    const x1 = cx + Math.cos(a1) * rMid, y1 = cy + Math.sin(a1) * rMid;
    const d = `M${x0.toFixed(2)},${y0.toFixed(2)} A${rMid},${rMid} 0 ${la} 1 ${x1.toFixed(2)},${y1.toFixed(2)}`;
    const hue = 275 - frameObj.t * 215;
    const lum = (0.55 + frameObj.t * 0.23).toFixed(2);
    const color = `oklch(${lum} 0.18 ${hue.toFixed(0)})`;
    const arcEl = document.getElementById("drift-pb-arc");
    if (arcEl) { arcEl.setAttribute("d", d); arcEl.setAttribute("stroke", color); }
    const glowEl = document.getElementById("drift-pb-arc-glow");
    if (glowEl) { glowEl.setAttribute("d", d); glowEl.setAttribute("stroke", color); }
    const bedDot = document.getElementById("drift-pb-dot-bed");
    if (bedDot) { bedDot.setAttribute("cx", x0.toFixed(2)); bedDot.setAttribute("cy", y0.toFixed(2)); bedDot.setAttribute("fill", color); }
    const wakeDot = document.getElementById("drift-pb-dot-wake");
    if (wakeDot) { wakeDot.setAttribute("cx", x1.toFixed(2)); wakeDot.setAttribute("cy", y1.toFixed(2)); wakeDot.setAttribute("fill", color); }
    const bedH = Math.floor(frameObj.avgBed / 60), bedM = Math.round(frameObj.avgBed % 60);
    const wkH = Math.floor(frameObj.avgWake / 60), wkM = Math.round(frameObj.avgWake % 60);
    const bedEl = document.getElementById("drift-pb-bed");
    if (bedEl) bedEl.textContent = `${pad(bedH)}:${pad(bedM)}`;
    const wakeEl = document.getElementById("drift-pb-wake");
    if (wakeEl) wakeEl.textContent = `${pad(wkH)}:${pad(wkM)}`;
    const dateEl = document.getElementById("drift-pb-date");
    if (dateEl) dateEl.textContent = frameObj.date.toLocaleDateString("fr-FR", { month: "short", year: "numeric" });
    const scrubEl = document.getElementById("drift-pb-scrub");
    if (scrubEl) scrubEl.value = driftPlayback.frameFrac;
  }

  function generateDemoSessions(n) {
    const sessions = [];
    const now = Date.now();
    for (let i = n - 1; i >= 0; i--) {
      const nominal = new Date(now - i * 86400000); nominal.setHours(0, 0, 0, 0);
      const doy = Math.floor((nominal - new Date(nominal.getFullYear(), 0, 0)) / 86400000);
      const seasonal = Math.sin(((doy - 80) / 365) * 2 * Math.PI) * 42;
      const jitter = (Math.random() - 0.5) * 68;
      const bedMins = 22 * 60 + 30 + seasonal + jitter;
      const bedTime = new Date(nominal.getTime() + bedMins * 60000);
      const durMs = (7 * 60 + (Math.random() - 0.5) * 60) * 60000;
      const wakeTime = new Date(bedTime.getTime() + durMs);
      const wk = wakeTime;
      sessions.push({
        sleep_start: bedTime, sleep_end: wakeTime, stages: [],
        duration_ms: durMs, duration_hours: durMs / 3600000,
        score: 65 + Math.round(Math.random() * 25),
        date_key: `${wk.getFullYear()}-${String(wk.getMonth()+1).padStart(2,"0")}-${String(wk.getDate()).padStart(2,"0")}`,
        totals: { deep: 0, light: 0, rem: 0, awake: 0 }, efficiency: 0.88, id: 9000 + i,
      });
    }
    return sessions;
  }

  function countCycles(stages) {
    if (!stages || !stages.length) return 0;
    const sorted = [...stages].sort((a, b) => a.stage_start - b.stage_start);
    let cycles = 0, prev = null;
    for (const st of sorted) {
      if (st.stage_type === "rem" && prev !== "rem") cycles++;
      prev = st.stage_type;
    }
    return cycles;
  }

  function computeElasticityData(sessions) {
    const episodes = groupByNight(sessions);
    if (episodes.length < 3) return null;
    const target = naturalSleepTarget();
    const points = episodes.map((e) => ({
      hours: e.totalHours,
      dev: e.totalHours - target,
      cycles: countCycles(e.segments.flatMap((s) => s.stages)),
      date: e.mainSeg.sleep_end,
    }));
    const hrs = points.map((p) => p.hours);
    const mean = hrs.reduce((a, b) => a + b, 0) / hrs.length;
    const stdDev = Math.sqrt(hrs.reduce((a, h) => a + (h - mean) ** 2, 0) / hrs.length);
    const cv = target > 0 ? stdDev / target : 0;
    const devs = points.map((p) => p.dev);
    const meanDev = devs.reduce((a, b) => a + b, 0) / devs.length;
    let num = 0, den = 0;
    for (let i = 0; i < devs.length - 1; i++) {
      num += (devs[i] - meanDev) * (devs[i + 1] - meanDev);
      den += (devs[i] - meanDev) ** 2;
    }
    const lag1 = den > 0 ? num / den : 0;
    const cycleDist = {};
    for (const p of points) { const k = Math.min(p.cycles, 7); cycleDist[k] = (cycleDist[k] || 0) + 1; }
    const sc = [...points].map((p) => p.cycles).sort((a, b) => a - b);
    const midC = Math.floor(sc.length / 2);
    const medianCycles = sc.length % 2 === 0 ? (sc[midC - 1] + sc[midC]) / 2 : sc[midC];
    return { points, cv, lag1, stdDev, cycleDist, medianCycles, target };
  }

  function elasticityBandSVG(points) {
    const barW = 5, gap = 1, padL = 44, padR = 12, padT = 20, padB = 28;
    const n = points.length;
    const svgW = padL + n * (barW + gap) + padR;
    const svgH = 200;
    const innerH = svgH - padT - padB;
    const maxDev = Math.max(4, ...points.map((p) => Math.abs(p.dev)));
    const baseline = padT + innerH / 2;
    const yScale = (v) => baseline - (v / maxDev) * (innerH / 2);
    let bars = "";
    for (let i = 0; i < n; i++) {
      const p = points[i];
      const x = padL + i * (barW + gap);
      const y1 = yScale(p.dev);
      const barY = Math.min(baseline, y1);
      const barH = Math.abs(baseline - y1) || 1;
      const col = p.dev < 0 ? "oklch(0.58 0.18 260)" : "oklch(0.78 0.15 60)";
      bars += `<rect x="${x}" y="${barY.toFixed(1)}" width="${barW}" height="${barH.toFixed(1)}" fill="${col}" opacity="0.75" rx="1"/>`;
    }
    let yTicks = "";
    for (const v of [-4, -2, 0, 2, 4]) {
      if (Math.abs(v) > maxDev + 0.5) continue;
      const y = yScale(v);
      yTicks += `<text x="${padL - 4}" y="${y}" dominant-baseline="middle" text-anchor="end" fill="#6a6488" font-family="Geist Mono,monospace" font-size="8">${v > 0 ? "+" : ""}${v}h</text>`;
      yTicks += `<line x1="${padL}" x2="${svgW - padR}" y1="${y}" y2="${y}" stroke="rgba(255,255,255,${v === 0 ? 0.14 : 0.04})"/>`;
    }
    let xTicks = "";
    let prevM = null;
    for (let i = 0; i < n; i++) {
      const d = points[i].date;
      const mk = `${d.getFullYear()}-${d.getMonth()}`;
      if (mk !== prevM) {
        const x = padL + i * (barW + gap);
        xTicks += `<text x="${x}" y="${svgH - 6}" fill="#4a4468" font-family="Geist Mono,monospace" font-size="7">${d.toLocaleDateString("en-US", { month: "short", year: "2-digit" })}</text>`;
        prevM = mk;
      }
    }
    return `<div style="overflow-x:auto;overflow-y:visible"><svg viewBox="0 0 ${svgW} ${svgH}" style="width:${Math.max(100, svgW)}px;height:${svgH}px;display:block">${yTicks}${xTicks}${bars}</svg></div>`;
  }

  function cycleDistSVG(cycleDist, totalN) {
    const w = 260, h = 176, padL = 20, padR = 52, padT = 8, padB = 8;
    const innerW = w - padL - padR, rowH = (h - padT - padB) / 7;
    const maxC = Math.max(1, ...Object.values(cycleDist));
    let rows = "";
    for (let i = 0; i < 7; i++) {
      const k = i + 1;
      const count = cycleDist[k] || 0;
      const bw = (count / maxC) * innerW;
      const y = padT + i * rowH;
      const isTarget = k === 4 || k === 5;
      const col = isTarget ? "oklch(0.78 0.14 155)" : "oklch(0.58 0.14 275)";
      rows += `<rect x="${padL}" y="${y + 2}" width="${bw.toFixed(1)}" height="${rowH - 4}" fill="${col}" opacity="${isTarget ? 0.75 : 0.4}" rx="2"/>`;
      rows += `<text x="${padL - 4}" y="${y + rowH / 2}" dominant-baseline="middle" text-anchor="end" fill="#6a6488" font-family="Geist Mono,monospace" font-size="8">${k === 7 ? "7+" : k}</text>`;
      if (count > 0) rows += `<text x="${padL + bw + 4}" y="${y + rowH / 2}" dominant-baseline="middle" fill="${isTarget ? "oklch(0.78 0.14 155)" : "#6a6488"}" font-family="Geist Mono,monospace" font-size="8">${((count / totalN) * 100).toFixed(0)}%</text>`;
    }
    return `<svg viewBox="0 0 ${w} ${h}" style="width:${w}px;height:${h}px">${rows}</svg>`;
  }

  function lagScatterSVG(points) {
    const w = 240, h = 240, padL = 32, padR = 12, padT = 12, padB = 32;
    const innerW = w - padL - padR, innerH = h - padT - padB;
    const devs = points.map((p) => p.dev);
    const maxD = Math.max(4, ...devs.map(Math.abs));
    const sx = (v) => padL + ((v + maxD) / (2 * maxD)) * innerW;
    const sy = (v) => padT + innerH - ((v + maxD) / (2 * maxD)) * innerH;
    let dots = "";
    for (let i = 0; i < devs.length - 1; i++) {
      const elastic = (devs[i] < 0 && devs[i + 1] > 0) || (devs[i] > 0 && devs[i + 1] < 0);
      dots += `<circle cx="${sx(devs[i]).toFixed(1)}" cy="${sy(devs[i + 1]).toFixed(1)}" r="2.5" fill="${elastic ? "oklch(0.78 0.15 60)" : "oklch(0.58 0.14 275)"}" opacity="0.45"/>`;
    }
    const cx = sx(0), cy = sy(0);
    const axes = `<line x1="${padL}" x2="${w-padR}" y1="${cy}" y2="${cy}" stroke="rgba(255,255,255,0.1)"/><line x1="${cx}" x2="${cx}" y1="${padT}" y2="${padT+innerH}" stroke="rgba(255,255,255,0.1)"/>`;
    const labels = `<text x="${padL + innerW/2}" y="${h-6}" text-anchor="middle" fill="#4a4468" font-family="Geist Mono,monospace" font-size="7">night n deviation</text><text x="8" y="${padT+innerH/2}" dominant-baseline="middle" text-anchor="middle" fill="#4a4468" font-family="Geist Mono,monospace" font-size="7" transform="rotate(-90 8 ${padT+innerH/2})">night n+1</text>`;
    return `<svg viewBox="0 0 ${w} ${h}" style="width:${w}px;height:${h}px">${axes}${labels}${dots}</svg>`;
  }

  function chapterElasticity() {
    const src = D.sessionsFull || D.sessions;
    const data = computeElasticityData(src);
    if (!data) return "";
    const { points, cv, lag1, stdDev, cycleDist, medianCycles, target } = data;
    let shape, pattern;
    if (cv < 0.15) shape = "Rigid — duration barely varies.";
    else if (cv < 0.30) shape = "Flexible — moderate duration variability.";
    else shape = "Elastic — duration swings significantly.";
    if (lag1 < -0.25) pattern = "Strong compression–recovery cycle: short nights reliably trigger longer ones.";
    else if (lag1 < 0.10) pattern = "Weak autocorrelation — consecutive durations are mostly independent.";
    else pattern = "Streaky — short nights tend to cluster together.";
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">10</span><span class="eyebrow">Duration regularity</span></div>
        <h2>Wood, spring, or <em>elastic band</em>?</h2>
        <p class="chapter-desc">${shape} ${pattern}</p>
      </div>
      <div class="elasticity-stats">
        <div class="el-stat"><span class="el-stat-val">${(cv * 100).toFixed(0)}<small>%</small></span><span class="el-stat-label">Coefficient of variation</span></div>
        <div class="el-stat"><span class="el-stat-val">${lag1.toFixed(2)}</span><span class="el-stat-label">Lag-1 autocorrelation</span></div>
        <div class="el-stat"><span class="el-stat-val">${stdDev.toFixed(1)}<small>h</small></span><span class="el-stat-label">Duration std dev</span></div>
        <div class="el-stat"><span class="el-stat-val">${medianCycles.toFixed(1)}</span><span class="el-stat-label">Median cycles / night</span></div>
      </div>
      <div class="panel">
        <div class="panel-label">Deviation from ${target.toFixed(1)}h median target · ${points.length} episodes · blue = compression · amber = extension</div>
        ${elasticityBandSVG(points)}
      </div>
      <div class="elasticity-bottom">
        <div class="panel">
          <div class="panel-label">Sleep cycles per episode · green = healthy range (4–5)</div>
          ${cycleDistSVG(cycleDist, points.length)}
        </div>
        <div class="panel">
          <div class="panel-label">Lag-1 · amber = compression→extension · violet = same direction</div>
          ${lagScatterSVG(points)}
        </div>
      </div>
    `;
  }

  function chapterDriftClock() {
    const realSrc = D.sessionsFull || D.sessions;
    const src = driftDemoMode ? generateDemoSessions(365) : realSrc;
    const demoLabel = driftDemoMode
      ? `<span class="drift-demo-badge">Demo — regular schedule</span>`
      : "";
    if (!D.sessionsFull && !driftPlayback._loading) {
      driftPlayback._loading = true;
      window.loadFullSessions().then(() => {
        driftPlayback._loading = false;
        driftPlayback.frames = null;
        driftPlayback.frameFrac = 0;
        render();
      }).catch(() => { driftPlayback._loading = false; });
    }
    if (!driftPlayback.frames || driftPlayback._src !== src) {
      driftPlayback.frames = computeDriftFrames(src, driftPlayback.windowSize);
      driftPlayback._src = src;
      driftPlayback.frameFrac = Math.min(driftPlayback.frameFrac, Math.max(0, driftPlayback.frames.length - 1));
    }
    const frames = driftPlayback.frames;
    const nf = frames.length;
    const curFrame = interpolatedFrame(driftPlayback.frameFrac);
    const curDateStr = curFrame ? curFrame.date.toLocaleDateString("fr-FR", { month: "short", year: "numeric" }) : "—";
    const w = driftPlayback.windowSize;
    const spd = driftPlayback.speed;
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">10</span><span class="eyebrow">Sleep clock drift</span></div>
        <h2>Your nights, mapped <em>around the clock</em>.</h2>
        <p class="chapter-desc">
          Three readings of ${driftDemoMode ? "a simulated regular schedule" : `your ${src.length} nights`} — each revealing a different dimension of the pattern.
          <button class="history-toggle" id="drift-demo-toggle">${driftDemoMode ? "← Your data" : "Demo (regular schedule) →"}</button>
          ${demoLabel}
        </p>
      </div>
      <div class="panel">
        <div class="drift-grid">
          <div class="drift-option">
            <div class="drift-option-label">A — Frequency density</div>
            ${driftDensitySVG(src)}
            <div class="drift-option-desc">Opacity = % of nights asleep at each 15-min slot. <em>Dense arc</em> = your core sleep window. <em>Fuzziness</em> at edges = bedtime variability. Irregular schedule → full ring.</div>
          </div>
          <div class="drift-option">
            <div class="drift-option-label">B — Trajectories (drift)</div>
            ${driftSpaghettiSVG(src)}
            <div class="drift-option-desc">Every session as an arc. Violet = oldest, amber = newest. <em>Colour shift in position</em> = temporal drift. If violet and amber arcs don't overlap, your schedule has moved.</div>
          </div>
          <div class="drift-option">
            <div class="drift-option-label">C — Playback</div>
            ${driftPlaybackSVG(frames, w)}
            <div class="drift-pb-controls">
              <button class="drift-pb-play" id="drift-pb-play">${driftPlayback.playing ? "⏸" : "▶"}</button>
              <input type="range" class="drift-pb-scrub" id="drift-pb-scrub" min="0" max="${nf - 1}" step="any" value="${driftPlayback.frameFrac}">
              <span class="drift-pb-date" id="drift-pb-date">${curDateStr}</span>
            </div>
            <div class="drift-pills-row">
              <div class="drift-window-pills">
                ${[[7,"7n"],[14,"14n"],[30,"30n"],[0,"All"]].map(([n,l]) => `<button class="drift-pill${w === n ? " active" : ""}" data-drift-window="${n}">${l}</button>`).join("")}
              </div>
              <div class="drift-speed-pills">
                ${[0.5, 1, 2, 4].map(s => `<button class="drift-pill${spd === s ? " active" : ""}" data-drift-speed="${s}">${s}×</button>`).join("")}
              </div>
            </div>
            <div class="drift-option-desc">${w > 0 ? `Last ${Math.min(w, src.length)}` : src.length} nights${driftPlayback._loading ? " · loading…" : ""}. One frame per real night.</div>
          </div>
        </div>
      </div>
    `;
  }

  function nightCardHTML(s, rank) {
    const total = s.duration_ms;
    const segs = ["deep", "light", "rem", "awake"].map((k) => `<div class="s ${k}" style="width:${(s.totals[k] / total) * 100}%"></div>`).join("");
    return `<div class="night-card"><div class="rank">Rank ${pad(rank)}</div><div class="date">${fmtDateLong(s.sleep_end)}</div><div class="score"><span class="n">${s.score}</span><span class="label">/ 100</span></div><div class="mini-stages">${segs}</div><div class="times"><span>${fmtHM(s.sleep_start)}</span><span>${hoursMinutes(s.duration_ms)}</span><span>${fmtHM(s.sleep_end)}</span></div></div>`;
  }

  function chapterCards() {
    const src = D.sessionsFull || D.sessions;
    const top = [...src].sort((a, b) => b.score - a.score).slice(0, 4);
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">07</span><span class="eyebrow">Top nights</span></div>
        <h2>The nights that <em>earned their stars</em>.</h2>
        <p class="chapter-desc">Your four highest-scoring nights — a blend of duration, efficiency, REM%, and low fragmentation.</p>
      </div>
      <div class="cards-grid">${top.map((s, i) => nightCardHTML(s, i + 1)).join("")}</div>
    `;
  }

  function chapterAgenda() {
    const last7 = D.sessions.slice(-7);
    const startHour = 18, totalH = 18;
    const hoursList = [];
    for (let i = 0; i <= totalH; i++) {
      const hour = (startHour + i) % 24;
      const topPct = (i / totalH) * 100;
      hoursList.push(`<div style="position:absolute;top:${topPct}%;right:8px;transform:translateY(-50%);font-family:'Geist Mono',monospace;font-size:9px;color:#6a6488;">${pad(hour)}:00</div>`);
    }
    const heads = last7.map((s) => {
      const wake = new Date(s.sleep_end);
      return `<div class="agenda-day-head">${fmtDay(wake)}<span class="num">${pad(wake.getDate())}</span></div>`;
    }).join("");
    const days = last7.map((s) => {
      const wake = new Date(s.sleep_end); wake.setHours(0, 0, 0, 0);
      const anchor = wake.getTime() - (24 - startHour) * 3600000;
      const spanMs = totalH * 3600000;
      const segs = s.stages.map((st) => {
        const topPct = ((st.stage_start.getTime() - anchor) / spanMs) * 100;
        const heightPct = ((st.stage_end - st.stage_start) / spanMs) * 100;
        return `<div class="agenda-seg ${st.stage_type}" style="top:${topPct}%;height:${heightPct}%"></div>`;
      }).join("");
      return `<div class="agenda-day" style="height:460px;">${segs}</div>`;
    }).join("");
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">08</span><span class="eyebrow">Week agenda</span></div>
        <h2>Your last seven nights, <em>stood upright</em>.</h2>
        <p class="chapter-desc">Time flows downward. Weekends push bedtime later.</p>
      </div>
      <div class="panel">
        <div style="display:grid;grid-template-columns:60px repeat(7,1fr);gap:2px;">
          <div></div>${heads}
          <div style="position:relative; height:460px; border-right:1px solid rgba(255,255,255,0.06);">${hoursList.join("")}</div>
          ${days}
        </div>
      </div>
    `;
  }

  function groupByNight(sessions, gapHours = 2) {
    if (!sessions.length) return [];
    const sorted = [...sessions].sort((a, b) => a.sleep_start - b.sleep_start);
    const episodes = [];
    let current = [sorted[0]];
    for (let i = 1; i < sorted.length; i++) {
      const lastEnd = current[current.length - 1].sleep_end;
      const gap = (sorted[i].sleep_start - lastEnd) / 3600000;
      if (gap < gapHours) {
        current.push(sorted[i]);
      } else {
        episodes.push(current);
        current = [sorted[i]];
      }
    }
    episodes.push(current);
    return episodes.map((segs) => {
      const totalMs = segs.reduce((a, s) => a + s.duration_ms, 0);
      const main = segs.reduce((a, b) => a.duration_ms >= b.duration_ms ? a : b);
      return { totalMs, totalHours: totalMs / 3600000, mainSeg: main, segments: segs };
    });
  }

  function naturalSleepTarget() {
    const src = D.sessionsFull || D.sessions;
    const episodes = groupByNight(src);
    if (!episodes.length) return 8;
    const durations = episodes.map((e) => e.totalHours).sort((a, b) => a - b);
    const mid = Math.floor(durations.length / 2);
    return durations.length % 2 === 0
      ? (durations[mid - 1] + durations[mid]) / 2
      : durations[mid];
  }

  function metricsAvailableMonths() {
    const src = D.sessionsFull || D.sessions;
    const months = new Set();
    for (const s of src) {
      const d = s.sleep_end;
      months.add(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`);
    }
    return [...months].sort();
  }

  function sessionsForMetricsMonth() {
    const src = D.sessionsFull || D.sessions;
    const months = metricsAvailableMonths();
    if (!months.length) return D.sessions;
    const m = metricsMonth || months[months.length - 1];
    return src.filter((s) => {
      const d = s.sleep_end;
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}` === m;
    });
  }

  function metricsMonthLabel(key) {
    const [y, mo] = key.split("-");
    return new Date(parseInt(y), parseInt(mo) - 1, 1).toLocaleDateString("en-US", { month: "long", year: "numeric" });
  }

  function computeMetricsSummary(sessions) {
    if (!sessions.length) return { debt: 0, regularity: 100, bedtimeStdDev: 0, avgRem: 0, avgDeep: 0, avgLight: 0, avgAwake: 0, avgRestingHR: 0 };
    const nights = groupByNight(sessions);
    const nN = nights.length;
    const target = naturalSleepTarget();
    let debt = 0;
    for (const n of nights) debt += target - n.totalHours;
    const bedMins = nights.map((n) => {
      let m = n.mainSeg.sleep_start.getHours() * 60 + n.mainSeg.sleep_start.getMinutes();
      if (m < 12 * 60) m += 24 * 60;
      return m;
    });
    const meanBed = bedMins.reduce((a, b) => a + b, 0) / nN;
    const bedtimeStdDev = Math.sqrt(bedMins.reduce((a, b) => a + (b - meanBed) ** 2, 0) / nN);
    const regularity = Math.max(0, 100 - bedtimeStdDev * 0.8);
    const dateKeys = new Set(sessions.map((s) => s.date_key));
    const hrVals = Object.entries(D.hr).filter(([k]) => dateKeys.has(k)).map(([, v]) => v);
    const avgRestingHR = hrVals.length ? hrVals.reduce((a, b) => a + b, 0) / hrVals.length : 0;
    return {
      debt, regularity, bedtimeStdDev,
      avgRem: sessions.reduce((a, s) => a + s.totals.rem, 0) / nN / 3600000,
      avgDeep: sessions.reduce((a, s) => a + s.totals.deep, 0) / nN / 3600000,
      avgLight: sessions.reduce((a, s) => a + s.totals.light, 0) / nN / 3600000,
      avgAwake: sessions.reduce((a, s) => a + s.totals.awake, 0) / nN / 3600000,
      avgRestingHR,
    };
  }

  function reRenderMetrics() {
    const scrollY = window.scrollY;
    render();
    requestAnimationFrame(() => window.scrollTo(0, scrollY));
  }

  function bedtimeScatterSVG(sessions) {
    const nights = groupByNight(sessions);
    const w = 500, h = 200, padL = 40, padR = 10, padT = 10, padB = 10;
    const innerW = w - padL - padR, innerH = h - padT - padB;
    const yMin = 18 * 60, yRange = 24 * 60;
    const n = nights.length;
    let dots = "";
    const values = [];
    for (let i = 0; i < n; i++) {
      let m = nights[i].mainSeg.sleep_start.getHours() * 60 + nights[i].mainSeg.sleep_start.getMinutes();
      if (m < 12 * 60) m += 24 * 60;
      values.push(m);
      const x = padL + (n > 1 ? (i / (n - 1)) : 0.5) * innerW;
      const y = padT + ((m - yMin) / yRange) * innerH;
      dots += `<circle class="scatter-dot" cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="2.5"/>`;
    }
    const mean = values.reduce((a, b) => a + b, 0) / values.length;
    const meanY = padT + ((mean - yMin) / yRange) * innerH;
    const horizLine = `<line class="scatter-guide" x1="${padL}" x2="${w - padR}" y1="${meanY.toFixed(1)}" y2="${meanY.toFixed(1)}"/>`;
    let yTicks = "";
    for (let step = 0; step <= 8; step++) {
      const m = yMin + step * 3 * 60;
      const hh = Math.floor((m / 60) % 24);
      const y = padT + (step / 8) * innerH;
      const isMaj = step % 2 === 0;
      yTicks += `<text class="scatter-label" x="${padL - 6}" y="${y.toFixed(1)}" dominant-baseline="middle" text-anchor="end" opacity="${isMaj ? 1 : 0.45}">${pad(hh)}h</text>`;
      yTicks += `<line x1="${padL}" x2="${w - padR}" y1="${y.toFixed(1)}" y2="${y.toFixed(1)}" stroke="rgba(255,255,255,${isMaj ? 0.06 : 0.03})"/>`;
    }
    return `<svg class="scatter-svg" viewBox="0 0 ${w} ${h}" preserveAspectRatio="none">${yTicks}${horizLine}${dots}</svg>`;
  }

  function hrSparkSVG(sessions) {
    const w = 500, h = 140, padL = 40, padR = 10, padT = 10, padB = 24;
    const innerW = w - padL - padR, innerH = h - padT - padB;
    const withHR = sessions.filter((s) => D.hr[s.date_key] !== undefined);
    if (withHR.length < 2) return `<div class="metric-no-data">No HR data for this period</div>`;
    const vals = withHR.map((s) => D.hr[s.date_key]);
    const min = Math.min(...vals) - 2, max = Math.max(...vals) + 2, range = max - min;
    const pts = vals.map((v, i) => [padL + (i / (vals.length - 1)) * innerW, padT + innerH - ((v - min) / range) * innerH]);
    const line = pts.map((p, i) => (i === 0 ? `M${p[0]},${p[1]}` : `L${p[0]},${p[1]}`)).join(" ");
    const area = `${line} L${pts[pts.length - 1][0]},${padT + innerH} L${pts[0][0]},${padT + innerH} Z`;
    let yTicks = "";
    for (let i = 0; i <= 2; i++) {
      const v = min + (range * i) / 2;
      const y = padT + innerH - ((v - min) / range) * innerH;
      yTicks += `<text class="scatter-label" x="${padL - 6}" y="${y + 3}" text-anchor="end">${Math.round(v)}</text><line class="scatter-guide" x1="${padL}" x2="${w - padR}" y1="${y}" y2="${y}"/>`;
    }
    return `<svg class="hr-svg" viewBox="0 0 ${w} ${h}" preserveAspectRatio="none"><defs><linearGradient id="hrGrad" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stop-color="oklch(0.82 0.13 220)" stop-opacity="0.5"/><stop offset="100%" stop-color="oklch(0.82 0.13 220)" stop-opacity="0"/></linearGradient></defs>${yTicks}<path class="hr-area" d="${area}"/><path class="hr-line" d="${line}"/></svg>`;
  }

  function debtBars(sessions) {
    const nights = groupByNight(sessions);
    const target = naturalSleepTarget();
    return nights.map((n) => {
      const wake = n.mainSeg.sleep_end;
      const diff = n.totalHours - target;
      const pct = Math.min(50, Math.abs(diff) * 14);
      const side = diff < 0 ? "deficit" : "surplus";
      const style = diff < 0 ? `right:50%;width:${pct}%` : `left:50%;width:${pct}%`;
      return `<div class="debt-row"><span>${fmtDay(wake)} ${pad(wake.getDate())}</span><div class="debt-bar-track"><div class="debt-bar ${side}" style="${style}"></div></div><span style="text-align:right;color:${diff<0?"oklch(0.7 0.17 25)":"oklch(0.78 0.14 155)"}">${diff>=0?"+":""}${diff.toFixed(1)}h</span></div>`;
    }).join("");
  }

  function stageGauge(sessions) {
    const targets = { deep: 0.15, rem: 0.22, light: 0.55, awake: 0.08 };
    const total = sessions.reduce((a, s) => a + s.duration_ms, 0);
    const totals = { deep: 0, rem: 0, light: 0, awake: 0 };
    for (const s of sessions) for (const k of Object.keys(totals)) totals[k] += s.totals[k];
    return Object.keys(targets).map((k) => {
      const pct = totals[k] / total, tpct = targets[k];
      const width = Math.min(100, (pct / (tpct * 1.6)) * 100);
      const targetX = (tpct / (tpct * 1.6)) * 100;
      return `<div class="stage-bar-row"><span class="name">${k}</span><div class="track"><div class="fill ${k}" style="width:${width}%"></div><div class="target" style="left:${targetX}%"></div></div><span class="val">${(pct * 100).toFixed(1)}%</span></div>`;
    }).join("");
  }

  function chapterMetrics() {
    if (!D.sessionsFull && !metricsFullLoading) {
      metricsFullLoading = true;
      window.loadFullSessions?.().then(() => {
        D = window.SleepData;
        metricsFullLoading = false;
        reRenderMetrics();
      });
    }
    const months = metricsAvailableMonths();
    const curMonth = metricsMonth || (months.length ? months[months.length - 1] : null);
    const mIdx = months.indexOf(curMonth);
    const mSessions = sessionsForMetricsMonth();
    const summary = computeMetricsSummary(mSessions);
    const debtSign = summary.debt > 0 ? "" : "+";
    const remTotal = summary.avgRem + summary.avgDeep + summary.avgLight + summary.avgAwake;
    const remPct = remTotal > 0 ? Math.round((summary.avgRem / remTotal) * 100) : 0;
    const monthLabel = curMonth ? metricsMonthLabel(curMonth) : "—";
    const loading = metricsFullLoading && !D.sessionsFull;
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">09</span><span class="eyebrow">Metrics</span></div>
        <h2>The <em>numbers</em> behind the nights.</h2>
        <p class="chapter-desc">Regularity, debt, stage composition, resting heart rate. Click any card for full history.</p>
      </div>
      <div class="metrics-month-nav">
        <button class="metrics-month-btn" id="metrics-month-prev" ${mIdx <= 0 ? "disabled" : ""}>←</button>
        <span class="metrics-month-label">${monthLabel}</span>
        <button class="metrics-month-btn" id="metrics-month-next" ${mIdx >= months.length - 1 ? "disabled" : ""}>→</button>
        ${loading ? `<span class="metrics-loading">loading full history…</span>` : `<span class="metrics-month-hint">${months.length} months · ${mSessions.length} nights</span>`}
      </div>
      <div class="metrics-grid">
        <div class="metric-card metric-card-clickable" data-view="history/scatter">
          <div class="head"><h3>Bedtime regularity</h3><span class="hint">σ = ${summary.bedtimeStdDev.toFixed(0)} min</span></div>
          <div class="metric-big">${Math.round(summary.regularity)}<small>/100</small></div>
          <div class="metric-body">${mSessions.length ? bedtimeScatterSVG(mSessions) : '<div class="metric-no-data">No data</div>'}</div>
          <p class="metric-caption">Each dot is a bedtime. The tighter the cloud, the more your circadian rhythm trusts you.</p>
        </div>
        <div class="metric-card metric-card-clickable" data-view="history/debt">
          <div class="head"><h3>Sleep debt</h3><span class="hint">vs ${naturalSleepTarget().toFixed(1)}h median</span></div>
          <div class="metric-big">${debtSign}${(-summary.debt).toFixed(1)}<small>h cumulative</small></div>
          <div class="metric-body" style="margin-top:10px;">${debtBars(mSessions.slice(-30))}</div>
          <p class="metric-caption">Last 30 nights. Bars left are hours short; bars right are hours over.</p>
        </div>
        <div class="metric-card metric-card-clickable" data-view="history/stages">
          <div class="head"><h3>Stage mix</h3><span class="hint">month avg</span></div>
          <div class="metric-big">${remPct}<small>% REM</small></div>
          <div class="metric-body"><div class="stage-bar-group">${mSessions.length ? stageGauge(mSessions) : ""}</div></div>
          <p class="metric-caption">White ticks mark healthy targets. Deep anchors physical recovery; REM consolidates memory.</p>
        </div>
        <div class="metric-card metric-card-clickable" data-view="history/hr">
          <div class="head"><h3>Nighttime heart rate</h3><span class="hint">resting bpm</span></div>
          <div class="metric-big">${summary.avgRestingHR ? Math.round(summary.avgRestingHR) : "—"}<small>bpm avg</small></div>
          <div class="metric-body">${hrSparkSVG(mSessions)}</div>
          <p class="metric-caption">Lower is calmer. Dips after good workouts; spikes after late meals or stress.</p>
        </div>
      </div>
    `;
  }

  function footer() {
    return `<footer><span>Nightfall · Samsung Health pipeline</span><span>health.db · ${D.sessions.length} sessions · ${D.sessions.reduce((a,s)=>a+s.stages.length,0)} stages</span><span>Press T for tweaks</span></footer>`;
  }

  function tweaksPanel() {
    return `
      <div id="tweaks">
        <h4>Tweaks</h4>
        <div class="tweak-row">
          <label>Accent</label>
          <div class="tweak-pills" data-key="accent">
            <button class="tweak-pill ${PREFS.accent==='violet'?'active':''}" data-v="violet">Violet</button>
            <button class="tweak-pill ${PREFS.accent==='cyan'?'active':''}" data-v="cyan">Cyan</button>
            <button class="tweak-pill ${PREFS.accent==='amber'?'active':''}" data-v="amber">Amber</button>
          </div>
        </div>
        <div class="tweak-row">
          <label>Grain</label>
          <div class="tweak-pills" data-key="showGrain">
            <button class="tweak-pill ${PREFS.showGrain?'active':''}" data-v="true">On</button>
            <button class="tweak-pill ${!PREFS.showGrain?'active':''}" data-v="false">Off</button>
          </div>
        </div>
      </div>
    `;
  }

  function render() {
    if (driftPlayback.rafId) { cancelAnimationFrame(driftPlayback.rafId); driftPlayback.rafId = null; driftPlayback.playing = false; }
    app.innerHTML = topbar() + hero() + chapterTimeline() + chapterHypnogram() + chapterRadial() + chapterCards() + chapterAgenda() + chapterMetrics() + chapterElasticity() + chapterDriftClock() + footer() + tweaksPanel() + `<div id="hover-tip"></div>`;
    bindEvents();
    applyPrefs();
  }

  function bindEvents() {
    document.getElementById("hypno-prev")?.addEventListener("click", () => {
      focusIdx = (focusIdx - 1 + D.sessions.length) % D.sessions.length;
      PREFS.focusNightIndex = focusIdx; savePrefs(); render();
    });
    document.getElementById("hypno-next")?.addEventListener("click", () => {
      focusIdx = (focusIdx + 1) % D.sessions.length;
      PREFS.focusNightIndex = focusIdx; savePrefs(); render();
    });
    document.getElementById("timeline-to-history")?.addEventListener("click", () => {
      navigateTo("history/timeline");
    });
    document.getElementById("metrics-month-prev")?.addEventListener("click", () => {
      const months = metricsAvailableMonths();
      const cur = metricsMonth || months[months.length - 1];
      const idx = months.indexOf(cur);
      if (idx > 0) { metricsMonth = months[idx - 1]; reRenderMetrics(); }
    });
    document.getElementById("metrics-month-next")?.addEventListener("click", () => {
      const months = metricsAvailableMonths();
      const cur = metricsMonth || months[months.length - 1];
      const idx = months.indexOf(cur);
      if (idx < months.length - 1) { metricsMonth = months[idx + 1]; reRenderMetrics(); }
    });
    document.querySelectorAll(".metric-card-clickable[data-view]").forEach((card) => {
      card.addEventListener("click", () => navigateTo(card.dataset.view));
    });
    document.getElementById("drift-demo-toggle")?.addEventListener("click", () => {
      driftDemoMode = !driftDemoMode;
      driftPlayback.frames = null;
      driftPlayback.frameFrac = 0;
      render();
    });
    document.getElementById("drift-pb-play")?.addEventListener("click", () => {
      driftPlayback.playing = !driftPlayback.playing;
      const btn = document.getElementById("drift-pb-play");
      if (btn) btn.textContent = driftPlayback.playing ? "⏸" : "▶";
      if (driftPlayback.playing) {
        if (driftPlayback.frameFrac >= driftPlayback.frames.length - 1) driftPlayback.frameFrac = 0;
        let lastTs = null;
        function loop(ts) {
          if (!driftPlayback.playing || !document.getElementById("drift-pb-arc")) {
            driftPlayback.playing = false; driftPlayback.rafId = null; return;
          }
          if (lastTs !== null) {
            driftPlayback.frameFrac += (ts - lastTs) * (8 * driftPlayback.speed / 1000);
            if (driftPlayback.frameFrac >= driftPlayback.frames.length - 1) {
              driftPlayback.frameFrac = driftPlayback.frames.length - 1;
              updateDriftArc(interpolatedFrame(driftPlayback.frameFrac));
              driftPlayback.playing = false;
              const b = document.getElementById("drift-pb-play");
              if (b) b.textContent = "▶";
              driftPlayback.rafId = null; return;
            }
            updateDriftArc(interpolatedFrame(driftPlayback.frameFrac));
          }
          lastTs = ts;
          driftPlayback.rafId = requestAnimationFrame(loop);
        }
        driftPlayback.rafId = requestAnimationFrame(loop);
      } else {
        if (driftPlayback.rafId) { cancelAnimationFrame(driftPlayback.rafId); driftPlayback.rafId = null; }
      }
    });
    document.getElementById("drift-pb-scrub")?.addEventListener("input", (e) => {
      if (driftPlayback.rafId) { cancelAnimationFrame(driftPlayback.rafId); driftPlayback.rafId = null; }
      driftPlayback.playing = false;
      const btn = document.getElementById("drift-pb-play");
      if (btn) btn.textContent = "▶";
      driftPlayback.frameFrac = parseFloat(e.target.value);
      updateDriftArc(interpolatedFrame(driftPlayback.frameFrac));
    });
    document.querySelectorAll("[data-drift-window]").forEach(btn => {
      btn.addEventListener("click", () => {
        if (driftPlayback.rafId) { cancelAnimationFrame(driftPlayback.rafId); driftPlayback.rafId = null; }
        driftPlayback.playing = false;
        driftPlayback.windowSize = parseInt(btn.dataset.driftWindow);
        driftPlayback.frames = null;
        driftPlayback.frameFrac = 0;
        render();
      });
    });
    document.querySelectorAll("[data-drift-speed]").forEach(btn => {
      btn.addEventListener("click", () => {
        driftPlayback.speed = parseFloat(btn.dataset.driftSpeed);
        btn.closest(".drift-speed-pills").querySelectorAll(".drift-pill").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
      });
    });
    const tip = document.getElementById("hover-tip");
    document.querySelectorAll("[data-tip]").forEach((el) => {
      el.addEventListener("mouseenter", () => { tip.textContent = el.dataset.tip; tip.classList.add("show"); });
      el.addEventListener("mousemove", (e) => { tip.style.left = e.clientX + 14 + "px"; tip.style.top = e.clientY + 14 + "px"; });
      el.addEventListener("mouseleave", () => tip.classList.remove("show"));
    });
    document.querySelectorAll(".tweak-pills").forEach((group) => {
      const key = group.dataset.key;
      group.querySelectorAll(".tweak-pill").forEach((btn) => {
        btn.addEventListener("click", () => {
          let v = btn.dataset.v;
          if (v === "true") v = true; else if (v === "false") v = false;
          PREFS[key] = v; savePrefs();
          try { window.parent.postMessage({ type: "__edit_mode_set_keys", edits: { [key]: v } }, "*"); } catch (e) {}
          group.querySelectorAll(".tweak-pill").forEach((b) => b.classList.remove("active"));
          btn.classList.add("active");
          applyPrefs();
        });
      });
    });
  }

  function applyPrefs() {
    const root = document.documentElement;
    const accentMap = { violet: "oklch(0.68 0.14 295)", cyan: "oklch(0.82 0.13 220)", amber: "oklch(0.78 0.15 60)" };
    root.style.setProperty("--stage-rem", accentMap[PREFS.accent] || accentMap.violet);
    root.style.setProperty("--grain-opacity", PREFS.showGrain ? "0.04" : "0");
  }

  document.addEventListener("mousemove", (e) => {
    document.documentElement.style.setProperty("--mx", e.clientX + "px");
    document.documentElement.style.setProperty("--my", e.clientY + "px");
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "t" || e.key === "T") document.getElementById("tweaks")?.classList.toggle("show");
    if (e.key === "ArrowLeft") { focusIdx = (focusIdx - 1 + D.sessions.length) % D.sessions.length; render(); }
    if (e.key === "ArrowRight") { focusIdx = (focusIdx + 1) % D.sessions.length; render(); }
  });
  window.addEventListener("message", (e) => {
    if (!e.data) return;
    if (e.data.type === "__activate_edit_mode") document.getElementById("tweaks")?.classList.add("show");
    if (e.data.type === "__deactivate_edit_mode") document.getElementById("tweaks")?.classList.remove("show");
  });
  try { window.parent.postMessage({ type: "__edit_mode_available" }, "*"); } catch (e) {}

  window.addEventListener("hashchange", () => {
    const hash = window.location.hash.replace("#", "");
    currentView = hash || "dashboard";
    window.scrollTo(0, 0);
    renderApp();
  });

  window.render = function () {
    D = window.SleepData;
    focusIdx = Math.min(Math.max(0, PREFS.focusNightIndex), D.sessions.length - 1);
    const hash = window.location.hash.replace("#", "");
    if (hash) currentView = hash;
    renderApp();
  };
})();
