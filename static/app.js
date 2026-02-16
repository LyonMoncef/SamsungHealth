const tooltip = document.getElementById("tooltip");
const tbody = document.querySelector("#sleep-grid tbody");
const thead = document.querySelector("#sleep-grid thead tr");
const monthLabel = document.getElementById("month-label");
const prevBtn = document.getElementById("prev-month");
const nextBtn = document.getElementById("next-month");

let currentYear = new Date().getFullYear();
let currentMonth = new Date().getMonth(); // 0-indexed

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

// Build a map: "YYYY-MM-DD" -> { hour: { sessions: [...], stages: [...] } }
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

            // Find stages overlapping this hour
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
        if (time > bestTime) {
            best = type;
            bestTime = time;
        }
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

async function render() {
    const year = currentYear;
    const month = currentMonth;
    const numDays = daysInMonth(year, month);
    const monthName = new Date(year, month).toLocaleString(undefined, {
        month: "long", year: "numeric",
    });
    monthLabel.textContent = monthName;

    // Fetch data for this month (with a day buffer on each side for crossover)
    const fromDate = formatDate(year, month, 1);
    const lastDay = formatDate(year, month, numDays);
    // Also fetch previous day for sessions that cross midnight into this month
    const prevDay = new Date(year, month, 0);
    const from = formatDate(prevDay.getFullYear(), prevDay.getMonth(), prevDay.getDate());

    const resp = await fetch(`/api/sleep?from=${from}&to=${lastDay}&include_stages=true`);
    const sessions = await resp.json();
    const hourMap = buildSleepMap(sessions);

    // Build header
    thead.innerHTML = "<th>Date</th>";
    for (let h = 0; h < 24; h++) {
        const th = document.createElement("th");
        th.textContent = String(h).padStart(2, "0");
        thead.appendChild(th);
    }

    // Build rows
    tbody.innerHTML = "";
    for (let day = 1; day <= numDays; day++) {
        const dateKey = formatDate(year, month, day);
        const tr = document.createElement("tr");

        const labelTd = document.createElement("td");
        const dayOfWeek = new Date(year, month, day).toLocaleString(undefined, { weekday: "short" });
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

                td.addEventListener("mouseenter", (e) => {
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

prevBtn.addEventListener("click", () => {
    currentMonth--;
    if (currentMonth < 0) {
        currentMonth = 11;
        currentYear--;
    }
    render();
});

nextBtn.addEventListener("click", () => {
    currentMonth++;
    if (currentMonth > 11) {
        currentMonth = 0;
        currentYear++;
    }
    render();
});

render();
