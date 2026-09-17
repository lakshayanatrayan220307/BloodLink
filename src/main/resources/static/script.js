/* =========================================================
   BloodLink — script.js
   Vanilla JS only. No frameworks.

   HOW TO CONNECT TO SPRING BOOT LATER:
   Every function inside the API object below is the ONLY
   place that talks to a data source. Right now each one
   returns mock data. When your Spring Boot backend is ready,
   replace the body of each function with a fetch() call to
   your REST endpoint and keep the same function signature —
   nothing else in this file needs to change.
   ========================================================= */

/* ============================================================
   1. CONFIG
   ============================================================ */
const CONFIG = {
  API_BASE_URL: "https://bloodlink-backend-9cxg.onrender.com/api",   
  USE_MOCK_DATA: false
};

/* ============================================================
   2. API LAYER  — swap the internals of these functions only
   ============================================================ */
const API = {

  /**
   * GET /api/dashboard/stats
   * Expected shape: { bloodUnits: number, donorCount: number, activeEmergencies: number }
   */
  async getDashboardStats() {
    if (CONFIG.USE_MOCK_DATA) {
      await mockDelay();
      const totalUnits = Object.values(MOCK_DB.inventory).reduce((a, b) => a + b, 0);
      return {
        bloodUnits: totalUnits,
        donorCount: MOCK_DB.donors.length,
        activeEmergencies: MOCK_DB.requests.length
      };
    }
    return httpGet(`${CONFIG.API_BASE_URL}/dashboard/stats`);
  },

  /**
   * GET /api/inventory
   * Expected shape: { "A+": number, "A-": number, ... }
   */
  async getInventory() {
    if (CONFIG.USE_MOCK_DATA) {
      await mockDelay();
      return { ...MOCK_DB.inventory };
    }
    return httpGet(`${CONFIG.API_BASE_URL}/inventory`);
  },

  /**
   * POST /api/requests
   * body: { hospitalName, bloodGroup, unitsNeeded, urgency, hospitalLocation }
   * Expected response: {
   *   request: { id, hospitalName, bloodGroup, unitsNeeded, urgency, hospitalLocation },
   *   inventoryCheck: { availableUnits, fulfilledFromInventory }
   * }
   */
  async createEmergencyRequest(payload) {
    if (CONFIG.USE_MOCK_DATA) {
      await mockDelay();

      const id = "R-" + String(MOCK_DB.requests.length + 101);
      const availableUnits = MOCK_DB.inventory[payload.bloodGroup] || 0;
      const fulfilledFromInventory = availableUnits >= payload.unitsNeeded;

      if (fulfilledFromInventory) {
        MOCK_DB.inventory[payload.bloodGroup] -= payload.unitsNeeded;
      }

      const request = { id, ...payload };
      MOCK_DB.requests.push(request);
      MOCK_DB.activeRequest = request;

      return {
        request,
        inventoryCheck: { availableUnits, fulfilledFromInventory }
      };
    }
    return httpPost(`${CONFIG.API_BASE_URL}/requests`, payload);
  },

  /**
   * GET /api/requests/active
   * Returns the most recently created / focused request, or null.
   */
  async getActiveRequest() {
    if (CONFIG.USE_MOCK_DATA) {
      await mockDelay();
      return MOCK_DB.activeRequest;
    }
    return httpGet(`${CONFIG.API_BASE_URL}/requests/active`);
  },

  /**
   * GET /api/requests/{requestId}/matches
   * Expected shape: Array<{
   *   donorId, name, bloodGroup, compatibility ("FULL" | "PARTIAL"),
   *   distanceKm, priorityScore, available
   * }>
   */
  async findBestDonors(requestId) {
    if (CONFIG.USE_MOCK_DATA) {
      await mockDelay(500);
      return computeMockMatches(requestId);
    }
    return httpGet(`${CONFIG.API_BASE_URL}/requests/${requestId}/matches`);
  }
};

/* ============================================================
   3. HTTP HELPERS (used once USE_MOCK_DATA = false)
   ============================================================ */
async function httpGet(url) {
  const res = await fetch(url, { method: "GET", headers: { "Accept": "application/json" } });
  if (!res.ok) throw new Error(`GET ${url} failed: ${res.status}`);
  return res.json();
}

