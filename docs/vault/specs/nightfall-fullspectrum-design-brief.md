---
type: spec
tags: [samsunghealth, design, nightfall, claude-design, superseded]
created: 2026-04-21
status: superseded
superseded_by: 2026-04-26-nightfall-rebrand-data-saillance
superseded_on: 2026-04-26
superseded_reason: "Charte graphique Data Saillance adoptée — voir 2026-04-26-nightfall-rebrand-data-saillance pour palette/typo/logos canoniques. Le présent brief reste consultable pour l'historique du concept design pré-rebrand."
---

# Nightfall Health — Full-Spectrum Dashboard Design Brief

## For Claude Design

---

## What already exists

**Nightfall** is a personal health dashboard I built for myself. The current design — which you produced in a previous session — covers sleep data exclusively.

**Existing layout:**
- A dark-indigo single-page app
- Left panel: scrollable list of the 30 most recent sleep sessions, each shown as a horizontal bar with stage breakdown (light / deep / REM / awake) and date
- Right panel: focused session detail — full stage timeline, duration, score ring, efficiency metric, steps and HR for that date
- Empty state: sync prompt when no data
- Interactions: click session → focus; keyboard arrows to navigate; cursor glow ambient effect

**Established aesthetic:**
- Background: deep indigo `#0B0F1A`, layered with 6–10% opacity panels
- Primary typography: **Instrument Serif** (headers, large metrics, section names)
- Data typography: **Geist** (all numbers, timestamps, axis labels, small metadata)
- Color language: violet/indigo = sleep; muted teal/emerald = biometrics; warm amber = flags and attention states
- Cursor glow, smooth transitions, no visual noise — the data speaks
- No frameworks. Vanilla JS in IIFE pattern, CSS custom properties

**Tech stack:**
- Backend: FastAPI + SQLite (Python)
- Frontend: static HTML/CSS/JS served at the root
- Data contract: backend API fetches populate `window.SleepData`, then `window.render()` draws the dashboard
- New sections will follow the same pattern: `window.HealthData.sleep`, `window.HealthData.vitality`, `window.HealthData.movement`, `window.HealthData.cardiovascular`

---

## What we're building now

An extension of Nightfall to cover 2.3 years of complete health data — December 2023 through April 2026 — across 15 data tables. The goal is a **personal health observatory**: longitudinal, introspective, quietly medical. Not a fitness tracker UI. Not a clinical dashboard. Something between a journal and a lab notebook.

The design should feel like Nightfall grew up.

---

## Full data inventory

### Note on data availability windows
Data richness increases over time as the user upgraded their Samsung watch:
- **Dec 2023**: sleep sessions, steps, heart rate begin
- **Jan 2024**: SpO2, skin temperature added
- **Dec 2024**: HRV, respiratory rate, vitality score added
- Visualizations must handle pre-sensor gaps gracefully — silence ≠ zero

---

### 1. `sleep_sessions` — 1,242 nights
```
sleep_start:        2023-12-22T05:48:00
sleep_end:          2023-12-22T11:38:00
sleep_score:        62          ← Samsung composite, 0–100
efficiency:         91.0        ← % time asleep vs. in bed
sleep_duration_min: 350         ← 5h 50min
sleep_cycle:        4           ← complete 90-min cycles
mental_recovery:    70.0        ← % subjective mental freshness
physical_recovery:  70.0        ← % subjective body recovery
sleep_type:         -1          ← -1 = night sleep, 1 = nap
```
Key stats: avg 5.7h/night (clinically low); score distribution peaks at 50–70; range Dec 2023 → Apr 2026.

---

### 2. `sleep_stages` — 58,941 stage intervals
Each interval linked to a session. Stage types: `light` / `deep` / `rem` / `awake`.
```
stage_type:  "light"
stage_start: 2023-12-22T05:48:00
stage_end:   2023-12-22T06:19:00   ← 31-min block
```
Distribution across all nights: **light 44% · awake 36% · REM 11.5% · deep 8.5%**
(Typical targets: light 50%, deep 15–25%, REM 20–25%, awake <10%. Deep and REM are below target.)

---

