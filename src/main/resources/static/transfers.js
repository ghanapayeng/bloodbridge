// ============================================
// BLOODBRIDGE HOSPITAL TRANSFERS MANAGEMENT
// ============================================

let currentUser = null;

document.addEventListener("DOMContentLoaded", async function () {
    currentUser = await getCurrentUser(true);
    if (!currentUser) return;

    loadTransfers();
});

async function loadTransfers() {
    const container = document.getElementById("transfersContainer");
    if (!container) return;

    const statusFilter = document.getElementById("tfStatusFilter").value;
    const params = new URLSearchParams();
    if (statusFilter) params.append("status", statusFilter);

    try {
        const transfers = await apiFetch("/api/transfers?" + params.toString(), { redirectOnUnauthorized: false }) || [];

        // Count stats
        const pending = transfers.filter(t => t.status === "REQUESTED").length;
        const inTransit = transfers.filter(t => t.status === "DISPATCHED").length;
        const delivered = transfers.filter(t => t.status === "DELIVERED").length;

        document.getElementById("tfPendingCount").textContent = pending;
        document.getElementById("tfInTransitCount").textContent = inTransit;
        document.getElementById("tfDeliveredCount").textContent = delivered;

        if (transfers.length === 0) {
            container.innerHTML = `
                <div style="background: white; border: 1px dashed #cbd5e1; border-radius: 14px; padding: 40px; text-align: center; color: #64748b;">
                    No blood transfers found matching this filter.
                </div>
            `;
            return;
        }

        container.innerHTML = transfers.map(t => {
            const bgDisplay = formatBloodGroup(t.bloodGroup);
            let statusBadge = "";
            let actionButtons = "";

            if (t.status === "REQUESTED") {
                statusBadge = `<span class="badge-tf-status tf-requested">Requested</span>`;
                actionButtons = `
                    <button class="btn-tf btn-tf-approve" onclick="changeTransferStatus(${t.id}, 'APPROVED', 'Approve transfer and reserve ${t.units} units from inventory?')">
                        Approve & Reserve
                    </button>
                    <button class="btn-tf btn-tf-reject" onclick="changeTransferStatus(${t.id}, 'REJECTED', 'Reject this transfer request?')">
                        Reject
                    </button>
                `;
            } else if (t.status === "APPROVED") {
                statusBadge = `<span class="badge-tf-status tf-approved">Approved (Stock Reserved)</span>`;
                actionButtons = `
                    <button class="btn-tf btn-tf-dispatch" onclick="changeTransferStatus(${t.id}, 'DISPATCHED', 'Confirm cold-chain courier dispatch?')">
                        Dispatch Units 🚚
                    </button>
                `;
            } else if (t.status === "DISPATCHED") {
                statusBadge = `<span class="badge-tf-status tf-dispatched">In Transit / Dispatched</span>`;
                actionButtons = `
                    <button class="btn-tf btn-tf-deliver" onclick="changeTransferStatus(${t.id}, 'RECEIVED', 'Confirm units safely received and added to inventory?')">
                        Confirm Delivery ✓
                    </button>
                `;
            } else if (t.status === "RECEIVED" || t.status === "DELIVERED") {
                statusBadge = `<span class="badge-tf-status tf-received">Received ✓</span>`;
                actionButtons = `<span style="font-size: 12px; color: #059669; font-weight: 600;">Inventory Updated</span>`;
            } else {
                statusBadge = `<span class="badge-tf-status tf-rejected">Rejected ✕</span>`;
                actionButtons = `<span style="font-size: 12px; color: #94a3b8;">Closed</span>`;
            }

            return `
                <div class="transfer-card">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; flex-wrap: wrap; gap: 8px;">
                        <div class="route-display">
                            <span>🏥 ${escapeHtml(t.sourceHospital)}</span>
                            <span class="route-arrow">➔</span>
                            <span>🏥 ${escapeHtml(t.destinationHospital)}</span>
                        </div>
                        ${statusBadge}
                    </div>

                    <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
                        <div>
                            <span style="display: inline-block; background: #fee2e2; color: #dc2626; font-weight: 800; padding: 3px 10px; border-radius: 6px; font-size: 13px; margin-right: 8px;">
                                ${bgDisplay}
                            </span>
                            <strong style="color: #1e293b; font-size: 14px;">${t.units} Units</strong>
                            <span style="color: #64748b; font-size: 12px; margin-left: 8px;">Requested by: ${escapeHtml(t.requestedByName)}</span>
                            ${t.notes ? `<p style="margin: 6px 0 0; font-size: 12px; color: #475569; background: #f8fafc; padding: 6px 10px; border-radius: 6px; white-space: pre-line;">${escapeHtml(t.notes)}</p>` : ''}
                        </div>

                        <div class="action-btn-group">
                            ${actionButtons}
                        </div>
                    </div>
                </div>
            `;
        }).join("");

    } catch (e) {
        console.error("Failed to load transfers:", e);
        container.innerHTML = `<div style="text-align: center; color: #dc2626; padding: 20px;">Failed to load transfers: ${escapeHtml(e.message)}</div>`;
    }
}

function openRequestTransferModal() {
    document.getElementById("transferModal").style.display = "flex";
}

function closeRequestTransferModal() {
    document.getElementById("transferModal").style.display = "none";
}

async function submitTransferRequest(event) {
    event.preventDefault();

    const dest = document.getElementById("destHospitalInput").value.trim();
    const source = document.getElementById("sourceHospitalInput").value.trim();
    const bg = document.getElementById("tfBloodGroup").value;
    const units = parseInt(document.getElementById("tfUnits").value, 10);
    const notes = document.getElementById("tfNotes").value.trim();

    if (!dest || !source) {
        showToast("Please enter both source and destination hospitals.", "warning");
        return;
    }

    try {
        await apiFetch("/api/transfers", {
            method: "POST",
            body: {
                destinationHospital: dest,
                sourceHospital: source,
                bloodGroup: bg,
                units: units,
                notes: notes
            }
        });

        showToast("Transfer requested successfully!", "success");
        closeRequestTransferModal();
        loadTransfers();

    } catch (e) {
        console.error("Failed to request transfer:", e);
        showToast(e.message || "Failed to submit transfer request.", "error");
    }
}

async function changeTransferStatus(id, newStatus, confirmPrompt) {
    if (!confirm(confirmPrompt)) return;

    let notes = "";
    if (newStatus === "DISPATCHED") {
        notes = prompt("Enter cold-chain courier tracking details or temperature log:", "Courier: MedExpress #4812, Temp: 4°C");
    }

    try {
        await apiFetch(`/api/transfers/${id}/status`, {
            method: "PATCH",
            body: {
                status: newStatus,
                trackingNotes: notes || ""
            }
        });

        showToast(`Transfer updated to ${newStatus}.`, "success");
        loadTransfers();

    } catch (e) {
        console.error("Failed to update transfer status:", e);
        showToast(e.message || "Failed to update transfer.", "error");
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/[&<>"']/g, function (m) {
        return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m];
    });
}
