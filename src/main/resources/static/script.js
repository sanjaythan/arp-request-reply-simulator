
const $ = (id) => document.getElementById(id);

async function requestJSON(url, options = {}) {
    const response = await fetch(url, options);

    let data;
    try {
        data = await response.json();
    } catch {
        throw new Error(`Invalid server response (${response.status}).`);
    }

    if (!response.ok) {
        throw new Error(
            data.message || data.error || `Request failed (${response.status}).`
        );
    }

    return data;
}

function formBody(values) {
    return new URLSearchParams(
        Object.entries(values).map(([key, value]) => [
            key,
            String(value ?? "")
        ])
    );
}

async function postForm(url, values) {
    return requestJSON(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: formBody(values)
    });
}

function showMessage(id, message) {
    const element = $(id);
    if (element) element.textContent = message;
}

function makeCell(value) {
    const cell = document.createElement("td");
    cell.textContent = String(value ?? "");
    return cell;
}

function makeOption(value, label) {
    const option = document.createElement("option");
    option.value = String(value);
    option.textContent = label;
    return option;
}

// -------------------- DEVICES --------------------

async function loadDevices() {
    const devices = await requestJSON("/api/devices");

    const sender = $("sender");
    const cacheDevice = $("cacheDevice");
    const editDevice = $("editDevice");

    const oldSender = sender.value;
    const oldCache = cacheDevice.value;
    const oldEdit = editDevice.value;

    sender.replaceChildren();
    cacheDevice.replaceChildren();
    editDevice.replaceChildren();

    editDevice.appendChild(makeOption("", "Choose a device"));

    for (const device of devices) {
        const label = `${device.name} (${device.ip})`;

        sender.appendChild(makeOption(device.index, label));
        cacheDevice.appendChild(makeOption(device.index, label));
        editDevice.appendChild(makeOption(device.index, label));
    }

    if (devices.some(d => String(d.index) === oldSender)) {
        sender.value = oldSender;
    }

    if (devices.some(d => String(d.index) === oldCache)) {
        cacheDevice.value = oldCache;
    }

    if (devices.some(d => String(d.index) === oldEdit)) {
        editDevice.value = oldEdit;
    }

    const table = $("deviceTable");
    table.replaceChildren();

    for (const device of devices) {
        const row = document.createElement("tr");
        row.append(
            makeCell(device.name),
            makeCell(device.ip),
            makeCell(device.mac)
        );
        table.appendChild(row);
    }

    $("deviceCount").textContent =
        `${devices.length} device${devices.length === 1 ? "" : "s"}`;

    await loadCache();
}

function clearDeviceForm() {
    $("deviceForm").reset();
    $("editDevice").value = "";
    showMessage("deviceMessage", "");
}

async function showSelectedDevice() {
    const index = $("editDevice").value;

    if (index === "") {
        $("deviceName").value = "";
        $("deviceIP").value = "";
        $("deviceMAC").value = "";
        return;
    }

    try {
        const devices = await requestJSON("/api/devices");
        const device = devices.find(d => String(d.index) === index);

        if (!device) {
            showMessage("deviceMessage", "Device not found.");
            return;
        }

        $("deviceName").value = device.name;
        $("deviceIP").value = device.ip;
        $("deviceMAC").value = device.mac;
        showMessage("deviceMessage", "Device loaded for editing.");
    } catch (error) {
        showMessage("deviceMessage", error.message);
    }
}

async function runDeviceAction(url, values) {
    try {
        const data = await postForm(url, values);

        showMessage("deviceMessage", data.message);
        await refreshDashboard();
        clearDeviceForm();
    } catch (error) {
        showMessage("deviceMessage", error.message);
        await loadEvents().catch(() => {});
    }
}

$("deviceForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    await runDeviceAction("/api/devices/add", {
        name: $("deviceName").value.trim(),
        ip: $("deviceIP").value.trim(),
        mac: $("deviceMAC").value.trim()
    });
});

$("editDeviceButton").addEventListener("click", async () => {
    const index = $("editDevice").value;

    if (index === "") {
        showMessage("deviceMessage", "Select a device to edit.");
        return;
    }

    await runDeviceAction("/api/devices/edit", {
        index,
        name: $("deviceName").value.trim(),
        ip: $("deviceIP").value.trim(),
        mac: $("deviceMAC").value.trim()
    });
});

$("removeDeviceButton").addEventListener("click", async () => {
    const index = $("editDevice").value;

    if (index === "") {
        showMessage("deviceMessage", "Select a device to remove.");
        return;
    }

    const confirmed = window.confirm(
        "Remove this device from the simulated network?"
    );

    if (!confirmed) return;

    await runDeviceAction("/api/devices/remove", { index });
});

$("editDevice").addEventListener("change", showSelectedDevice);
$("clearDeviceFormButton").addEventListener("click", clearDeviceForm);

// -------------------- STATISTICS --------------------

