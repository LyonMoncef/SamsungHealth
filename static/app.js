const tooltip = document.getElementById("tooltip");
const monthLabel = document.getElementById("month-label");
const prevBtn = document.getElementById("prev-month");
const nextBtn = document.getElementById("next-month");

let currentYear = new Date().getFullYear();
let currentMonth = new Date().getMonth();
let activeTab = "sleep";

// Tab switching
document.querySelectorAll(".tab").forEach((btn) => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".tab").forEach((b) => b.classList.remove("active"));
        document.querySelectorAll(".panel").forEach((p) => p.classList.remove("active"));
        btn.classList.add("active");
        const tab = btn.dataset.tab;
        document.getElementById("panel-" + tab).classList.add("active");
        activeTab = tab;
        renderActiveTab();
    });
});

function daysInMonth(year, month) {
    return new Date(year, month + 1, 0).getDate();
}

function formatDate(year, month, day) {
    const mm = String(month + 1).padStart(2, "0");
    const dd = String(day).padStart(2, "0");
    return `${year}-${mm}-${dd}`;
}

function parseLocalDate(isoStr) {
    return new Date(isoStr);
}

function updateMonthLabel() {
    monthLabel.textContent = new Date(currentYear, currentMonth).toLocaleString(undefined, {
        month: "long", year: "numeric",
    });
}

function getMonthRange() {
    const numDays = daysInMonth(currentYear, currentMonth);
    const fromDate = formatDate(currentYear, currentMonth, 1);
    const toDate = formatDate(currentYear, currentMonth, numDays);
    const prevDay = new Date(currentYear, currentMonth, 0);
    const from = formatDate(prevDay.getFullYear(), prevDay.getMonth(), prevDay.getDate());
    return { from, fromDate, toDate, numDays };
}

// ─── Sleep ───

function buildSleepMap(sessions) {
    const hourMap = {};
    for (const s of sessions) {
        const start = parseLocalDate(s.sleep_start);
        const end = parseLocalDate(s.sleep_end);
        let cursor = new Date(start);
        cursor.setMinutes(0, 0, 0);
        while (cursor < end) {
            const y = cursor.getFullYear();
            const m = cursor.getMonth();
            const d = cursor.getDate();
            const h = cursor.getHours();
            const dateKey = formatDate(y, m, d);
            if (!hourMap[dateKey]) hourMap[dateKey] = {};
            if (!hourMap[dateKey][h]) hourMap[dateKey][h] = { sessions: [], stages: [] };
            hourMap[dateKey][h].sessions.push(s);
            if (s.stages) {
                const hourStart = cursor.getTime();
                const hourEnd = hourStart + 3600000;
                for (const st of s.stages) {
                    const stStart = parseLocalDate(st.stage_start).getTime();
                    const stEnd = parseLocalDate(st.stage_end).getTime();
                    if (stStart < hourEnd && stEnd > hourStart) {
                        const overlap = Math.min(hourEnd, stEnd) - Math.max(hourStart, stStart);
                        hourMap[dateKey][h].stages.push({ type: st.stage_type, duration: overlap });
                    }
                }
            }
            cursor = new Date(cursor.getTime() + 3600000);
        }
    }
    return hourMap;
}

function dominantStage(stages) {
    if (!stages || stages.length === 0) return null;
    const totals = {};
    for (const s of stages) {
        totals[s.type] = (totals[s.type] || 0) + s.duration;
    }
    let best = null;
    let bestTime = 0;
    for (const [type, time] of Object.entries(totals)) {
        if (time > bestTime) { best = type; bestTime = time; }
    }
    return best;
}

function formatTime(isoStr) {
    const d = parseLocalDate(isoStr);
    return d.toLocaleString(undefined, {
        month: "short", day: "numeric",
        hour: "2-digit", minute: "2-digit",
    });
}

function stageBreakdown(stages) {
    if (!stages || stages.length === 0) return "";
    const totals = {};
    for (const s of stages) {
        totals[s.type] = (totals[s.type] || 0) + s.duration;
    }
    const parts = Object.entries(totals)
        .sort((a, b) => b[1] - a[1])
        .map(([type, ms]) => `${type}: ${Math.round(ms / 60000)}m`);
    return parts.join(", ");
}

