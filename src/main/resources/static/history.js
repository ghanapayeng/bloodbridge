// ============================================
// BLOODBRIDGE HISTORY & DONATION TRACKING
// ============================================

let currentUser = null;
let currentProfile = null;
let acceptedRequests = [];

document.addEventListener("DOMContentLoaded", async function () {
    currentUser = await getCurrentUser(true);
    if (!currentUser) return;

    // Set today's date in modal by default
    const dateInput = document.getElementById("donationDate");
    if (dateInput) {
        dateInput.value = new Date().toISOString().split("T")[0];
    }

    setupModal();
    await loadHistoryData();
});

async function loadHistoryData() {
    try {
        const [donations, myRequests, myInterests, profile] = await Promise.all([
            apiFetch("/api/donations/me", { redirectOnUnauthorized: false }).catch(() => []),
            apiFetch("/api/blood-requests/me", { redirectOnUnauthorized: false }).catch(() => []),
            apiFetch("/api/blood-requests/interests/me", { redirectOnUnauthorized: false }).catch(() => []),
            apiFetch("/api/donor-profiles/me", { redirectOnUnauthorized: false }).catch(() => null)
        ]);

        currentProfile = profile;
        acceptedRequests = (myInterests || []).filter(i => i.interestStatus === "ACCEPTED");

        // 1. Update Stats
        updateSummaryStats(donations || [], myRequests || [], myInterests || []);

        // 2. Render Donation Records
        renderDonationRecords(donations || []);

        // 3. Render Request Records
        renderRequestRecords(myRequests || [], myInterests || []);

        // 4. Update Modal Request Dropdown
        updateModalDropdown(myInterests || []);

    } catch (e) {
        console.error("Failed to load history data:", e);
    }
}

function updateSummaryStats(donations, myRequests, myInterests) {
    const totalDonations = donations.length;
    const requestsHelped = myInterests.filter(i => i.interestStatus === "ACCEPTED").length;

    // Calculate volume: default 1 unit = ~0.45L
    const totalUnits = donations.reduce((sum, d) => sum + (parseFloat(d.units) || 1.0), 0);
    const volumeLiters = (totalUnits * 0.45).toFixed(1);

    const statDonationsEl = document.getElementById("statDonationsCount");
    if (statDonationsEl) statDonationsEl.textContent = totalDonations;

    const statRequestsHelpedEl = document.getElementById("statRequestsHelped");
    if (statRequestsHelpedEl) statRequestsHelpedEl.textContent = requestsHelped || totalDonations;

    const statVolumeEl = document.getElementById("statBloodVolume");
    if (statVolumeEl) statVolumeEl.textContent = volumeLiters + " L";

    const impactBadge = document.getElementById("historyImpactBadge");
    if (impactBadge) {
        const lives = totalDonations * 3 || totalDonations || 0;
        impactBadge.textContent = `♥ ${lives} lives impacted`;
    }

    const donationCountLabel = document.getElementById("donationCountLabel");
    if (donationCountLabel) {
        donationCountLabel.textContent = `${totalDonations} record${totalDonations === 1 ? '' : 's'}`;
    }

    const totalReqCount = myRequests.length + myInterests.length;
    const requestCountLabel = document.getElementById("requestCountLabel");
    if (requestCountLabel) {
        requestCountLabel.textContent = `${totalReqCount} record${totalReqCount === 1 ? '' : 's'}`;
    }
}

function renderDonationRecords(donations) {
    const container = document.getElementById("donationHistoryList");
    if (!container) return;

    if (!donations || donations.length === 0) {
        container.innerHTML = `
            <div style="padding: 36px 20px; text-align: center; color: #64748b;">
                <div style="font-size: 28px; margin-bottom: 8px;">🩸</div>
                <h4 style="margin: 0 0 6px 0; color: #1e293b; font-size: 15px;">No donation records logged yet</h4>
                <p style="margin: 0 0 16px 0; font-size: 13px;">Donated blood recently? Record it here to keep your donor profile up to date.</p>
                <button onclick="openModal()" style="cursor: pointer; padding: 8px 16px; background: #dc2626; color: white; border: none; border-radius: 8px; font-weight: 600; font-size: 13px;">
                    + Log Your First Donation
                </button>
            </div>
        `;
        return;
    }

    container.innerHTML = donations.map(d => {
        const dateObj = new Date(d.donatedAt || d.createdAt);
        const day = isNaN(dateObj.getDate()) ? "—" : String(dateObj.getDate()).padStart(2, "0");
        const month = isNaN(dateObj.getMonth()) ? "" : dateObj.toLocaleString("en-US", { month: "short" }).toUpperCase();
        const hospitalName = d.hospital || "Hospital / Donation Centre";
        const cityName = d.city || (currentProfile ? currentProfile.city : "");
        const bg = d.bloodGroup ? formatBloodGroup(d.bloodGroup) : (currentProfile ? formatBloodGroup(currentProfile.bloodGroup) : "—");

        return `
            <div class="history-item">
                <div class="history-date">
                    <strong>${day}</strong>
                    <span>${month}</span>
                </div>

                <div class="history-details">
                    <h3>Blood donation completed</h3>
                    <p>${escapeHtml(hospitalName)}${cityName ? ' • ' + escapeHtml(cityName) : ''}</p>
                    <div style="display: flex; gap: 8px; align-items: center; margin-top: 4px;">
                        <span class="blood-type">${bg}</span>
                        <span style="font-size: 11px; color: #64748b; font-weight: 500;">${d.units || 1.0} unit</span>
                    </div>
                </div>

                <div class="history-status completed">
                    Completed
                </div>
            </div>
        `;
    }).join("");
}