### 3. `spo2` — 1,151 nights (Jan 2024 → Apr 2026)
**This is the most medically significant table.** Wrist SpO2 measured throughout each sleep period.
```
spo2:           93.0    ← average for the night
min_spo2:       86.0    ← lowest point during the night
max_spo2:       100.0
low_duration_s: 616     ← seconds below threshold (~10 min this night)
```
**Critical pattern: 1,066 out of 1,151 nights (92.6%) have min_spo2 < 90%. Average low-oxygen duration: ~9 min/night. Worst recorded: 70% SpO2.** This is a consistent, long-running signal of sleep-disordered breathing. The dashboard must surface this — honestly but without alarm.

---

### 4. `skin_temperature` — 1,167 nights (Jan 2024 → Apr 2026)
Wrist skin temperature throughout each sleep period.
```
temperature: 34.34°C   ← avg for the night (normal: 33–37°C)
min_temp:    32.52°C
max_temp:    35.69°C
```
Temperature drops during deep sleep, rises into REM — a circadian rhythm proxy. Useful overlaid against stage timeline.

---

### 5. `respiratory_rate` — 662 intervals (Dec 2024 → Apr 2026)
Breathing rate sampled in sub-hour windows during sleep.
```
start_time: 2024-12-28T02:57:00
end_time:   2024-12-28T04:04:00
average:    14.7 breaths/min     ← normal rest: 12–20
```
Multiple windows per night. Enables a "breathing rate curve" per night.

---

### 6. `hrv` — 4,391 hourly windows (Dec 2024 → Apr 2026)
HRV measurement windows during sleep (hourly slots). Note: actual HRV value not yet extracted — only measurement timing is stored. Window count per night = proxy for measurement quality and sleep depth.
```
start_time: 2024-12-28T03:00:00
end_time:   2024-12-28T04:00:00
```

---

### 7. `heart_rate_hourly` — 18,442 hourly buckets (Dec 2023 → Apr 2026)
Aggregated from individual spot measurements. Covers all hours, day and night.
```
date: 2023-12-21   hour: 19
min_bpm: 74   max_bpm: 79   avg_bpm: 76   sample_count: 2
```
**Resting HR** is derived by selecting overnight hours (0:00–6:00). Full 2+ year range available. Sample counts are sparse (1–5 per hour).

---

### 8. `vitality_score` — 311 daily scores (Dec 2024 → Apr 2026)
Samsung's composite daily wellness score, decomposed into 4 pillars.
```
day_date:         2024-12-28
total_score:      47.1     ← overall vitality (0–100)
sleep_score:      30.8     ← sleep quality pillar
activity_score:    1.0     ← physical activity pillar
shr_score:        89.5     ← stress/heart-rate balance
shrv_score:       82.9     ← HRV balance
sleep_balance:     2.85    ← consistency vs personal baseline
sleep_regularity:  1.0     ← bedtime regularity
sleep_timing:      3.95    ← circadian alignment
active_time_ms:  6038719   ← ~100 min active that day
mvpa_time_ms:    4390843   ← ~73 min moderate-vigorous
```
**Averages over 311 days: total=71.8 · sleep=65.0 · activity=65.1 · shr=75.7 · shrv=83.9**
SHR and SHRV are strong; sleep and activity are the limiting pillars.

---

### 9. `steps_daily` — 853 days (Dec 2023 → Apr 2026)
Daily step totals with walk/run split.
```
day_date:       2023-12-22
step_count:     7286
walk_step_count: 7247
run_step_count:    39
distance_m:     5502.3   ← 5.5km
calorie_kcal:   374.7
active_time_ms: 4307791  ← ~72 min
```

---

### 10. `exercise_sessions` — 248 sessions (Jan 2024 → Apr 2026)
Structured workouts logged by the watch.
```
exercise_type:   1001              ← Running (213 sessions) | 0 = other (34)
exercise_start:  2024-01-29T07:42:52   ← mostly 7–9am
duration_minutes: 14.3
calorie_kcal:    99.2
mean_heart_rate: 142.0             ← vigorous effort
max_heart_rate:  164.0
```
**213 running sessions, avg 27 min, avg HR 110 bpm.** 344 (exercise day → next night) pairs available for sleep correlation.

---