async function renderSleep() {
    const tbody = document.querySelector("#sleep-grid tbody");
    const thead = document.querySelector("#sleep-grid thead tr");
    const { from, toDate, numDays } = getMonthRange();

    const resp = await fetch(`/api/sleep?from=${from}&to=${toDate}&include_stages=true`);
    const sessions = await resp.json();
    const hourMap = buildSleepMap(sessions);

    thead.innerHTML = "<th>Date</th>";
    for (let h = 0; h < 24; h++) {
        const th = document.createElement("th");
        th.textContent = String(h).padStart(2, "0");
        thead.appendChild(th);
    }

    tbody.innerHTML = "";
    for (let day = 1; day <= numDays; day++) {
        const dateKey = formatDate(currentYear, currentMonth, day);
        const tr = document.createElement("tr");
        const labelTd = document.createElement("td");
        const dayOfWeek = new Date(currentYear, currentMonth, day).toLocaleString(undefined, { weekday: "short" });
        labelTd.textContent = `${dayOfWeek} ${day}`;
        tr.appendChild(labelTd);

        for (let h = 0; h < 24; h++) {
            const td = document.createElement("td");
            const cell = hourMap[dateKey]?.[h];
            if (cell && cell.sessions.length > 0) {
                const dominant = dominantStage(cell.stages);
                if (dominant && dominant !== "unknown") {
                    td.classList.add(`stage-${dominant}`);
                } else {
                    td.classList.add("sleep");
                }
                td.addEventListener("mouseenter", () => {
                    const lines = cell.sessions.map(
                        (s) => `${formatTime(s.sleep_start)} → ${formatTime(s.sleep_end)}`
                    );
                    const breakdown = stageBreakdown(cell.stages);
                    if (breakdown) lines.push(`Stages: ${breakdown}`);
                    tooltip.textContent = lines.join("\n");
                    tooltip.classList.add("visible");
                });
                td.addEventListener("mousemove", (e) => {
                    tooltip.style.left = e.clientX + 12 + "px";
                    tooltip.style.top = e.clientY + 12 + "px";
                });
                td.addEventListener("mouseleave", () => {
                    tooltip.classList.remove("visible");
                });
            }
            tr.appendChild(td);
        }
        tbody.appendChild(tr);
    }
}

// ─── Steps ───

async function renderSteps() {
    const container = document.getElementById("steps-chart");
    const { fromDate, toDate, numDays } = getMonthRange();

    const resp = await fetch(`/api/steps?from=${fromDate}&to=${toDate}`);
    const records = await resp.json();

    const dailyTotals = {};
    for (const r of records) {
        dailyTotals[r.date] = (dailyTotals[r.date] || 0) + r.step_count;
    }

    const maxSteps = Math.max(1, ...Object.values(dailyTotals));

    container.innerHTML = "";
    for (let day = 1; day <= numDays; day++) {
        const dateKey = formatDate(currentYear, currentMonth, day);
        const total = dailyTotals[dateKey] || 0;
        const pct = (total / maxSteps) * 100;
        const dayOfWeek = new Date(currentYear, currentMonth, day).toLocaleString(undefined, { weekday: "short" });

        const row = document.createElement("div");
        row.className = "steps-day";
        row.innerHTML = `
            <div class="steps-label">${dayOfWeek} ${day}</div>
            <div class="steps-bar-wrapper">
                <div class="steps-bar" style="width:${pct}%"></div>
            </div>
            <div class="steps-value">${total.toLocaleString()}</div>
        `;
        container.appendChild(row);
    }
}

// ─── Heart Rate ───

async function renderHeartRate() {
    const container = document.getElementById("hr-chart");
    const { fromDate, toDate, numDays } = getMonthRange();

    const resp = await fetch(`/api/heartrate?from=${fromDate}&to=${toDate}`);
    const records = await resp.json();

    const dailyStats = {};
    for (const r of records) {
        if (!dailyStats[r.date]) {
            dailyStats[r.date] = { min: r.min_bpm, max: r.max_bpm, sumAvg: 0, count: 0 };
        }
        const d = dailyStats[r.date];
        d.min = Math.min(d.min, r.min_bpm);
        d.max = Math.max(d.max, r.max_bpm);
        d.sumAvg += r.avg_bpm;
        d.count++;
    }

    const globalMin = 40;
    const globalMax = 180;
    const range = globalMax - globalMin;

    container.innerHTML = "";
    for (let day = 1; day <= numDays; day++) {
        const dateKey = formatDate(currentYear, currentMonth, day);
        const stats = dailyStats[dateKey];
        const dayOfWeek = new Date(currentYear, currentMonth, day).toLocaleString(undefined, { weekday: "short" });

        const row = document.createElement("div");
        row.className = "hr-day";

        if (stats) {
            const avg = Math.round(stats.sumAvg / stats.count);
            const leftPct = ((stats.min - globalMin) / range) * 100;
            const widthPct = ((stats.max - stats.min) / range) * 100;
            const avgPct = ((avg - globalMin) / range) * 100;

            row.innerHTML = `
                <div class="hr-label">${dayOfWeek} ${day}</div>
                <div class="hr-range-wrapper">
                    <div class="hr-range-bar" style="left:${leftPct}%;width:${widthPct}%"></div>
                    <div class="hr-avg-marker" style="left:${avgPct}%"></div>
                </div>
                <div class="hr-value">${stats.min}-${stats.max} avg ${avg}</div>
            `;
        } else {
            row.innerHTML = `
                <div class="hr-label">${dayOfWeek} ${day}</div>
                <div class="hr-range-wrapper"></div>
                <div class="hr-value">—</div>
            `;
        }
        container.appendChild(row);
    }
}

// ─── Exercise ───