async function httpPost(url, body) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error(`POST ${url} failed: ${res.status}`);
  return res.json();
}

function mockDelay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms || 250));
}

/* ============================================================
   4. MOCK DATA (delete this block once Spring Boot is wired up)
   ============================================================ */
const MOCK_DB = {
  inventory: { "A+": 8, "A-": 2, "B+": 5, "B-": 3, "AB+": 2, "AB-": 1, "O+": 10, "O-": 4 },
  donors: [
    { id: "D-101", name: "Anu Priya", bloodGroup: "O+", location: "Velachery", available: true, lastDonationDays: 120 },
    { id: "D-102", name: "Karthik R", bloodGroup: "O-", location: "Adyar", available: true, lastDonationDays: 95 },
    { id: "D-103", name: "Divya S", bloodGroup: "A+", location: "T Nagar", available: true, lastDonationDays: 200 },
    { id: "D-104", name: "Mohammed Faiz", bloodGroup: "O+", location: "Guindy", available: false, lastDonationDays: 40 },
    { id: "D-105", name: "Sneha Raghavan", bloodGroup: "B+", location: "Porur", available: true, lastDonationDays: 150 },
    { id: "D-106", name: "Arjun Menon", bloodGroup: "AB+", location: "Anna Nagar", available: true, lastDonationDays: 110 }
  ],
  requests: [],
  activeRequest: null
};

const COMPATIBILITY = {
  "O-": g => true,
  "O+": g => ["O+", "A+", "B+", "AB+"].includes(g),
  "A-": g => ["A-", "A+", "AB-", "AB+"].includes(g),
  "A+": g => ["A+", "AB+"].includes(g),
  "B-": g => ["B-", "B+", "AB-", "AB+"].includes(g),
  "B+": g => ["B+", "AB+"].includes(g),
  "AB-": g => ["AB-", "AB+"].includes(g),
  "AB+": g => ["AB+"].includes(g)
};

function computeMockMatches(requestId) {
  const request = MOCK_DB.requests.find(r => r.id === requestId) || MOCK_DB.activeRequest;
  if (!request) return [];

  return MOCK_DB.donors
    .filter(d => COMPATIBILITY[d.bloodGroup] && COMPATIBILITY[d.bloodGroup](request.bloodGroup))
    .map(d => {
      // Deterministic mock distance/score so results are stable.
      const distanceKm = ((d.name.length * 7) % 15) + 1.2;
      const recencyBoost = Math.min(30, Math.floor(d.lastDonationDays / 10));
      const availabilityBoost = d.available ? 25 : 0;
      const groupMatchBoost = d.bloodGroup === request.bloodGroup ? 15 : 5;
      const distancePenalty = distanceKm * 2;

      const priorityScore = Math.max(
        0,
        Math.min(100, 50 + recencyBoost + availabilityBoost + groupMatchBoost - distancePenalty)
      );

      return {
        donorId: d.id,
        name: d.name,
        bloodGroup: d.bloodGroup,
        compatibility: d.bloodGroup === request.bloodGroup ? "FULL" : "PARTIAL",
        distanceKm: Number(distanceKm.toFixed(1)),
        priorityScore: Number(priorityScore.toFixed(1)),
        available: d.available
      };
    })
    .sort((a, b) => b.priorityScore - a.priorityScore);
}

/* ============================================================
   5. DOM HELPERS
   ============================================================ */
const $ = (sel, ctx) => (ctx || document).querySelector(sel);
const $all = (sel, ctx) => Array.from((ctx || document).querySelectorAll(sel));

function esc(str) {
  return String(str).replace(/[&<>"']/g, c => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[c]));
}

let toastTimer = null;
function showToast(message, type) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.className = "toast show" + (type ? " " + type : "");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("show"), 3200);
}

function setApiStatus(state, label) {
  const dot = $("#apiStatusDot");
  const text = $("#apiStatusText");
  dot.classList.remove("live", "error");
  if (state === "live") dot.classList.add("live");
  if (state === "error") dot.classList.add("error");
  text.textContent = label;
}

/* ============================================================
   6. NAVIGATION — tabbed sections inside the single index.html
   ============================================================ */