function renderRequestRecords(myRequests, myInterests) {
    const container = document.getElementById("requestHistoryList");
    if (!container) return;

    const allRecords = [];

    (myRequests || []).forEach(r => {
        allRecords.push({
            type: "created",
            bloodGroup: formatBloodGroup(r.bloodGroup),
            hospital: r.hospital,
            city: r.city,
            date: r.createdAt,
            status: r.status,
            title: `${formatBloodGroup(r.bloodGroup)} blood request`
        });
    });

    (myInterests || []).forEach(i => {
        allRecords.push({
            type: "volunteered",
            bloodGroup: formatBloodGroup(i.bloodGroup),
            hospital: i.hospital,
            city: i.city,
            date: i.createdAt,
            status: i.interestStatus,
            title: `Volunteered for ${formatBloodGroup(i.bloodGroup)}`
        });
    });

    if (allRecords.length === 0) {
        container.innerHTML = `
            <div style="padding: 30px; text-align: center; color: #64748b; font-size: 13px;">
                No blood requests or volunteer activity recorded yet.
            </div>
        `;
        return;
    }

    allRecords.sort((a, b) => new Date(b.date) - new Date(a.date));

    container.innerHTML = allRecords.map(r => {
        const isFulfilled = r.status === "FULFILLED" || r.status === "ACCEPTED";
        const statusClass = isFulfilled ? "fulfilled" : "matched";
        const displayDate = formatDate(r.date);

        return `
            <div class="request-history">
                <div class="request-icon">
                    ${r.type === 'created' ? '+' : '✓'}
                </div>

                <div class="request-details">
                    <h3>${escapeHtml(r.title)}</h3>
                    <p>${escapeHtml(r.hospital)} • ${escapeHtml(r.city)}</p>
                    <span>${displayDate}</span>
                </div>

                <div class="request-status ${statusClass}">
                    ${r.status}
                </div>
            </div>
        `;
    }).join("");
}

function updateModalDropdown(interests) {
    const select = document.getElementById("donationRequest");
    if (!select) return;

    select.innerHTML = '<option value="">Walk-in / Direct hospital donation</option>';

    interests.forEach(i => {
        const opt = document.createElement("option");
        opt.value = i.requestId;
        opt.textContent = `Request #${i.requestId}: ${formatBloodGroup(i.bloodGroup)} at ${i.hospital} (${i.interestStatus})`;
        select.appendChild(opt);
    });
}

function setupModal() {
    const modal = document.getElementById("recordDonationModal");
    const openBtn = document.getElementById("openRecordDonationModal");
    const closeBtn = document.getElementById("closeModalBtn");
    const form = document.getElementById("recordDonationForm");

    if (openBtn) openBtn.addEventListener("click", openModal);
    if (closeBtn) closeBtn.addEventListener("click", closeModal);

    if (modal) {
        modal.addEventListener("click", function (e) {
            if (e.target === modal) closeModal();
        });
    }

    if (form) {
        form.addEventListener("submit", async function (e) {
            e.preventDefault();

            const date = document.getElementById("donationDate").value;
            const units = parseFloat(document.getElementById("donationUnits").value) || 1.0;
            const requestIdVal = document.getElementById("donationRequest").value;

            if (!date) {
                showToast("Please select a donation date.", "warning");
                return;
            }

            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) submitBtn.disabled = true;

            try {
                const payload = {
                    donatedAt: date,
                    units: units,
                    bloodRequestId: requestIdVal ? parseInt(requestIdVal, 10) : null
                };

                await apiFetch("/api/donations", {
                    method: "POST",
                    body: payload
                });

                showToast("Donation record successfully saved!", "success");
                closeModal();
                await loadHistoryData();

            } catch (err) {
                console.error("Record donation error:", err);
                showToast(err.message || "Unable to save donation. Please check details.", "error");
            } finally {
                if (submitBtn) submitBtn.disabled = false;
            }
        });
    }
}

function openModal() {
    const modal = document.getElementById("recordDonationModal");
    if (modal) modal.classList.add("active");
}

function closeModal() {
    const modal = document.getElementById("recordDonationModal");
    if (modal) modal.classList.remove("active");
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/[&<>"']/g, function (m) {
        return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m];
    });
}