// ════════════════════════════════════════════════════════════════
//  script.js  —  Sports Performance Tracker
//  Single shared file used by BOTH login.html and dashboard.html
//
//  Connections:
//    login()          → POST  /login     → AuthController.java
//    saveData()       → POST  /save      → PerformanceController.java
//    fetchDashboard() → GET   /dashboard → DashboardController.java
//
//  Author: Team Lead (integration)
// ════════════════════════════════════════════════════════════════

const BASE_URL = "http://localhost:8080";
let chartInstance;

// ────────────────────────────────────────────────────────────────
//  PAGE DETECTION — run different init depending on which page
// ────────────────────────────────────────────────────────────────

window.onload = function () {

    const onLoginPage    = !!document.getElementById("loginBtn");
    const onDashboard    = !!document.getElementById("tableBody");

    if (onLoginPage) {
        initLoginPage();
    }

    if (onDashboard) {
        initDashboard();
    }
};

// ════════════════════════════════════════════════════════════════
//  LOGIN PAGE  (login.html)
// ════════════════════════════════════════════════════════════════

function initLoginPage() {
    // Enter key support on login page
    document.addEventListener("keydown", function (e) {
        if (e.key === "Enter") login();
    });
}

/**
 * Called by: <button onclick="login()"> in login.html
 * Sends to:  POST /login  →  AuthController.java
 *            → AuthService.java → UserDAO.java → MySQL users table
 * On success: saves email to sessionStorage, redirects to dashboard.html
 */
function login() {
    const email    = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    const btn      = document.getElementById("loginBtn");

    hideMsg("errorMsg");
    hideMsg("successMsg");

    if (!email || !password) {
        showError("Please fill in both fields.");
        return;
    }

    btn.textContent = "AUTHENTICATING...";
    btn.disabled    = true;

    fetch(BASE_URL + "/login", {
        method:  "POST",
        headers: { "Content-Type": "application/json" },
        body:    JSON.stringify({ email: email, password: password })
    })
    .then(function (res) {
        if (!res.ok) throw new Error("Invalid credentials");
        return res.text();
    })
    .then(function (msg) {
        showSuccess(msg);                              // "Login Successful"
        sessionStorage.setItem("userEmail", email);   // store for dashboard badge
        setTimeout(function () {
            window.location.href = "dashboard.html";
        }, 800);
    })
    .catch(function (err) {
        showError(err.message || "Server not reachable. Is the Java server running on port 8080?");
        btn.textContent = "LOGIN";
        btn.disabled    = false;
    });
}

/**
 * Called by: <button onclick="guestLogin()"> in login.html
 * Skips auth and goes straight to dashboard as a guest.
 */
function guestLogin() {
    sessionStorage.setItem("guest",     "true");
    sessionStorage.setItem("userEmail", "Guest");
    window.location.href = "dashboard.html";
}

// ════════════════════════════════════════════════════════════════
//  DASHBOARD PAGE  (dashboard.html)
// ════════════════════════════════════════════════════════════════

function initDashboard() {
    // Show logged-in user email in navbar badge
    var email = sessionStorage.getItem("userEmail") || "coach@example.com";
    var badge = document.getElementById("userBadge");
    if (badge) badge.textContent = email;

    // 1. Pull analysis stats from Java DB → fill top 3 stat cards
    fetchDashboard();

    // 2. Render local records from localStorage → fill table + chart + speed cards
    updateUI();
}

/**
 * Calls:    GET /dashboard  →  DashboardController.java
 *           → PerformanceService.getDashboardStats()
 *           → PerformanceDAO.getAllScores() from MySQL
 *           → Returns: average, trend, improvement, level, totalSessions, scores[]
 *
 * Updates:  #totalSessions, #avgScore, #stat-trend  (DB stat cards)
 */
function fetchDashboard() {
    fetch(BASE_URL + "/dashboard")
    .then(function (res) { return res.json(); })
    .then(function (data) {
        var s = data.summary;

        // Populate the 3 DB-powered stat cards
        setEl("totalSessions", s.totalSessions !== undefined ? s.totalSessions : "—");
        setEl("avgScore",      parseFloat(s.average).toFixed(2));

        // Trend card — colour matches Java PerformanceService.detectTrend() output
        var trendEl = document.getElementById("stat-trend");
        if (trendEl) {
            trendEl.textContent = s.trend;
            if      (s.trend === "Improving") trendEl.className = "stat-card-value green";
            else if (s.trend === "Declining") trendEl.className = "stat-card-value red";
            else                              trendEl.className = "stat-card-value yellow";
        }

        console.log("[script.js] Dashboard stats from DB:", s);
    })
    .catch(function () {
        // DB/server offline — stat cards show "—", local table/chart still works
        console.warn("[script.js] /dashboard unavailable. Using local data only.");
        setEl("totalSessions", "—");
        setEl("avgScore",      "—");
        setEl("stat-trend",    "—");
    });
}