async function renderExercise() {
    const container = document.getElementById("exercise-list");
    const { fromDate, toDate } = getMonthRange();

    const resp = await fetch(`/api/exercise?from=${fromDate}&to=${toDate}`);
    const sessions = await resp.json();

    const grouped = {};
    for (const s of sessions) {
        const date = s.exercise_start.split("T")[0];
        if (!grouped[date]) grouped[date] = [];
        grouped[date].push(s);
    }

    container.innerHTML = "";
    const dates = Object.keys(grouped).sort().reverse();

    if (dates.length === 0) {
        container.innerHTML = '<div style="color:#8888aa;text-align:center;padding:40px">No exercise sessions this month</div>';
        return;
    }

    for (const date of dates) {
        const group = document.createElement("div");
        group.className = "exercise-date-group";

        const d = new Date(date + "T00:00:00");
        const header = document.createElement("div");
        header.className = "exercise-date-header";
        header.textContent = d.toLocaleString(undefined, { weekday: "long", month: "short", day: "numeric" });
        group.appendChild(header);

        for (const s of grouped[date]) {
            const start = parseLocalDate(s.exercise_start);
            const timeStr = start.toLocaleString(undefined, { hour: "2-digit", minute: "2-digit" });
            const dur = Math.round(s.duration_minutes);
            const typeName = s.exercise_type.replace(/_/g, " ");

            const card = document.createElement("div");
            card.className = "exercise-card";
            card.innerHTML = `
                <div>
                    <div class="exercise-type">${typeName}</div>
                    <div class="exercise-time">${timeStr}</div>
                </div>
                <div class="exercise-duration">${dur} min</div>
            `;
            group.appendChild(card);
        }
        container.appendChild(group);
    }
}

// ─── Trends ───

async function renderTrends() {
    const container = document.getElementById("trends-grid");
    const { from, fromDate, toDate } = getMonthRange();

    const [sleepResp, stepsResp, hrResp, exResp] = await Promise.all([
        fetch(`/api/sleep?from=${from}&to=${toDate}`),
        fetch(`/api/steps?from=${fromDate}&to=${toDate}`),
        fetch(`/api/heartrate?from=${fromDate}&to=${toDate}`),
        fetch(`/api/exercise?from=${fromDate}&to=${toDate}`),
    ]);

    const sleepSessions = await sleepResp.json();
    const stepsRecords = await stepsResp.json();
    const hrRecords = await hrResp.json();
    const exSessions = await exResp.json();

    // Avg sleep duration
    let avgSleep = "—";
    if (sleepSessions.length > 0) {
        const totalMs = sleepSessions.reduce((sum, s) => {
            return sum + (parseLocalDate(s.sleep_end) - parseLocalDate(s.sleep_start));
        }, 0);
        const avgHrs = (totalMs / sleepSessions.length / 3600000).toFixed(1);
        avgSleep = avgHrs;
    }

    // Daily step average
    const dailySteps = {};
    for (const r of stepsRecords) {
        dailySteps[r.date] = (dailySteps[r.date] || 0) + r.step_count;
    }
    const stepDays = Object.keys(dailySteps).length;
    const avgSteps = stepDays > 0 ? Math.round(Object.values(dailySteps).reduce((a, b) => a + b, 0) / stepDays) : "—";

    // Resting HR (avg of nighttime hours 0-5)
    const restingBpms = hrRecords.filter((r) => r.hour >= 0 && r.hour < 6);
    const restingHR = restingBpms.length > 0
        ? Math.round(restingBpms.reduce((sum, r) => sum + r.avg_bpm, 0) / restingBpms.length)
        : "—";

    // Exercise frequency
    const exFreq = exSessions.length;

    container.innerHTML = `<div class="stat-grid">
        <div class="stat-card">
            <div class="stat-label">Avg Sleep Duration</div>
            <div class="stat-value">${avgSleep}</div>
            <div class="stat-unit">hours / night</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Daily Step Average</div>
            <div class="stat-value">${typeof avgSteps === "number" ? avgSteps.toLocaleString() : avgSteps}</div>
            <div class="stat-unit">steps / day</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Resting Heart Rate</div>
            <div class="stat-value">${restingHR}</div>
            <div class="stat-unit">bpm (night avg)</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Exercise Sessions</div>
            <div class="stat-value">${exFreq}</div>
            <div class="stat-unit">this month</div>
        </div>
    </div>`;
}

// ─── Navigation ───

function renderActiveTab() {
    updateMonthLabel();
    switch (activeTab) {
        case "sleep": renderSleep(); break;
        case "steps": renderSteps(); break;
        case "heartrate": renderHeartRate(); break;
        case "exercise": renderExercise(); break;
        case "trends": renderTrends(); break;
    }
}

prevBtn.addEventListener("click", () => {
    currentMonth--;
    if (currentMonth < 0) { currentMonth = 11; currentYear--; }
    renderActiveTab();
});

nextBtn.addEventListener("click", () => {
    currentMonth++;
    if (currentMonth > 11) { currentMonth = 0; currentYear++; }
    renderActiveTab();
});

renderActiveTab();
