// ============================================
// BLOODBRIDGE INVENTORY & EXPIRY MANAGEMENT
// ============================================

let currentUser = null;

document.addEventListener("DOMContentLoaded", async function () {
    currentUser = await getCurrentUser(true);
    if (!currentUser) return;

    setupDateDefaults();
    await Promise.all([
        loadInventoryAlerts(),
        loadInventoryData()
    ]);
});

function setupDateDefaults() {
    const today = new Date();
    const todayStr = today.toISOString().split("T")[0];
    
    // Default whole blood expiry is 35 days from collection
    const expiry = new Date();
    expiry.setDate(today.getDate() + 35);
    const expiryStr = expiry.toISOString().split("T")[0];

    const collInput = document.getElementById("newBatchCollectionDate");
    const expInput = document.getElementById("newBatchExpiryDate");
    if (collInput) collInput.value = todayStr;
    if (expInput) expInput.value = expiryStr;
}

async function loadInventoryAlerts() {
    try {
        const alerts = await apiFetch("/api/inventory/alerts", { redirectOnUnauthorized: false });
        if (!alerts) return;

        document.getElementById("totalAvailableCount").textContent = alerts.totalAvailableUnits;
        document.getElementById("totalReservedCount").textContent = alerts.totalReservedUnits;
        document.getElementById("expiringSoonCount").textContent = alerts.expiringUnitsCount;

        const lowStockTypes = (alerts.stockSummary || []).filter(s => s.isLowStock);
        document.getElementById("lowStockCount").textContent = lowStockTypes.length;

        // Render stock breakdown grid
        const grid = document.getElementById("stockGroupsContainer");
        if (grid && alerts.stockSummary) {
            grid.innerHTML = alerts.stockSummary.map(item => {
                const bgDisplay = formatBloodGroup(item.bloodGroup);
                const isLow = item.isLowStock;
                return `
                    <div style="text-align: center; padding: 12px 8px; border-radius: 10px; background: ${isLow ? '#fff1f2' : '#f8fafc'}; border: 1.5px solid ${isLow ? '#fecdd3' : '#e2e8f0'};">
                        <div style="font-weight: 800; font-size: 16px; color: ${isLow ? '#e11d48' : '#0f172a'};">${bgDisplay}</div>
                        <div style="font-size: 18px; font-weight: 700; margin: 4px 0; color: ${isLow ? '#be123c' : '#059669'};">${item.totalQuantity} <small style="font-size: 11px; font-weight: 500;">units</small></div>
                        <span style="font-size: 10px; font-weight: 700; text-transform: uppercase; color: ${isLow ? '#e11d48' : '#64748b'};">${isLow ? '⚠️ Low Stock' : 'Optimal'}</span>
                    </div>
                `;
            }).join("");
        }

    } catch (e) {
        console.warn("Failed to load inventory alerts:", e);
    }
}