/**
 * Shows only the section whose id matches sectionId and hides
 * every other .page-section. Also updates which nav link has
 * the "active" class.
 */
function showSection(sectionId) {
  $all(".page-section").forEach(section => {
    section.classList.toggle("active", section.id === sectionId);
  });

  $all(".nav-link").forEach(link => {
    link.classList.toggle("active", link.dataset.section === sectionId);
  });

  // Reset scroll position so the newly shown section starts at the top.
  window.scrollTo({ top: 0, behavior: "instant" in window ? "instant" : "auto" });
}

// Alias kept for clarity / in case Spring Boot templates call it directly.
function switchSection(sectionId) {
  showSection(sectionId);
}

function initNav() {
  const links = $all(".nav-link");
  links.forEach(link => {
    link.addEventListener("click", (e) => {
      e.preventDefault();
      showSection(link.dataset.section);
    });
  });
}

/* ============================================================
   7. DASHBOARD STATS
   ============================================================ */
async function loadStats() {
  try {
    const stats = await API.getDashboardStats();
    $("#statUnits").textContent = stats.bloodUnits;
    $("#statDonors").textContent = stats.donorCount;
    $("#statEmergencies").textContent = stats.activeEmergencies;
  } catch (err) {
    console.error(err);
    showToast("Could not load dashboard stats.", "error");
  }
}

/* ============================================================
   8. INVENTORY
   ============================================================ */
async function loadInventory() {
  try {
    const inventory = await API.getInventory();
    renderInventory(inventory);
  } catch (err) {
    console.error(err);
    showToast("Could not load inventory.", "error");
  }
}

function renderInventory(inventory) {
  const list = $("#inventoryList");
  const groups = Object.keys(inventory);
  const maxUnits = Math.max(...Object.values(inventory), 1);

  list.innerHTML = groups.map(group => {
    const units = inventory[group];
    const pct = Math.round((units / maxUnits) * 100);
    const low = units < 4;
    return `
      <div class="inventory-row">
        <div class="inventory-group">${esc(group)}</div>
        <div class="inventory-bar-track">
          <div class="inventory-bar-fill ${low ? "low" : ""}" style="width:${pct}%"></div>
        </div>
        <div class="inventory-count">${units}</div>
      </div>
    `;
  }).join("");
}

/* ============================================================
   9. EMERGENCY REQUEST FORM
   ============================================================ */
function initEmergencyForm() {
  const form = $("#emergencyForm");
  const errorBox = $("#emergencyFormError");
  const submitBtn = $("#createRequestBtn");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    errorBox.textContent = "";

    const payload = {
      hospitalName: $("#hospitalName").value.trim(),
      bloodGroup: $("#bloodGroup").value,
      unitsNeeded: parseInt($("#unitsNeeded").value, 10),
      urgency: $("#urgency").value,
      hospitalLocation: $("#hospitalLocation").value.trim()
    };

    const validationError = validateRequestPayload(payload);
    if (validationError) {
      errorBox.textContent = validationError;
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Creating…";

    try {
      const result = await API.createEmergencyRequest(payload);
      showToast(`Request ${result.request.id} created for ${result.request.hospitalName}.`, "success");
      renderInventoryCheck(result);
      form.reset();
      $("#urgency").value = "Choose Urgency LevFel";
      $("#unitsNeeded").value = 0;

      // Refresh dependent panels
await Promise.all([
  loadStats(),
  loadInventory(),
  loadAllRequests()
]);
      $("#matchSubtitle").textContent = `Active request: ${result.request.id} — click "Find Best Donors"`;
      clearDonorTable();
    } catch (err) {
      console.error(err);
      errorBox.textContent = "Could not create the request. Please try again.";
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Create Request";
    }
  });
}

function validateRequestPayload(payload) {
  if (!payload.hospitalName) return "Hospital name is required.";
  if (!payload.bloodGroup) return "Select a blood group.";
  if (!payload.unitsNeeded || payload.unitsNeeded <= 0) return "Units must be greater than 0.";
  if (!payload.hospitalLocation) return "Hospital location is required.";
  return null;
}