### 11. `activity_daily` — 879 days (Nov 2023 → Apr 2026)
Daily composite: steps + exercise time + floors + Samsung activity score.
```
day_date:         2024-03-15
step_count:       8423
distance_m:       6340.0
calorie_kcal:     411.2
exercise_time_ms: 1620000   ← 27 min exercise
active_time_ms:   4800000   ← 80 min total active
floor_count:      3
score:            72
```

---

### 12. `floors_daily` — 303 rows
Floors climbed. Sparse (1–5 floors most days).

---

### 13. `ecg` — 1 record (single measurement)
```
start_time: 2024-02-20T00:02:42
duration: 30 seconds   sample_frequency: 500 Hz   sample_count: 15,000
mean_heart_rate: 101.0 bpm   classification: null
```
Design should accommodate this as a future-rich table. Currently: snapshot card.

---

### 14. `blood_pressure` — 2 records
```
2024-02-20 00:01: systolic 134 · diastolic 77 · pulse 98
2024-02-20 00:01: systolic 146 · diastolic 83 · pulse 104
```
Two consecutive readings, both slightly elevated. Same midnight session as the ECG.

---

### 15. `weight` / `height`
```
weight_kg: 99.0   body_fat_pct: 34.6%   BMR: 1,768 kcal/day
height_cm: 180.0  → BMI 30.6
```
Single snapshots. Design as a "body snapshot" card that gracefully shows "not tracked" when empty.

---

## Key health narratives

These are the signals the design should make discoverable — not diagnosed, but visible:

**1. Sleep-disordered breathing**
92.6% of nights, SpO2 dips below 90% for an average of 9 min. The minimum recorded was 70%. This is a long-running, consistent pattern. The design should surface it as a prominent, calm signal — not a red alert, but unmissable context for understanding sleep quality.

**2. Chronically short sleep**
Average 5.7h over 2+ years. The distribution shows many nights under 5h. Is the trend improving over time? The dashboard should let this story tell itself.

**3. The sleep quality formula**
What predicts a high-score night? Candidate inputs: steps that day, exercise, bedtime consistency, SpO2 that night, skin temp. The "best nights" analysis should surface drivers.

**4. Running and recovery**
213 runs, mostly 7–9am. The correlation between run-day mornings and the preceding night's quality (and the following night) is a compelling loop to visualize.

**5. Vitality pillar imbalance**
Heart (SHR=75.7, SHRV=83.9) is strong. Sleep (65.0) and Activity (65.1) are the ceiling. The breakdown over time shows which pillar fluctuates most.

**6. The sensor story**
Data gets richer over time. Dec 2023: basic. Jan 2024: SpO2 and temp added. Dec 2024: breathing, HRV, vitality. The timeline itself is a story about growing self-awareness.

---

## Sections to design

Four sections extending Nightfall. Each follows the same "list + detail" pattern but adapted to its data rhythm.

---

### A. Sleep (evolution of existing)

Carry forward the existing calendar and stage-bar layout. Extend with:

- **Health overlay in the session list**: each session block gets a subtle SpO2 indicator — a colored dot or thin bottom border (green: avg ≥ 95%, amber: 90–94%, red: < 90%). Since 92.6% of nights are amber/red, this will make the pattern immediately visible.
- **Sleep health panel in the focus view**: alongside the existing stage timeline, show:
  - SpO2 mini-chart for that night (avg line + min point + low_duration label)
  - Skin temperature curve (cooler during deep, warmer into REM)
  - Respiratory rate (if available — dec 2024+)
- **Full history navigation**: with 1,242 sessions, "last 30" is no longer enough. Add a month/year selector or a mini year-heatmap for navigating the full archive.
- **Trend ribbon**: a narrow 90-day trend line for sleep_score, mental_recovery, physical_recovery — shown below the session list.

---

### B. Vitality

Samsung's 4-pillar wellness model. The most holistic view.