async function loadInventoryData() {
    const tableBody = document.getElementById("inventoryTableBody");
    if (!tableBody) return;

    const bgVal = document.getElementById("filterBloodGroup").value;
    const statusVal = document.getElementById("filterStatus").value;

    const params = new URLSearchParams();
    if (bgVal) params.append("bloodGroup", bgVal);
    if (statusVal) params.append("status", statusVal);

    try {
        const items = await apiFetch("/api/inventory?" + params.toString(), { redirectOnUnauthorized: false }) || [];
        if (items.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; color: #64748b; padding: 36px;">
                        No blood batches found for the selected filter.
                    </td>
                </tr>
            `;
            return;
        }

        tableBody.innerHTML = items.map(item => {
            const bgDisplay = formatBloodGroup(item.bloodGroup);
            let statusBadge = "";
            if (item.status === "AVAILABLE") {
                statusBadge = `<span class="badge-status status-available">Available</span>`;
            } else if (item.status === "RESERVED") {
                statusBadge = `<span class="badge-status status-reserved">Reserved</span>`;
            } else if (item.status === "ISSUED") {
                statusBadge = `<span class="badge-status status-issued">Issued</span>`;
            } else {
                statusBadge = `<span class="badge-status status-expired">Expired</span>`;
            }

            const rowClass = item.expiringSoon ? "expiring-alert-row" : "";
            const expiryNotice = item.expiringSoon ? `<br><small style="color: #dc2626; font-weight: 700;">⚠️ Expiring in ${item.daysUntilExpiry} days</small>` : '';

            let actionsHtml = "—";
            if (item.status === "AVAILABLE") {
                actionsHtml = `
                    <div style="display: flex; gap: 6px;">
                        <button class="btn-action-sm btn-reserve" onclick="reserveBatchUnits(${item.id}, ${item.quantity})">Reserve</button>
                        <button class="btn-action-sm btn-issue" onclick="issueBatchUnits(${item.id}, ${item.quantity})">Issue</button>
                    </div>
                `;
            } else if (item.status === "RESERVED") {
                actionsHtml = `
                    <button class="btn-action-sm btn-issue" onclick="issueBatchUnits(${item.id}, ${item.quantity})">Issue Units</button>
                `;
            }

            return `
                <tr class="${rowClass}">
                    <td><code>${escapeHtml(item.batchUnitId)}</code></td>
                    <td><strong>${bgDisplay}</strong></td>
                    <td><strong>${item.quantity}</strong> units</td>
                    <td>${escapeHtml(item.facilityName)}</td>
                    <td>${item.collectionDate}</td>
                    <td>${item.expiryDate}${expiryNotice}</td>
                    <td>${statusBadge}</td>
                    <td>${actionsHtml}</td>
                </tr>
            `;
        }).join("");

    } catch (e) {
        console.error("Failed to load inventory data:", e);
        tableBody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: #dc2626; padding: 20px;">Failed to load inventory: ${escapeHtml(e.message)}</td></tr>`;
    }
}

function openAddBatchModal() {
    document.getElementById("addBatchModal").style.display = "flex";
}

function closeAddBatchModal() {
    document.getElementById("addBatchModal").style.display = "none";
}

async function submitNewBatch(event) {
    event.preventDefault();

    const bgVal = document.getElementById("newBatchBg").value;
    const qtyVal = parseInt(document.getElementById("newBatchQty").value, 10);
    const facilityVal = document.getElementById("newBatchFacility").value.trim();
    const collVal = document.getElementById("newBatchCollectionDate").value;
    const expVal = document.getElementById("newBatchExpiryDate").value;

    if (!facilityVal) {
        showToast("Please enter a facility name.", "warning");
        return;
    }

    try {
        await apiFetch("/api/inventory", {
            method: "POST",
            body: {
                bloodGroup: bgVal,
                quantity: qtyVal,
                facilityName: facilityVal,
                collectionDate: collVal,
                expiryDate: expVal
            }
        });

        showToast("Blood batch added to inventory successfully!", "success");
        closeAddBatchModal();
        await Promise.all([
            loadInventoryAlerts(),
            loadInventoryData()
        ]);

    } catch (e) {
        console.error("Failed to add batch:", e);
        showToast(e.message || "Failed to add inventory batch.", "error");
    }
}

async function reserveBatchUnits(id, maxQty) {
    const input = prompt(`How many units would you like to reserve? (Max available: ${maxQty})`, "1");
    if (!input) return;
    const qty = parseInt(input, 10);
    if (isNaN(qty) || qty <= 0 || qty > maxQty) {
        showToast("Invalid quantity entered.", "warning");
        return;
    }

    try {
        await apiFetch(`/api/inventory/${id}/reserve`, {
            method: "PATCH",
            body: { quantity: qty }
        });
        showToast(`Successfully reserved ${qty} units.`, "success");
        await Promise.all([
            loadInventoryAlerts(),
            loadInventoryData()
        ]);
    } catch (e) {
        console.error("Failed to reserve units:", e);
        showToast(e.message || "Failed to reserve units.", "error");
    }
}

async function issueBatchUnits(id, maxQty) {
    const input = prompt(`How many units would you like to issue/transfuse? (Max: ${maxQty})`, "1");
    if (!input) return;
    const qty = parseInt(input, 10);
    if (isNaN(qty) || qty <= 0 || qty > maxQty) {
        showToast("Invalid quantity entered.", "warning");
        return;
    }

    try {
        await apiFetch(`/api/inventory/${id}/issue`, {
            method: "PATCH",
            body: { quantity: qty }
        });
        showToast(`Successfully issued ${qty} units.`, "success");
        await Promise.all([
            loadInventoryAlerts(),
            loadInventoryData()
        ]);
    } catch (e) {
        console.error("Failed to issue units:", e);
        showToast(e.message || "Failed to issue units.", "error");
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/[&<>"']/g, function (m) {
        return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m];
    });
}