function renderInventoryCheck(result) {
  const box = $("#inventoryCheck");
  box.hidden = false;

  $("#checkRequired").textContent =
    `${result.request.unitsNeeded} × ${result.request.bloodGroup}`;

  $("#checkAvailable").textContent =
    `${result.inventoryCheck.availableUnits} units`;

  const statusEl = $("#checkStatus");

  // Enough blood available
  if (result.inventoryCheck.fulfilledFromInventory) {

    statusEl.className = "inventory-check-status ok";
    statusEl.textContent =
      "Fulfilled from inventory — donor search optional.";

  } 
  
  // Not enough blood available
  else {

    const remaining =
      result.request.unitsNeeded -
      result.inventoryCheck.availableUnits;

    statusEl.className = "inventory-check-status short";

    statusEl.innerHTML = `
      Inventory short by ${remaining} unit(s).
      <button
        type="button"
        class="find-priority-btn"
        onclick="goToPriorityDonors('${result.request.requestId}')">
        Find Priority Donors →
      </button>
    `;
  }
}

function goToPriorityDonors(requestId) {
    window.activePriorityRequestId = requestId;

    // Open Donors section
    const donorsNav = document.querySelector(
        '[data-section="donorsSection"]'
    );

    if (donorsNav) {
        donorsNav.click();
    }

    // Wait for Donors section to become visible
    setTimeout(() => {

        // Find the actual Priority Donors panel
        const priorityPanel = document.querySelector(
            '#donorsSection .page:last-child .panel-wide'
        );

        if (priorityPanel) {
            priorityPanel.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        }

        // Run donor matching
        const findButton = $("#findDonorsBtn");

        if (findButton) {
            findButton.click();
        }

    }, 350);
}

/* ============================================================
   10. DONOR MATCHING TABLE
   ============================================================ */
function initFindDonorsButton() {
  $("#findDonorsBtn").addEventListener("click", async () => {
    const btn = $("#findDonorsBtn");
    const active = await API.getActiveRequest();

    if (!active) {
      showToast("Create an emergency request first.", "error");
      return;
    }

    btn.disabled = true;
    btn.textContent = "Searching…";

    try {
      const matches = await API.findBestDonors(active.id);

      const tableBody = $("#donorTableBody");
      const emptyBox = $("#donorTableEmpty");
      const subtitle = $("#matchSubtitle");

      // Clear previous results
      tableBody.innerHTML = "";

      // NO DONORS FOUND
      if (!matches || matches.length === 0) {
        emptyBox.textContent =
          "No suitable donors are currently available for this emergency request.";

        emptyBox.classList.add("visible");

        subtitle.textContent =
          "No compatible, eligible, and available donors found for this request.";

        return;
      }

      // DONORS FOUND
      emptyBox.classList.remove("visible");

      subtitle.textContent =
        `Found ${matches.length} suitable donor${matches.length === 1 ? "" : "s"}.`;

      renderDonorTable(matches, active);

    } catch (err) {
      console.error(err);
      showToast("Could not fetch donor matches.", "error");
    } finally {
      btn.disabled = false;
      btn.textContent = "Find Best Donors";
    }
  });
}

function clearDonorTable() {
  $("#donorTableBody").innerHTML = "";
  $("#donorTableEmpty").classList.add("visible");
  $(".priority-donor-table").classList.add("hidden");
}