async function loadStats() {
    const stats = await requestJSON("/api/stats");

    $("requests").textContent = stats.totalRequests;
    $("replies").textContent = stats.totalReplies;
    $("hits").textContent = stats.cacheHits;
    $("misses").textContent = stats.cacheMisses;
    $("failures").textContent = stats.failedResolutions;
}

$("resetStatsButton").addEventListener("click", async () => {
    if (!window.confirm("Reset all network statistics?")) return;

    try {
        const data = await requestJSON("/api/stats/reset", {
            method: "POST"
        });

        showMessage("cacheConfigMessage", data.message);
        await refreshDashboard();
    } catch (error) {
        showMessage("cacheConfigMessage", error.message);
    }
});

// -------------------- ARP RESOLUTION --------------------

async function resolveAddress() {
    const senderIndex = $("sender").value;
    const targetIP = $("target").value.trim();

    if (senderIndex === "") {
        showMessage("resolveResult", "Select a sender device.");
        return;
    }

    if (!targetIP) {
        showMessage("resolveResult", "Enter a target IPv4 address.");
        return;
    }

    showMessage("resolveResult", "Resolving address...");

    try {
        const data = await postForm("/api/resolve", {
            senderIndex,
            targetIP
        });

        if (data.success) {
            showMessage(
                "resolveResult",
                `SUCCESS: ${targetIP} resolves to ${data.mac}`
            );
        } else {
            showMessage(
                "resolveResult",
                `FAILED: ${data.message}`
            );
        }

        await refreshDashboard();
    } catch (error) {
        showMessage("resolveResult", `Error: ${error.message}`);
    }
}

$("resolveButton").addEventListener("click", resolveAddress);

$("target").addEventListener("keydown", (event) => {
    if (event.key === "Enter") resolveAddress();
});

// -------------------- ARP CACHE --------------------

async function loadCache() {
    const index = $("cacheDevice").value;

    if (index === "") {
        $("cacheTable").replaceChildren();
        return;
    }

    const entries = await requestJSON(
        `/api/cache?index=${encodeURIComponent(index)}`
    );

    const table = $("cacheTable");
    table.replaceChildren();

    if (entries.length === 0) {
        const row = document.createElement("tr");
        const cell = makeCell("No cached entries. Resolve an IP first.");
        cell.colSpan = 3;
        row.appendChild(cell);
        table.appendChild(row);
        return;
    }

    for (const entry of entries) {
        const row = document.createElement("tr");
        row.append(
            makeCell(entry.ip),
            makeCell(entry.mac),
            makeCell(`${entry.ageSeconds}s`)
        );
        table.appendChild(row);
    }
}

$("cacheDevice").addEventListener("change", () => {
    loadCache().catch(error => showMessage("cacheMessage", error.message));
});

$("clearCacheButton").addEventListener("click", async () => {
    if (!window.confirm("Clear all ARP caches?")) return;

    try {
        const data = await requestJSON("/api/cache/clear", {
            method: "POST"
        });

        showMessage("cacheMessage", data.message);
        await refreshDashboard();
    } catch (error) {
        showMessage("cacheMessage", error.message);
    }
});

// -------------------- TTL --------------------

async function loadTTL() {
    const data = await requestJSON("/api/ttl");
    $("ttlInput").value = data.ttlSeconds;
}

$("saveTTLButton").addEventListener("click", async () => {
    const ttl = Number($("ttlInput").value);

    if (!Number.isInteger(ttl) || ttl < 5 || ttl > 3600) {
        showMessage(
            "cacheConfigMessage",
            "TTL must be a whole number between 5 and 3600 seconds."
        );
        return;
    }

    try {
        const data = await postForm("/api/ttl", {
            ttlSeconds: ttl
        });

        showMessage("cacheConfigMessage", data.message);
        await refreshDashboard();
    } catch (error) {
        showMessage("cacheConfigMessage", error.message);
    }
});

// -------------------- EVENT LOG --------------------

async function loadEvents() {
    const events = await requestJSON("/api/events");

    $("eventLog").textContent = events.length
        ? events.slice().reverse().join("\n")
        : "No events yet.";
}

// -------------------- DASHBOARD REFRESH --------------------

async function refreshDashboard() {
    try {
        await Promise.all([
            loadDevices(),
            loadStats(),
            loadEvents()
        ]);
    } catch (error) {
        console.error("Dashboard refresh failed:", error);
        showMessage(
            "resolveResult",
            `Could not refresh dashboard: ${error.message}`
        );
    }
}

$("refreshButton").addEventListener("click", refreshDashboard);

// Initial page load
async function initializeDashboard() {
    try {
        await Promise.all([
            loadDevices(),
            loadStats(),
            loadEvents(),
            loadTTL()
        ]);
    } catch (error) {
        console.error("Initialization failed:", error);
        showMessage(
            "resolveResult",
            `Could not initialize dashboard: ${error.message}`
        );
    }
}

initializeDashboard();
