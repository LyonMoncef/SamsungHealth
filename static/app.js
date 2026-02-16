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

// Build a map: "YYYY-MM-DD" -> Set of hours (0-23) that have sleep
// Also track which sessions overlap each (date, hour) for tooltips
function buildSleepMap(sessions) {
    const hourMap = {}; // "YYYY-MM-DD" -> { hour: [session, ...] }

    for (const s of sessions) {
        const start = parseLocalDate(s.sleep_start);
        const end = parseLocalDate(s.sleep_end);

        // Walk hour by hour from start to end
        let cursor = new Date(start);
        cursor.setMinutes(0, 0, 0);

        while (cursor < end) {
            const y = cursor.getFullYear();
            const m = cursor.getMonth();
            const d = cursor.getDate();
            const h = cursor.getHours();
            const dateKey = formatDate(y, m, d);

            if (!hourMap[dateKey]) hourMap[dateKey] = {};
            if (!hourMap[dateKey][h]) hourMap[dateKey][h] = [];
            hourMap[dateKey][h].push(s);

            cursor = new Date(cursor.getTime() + 3600000);
        }
    }

    return hourMap;
}

function formatTime(isoStr) {
    const d = parseLocalDate(isoStr);
    return d.toLocaleString(undefined, {
        month: "short", day: "numeric",
        hour: "2-digit", minute: "2-digit",
    });
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

    const resp = await fetch(`/api/sleep?from=${from}&to=${lastDay}`);
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
            const sessionsAtHour = hourMap[dateKey]?.[h];

            if (sessionsAtHour && sessionsAtHour.length > 0) {
                td.classList.add("sleep");
                td.addEventListener("mouseenter", (e) => {
                    const lines = sessionsAtHour.map(
                        (s) => `${formatTime(s.sleep_start)} → ${formatTime(s.sleep_end)}`
                    );
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

// Start on current month
render();