function renderDonorTable(matches, activeRequest) {
  const tbody = $("#donorTableBody");
  const emptyEl = $("#donorTableEmpty");

  // Get ONLY the Priority Donors table
  const table = document.querySelector(
    "#donorsSection .priority-donor-table"
  );

  if (!matches || matches.length === 0) {
    tbody.innerHTML = "";

    emptyEl.textContent =
      "No suitable donors are currently available for this emergency request.";

    emptyEl.classList.add("visible");

    if (table) {
      table.classList.add("hidden");
    }

    $("#matchSubtitle").textContent =
      "No compatible, eligible, and available donors found for this request.";

    return;
  }

  // Donors found
  emptyEl.classList.remove("visible");

  if (table) {
    table.classList.remove("hidden");
  }

  $("#matchSubtitle").textContent =
    `Ranked donors for ${activeRequest.id} — ${activeRequest.hospitalName}`;

  tbody.innerHTML = matches.map((m, i) => {
    const rank = i + 1;

    const compatClass =
      m.compatibility === "FULL" ? "full" : "partial";

    const compatLabel =
      m.compatibility === "FULL"
        ? "Exact match"
        : "Compatible";

    const availClass =
      m.available ? "available" : "unavailable";

    const availLabel =
      m.available ? "Available" : "Unavailable";

    return `
      <tr>
        <td>
          <span class="rank-badge ${rank === 1 ? "top" : ""}">
            #${rank}
          </span>
        </td>

        <td>
          <span class="donor-name">${esc(m.name)}</span>
          <span class="donor-id">${esc(m.donorId)}</span>
        </td>

        <td>
          <span class="chip chip-group">
            ${esc(m.bloodGroup)}
          </span>
        </td>

        <!-- PHONE NUMBER -->
        <td>
          <a
            href="tel:${esc(m.phone)}"
            class="donor-phone-link"
            title="Call ${esc(m.name)}"
          >
            📞 ${esc(m.phone)}
          </a>
        </td>

        <td>
          <span class="compat ${compatClass}">
            ${compatLabel}
          </span>
        </td>

        <td>${m.distanceKm} km</td>

        <td>
          <div class="score-value">${m.priorityScore}</div>
          <div class="score-track">
            <div
              class="score-fill"
              style="width:${m.priorityScore}%">
            </div>
          </div>
        </td>

        <td>
          <span class="avail-chip ${availClass}">
            ${availLabel}
          </span>
        </td>
      </tr>
    `;
  }).join("");
}
async function loadAllDonors() {
    try {
        const donors = await httpGet(`${CONFIG.API_BASE_URL}/donors`);

        console.log("Registered donors:", donors);

        const tbody = document.getElementById("registeredDonorTableBody");
const emptyEl = document.getElementById("registeredDonorEmpty");

console.log("Registered donor tbody:", tbody);
console.log("Registered donor empty:", emptyEl);

if (!tbody) return;

        if (!donors || donors.length === 0) {
            tbody.innerHTML = "";
            emptyEl.classList.add("visible");
            return;
        }

        emptyEl.classList.remove("visible");

        tbody.innerHTML = donors.map(donor => `
            <tr>
                <td>${esc(donor.donorId)}</td>
                <td>${esc(donor.name)}</td>
                <td>${donor.age}</td>
                <td><span class="chip chip-group">${esc(donor.bloodGroup)}</span></td>
                <td>${esc(donor.location)}</td>
                <td>${donor.unitsDonated}</td>
                <td>
                    <span class="avail-chip ${donor.available ? "available" : "unavailable"}">
                        ${donor.available ? "Available" : "Not Available"}
                    </span>
                </td>
                <td>
    <button class="delete-donor-btn" onclick="deleteDonor('${esc(donor.donorId)}')">
        🗑️
    </button>
</td>
            </tr>
        `).join("");

    } catch (error) {
        console.error("Could not load donors:", error);
    }
}

/* ============================================================
   11. VIEW FULL INVENTORY — jumps to the Inventory tab
   ============================================================ */
function initViewFullInventoryButton() {
  const btn = $("#viewFullInventoryBtn");
  if (!btn) return;
  btn.addEventListener("click", () => {
    showSection("inventorySection");
  });
}

// ==================== DONOR REGISTRATION ====================

function initDonorForm() {
    const form = document.getElementById("donorForm");

    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const errorBox = document.getElementById("donorFormError");
        errorBox.textContent = "";

        const donorData = {
            donorId: document.getElementById("donorId").value.trim(),
            name: document.getElementById("donorName").value.trim(),
            age: Number(document.getElementById("donorAge").value),
            bloodGroup: document.getElementById("donorBloodGroup").value,
            phone: document.getElementById("donorPhone").value.trim(),
            location: document.getElementById("donorLocation").value.trim(),
            unitsDonated:Number(document.getElementById("unitsDonated").value),
            lastDonationDate:
                document.getElementById("lastDonationDate").value.trim(),
            available:
                document.getElementById("donorAvailable").value === "true"
        };

        try {
            const response = await httpPost(
                "/api/donors",
                donorData
            );

            if (response.success) {
    showToast("Donor registered successfully!");

    form.reset();

    // Stay on Donors page
    showSection("donorsSection");

    // Refresh donor count
    loadStats();

    loadAllDonors();

    // Refresh donor table if needed
    clearDonorTable();
} else {
                errorBox.textContent =
                    response.message || "Unable to register donor.";
            }

        } catch (error) {
            console.error(error);

            errorBox.textContent =
                "Could not connect to the Java backend.";
        }
    });
}