// ════════════════════════════════════════════════════════════════
//  SAVE RECORD  (dashboard.html form)
// ════════════════════════════════════════════════════════════════

/**
 * Called by: <button onclick="saveData()"> in dashboard.html
 * Sends to:  POST /save  →  PerformanceController.java
 *            → ValidationService (validate)
 *            → PerformanceService.calculateScore()
 *            → PerformanceLevel.getLevel()
 *            → PerformanceDAO.insertRecord() → MySQL
 *            → Returns JSON: { success, athlete, speed, accuracy, stamina, score, level }
 *
 * On success: saves record to localStorage, updates table, chart, and DB stat cards
 * On offline: calculates score locally so UI still works
 */
function saveData() {
    var d        = parseFloat(document.getElementById("distance").value);
    var t        = parseFloat(document.getElementById("time").value);
    var accuracy = parseFloat(document.getElementById("accuracy").value);
    var stamina  = parseFloat(document.getElementById("stamina").value);

    // ── Client-side empty check ────────────────────────────────
    if (!d || !t || isNaN(accuracy) || isNaN(stamina)) {
        showToast("Please fill in all fields.", "red"); return;
    }
    if (t <= 0) {
        showToast("Time must be greater than 0.", "red"); return;
    }

    var speed = d / t;

    // ── Client-side validation (mirrors Java ValidationService) ─
    if (speed < 0 || speed > 200)       { showToast("Speed out of range (0–200).", "red"); return; }
    if (accuracy < 0 || accuracy > 100) { showToast("Accuracy must be 0–100.", "red");     return; }
    if (stamina < 0 || stamina > 100)   { showToast("Stamina must be 0–100.", "red");      return; }

    // ── POST to Java backend ────────────────────────────────────
    fetch(BASE_URL + "/save", {
        method:  "POST",
        headers: { "Content-Type": "application/json" },
        body:    JSON.stringify({
            athlete:  sessionStorage.getItem("userEmail") || "Demo Athlete",
            distance: d,
            time:     t,
            accuracy: accuracy,
            stamina:  stamina
        })
    })
    .then(function (res) { return res.json(); })
    .then(function (data) {
        if (!data.success) {
            showToast(data.error || "Save failed.", "red"); return;
        }

        // Server returned calculated score + level — save to localStorage
        saveToLocal(d, t, data.speed, accuracy, stamina, data.score, data.level);
        showLevelBadge(data.level, data.score);
        showToast("Saved! Score: " + parseFloat(data.score).toFixed(1) + " — " + data.level);
        clearForm();
        updateUI();
        fetchDashboard();  // refresh DB stat cards after new record
    })
    .catch(function () {
        // Server offline — calculate locally using same formula as Java
        var score = (speed * 0.4) + (accuracy * 0.3) + (stamina * 0.3);
        var level = getLocalLevel(score);

        saveToLocal(d, t, speed, accuracy, stamina, score, level);
        showLevelBadge(level, score);
        showToast("Saved locally (server offline). Score: " + score.toFixed(1), "red");
        clearForm();
        updateUI();
    });
}

// ════════════════════════════════════════════════════════════════
//  TABLE + CHART  (dashboard.html)
// ════════════════════════════════════════════════════════════════

/**
 * Reads all records from localStorage and renders:
 *  - Records table (#tableBody)
 *  - Speed/sessions stat cards (#avgSpeed, #bestSpeed, #totalSessions fallback)
 *  - Line chart (#chart)
 */
