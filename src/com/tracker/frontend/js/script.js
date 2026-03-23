let chart;

function login() {

    fetch("http://localhost:8080/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        }
    })
    .then(res => res.text())
    .then(data => {
        alert(data);
        window.location.href = "dashboard.html";
    })
    .catch(err => {
        console.error(err);
        alert("Server not running or login failed");
    });
}

function getData() {
    return JSON.parse(localStorage.getItem("records")) || [];
}

function saveData() {

    let d = parseFloat(document.getElementById("distance").value);
    let t = parseFloat(document.getElementById("time").value);

    if (!d || !t) {
        alert("Enter valid values");
        return;
    }

    let speed = d / t;

    // ✅ Local storage (UI)
    let data = getData();
    data.push({ d, t, speed });
    localStorage.setItem("records", JSON.stringify(data));

    updateUI();

    // ✅ Backend API call
    fetch("http://localhost:8080/save", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            distance: d,
            time: t,
            speed: speed
        })
    })
    .then(res => res.text())
    .then(data => console.log("Backend:", data))
    .catch(err => console.error("Error:", err));
}

function updateUI() {

    let data = getData();
    let table = document.getElementById("table");

    table.innerHTML = `
        <tr>
            <th>#</th>
            <th>Distance</th>
            <th>Time</th>
            <th>Speed</th>
            <th>Action</th>
        </tr>
    `;

    let total = 0;
    let best = 0;

    data.forEach((r, i) => {

        total += r.speed;
        if (r.speed > best) best = r.speed;

        table.innerHTML += `
            <tr>
                <td>${i + 1}</td>
                <td>${r.d}</td>
                <td>${r.t}</td>
                <td>${r.speed.toFixed(2)}</td>
                <td><button onclick="deleteRow(${i})">❌</button></td>
            </tr>
        `;
    });

    document.getElementById("avg").innerText =
        data.length ? (total / data.length).toFixed(2) : 0;

    document.getElementById("best").innerText =
        data.length ? best.toFixed(2) : 0;

    drawChart(data);
}

function deleteRow(index) {
    let data = getData();
    data.splice(index, 1);
    localStorage.setItem("records", JSON.stringify(data));
    updateUI();
}

function resetData() {
    localStorage.removeItem("records");
    updateUI();
}

function drawChart(data) {

    let labels = data.map((_, i) => i + 1);
    let speeds = data.map(r => r.speed);

    if (chart) chart.destroy();

    chart = new Chart(document.getElementById("chart"), {
        type: "line",
        data: {
            labels: labels,
            datasets: [{
                label: "Speed",
                data: speeds,
                fill: false
            }]
        }
    });
}

window.onload = updateUI;