/* ============================================================
   12. INIT
   ============================================================ */
async function init() {
  setApiStatus(
    CONFIG.USE_MOCK_DATA ? "mock" : "live",
    CONFIG.USE_MOCK_DATA ? "API: Mock data" : "API: Connected"
  );

  initNav();
  initEmergencyForm();
  initDonorForm();
  loadAllDonors();
  initFindDonorsButton();
  initViewFullInventoryButton();
  clearDonorTable();
  loadAllRequests();

  await Promise.all([loadStats(), loadInventory()]);
}

async function deleteDonor(donorId) {

    const confirmDelete = confirm(
        "Are you sure you want to delete this donor?"
    );

    if (!confirmDelete) {
        return;
    }

    try {

        const response = await fetch(`/api/donors/${encodeURIComponent(donorId)}`, {
            method: "DELETE"
        });

        const result = await response.json();

        if (result.success) {

            showToast("Donor deleted successfully!");

            loadAllDonors();
            loadStats();
            loadAllRequests();

        } else {

            alert(result.message || "Unable to delete donor.");
        }

    } catch (error) {

        console.error("Delete donor error:", error);

        alert("Could not connect to the Java backend.");
    }
}

async function loadAllRequests() {

    try {

        const requests = await httpGet(`${CONFIG.API_BASE_URL}/requests`);

        const tbody = document.getElementById("requestTableBody");
        const emptyEl = document.getElementById("requestTableEmpty");

        if (!tbody) return;

        if (!requests || requests.length === 0) {
            tbody.innerHTML = "";
            emptyEl.classList.add("visible");
            return;
        }

        emptyEl.classList.remove("visible");

        // Highest priority first
        const priorityOrder = {
            "CRITICAL": 1,
            "HIGH": 2,
            "MEDIUM": 3,
            "LOW": 4
        };

        requests.sort((a, b) => {
            return (priorityOrder[a.urgency] || 5) -
                   (priorityOrder[b.urgency] || 5);
        });

        tbody.innerHTML = requests.map(request => {

            let urgencyClass = request.urgency.toLowerCase();

            return `
                <tr>
                    <td>${esc(request.requestId)}</td>
                    <td>${esc(request.hospitalName)}</td>
                    <td>
                        <span class="chip chip-group">
                            ${esc(request.requiredBloodGroup)}
                        </span>
                    </td>
                    <td>${request.unitsNeeded}</td>
                    <td>
                        <span class="urgency-chip ${urgencyClass}">
                            ${esc(request.urgency)}
                        </span>
                    </td>
                    <td>${esc(request.hospitalLocation)}</td>
                    <td>
                        <span class="avail-chip available">
                            Active
                        </span>
                    </td>
                    <td>
    <button class="delete-donor-btn"
            onclick="deleteRequest('${esc(request.requestId)}')">
        🗑️
    </button>
</td>
                </tr>
            `;

        }).join("");

    } catch (error) {

        console.error("Could not load emergency requests:", error);

    }
}

async function deleteRequest(requestId) {

    const confirmDelete = confirm(
        "Are you sure you want to delete this emergency request?"
    );

    if (!confirmDelete) {
        return;
    }

    try {

        const response = await fetch(
            `/api/requests/${encodeURIComponent(requestId)}`,
            {
                method: "DELETE"
            }
        );

        const result = await response.json();

        if (result.success) {

            showToast("Emergency request deleted successfully!");

            loadAllRequests();
            loadStats();

        } else {

            alert(result.message || "Unable to delete request.");
        }

    } catch (error) {

        console.error("Delete request error:", error);

        alert("Could not connect to the Java backend.");
    }
}

document.addEventListener("DOMContentLoaded", init);