function updateUI() {
    var records = getRecords();
    var tbody   = document.getElementById("tableBody");
    if (!tbody) return;

    if (!records.length) {
        tbody.innerHTML = '<tr class="empty-row"><td colspan="9">No sessions yet. Add your first one!</td></tr>';
        setEl("avgSpeed",  "0.00");
        setEl("bestSpeed", "0.00");
        drawChart([]);
        return;
    }

    var totalSpeed = 0, bestSpeed = 0, totalScore = 0;
    tbody.innerHTML = "";

    records.forEach(function (r, i) {
        totalSpeed += r.speed;
        totalScore += r.score;
        if (r.speed > bestSpeed) bestSpeed = r.speed;

        var cls = r.level === "Needs Improvement" ? "level-Needs" : "level-" + r.level;
        tbody.innerHTML +=
            "<tr>" +
            "<td>" + (i + 1) + "</td>" +
            "<td>" + r.d + " m</td>" +
            "<td>" + r.t + " s</td>" +
            "<td>" + r.speed.toFixed(2) + "</td>" +
            "<td>" + r.accuracy + "</td>" +
            "<td>" + r.stamina + "</td>" +
            "<td>" + r.score.toFixed(1) + "</td>" +
            "<td><span class='level-badge " + cls + "' style='font-size:11px;padding:3px 10px'>" + r.level + "</span></td>" +
            "<td><button class='del-btn' onclick='deleteRow(" + i + ")'>✕</button></td>" +
            "</tr>";
    });

    // Update speed stat cards (local data)
    setEl("avgSpeed",  (totalSpeed / records.length).toFixed(2));
    setEl("bestSpeed", bestSpeed.toFixed(2));

    drawChart(records);
}

/**
 * Renders a Chart.js line chart of speed over sessions.
 * Called by updateUI() every time records change.
 */
function drawChart(records) {
    var canvas = document.getElementById("chart");
    if (!canvas) return;

    var labels = records.map(function (_, i) { return "#" + (i + 1); });
    var speeds = records.map(function (r) { return r.speed.toFixed(2); });

    if (chartInstance) chartInstance.destroy();

    chartInstance = new Chart(canvas, {
        type: "line",
        data: {
            labels: labels,
            datasets: [{
                label:                "Speed (m/s)",
                data:                 speeds,
                fill:                 true,
                borderColor:          "#E83A2F",
                backgroundColor:      "rgba(232,58,47,0.08)",
                pointBackgroundColor: "#E83A2F",
                tension:              0.4
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

// ════════════════════════════════════════════════════════════════
//  DELETE / RESET / LOGOUT
// ════════════════════════════════════════════════════════════════

function deleteRow(i) {
    var records = getRecords();
    records.splice(i, 1);
    localStorage.setItem("records", JSON.stringify(records));
    updateUI();
    showToast("Record removed.");
}

function resetData() {
    if (!confirm("Reset all local records?")) return;
    localStorage.removeItem("records");
    updateUI();
    showToast("All records cleared.");
}

function logout() {
    sessionStorage.clear();
    window.location.href = "login.html";
}

// ════════════════════════════════════════════════════════════════
//  HELPERS
// ════════════════════════════════════════════════════════════════

function getRecords() {
    return JSON.parse(localStorage.getItem("records")) || [];
}

function saveToLocal(d, t, speed, accuracy, stamina, score, level) {
    var records = getRecords();
    records.push({ d: d, t: t, speed: speed, accuracy: accuracy, stamina: stamina, score: score, level: level });
    localStorage.setItem("records", JSON.stringify(records));
}

function setEl(id, val) {
    var el = document.getElementById(id);
    if (el) el.textContent = val;
}

function clearForm() {
    ["distance", "time", "accuracy", "stamina"].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) el.value = "";
    });
}

// Mirrors Java PerformanceLevel.getLevel() — used as offline fallback only
function getLocalLevel(score) {
    if (score >= 85) return "Excellent";
    if (score >= 70) return "Good";
    if (score >= 50) return "Average";
    return "Needs Improvement";
}

function showLevelBadge(level, score) {
    var badge = document.getElementById("levelBadge");
    if (!badge) return;
    var cls = level === "Needs Improvement" ? "level-Needs" : "level-" + level;
    badge.innerHTML = '<span class="level-badge ' + cls + '">Score: ' + parseFloat(score).toFixed(1) + ' — ' + level + '</span>';
}

function showToast(msg, type) {
    var t = document.getElementById("toast");
    if (!t) return;
    t.textContent = msg;
    t.className   = "toast " + (type || "green") + " show";
    setTimeout(function () { t.classList.remove("show"); }, 2800);
}

function showError(msg) {
    var el = document.getElementById("errorMsg");
    if (!el) return;
    el.textContent = msg;
    el.classList.add("show");
    hideMsg("successMsg");
}

function showSuccess(msg) {
    var el = document.getElementById("successMsg");
    if (!el) return;
    el.textContent = msg;
    el.classList.add("show");
    hideMsg("errorMsg");
}

function hideMsg(id) {
    var el = document.getElementById(id);
    if (el) el.classList.remove("show");
}
