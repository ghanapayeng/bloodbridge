// ============================================
// BLOODBRIDGE INVENTORY & EXPIRY MANAGEMENT
// ============================================

let currentUser = null;

document.addEventListener("DOMContentLoaded", async function () {
    currentUser = await getCurrentUser(true);
    if (!currentUser) return;

    setupDateDefaults();
    await Promise.all([
        loadInventorySummary(),
        loadInventoryData()
    ]);
});

function setupDateDefaults() {
    const today = new Date();
    const todayStr = today.toISOString().split("T")[0];

    // Default PRBC expiry is 42 days from collection
    const expiry = new Date();
    expiry.setDate(today.getDate() + 42);
    const expiryStr = expiry.toISOString().split("T")[0];

    const collInput = document.getElementById("newBatchCollectionDate");
    const expInput = document.getElementById("newBatchExpiryDate");
    if (collInput) collInput.value = todayStr;
    if (expInput) expInput.value = expiryStr;
}

async function loadInventorySummary() {
    try {
        const summary = await apiFetch("/api/v1/inventory/summary", { redirectOnUnauthorized: false });
        if (!summary) return;

        // Stat cards
        const elAvail = document.getElementById("totalAvailableCount");
        const elRes = document.getElementById("totalReservedCount");
        const elExp = document.getElementById("expiringSoonCount");
        const elCrit = document.getElementById("criticalShortageCount");
        const elLow = document.getElementById("lowStockCount");

        if (elAvail) elAvail.textContent = summary.totalAvailableUnits ?? 0;
        if (elRes) elRes.textContent = summary.totalReservedUnits ?? 0;
        if (elExp) elExp.textContent = summary.expiringUnitsCount ?? 0;
        if (elCrit) elCrit.textContent = summary.criticalShortageCount ?? 0;
        if (elLow) elLow.textContent = summary.lowStockCount ?? 0;

        // Render all 8 blood groups in grid
        const grid = document.getElementById("stockGroupsContainer");
        if (grid && summary.bloodGroups) {
            grid.innerHTML = summary.bloodGroups.map(item => {
                const bgDisplay = item.bloodGroupDisplay || formatBloodGroup(item.bloodGroup);
                const isCrit = item.isCriticalShortage;
                const isLow = item.isLowStock;

                let cardBg = "#f8fafc";
                let borderColor = "#e2e8f0";
                let badgeText = "Optimal";
                let badgeStyle = "background: #dcfce7; color: #15803d;";

                if (isCrit) {
                    cardBg = "#fff1f2";
                    borderColor = "#fecdd3";
                    badgeText = "Critical Deficit";
                    badgeStyle = "background: #fee2e2; color: #dc2626;";
                } else if (isLow) {
                    cardBg = "#fffbeb";
                    borderColor = "#fef3c7";
                    badgeText = "Low Stock";
                    badgeStyle = "background: #fef3c7; color: #b45309;";
                }

                // Component breakdown pills
                let compPills = "";
                if (item.componentBreakdown && Object.keys(item.componentBreakdown).length > 0) {
                    compPills = `<div style="display: flex; flex-wrap: wrap; gap: 4px; justify-content: center; margin-top: 6px;">` +
                        Object.entries(item.componentBreakdown).map(([code, qty]) => {
                            return `<span style="font-size: 9.5px; background: white; border: 1px solid #cbd5e1; padding: 1px 5px; border-radius: 4px; font-weight: 600; color: #475569;">${code}: ${qty}</span>`;
                        }).join("") + `</div>`;
                }

                return `
                    <div style="text-align: center; padding: 14px 10px; border-radius: 12px; background: ${cardBg}; border: 1.5px solid ${borderColor}; display: flex; flex-direction: column; justify-content: space-between;">
                        <div>
                            <div style="font-weight: 800; font-size: 17px; color: #0f172a;">${bgDisplay}</div>
                            <div style="font-size: 20px; font-weight: 800; margin: 4px 0; color: ${isCrit ? '#be123c' : (isLow ? '#d97706' : '#059669')};">
                                ${item.totalQuantity} <small style="font-size: 11px; font-weight: 600;">units</small>
                            </div>
                            <span style="font-size: 10px; font-weight: 800; text-transform: uppercase; padding: 2px 8px; border-radius: 9999px; ${badgeStyle}">
                                ${badgeText}
                            </span>
                        </div>
                        ${compPills}
                    </div>
                `;
            }).join("");
        }

        // Render components breakdown
        const compGrid = document.getElementById("componentsContainer");
        if (compGrid && summary.components) {
            compGrid.innerHTML = summary.components.map(c => {
                let badgeColor = "#2563eb";
                if (c.componentCode === "PLT") badgeColor = "#d97706"; // Platelets have 5-day shelf life
                if (c.componentCode === "CRYO") badgeColor = "#7c3aed";

                return `
                    <div class="comp-card">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span class="badge-comp" style="background: #eff6ff; color: ${badgeColor}; border-color: #bfdbfe;">${c.componentCode}</span>
                            <small style="font-size: 11px; color: #64748b; font-weight: 600;">Shelf Life: ${c.shelfLifeDays} days</small>
                        </div>
                        <strong style="font-size: 14px; color: #1e293b; margin-top: 4px;">${c.componentName}</strong>
                        <div style="font-size: 22px; font-weight: 800; color: #0f172a; margin-top: 2px;">
                            ${c.totalQuantity} <small style="font-size: 12px; font-weight: 600; color: #64748b;">units available</small>
                        </div>
                    </div>
                `;
            }).join("");
        }

    } catch (e) {
        console.warn("Failed to load inventory summary:", e);
    }
}

