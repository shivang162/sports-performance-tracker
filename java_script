let chart;

function login() {
    window.location.href = "dashboard.html";
}

function getData() {
    return JSON.parse(localStorage.getItem("records")) || [];
}

function saveData() {
    let d = parseFloat(distance.value);
    let t = parseFloat(time.value);

    if (!d || !t) return alert("Enter valid values");

    let speed = d / t;

    let data = getData();
    data.push({ d, t, speed });

    localStorage.setItem("records", JSON.stringify(data));

    updateUI();
}

function updateUI() {
    let data = getData();
    let table = document.getElementById("table");

    table.innerHTML = `
        <tr>
            <th>#</th><th>Distance</th><th>Time</th><th>Speed</th><th>Action</th>
        </tr>
    `;

    let total = 0, best = 0;

    data.forEach((r, i) => {
        total += r.speed;
        if (r.speed > best) best = r.speed;

        table.innerHTML += `
            <tr>
                <td>${i+1}</td>
                <td>${r.d}</td>
                <td>${r.t}</td>
                <td>${r.speed.toFixed(2)}</td>
                <td><button onclick="deleteRow(${i})">❌</button></td>
            </tr>
        `;
    });

    document.getElementById("avg").innerText = data.length ? (total/data.length).toFixed(2) : 0;
    document.getElementById("best").innerText = best.toFixed(2);

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
                data: speeds
            }]
        }
    });
}

window.onload = updateUI;
