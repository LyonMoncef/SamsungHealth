(async function () {
  const MAX_SESSIONS = 30;

  function toDate(isoStr) {
    return new Date(isoStr);
  }

  function dateKey(d) {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  }

  function computeSession(raw, id) {
    const sleep_start = toDate(raw.sleep_start);
    const sleep_end = toDate(raw.sleep_end);
    const stages = (raw.stages || []).map((st) => ({
      stage_type: st.stage_type,
      stage_start: toDate(st.stage_start),
      stage_end: toDate(st.stage_end),
    }));

    const totals = { light: 0, deep: 0, rem: 0, awake: 0 };
    for (const st of stages) {
      const ms = st.stage_end - st.stage_start;
      if (totals[st.stage_type] !== undefined) totals[st.stage_type] += ms;
    }

    const duration_ms = sleep_end - sleep_start;
    const duration_hours = duration_ms / 3600000;
    const asleep = duration_ms - totals.awake;
    const efficiency = duration_ms > 0 ? asleep / duration_ms : 0;

    const remPct = totals.rem / duration_ms;
    const deepPct = totals.deep / duration_ms;
    const awakePct = totals.awake / duration_ms;
    const durScore = Math.max(0, 100 - Math.abs(duration_hours - 8) * 15);
    const remScore = Math.min(100, (remPct / 0.22) * 100);
    const deepScore = Math.min(100, (deepPct / 0.18) * 100);
    const awakeScore = Math.max(0, 100 - awakePct * 400);
    const score = Math.round(durScore * 0.35 + remScore * 0.2 + deepScore * 0.25 + awakeScore * 0.2);

    return {
      id,
      sleep_start,
      sleep_end,
      stages,
      totals,
      duration_ms,
      duration_hours,
      efficiency,
      score,
      date_key: dateKey(sleep_end),
    };
  }

  function aggregateSteps(records) {
    const steps = {};
    for (const r of records) {
      steps[r.date] = (steps[r.date] || 0) + r.step_count;
    }
    return steps;
  }

  function aggregateHR(records) {
    const hr = {};
    const counts = {};
    for (const r of records) {
      if (r.hour >= 0 && r.hour <= 6) {
        hr[r.date] = (hr[r.date] || 0) + r.avg_bpm;
        counts[r.date] = (counts[r.date] || 0) + 1;
      }
    }
    const result = {};
    for (const d of Object.keys(hr)) {
      result[d] = Math.round(hr[d] / counts[d]);
    }
    return result;
  }

  function computeSummary(sessions, steps, hr) {
    const n = sessions.length;
    if (n === 0) return {};

    const avgDur = sessions.reduce((a, s) => a + s.duration_hours, 0) / n;
    const avgScore = sessions.reduce((a, s) => a + s.score, 0) / n;
    const avgRem = sessions.reduce((a, s) => a + s.totals.rem, 0) / n / 3600000;
    const avgDeep = sessions.reduce((a, s) => a + s.totals.deep, 0) / n / 3600000;
    const avgLight = sessions.reduce((a, s) => a + s.totals.light, 0) / n / 3600000;
    const avgAwake = sessions.reduce((a, s) => a + s.totals.awake, 0) / n / 3600000;

    const bedMinutes = sessions.map((s) => {
      let m = s.sleep_start.getHours() * 60 + s.sleep_start.getMinutes();
      if (m < 12 * 60) m += 24 * 60;
      return m;
    });
    const mean = bedMinutes.reduce((a, b) => a + b, 0) / bedMinutes.length;
    const variance = bedMinutes.reduce((a, b) => a + (b - mean) ** 2, 0) / bedMinutes.length;
    const bedtimeStdDev = Math.sqrt(variance);
    const regularity = Math.max(0, 100 - bedtimeStdDev * 0.8);

    let debt = 0;
    for (const s of sessions) debt += 8 - s.duration_hours;

    const hrVals = Object.values(hr);
    const avgRestingHR = hrVals.length
      ? hrVals.reduce((a, b) => a + b, 0) / hrVals.length
      : 0;

    const stepVals = Object.values(steps);
    const avgSteps = stepVals.length
      ? stepVals.reduce((a, b) => a + b, 0) / stepVals.length
      : 0;

    return {
      avgDur, avgScore, avgRem, avgDeep, avgLight, avgAwake,
      regularity, bedtimeStdDev, debt,
      nights: n,
      avgRestingHR,
      avgSteps,
    };
  }

  function showEmptyState() {
    document.getElementById("app").innerHTML = `
      <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:70vh;gap:24px;text-align:center;">
        <div style="font-size:48px;opacity:0.3">🌙</div>
        <h2 style="font-family:'Instrument Serif',serif;font-size:clamp(28px,4vw,48px);font-weight:400;color:#e8e4f5;">Aucune donnée de sommeil</h2>
        <p style="color:#6a6488;max-width:40ch;line-height:1.6;">Lancez une synchronisation depuis l'application Android pour importer vos nuits depuis Samsung Health.</p>
      </div>
    `;
  }

  async function loadFullSessions() {
    if (window.SleepData.sessionsFull) return;
    try {
      const res = await fetch("/api/sleep?include_stages=true");
      const allRaw = await res.json();
      window.SleepData.sessionsFull = allRaw.map((s, i) => computeSession(s, i));
    } catch (e) {
      console.warn("Nightfall: could not load full history", e);
    }
  }
  window.loadFullSessions = loadFullSessions;

  try {
    const sleepRes = await fetch(`/api/sleep?include_stages=true&limit=${MAX_SESSIONS}`);
    const rawSessions = await sleepRes.json();

    if (!Array.isArray(rawSessions) || rawSessions.length === 0) {
      showEmptyState();
      return;
    }

    const fromDate = rawSessions[0].sleep_start.slice(0, 10);
    const toDate = rawSessions[rawSessions.length - 1].sleep_end.slice(0, 10);

    const [stepsRes, hrRes] = await Promise.all([
      fetch(`/api/steps?from=${fromDate}&to=${toDate}`),
      fetch(`/api/heartrate?from=${fromDate}&to=${toDate}`),
    ]);

    const rawSteps = stepsRes.ok ? await stepsRes.json() : [];
    const rawHR = hrRes.ok ? await hrRes.json() : [];

    const sessions = rawSessions.map((s, i) => computeSession(s, i));
    const steps = aggregateSteps(rawSteps);
    const hr = aggregateHR(rawHR);

    window.SleepData = {
      sessions,
      sessionsFull: null,
      steps,
      hr,
      summary: computeSummary(sessions, steps, hr),
    };

    window.render();
  } catch (err) {
    console.error("Nightfall: failed to load data", err);
    showEmptyState();
  }
})();
