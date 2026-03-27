// script.js — Sports Performance Tracker
// Connects login.html and dashboard.html to the Java HTTP backend.
// Author: Team Lead (integration)

const BASE_URL = "http://localhost:8080";
let chartInstance;

// ══════════════════════════════════════════════════════════
//  LOGIN PAGE  (login.html)
// ══════════════════════════════════════════════════════════

function login() {
    const email    = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    const btn      = document.getElementById("loginBtn");

    // Clear previous messages
    document.getElementById("errorMsg").classList.remove("show");
    document.getElementById("successMsg").classList.remove("show");

    if (!email || !password) {
        showError("Please fill in both fields.");
        return;
    }

    btn.textContent = "AUTHENTICATING...";
    btn.disabled    = true;

    // POST → Java AuthController → AuthService → UserDAO → MySQL
    fetch(`${BASE_URL}/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email, password: password })  // sends body correctly
    })
    .then(res => {
        if (!res.ok) throw new Error("Invalid credentials");
        return res.text();
    })
    .then(msg => {
        showSuccess(msg);
        sessionStorage.setItem("userEmail", email);
        setTimeout(() => { window.location.href = "dashboard.html"; }, 800);
    })
    .catch(err => {
        showError(err.message || "Server not reachable. Is the Java server running on port 8080?");
        btn.textContent = "LOGIN";
        btn.disabled    = false;
    });
}

function guestLogin() {
    sessionStorage.setItem("guest", "true");
    sessionStorage.setItem("userEmail", "Guest");
    window.location.href = "dashboard.html";
}

// Enter key triggers login on login page
if (document.getElementById("loginBtn")) {
    document.addEventListener("keydown", e => {
        if (e.key === "Enter") login();
    });
}

// ══════════════════════════════════════════════════════════
//  DASHBOARD INIT  (dashboard.html)
// ══════════════════════════════════════════════════════════

window.onload = function () {
    // Only run on dashboard page
    if (!document.getElementById("tableBody")) return;

    // Show logged-in user in navbar badge
    const email = sessionStorage.getItem("userEmail") || "coach@example.com";
    const badge = document.getElementById("userBadge");
    if (badge) badge.textContent = email;

    // Load DB stats into stat cards
    fetchDashboard();

    // Render local records in table + chart
    updateUI();
};

// ══════════════════════════════════════════════════════════
//  GET /dashboard  → Java DashboardController
// ══════════════════════════════════════════════════════════

function fetchDashboard() {
    fetch(`${BASE_URL}/dashboard`)
    .then(res => res.json())
    .then(data => {
        const s = data.summary;

        // Populate stat cards from DB analysis
        setEl("totalSessions", s.totalSessions ?? "—");
        setEl("avgScore",      parseFloat(s.average).toFixed(2));

        // Trend with color — matches stat-card-value color classes in dashboard.html
        const trendEl = document.getElementById("stat-trend");
        if (trendEl) {
            trendEl.textContent = s.trend;
            trendEl.className = "stat-card-value " +
                (s.trend === "Improving" ? "green" : s.trend === "Declining" ? "red" : "yellow");
        }

        console.log("[Dashboard] DB stats loaded:", s);
    })
    .catch(() => {
        // Server offline — local data still shows in table/chart
        console.warn("[Dashboard] Backend unavailable, using local data only.");
    });
}

// ══════════════════════════════════════════════════════════
//  POST /save  → Java PerformanceController
// ══════════════════════════════════════════════════════════

function saveData() {
    const d        = parseFloat(document.getElementById("distance").value);
    const t        = parseFloat(document.getElementById("time").value);
    const accuracy = parseFloat(document.getElementById("accuracy").value);
    const stamina  = parseFloat(document.getElementById("stamina").value);

    if (!d || !t || isNaN(accuracy) || isNaN(stamina)) {
        showToast("Please fill in all fields.", "red"); return;
    }
    if (t <= 0) {
        showToast("Time must be greater than 0.", "red"); return;
    }

    const speed = d / t;

    // Client-side validation (mirrors Java ValidationService)
    if (speed < 0 || speed > 200) {
        showToast("Speed out of range (0–200).", "red"); return;
    }
    if (accuracy < 0 || accuracy > 100) {
        showToast("Accuracy must be 0–100.", "red"); return;
    }
    if (stamina < 0 || stamina > 100) {
        showToast("Stamina must be 0–100.", "red"); return;
    }

    // POST → Java PerformanceController
    // Server validates → calculates score → saves to MySQL → returns JSON
    fetch(`${BASE_URL}/save`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            athlete:  "Demo Athlete",
            distance: d,
            time:     t,
            accuracy: accuracy,
            stamina:  stamina
        })
    })
    .then(res => res.json())
    .then(data => {
        if (!data.success) {
            showToast(data.error || "Save failed.", "red"); return;
        }

        // Server returned score + level — save locally for table/chart
        const records = getRecords();
        records.push({
            d:        d,
            t:        t,
            speed:    data.speed,
            accuracy: accuracy,
            stamina:  stamina,
            score:    data.score,
            level:    data.level
        });
        localStorage.setItem("records", JSON.stringify(records));

        showLevelBadge(data.level, data.score);
        showToast("Saved! Score: " + parseFloat(data.score).toFixed(1) + " — " + data.level);
        clearForm();
        updateUI();
        fetchDashboard(); // refresh stat cards with new DB data
    })
    .catch(() => {
        // Server offline — calculate locally so UI still works
        const score = (speed * 0.4) + (accuracy * 0.3) + (stamina * 0.3);
        const level = getLocalLevel(score);

        const records = getRecords();
        records.push({ d, t, speed, accuracy, stamina, score, level });
        localStorage.setItem("records", JSON.stringify(records));

        showLevelBadge(level, score);
        showToast("Saved locally (server offline). Score: " + score.toFixed(1), "red");
        clearForm();
        updateUI();
    });
}

// ══════════════════════════════════════════════════════════
//  TABLE + CHART
// ══════════════════════════════════════════════════════════

function updateUI() {
    const records = getRecords();
    const tbody   = document.getElementById("tableBody");
    if (!tbody) return;

    if (!records.length) {
        tbody.innerHTML = `<tr class="empty-row"><td colspan="9">No sessions yet. Add your first one!</td></tr>`;
        ["totalSessions","avgSpeed","bestSpeed","avgScore"].forEach(id => setEl(id, "0"));
        drawChart([]);
        return;
    }

    let totalSpeed = 0, bestSpeed = 0, totalScore = 0;
    tbody.innerHTML = "";

    records.forEach((r, i) => {
        totalSpeed += r.speed;
        totalScore += r.score;
        if (r.speed > bestSpeed) bestSpeed = r.speed;

        const cls = r.level === "Needs Improvement" ? "level-Needs" : `level-${r.level}`;
        tbody.innerHTML += `
            <tr>
                <td>${i + 1}</td>
                <td>${r.d} m</td>
                <td>${r.t} s</td>
                <td>${r.speed.toFixed(2)}</td>
                <td>${r.accuracy}</td>
                <td>${r.stamina}</td>
                <td>${r.score.toFixed(1)}</td>
                <td><span class="level-badge ${cls}" style="font-size:11px;padding:3px 10px">${r.level}</span></td>
                <td><button class="del-btn" onclick="deleteRow(${i})">✕</button></td>
            </tr>
        `;
    });

    setEl("totalSessions", records.length);
    setEl("avgSpeed",  (totalSpeed / records.length).toFixed(2));
    setEl("bestSpeed", bestSpeed.toFixed(2));
    setEl("avgScore",  (totalScore / records.length).toFixed(2));

    drawChart(records);
}

function drawChart(records) {
    const canvas = document.getElementById("chart");
    if (!canvas) return;

    const labels = records.map((_, i) => `#${i + 1}`);
    const speeds = records.map(r => r.speed.toFixed(2));

    if (chartInstance) chartInstance.destroy();

    chartInstance = new Chart(canvas, {
        type: "line",
        data: {
            labels,
            datasets: [{
                label: "Speed (m/s)",
                data: speeds,
                fill: true,
                borderColor: "#E83A2F",
                backgroundColor: "rgba(232,58,47,0.08)",
                pointBackgroundColor: "#E83A2F",
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { labels: { color: "#666" } } },
            scales: {
                x: { ticks: { color: "#666" }, grid: { color: "#1e1e1e" } },
                y: { ticks: { color: "#666" }, grid: { color: "#1e1e1e" } }
            }
        }
    });
}

// ══════════════════════════════════════════════════════════
//  HELPERS
// ══════════════════════════════════════════════════════════

function getRecords() {
    return JSON.parse(localStorage.getItem("records")) || [];
}

function setEl(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

function clearForm() {
    ["distance","time","accuracy","stamina"].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = "";
    });
}

function getLocalLevel(score) {
    if (score >= 85) return "Excellent";
    if (score >= 70) return "Good";
    if (score >= 50) return "Average";
    return "Needs Improvement";
}

function showLevelBadge(level, score) {
    const badge = document.getElementById("levelBadge");
    if (!badge) return;
    const cls = level === "Needs Improvement" ? "level-Needs" : `level-${level}`;
    badge.innerHTML = `<span class="level-badge ${cls}">Score: ${parseFloat(score).toFixed(1)} — ${level}</span>`;
}

function showToast(msg, type = "green") {
    const t = document.getElementById("toast");
    if (!t) return;
    t.textContent = msg;
    t.className   = `toast ${type} show`;
    setTimeout(() => t.classList.remove("show"), 2800);
}

function showError(msg) {
    const el = document.getElementById("errorMsg");
    if (!el) return;
    el.textContent = msg;
    el.classList.add("show");
    const s = document.getElementById("successMsg");
    if (s) s.classList.remove("show");
}

function showSuccess(msg) {
    const el = document.getElementById("successMsg");
    if (!el) return;
    el.textContent = msg;
    el.classList.add("show");
    const e = document.getElementById("errorMsg");
    if (e) e.classList.remove("show");
}

function deleteRow(i) {
    const records = getRecords();
    records.splice(i, 1);
    localStorage.setItem("records", JSON.stringify(records));
    updateUI();
    showToast("Record removed.");
}

function resetData() {
    if (!confirm("Reset all records?")) return;
    localStorage.removeItem("records");
    updateUI();
    showToast("All records cleared.");
}

function logout() {
    sessionStorage.clear();
    window.location.href = "login.html";
}