async function loadInventoryData() {
    const tableBody = document.getElementById("inventoryTableBody");
    if (!tableBody) return;

    const bgVal = document.getElementById("filterBloodGroup")?.value || "";
    const compVal = document.getElementById("filterComponent")?.value || "";
    const statusVal = document.getElementById("filterStatus")?.value || "";

    const params = new URLSearchParams();
    if (bgVal) params.append("bloodGroup", bgVal);
    if (compVal) params.append("component", compVal);
    if (statusVal) params.append("status", statusVal);

    try {
        const items = await apiFetch("/api/v1/inventory?" + params.toString(), { redirectOnUnauthorized: false }) || [];
        if (items.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="9" style="text-align: center; color: #64748b; padding: 36px;">
                        No blood batches found for the selected filter criteria.
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

            const compName = item.component || "WHOLE_BLOOD";
            const rowClass = item.expiringSoon ? "expiring-alert-row" : "";
            const expiryNotice = item.expiringSoon
                ? `<br><small style="color: #dc2626; font-weight: 700;">⚠️ Expiring in ${item.daysUntilExpiry} days</small>`
                : `<br><small style="color: #64748b;">${item.daysUntilExpiry} days remaining</small>`;

            const demoBadge = item.isDemo
                ? `<span class="badge-demo-item" title="Demonstration batch directly in MySQL database">Demo</span>`
                : '';

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
                    <td><code>${escapeHtml(item.batchUnitId)}</code>${demoBadge}</td>
                    <td><strong>${bgDisplay}</strong></td>
                    <td><span class="badge-comp">${escapeHtml(compName)}</span></td>
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
        tableBody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: #dc2626; padding: 20px;">Failed to load inventory: ${escapeHtml(e.message)}</td></tr>`;
    }
}

function openAddBatchModal() {
    const modal = document.getElementById("addBatchModal");
    if (modal) modal.style.display = "flex";
}

function closeAddBatchModal() {
    const modal = document.getElementById("addBatchModal");
    if (modal) modal.style.display = "none";
}

async function submitNewBatch(event) {
    event.preventDefault();

    const bgVal = document.getElementById("newBatchBg").value;
    const compVal = document.getElementById("newBatchComp").value;
    const qtyVal = parseInt(document.getElementById("newBatchQty").value, 10);
    const facilityVal = document.getElementById("newBatchFacility").value.trim();
    const collVal = document.getElementById("newBatchCollectionDate").value;
    const expVal = document.getElementById("newBatchExpiryDate").value;

    if (!facilityVal) {
        showToast("Please enter a facility name.", "warning");
        return;
    }

    const submitBtn = event.target ? event.target.querySelector('button[type="submit"]') : null;
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = "Saving Batch...";
    }

    try {
        await apiFetch("/api/v1/inventory", {
            method: "POST",
            body: {
                bloodGroup: bgVal,
                component: compVal,
                quantity: qtyVal,
                facilityName: facilityVal,
                collectionDate: collVal,
                expiryDate: expVal,
                isDemo: true
            }
        });

        showToast("Blood batch added to inventory successfully!", "success");
        closeAddBatchModal();
        await Promise.all([
            loadInventorySummary(),
            loadInventoryData()
        ]);

    } catch (e) {
        console.error("Failed to add batch:", e);
        showToast(e.message || "Failed to add inventory batch.", "error");
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = "Save Batch";
        }
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
        await apiFetch(`/api/v1/inventory/${id}/reserve`, {
            method: "PATCH",
            body: { quantity: qty }
        });
        showToast(`Successfully reserved ${qty} units.`, "success");
        await Promise.all([
            loadInventorySummary(),
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
        await apiFetch(`/api/v1/inventory/${id}/issue`, {
            method: "PATCH",
            body: { quantity: qty }
        });
        showToast(`Successfully issued ${qty} units.`, "success");
        await Promise.all([
            loadInventorySummary(),
            loadInventoryData()
        ]);
    } catch (e) {
        console.error("Failed to issue units:", e);
        showToast(e.message || "Failed to issue units.", "error");
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/[&<>"']/g, function (m) {
        return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m];
    });
}