- **Daily total_score timeline**: a primary trend line for the full 311-day range (Dec 2024+). Smooth, filled area chart. Hover shows the day's breakdown.
- **Four pillars**: Sleep / Activity / SHR (heart rate balance) / SHRV (HRV balance). Shown as rings, arcs, or score bars with current value + 30-day average. Visual language: which pillars are your strength vs. your floor?
- **Sleep sub-components**: balance (consistency vs baseline), regularity (bedtime variance), timing (circadian alignment). These three sub-scores are what drive the sleep pillar — their meaning should be explained inline, not in a tooltip. *Balance* = how close to your typical sleep duration. *Regularity* = how consistent your bedtime is. *Timing* = how well-aligned with your circadian window.
- **Weekly rhythm**: M–Su average vitality (do weekends recover or drain?).
- **Low-vitality event focus**: click a low-score day to see which pillar dropped and why.

---

### C. Movement

The activity layer — running, steps, weekly rhythm.

- **2-year contribution map**: a GitHub-style heatmap spanning Dec 2023 → present. Cell = one day. Color intensity = step count. Exercise days get a subtle marker (run icon, dot, or border). This gives the long-view rhythm of activity at a glance.
- **Exercise log**: a scrollable chronological list of exercise sessions. Each entry: date, type (running / other), duration, avg HR, calories. Expandable inline.
- **Weekly step chart**: last 52 weeks, bar chart. Personal average line. Annotate personal bests.
- **Run frequency**: sessions per month over the full range — is the running getting more or less consistent?
- **Day detail**: select a week or day → show steps + exercise + distance + calories in a summary panel.

---

### D. Cardiovascular

The physiological layer. The headline here is SpO2.

- **SpO2 timeline** (primary visualization): a full-range chart of nightly avg SpO2 + min_spo2 across all 1,151 nights. Use a reference band (90–100% = normal zone in soft green). Values below 90% shown in amber/rose. A secondary axis for low_duration_s (bars at the bottom). This is the most important health visualization in the entire dashboard. It should be beautiful and legible.
- **Resting HR trend**: derived from heart_rate_hourly overnight hours (0:00–5:00 avg_bpm). One point per night, smoothed 7-day rolling average. Show over the full 2+ year range to surface fitness improvements.
- **Skin temperature seasonality**: nightly avg temperature over time. Is there a winter/summer pattern? Does temperature correlate with sleep score?
- **Respiratory rate**: available Dec 2024+. Nightly average breathing rate per measurement window. Show alongside SpO2 for the same nights.
- **Body snapshot card**: weight (99 kg, BMI 30.6, 34.6% body fat), height, BMR. Single measurement context. Design as a stable reference card, not a tracking chart — it doesn't change often.
- **Sparse data cards**: ECG (1 measurement) and blood pressure (2 readings) should each be a compact "event card" — date, reading, context — clearly marked as single snapshots, not trends.

---

## Design decisions to make

These are open choices — propose what you think works best, with rationale:

1. **Section navigation**: horizontal tab bar, left sidebar icons, or scroll with sticky headers?
2. **Cross-section linking**: if I'm looking at a specific night in Sleep, can I jump to that day in Movement or Cardiovascular? Propose a navigation mechanism.
3. **Date range controls**: global date range selector (affects all sections) vs. per-section range selectors?
4. **SpO2 framing**: the data shows a concerning pattern. How do you present medically significant data in a personal dashboard without being alarmist or overly clinical?
5. **Data gaps**: pre-Jan 2024 (no SpO2), pre-Dec 2024 (no HRV/respiratory rate). How does the design communicate sensor availability vs. data absence?

---

## Deliverable

Same format as the original Nightfall handoff (ZIP):

- `index.html` — full layout, section structure, navigation
- `dashboard.css` — full visual system (extend or refactor existing)
- One JS file per section: `sleep.js`, `vitality.js`, `movement.js`, `cardiovascular.js` — each an IIFE, each reading from `window.HealthData.<section>`
- `sample_data.js` — plausible realistic data for all sections so the design renders without a backend
- Each section should be independently previewable

**All sections must render with sample data.** I need to review the full design before wiring up the FastAPI backend.

---

## Spirit

Nightfall started as a sleep journal. We're evolving it into a full health observatory.

The data spans 2+ years, covers 15 biological dimensions, and contains at least one signal that most people would want to know about (92.6% of nights with oxygen drops). This isn't a fitness app celebrating streaks. It's a quiet mirror — honest about what the body is doing, beautiful in the way it shows it, and designed for someone who wants to understand themselves rather than compete with themselves.

Same aesthetic restraint. Same editorial weight. Wider aperture.
