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

  function chapterHeatmap() {
    const rows = [];
    let header = '<div></div>';
    for (let h = 0; h < 24; h++) header += `<div class="hour-head">${h % 3 === 0 ? pad(h) : ""}</div>`;
    rows.push(header);

    const hourMap = {};
    for (const s of D.sessions) {
      let cursor = new Date(s.sleep_start);
      cursor.setMinutes(0, 0, 0);
      while (cursor < s.sleep_end) {
        const key = cursor.toISOString().slice(0, 10);
        const h = cursor.getHours();
        if (!hourMap[key]) hourMap[key] = {};
        if (!hourMap[key][h]) hourMap[key][h] = { stages: {} };
        const hs = cursor.getTime(), he = hs + 3600000;
        for (const st of s.stages) {
          const ss = st.stage_start.getTime(), se = st.stage_end.getTime();
          if (ss < he && se > hs) {
            const overlap = Math.min(he, se) - Math.max(hs, ss);
            hourMap[key][h].stages[st.stage_type] = (hourMap[key][h].stages[st.stage_type] || 0) + overlap;
          }
        }
        cursor = new Date(cursor.getTime() + 3600000);
      }
    }

    for (const s of D.sessions) {
      const day = new Date(s.sleep_end);
      day.setHours(0, 0, 0, 0);
      const label = `${fmtDay(day)} ${day.getDate()}`;
      let row = `<div class="day-label">${label}</div>`;
      for (let h = 0; h < 24; h++) {
        let cell = null;
        const yesterday = new Date(day); yesterday.setDate(yesterday.getDate() - 1);
        const todayKey = day.toISOString().slice(0, 10);
        const yestKey = yesterday.toISOString().slice(0, 10);
        if (h >= 18 && hourMap[yestKey]?.[h]) cell = hourMap[yestKey][h];
        else if (h < 18 && hourMap[todayKey]?.[h]) cell = hourMap[todayKey][h];
        if (cell) {
          let best = null, max = 0;
          for (const [t, ms] of Object.entries(cell.stages)) if (ms > max) { max = ms; best = t; }
          const tip = Object.entries(cell.stages).map(([t, ms]) => `${t}: ${Math.round(ms / 60000)}m`).join(" · ");
          row += `<div class="cell ${best}" data-tip="${fmtDateLong(day)} · ${pad(h)}:00\n${tip}"></div>`;
        } else row += `<div class="cell"></div>`;
      }
      rows.push(row);
    }

    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">01</span><span class="eyebrow">Calendar heatmap</span></div>
        <h2>Thirty nights, <em>unfolded</em> hour by hour.</h2>
        <p class="chapter-desc">Each row is a night. Each cell is one hour, coloured by the dominant sleep stage.</p>
      </div>
      <div class="panel">
        <div class="heatmap-wrap"><div class="heatmap">${rows.join("")}</div></div>
        <div class="legend">
          <span><span class="sw" style="background:${STAGE_COLORS.deep}"></span>Deep</span>
          <span><span class="sw" style="background:${STAGE_COLORS.light}"></span>Light</span>
          <span><span class="sw" style="background:${STAGE_COLORS.rem}"></span>REM</span>
          <span><span class="sw" style="background:${STAGE_COLORS.awake}"></span>Awake</span>
        </div>
      </div>
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

  function chapterTimeline() {
    const days = aggregateByDay(D.sessions);
    const n = (D.sessionsFull || D.sessions).length;
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">02</span><span class="eyebrow">Stacked timeline</span></div>
        <h2>Your bedtime <em>drifts</em>, but not by much.</h2>
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
      `<div class="timeline-month-sep"><span class="timeline-month-sep-label">${g.label}</span><span class="timeline-month-count">${g.nights} nights</span></div>${g.rows.join("")}`
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
          <div class="timeline-axis timeline-axis-sticky">${timelineTicks()}</div>
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

  function renderApp() {
    if (currentView === "history/timeline") {
      renderHistoryTimelineAsync();
    } else {
      render();
    }
  }

  let focusIdx = 0;
  let currentView = "dashboard";

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
      const r1 = rOuter + 4, r2 = rOuter + (h % 6 === 0 ? 16 : 8);
      ticks += `<line x1="${cx + Math.cos(a) * r1}" y1="${cy + Math.sin(a) * r1}" x2="${cx + Math.cos(a) * r2}" y2="${cy + Math.sin(a) * r2}" stroke="rgba(255,255,255,${h % 6 === 0 ? 0.4 : 0.15})" stroke-width="1"/>`;
      if (h % 6 === 0) {
        const rt = rOuter + 32;
        ticks += `<text x="${cx + Math.cos(a) * rt}" y="${cy + Math.sin(a) * rt + 4}" text-anchor="middle" font-family="Geist Mono" font-size="11" fill="rgba(232,228,245,0.6)">${pad(h)}</text>`;
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
      const r1 = rOuter + 4, r2 = rOuter + (h % 6 === 0 ? 16 : 8);
      t += `<line x1="${cx + Math.cos(a) * r1}" y1="${cy + Math.sin(a) * r1}" x2="${cx + Math.cos(a) * r2}" y2="${cy + Math.sin(a) * r2}" stroke="rgba(255,255,255,${h % 6 === 0 ? 0.4 : 0.15})" stroke-width="1"/>`;
      if (h % 6 === 0) {
        const rt = rOuter + 30;
        t += `<text x="${cx + Math.cos(a) * rt}" y="${cy + Math.sin(a) * rt + 4}" text-anchor="middle" font-family="Geist Mono" font-size="11" fill="rgba(232,228,245,0.6)">${pad(h)}</text>`;
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
    return `<svg viewBox="0 0 ${w} ${w}" class="drift-svg"><circle cx="${cx}" cy="${cy}" r="${(rO + rI) / 2}" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="${rO - rI}"/>${arcs}${clockTicks(cx, cy, rO)}${center}</svg>`;
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
    return `<svg viewBox="0 0 ${w} ${w}" class="drift-svg"><circle cx="${cx}" cy="${cy}" r="${(rO + rI) / 2}" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="${rO - rI}"/>${arcs}${clockTicks(cx, cy, rO)}${legend}${center}</svg>`;
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

  function driftRollingAvgSVG(sessions) {
    const w = 400, cx = w / 2, cy = w / 2, rO = w * 0.43, rI = w * 0.29;
    const WINDOW = 7, n = sessions.length;
    const rMid = (rO + rI) / 2;
    let arcs = "";
    for (let i = WINDOW - 1; i < n; i++) {
      const slice = sessions.slice(i - WINDOW + 1, i + 1);
      const t = (n > WINDOW) ? (i - WINDOW + 1) / (n - WINDOW) : 0;
      const bedMins = slice.map(s => s.sleep_start.getHours() * 60 + s.sleep_start.getMinutes());
      const wakeMins = slice.map(s => s.sleep_end.getHours() * 60 + s.sleep_end.getMinutes());
      const avgBed = circularMeanMinutes(bedMins);
      const avgWake = circularMeanMinutes(wakeMins);
      const hue = 275 - t * 215;
      const lum = (0.55 + t * 0.23).toFixed(2);
      const color = `oklch(${lum} 0.16 ${hue.toFixed(0)})`;
      const a0 = -Math.PI / 2 + (avgBed / (24 * 60)) * Math.PI * 2;
      const a1 = -Math.PI / 2 + (avgWake / (24 * 60)) * Math.PI * 2;
      let span = a1 - a0; if (span <= 0) span += Math.PI * 2;
      const largeArc = span > Math.PI ? 1 : 0;
      const x0 = cx + Math.cos(a0) * rMid, y0 = cy + Math.sin(a0) * rMid;
      const x1 = cx + Math.cos(a1) * rMid, y1 = cy + Math.sin(a1) * rMid;
      arcs += `<path d="M${x0},${y0} A${rMid},${rMid} 0 ${largeArc} 1 ${x1},${y1}" fill="none" stroke="${color}" stroke-width="2.5" opacity="${(0.18 + t * 0.55).toFixed(3)}" stroke-linecap="round"/>`;
    }
    const yr0 = new Date(sessions[0].sleep_start).getFullYear();
    const yr1 = new Date(sessions[n - 1].sleep_start).getFullYear();
    const ly = w - 22, lx1 = cx - 56, lx2 = cx + 56;
    const legend = `<defs><linearGradient id="drgrd"><stop offset="0%" stop-color="oklch(0.55 0.16 275)"/><stop offset="100%" stop-color="oklch(0.78 0.16 60)"/></linearGradient></defs><rect x="${lx1}" y="${ly}" width="${lx2 - lx1}" height="3" fill="url(#drgrd)" rx="1.5"/><text x="${lx1}" y="${ly + 13}" font-family="Geist Mono" font-size="9" fill="rgba(232,228,245,0.45)" text-anchor="start">${yr0}</text><text x="${lx2}" y="${ly + 13}" font-family="Geist Mono" font-size="9" fill="rgba(232,228,245,0.45)" text-anchor="end">${yr1}</text>`;
    const center = `<text x="${cx}" y="${cy - 4}" text-anchor="middle" font-family="Instrument Serif" font-size="32" fill="#e8e4f5">${WINDOW}d avg</text><text x="${cx}" y="${cy + 16}" text-anchor="middle" font-family="Geist Mono" font-size="9" fill="#6a6488" letter-spacing="0.12em">ROLLING</text>`;
    return `<svg viewBox="0 0 ${w} ${w}" class="drift-svg"><circle cx="${cx}" cy="${cy}" r="${(rO + rI) / 2}" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="${rO - rI}"/>${arcs}${clockTicks(cx, cy, rO)}${legend}${center}</svg>`;
  }

  function chapterDriftClock() {
    const src = D.sessionsFull || D.sessions;
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">10</span><span class="eyebrow">Sleep clock drift</span></div>
        <h2>Your nights, mapped <em>around the clock</em>.</h2>
        <p class="chapter-desc">Three readings of the same 2+ years of sleep — each revealing a different dimension of the pattern.</p>
      </div>
      <div class="panel">
        <div class="drift-grid">
          <div class="drift-option">
            <div class="drift-option-label">A — Frequency density</div>
            ${driftDensitySVG(src)}
            <div class="drift-option-desc">Opacity = fraction of nights asleep during each 15-min slot. Fuzziness at the edges shows variability.</div>
          </div>
          <div class="drift-option">
            <div class="drift-option-label">B — Trajectories (drift)</div>
            ${driftSpaghettiSVG(src)}
            <div class="drift-option-desc">Every session drawn as an arc. Violet = oldest nights, amber = most recent. Density reveals the core window; colour reveals temporal drift.</div>
          </div>
          <div class="drift-option">
            <div class="drift-option-label">C — 7-night rolling average</div>
            ${driftRollingAvgSVG(src)}
            <div class="drift-option-desc">Each stroke is the average bedtime→wake over a 7-night window. Violet = 2023, amber = 2026. Read the drift as a shifting arc.</div>
          </div>
        </div>
      </div>
    `;
  }

  function smCell(session, idx) {
    const w = 100, h = 36;
    const yForStage = { awake: h * 0.08, rem: h * 0.3, light: h * 0.55, deep: h * 0.92 };
    const start = session.sleep_start.getTime(), total = session.duration_ms;
    let segs = "";
    for (const st of session.stages) {
      const x0 = ((st.stage_start.getTime() - start) / total) * w;
      const x1 = ((st.stage_end.getTime() - start) / total) * w;
      const y = yForStage[st.stage_type];
      segs += `<line x1="${x0}" x2="${x1}" y1="${y}" y2="${y}" stroke="${STAGE_COLORS[st.stage_type]}" stroke-width="2.2" stroke-linecap="round"/>`;
    }
    const wake = new Date(session.sleep_end);
    return `<div class="sm-cell${idx === focusIdx ? ' active' : ''}" data-idx="${idx}"><div class="sm-cell-head"><span class="sm-date">${fmtDay(wake)} ${wake.getDate()}</span><span class="sm-score">${session.score}</span></div><svg viewBox="0 0 ${w} ${h}" preserveAspectRatio="none" class="sm-svg">${segs}</svg><div class="sm-duration">${hoursMinutes(session.duration_ms)}</div></div>`;
  }

  function chapterSmallMultiples() {
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">05</span><span class="eyebrow">Small multiples</span></div>
        <h2>Thirty <em>signatures</em>, side by side.</h2>
        <p class="chapter-desc">Click any night to send it to the hypnogram and radial clock above.</p>
      </div>
      <div class="panel"><div class="sm-grid" id="sm-grid">${D.sessions.map(smCell).join("")}</div></div>
    `;
  }

  function chapterRidgeline() {
    const w = 1000, rowH = 22, amp = 28, n = D.sessions.length, h = n * rowH + 80;
    const padL = 80, padR = 20, innerW = w - padL - padR, spanH = 18;
    let paths = "";
    for (let i = 0; i < n; i++) {
      const s = D.sessions[i];
      const y0 = 40 + i * rowH;
      const wake = new Date(s.sleep_end); wake.setHours(0, 0, 0, 0);
      const anchor = wake.getTime() - 6 * 3600000;
      const samples = 200;
      const points = [];
      for (let k = 0; k <= samples; k++) {
        const t = anchor + (k / samples) * spanH * 3600000;
        let depth = 0;
        for (const st of s.stages) {
          if (st.stage_start.getTime() <= t && t < st.stage_end.getTime()) {
            depth = { awake: 0.1, rem: 0.35, light: 0.6, deep: 1.0 }[st.stage_type];
            break;
          }
        }
        points.push([padL + (k / samples) * innerW, y0 - depth * amp]);
      }
      let d = `M${padL},${y0}`;
      for (const [x, y] of points) d += ` L${x.toFixed(1)},${y.toFixed(1)}`;
      d += ` L${padL + innerW},${y0} Z`;
      const label = `${fmtDay(wake)} ${pad(wake.getDate())}`;
      paths += `<path d="${d}" fill="url(#ridgeGrad)" stroke="oklch(0.75 0.12 280)" stroke-width="1" opacity="${0.3 + (i / n) * 0.55}"/><text class="ridge-label" x="${padL - 10}" y="${y0 + 3}" text-anchor="end">${label}</text>`;
    }
    let ticks = "";
    for (let k = 0; k <= spanH; k += 3) {
      const x = padL + (k / spanH) * innerW;
      const hour = (18 + k) % 24;
      ticks += `<text class="ridge-label" x="${x}" y="20" text-anchor="middle">${pad(hour)}:00</text><line x1="${x}" x2="${x}" y1="28" y2="${h - 20}" stroke="rgba(255,255,255,0.04)" stroke-width="1"/>`;
    }
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">06</span><span class="eyebrow">Ridgeline</span></div>
        <h2>A range of <em>mountains</em> — each one a night.</h2>
        <p class="chapter-desc">Depth maps to altitude. The thickest peaks are your deepest dives.</p>
      </div>
      <div class="panel">
        <svg viewBox="0 0 ${w} ${h}" class="ridge-svg" preserveAspectRatio="none" style="height:${h * 0.7}px">
          <defs><linearGradient id="ridgeGrad" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stop-color="oklch(0.7 0.15 285)" stop-opacity="0.75"/><stop offset="100%" stop-color="oklch(0.25 0.08 275)" stop-opacity="0.2"/></linearGradient></defs>
          ${ticks}${paths}
        </svg>
      </div>
    `;
  }

  function chapterCards() {
    const top = [...D.sessions].sort((a, b) => b.score - a.score).slice(0, 4);
    const cards = top.map((s, i) => {
      const total = s.duration_ms;
      const segs = ["deep", "light", "rem", "awake"].map((k) => `<div class="s ${k}" style="width:${(s.totals[k] / total) * 100}%"></div>`).join("");
      return `<div class="night-card"><div class="rank">Rank ${pad(i + 1)}</div><div class="date">${fmtDateLong(s.sleep_end)}</div><div class="score"><span class="n">${s.score}</span><span class="label">/ 100</span></div><div class="mini-stages">${segs}</div><div class="times"><span>${fmtHM(s.sleep_start)}</span><span>${hoursMinutes(s.duration_ms)}</span><span>${fmtHM(s.sleep_end)}</span></div></div>`;
    }).join("");
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">07</span><span class="eyebrow">Top nights</span></div>
        <h2>The nights that <em>earned their stars</em>.</h2>
        <p class="chapter-desc">Your four highest-scoring nights — a blend of duration, efficiency, REM%, and low fragmentation.</p>
      </div>
      <div class="cards-grid">${cards}</div>
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

  function bedtimeScatterSVG() {
    const w = 500, h = 140, padL = 40, padR = 10, padT = 10, padB = 24;
    const innerW = w - padL - padR, innerH = h - padT - padB;
    const yMinHour = 20 * 60, yRange = 6 * 60;
    let dots = "";
    const values = [];
    for (let i = 0; i < D.sessions.length; i++) {
      let minOfDay = D.sessions[i].sleep_start.getHours() * 60 + D.sessions[i].sleep_start.getMinutes();
      if (minOfDay < 12 * 60) minOfDay += 24 * 60;
      values.push(minOfDay);
      const x = padL + (i / (D.sessions.length - 1)) * innerW;
      const y = padT + ((minOfDay - yMinHour) / yRange) * innerH;
      dots += `<circle class="scatter-dot" cx="${x}" cy="${y}" r="3"/>`;
    }
    const mean = values.reduce((a, b) => a + b, 0) / values.length;
    const meanY = padT + ((mean - yMinHour) / yRange) * innerH;
    const horizLine = `<line class="scatter-guide" x1="${padL}" x2="${w - padR}" y1="${meanY}" y2="${meanY}"/>`;
    let yTicks = "";
    for (let m = yMinHour; m <= yMinHour + yRange; m += 60) {
      const y = padT + ((m - yMinHour) / yRange) * innerH;
      const hh = Math.floor((m / 60) % 24);
      yTicks += `<text class="scatter-label" x="${padL - 6}" y="${y + 3}" text-anchor="end">${pad(hh)}:00</text>`;
    }
    return `<svg class="scatter-svg" viewBox="0 0 ${w} ${h}" preserveAspectRatio="none">${yTicks}${horizLine}${dots}</svg>`;
  }

  function hrSparkSVG() {
    const w = 500, h = 140, padL = 40, padR = 10, padT = 10, padB = 24;
    const innerW = w - padL - padR, innerH = h - padT - padB;
    const vals = D.sessions.map((s) => D.hr[s.date_key]);
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

  function debtBars() {
    return D.sessions.slice(-14).map((s) => {
      const wake = new Date(s.sleep_end);
      const diff = s.duration_hours - 8;
      const pct = Math.min(50, Math.abs(diff) * 14);
      const side = diff < 0 ? "deficit" : "surplus";
      const style = diff < 0 ? `right:50%;width:${pct}%` : `left:50%;width:${pct}%`;
      return `<div class="debt-row"><span>${fmtDay(wake)} ${pad(wake.getDate())}</span><div class="debt-bar-track"><div class="debt-bar ${side}" style="${style}"></div></div><span style="text-align:right;color:${diff<0?"oklch(0.7 0.17 25)":"oklch(0.78 0.14 155)"}">${diff>=0?"+":""}${diff.toFixed(1)}h</span></div>`;
    }).join("");
  }

  function stageGauge() {
    const targets = { deep: 0.15, rem: 0.22, light: 0.55, awake: 0.08 };
    const total = D.sessions.reduce((a, s) => a + s.duration_ms, 0);
    const totals = { deep: 0, rem: 0, light: 0, awake: 0 };
    for (const s of D.sessions) for (const k of Object.keys(totals)) totals[k] += s.totals[k];
    return Object.keys(targets).map((k) => {
      const pct = totals[k] / total, tpct = targets[k];
      const width = Math.min(100, (pct / (tpct * 1.6)) * 100);
      const targetX = (tpct / (tpct * 1.6)) * 100;
      return `<div class="stage-bar-row"><span class="name">${k}</span><div class="track"><div class="fill ${k}" style="width:${width}%"></div><div class="target" style="left:${targetX}%"></div></div><span class="val">${(pct * 100).toFixed(1)}%</span></div>`;
    }).join("");
  }

  function chapterMetrics() {
    const summary = D.summary;
    const debtSign = summary.debt > 0 ? "" : "+";
    return `
      <div class="chapter">
        <div class="chapter-label"><span class="chapter-num">09</span><span class="eyebrow">Metrics</span></div>
        <h2>The <em>numbers</em> behind the nights.</h2>
        <p class="chapter-desc">Regularity, debt, stage composition, resting heart rate.</p>
      </div>
      <div class="metrics-grid">
        <div class="metric-card">
          <div class="head"><h3>Bedtime regularity</h3><span class="hint">σ = ${summary.bedtimeStdDev.toFixed(0)} min</span></div>
          <div class="metric-big">${Math.round(summary.regularity)}<small>/100</small></div>
          <div class="metric-body">${bedtimeScatterSVG()}</div>
          <p class="metric-caption">Each dot is a bedtime. The tighter the cloud, the more your circadian rhythm trusts you.</p>
        </div>
        <div class="metric-card">
          <div class="head"><h3>Sleep debt</h3><span class="hint">vs 8h target</span></div>
          <div class="metric-big">${debtSign}${(-summary.debt).toFixed(1)}<small>h cumulative</small></div>
          <div class="metric-body" style="margin-top:10px;">${debtBars()}</div>
          <p class="metric-caption">Last 14 nights. Bars left are hours short; bars right are hours over.</p>
        </div>
        <div class="metric-card">
          <div class="head"><h3>Stage mix</h3><span class="hint">30-night avg</span></div>
          <div class="metric-big">${Math.round((D.summary.avgRem / (D.summary.avgRem + D.summary.avgDeep + D.summary.avgLight + D.summary.avgAwake)) * 100)}<small>% REM</small></div>
          <div class="metric-body"><div class="stage-bar-group">${stageGauge()}</div></div>
          <p class="metric-caption">White ticks mark healthy targets. Deep anchors physical recovery; REM consolidates memory.</p>
        </div>
        <div class="metric-card">
          <div class="head"><h3>Nighttime heart rate</h3><span class="hint">resting, 30 nights</span></div>
          <div class="metric-big">${Math.round(summary.avgRestingHR)}<small>bpm avg</small></div>
          <div class="metric-body">${hrSparkSVG()}</div>
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
    app.innerHTML = topbar() + hero() + chapterHeatmap() + chapterTimeline() + chapterHypnogram() + chapterRadial() + chapterSmallMultiples() + chapterRidgeline() + chapterCards() + chapterAgenda() + chapterMetrics() + chapterDriftClock() + footer() + tweaksPanel() + `<div id="hover-tip"></div>`;
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
    document.querySelectorAll(".sm-cell[data-idx]").forEach((el) => {
      el.addEventListener("click", () => {
        focusIdx = parseInt(el.dataset.idx, 10);
        PREFS.focusNightIndex = focusIdx; savePrefs(); render();
      });
    });
    document.getElementById("timeline-to-history")?.addEventListener("click", () => {
      navigateTo("history/timeline");
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
