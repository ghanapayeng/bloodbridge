// ============================================
// BLOODBRIDGE DASHBOARD (LIVE API INTEGRATION)
// ============================================

let currentUser = null;
let currentProfile = null;
let myRespondedRequestIds = new Set();

document.addEventListener("DOMContentLoaded", async function () {
    // 1. Authenticate user
    currentUser = await getCurrentUser(true);
    if (!currentUser) return;

    updateIdentity(currentUser);
    updateGreeting(currentUser);
    setupProfileDropdown();

    // 2. Load donor profile
    await loadDonorProfile();

    // 3. Initialize communication listeners (Chat & Voice Calls)
    setupChatListeners();
    startCallSignalListener();

    // 4. Load stats, requests, emergencies, notifications, and activity
    await Promise.all([
        loadDashboardStats(),
        loadEmergencyBanner(),
        loadNotificationsBadge(),
        loadOpenRequests(),
        loadMyRequests(),
        loadRecentActivity()
    ]);
});

function updateIdentity(user) {
    const name = user.fullName || "Your account";
    ["dashboardName", "topbarName", "welcomeName", "menuUserName"].forEach(function (id) {
        const el = document.getElementById(id);
        if (el) el.textContent = name;
    });

    const emailEl = document.getElementById("menuUserEmail");
    if (emailEl) emailEl.textContent = user.email || "";

    const initials = name
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map(p => p.charAt(0).toUpperCase())
        .join("") || "?";

    ["dashboardAvatar", "dashboardProfileAvatar"].forEach(function (id) {
        const el = document.getElementById(id);
        if (el) el.textContent = initials;
    });

    const isHospitalOrAdmin = user.role === "ROLE_ADMIN" || user.role === "ROLE_HOSPITAL" || user.role === "ADMIN" || user.role === "HOSPITAL";
    const flaggedLink = document.getElementById("sidebarFlaggedLink");
    if (flaggedLink) {
        flaggedLink.style.display = isHospitalOrAdmin ? "flex" : "none";
        if (isHospitalOrAdmin) {
            loadFlaggedBadge();
        }
    }
}

function updateGreeting(user) {
    const welcomeEl = document.querySelector(".welcome h1");
    if (!welcomeEl) return;
    const hour = new Date().getHours();
    let timeGreeting = "Good morning";
    if (hour >= 12 && hour < 17) {
        timeGreeting = "Good afternoon";
    } else if (hour >= 17) {
        timeGreeting = "Good evening";
    }
    const firstName = (user.fullName || "there").split(" ")[0];
    welcomeEl.innerHTML = `${timeGreeting}, <span id="welcomeName">${escapeHtml(firstName)}</span>.`;

    const dateEl = document.querySelector(".welcome .date");
    if (dateEl) {
        dateEl.textContent = new Date().toLocaleDateString("en-US", {
            weekday: "long",
            month: "long",
            day: "numeric",
            year: "numeric"
        }).toUpperCase();
    }
}

async function loadDonorProfile() {
    try {
        currentProfile = await apiFetch("/api/donor-profiles/me", { redirectOnUnauthorized: false });
        if (currentProfile) {
            const bloodGroupEl = document.getElementById("dashboardBloodGroup");
            if (bloodGroupEl) {
                bloodGroupEl.textContent = formatBloodGroup(currentProfile.bloodGroup) || "—";
            }
            const menuBgEl = document.getElementById("menuUserBloodGroup");
            if (menuBgEl) {
                menuBgEl.textContent = formatBloodGroup(currentProfile.bloodGroup) || "Donor";
            }

            const availabilityText = document.getElementById("availabilityText");
            if (availabilityText) {
                availabilityText.textContent = currentProfile.available ? "Available to donate" : "Currently unavailable";
            }

            const availabilityBtn = document.querySelector("button.availability");
            if (availabilityBtn) {
                const dot = availabilityBtn.querySelector(".status-dot");
                if (dot) {
                    dot.style.background = currentProfile.available ? "#10b981" : "#94a3b8";
                }
            }

            updateProfileCompletion(currentProfile);
        }
    } catch (e) {
        console.log("No donor profile completed yet.");
        const bloodGroupEl = document.getElementById("dashboardBloodGroup");
        if (bloodGroupEl) bloodGroupEl.textContent = "—";
        const menuBgEl = document.getElementById("menuUserBloodGroup");
        if (menuBgEl) menuBgEl.textContent = "—";
        updateProfileCompletion(null);
    }
}

function updateProfileCompletion(profile) {
    let completed = 0;
    const total = 5;

    if (currentUser && currentUser.fullName) completed++;
    if (currentUser && currentUser.email) completed++;

    if (profile) {
        if (profile.phone && profile.phone.trim()) completed++;
        if (profile.city && profile.city.trim()) completed++;
        if (profile.bloodGroup) completed++;
    }

    const percentage = Math.round((completed / total) * 100);

    const percentageEl = document.getElementById("completionPercentage");
    if (percentageEl) percentageEl.textContent = percentage + "%";

    const progressBar = document.getElementById("profileProgress");
    if (progressBar) progressBar.style.width = percentage + "%";

    const msgEl = document.getElementById("completionMessage");
    if (msgEl) {
        if (percentage === 100) {
            msgEl.textContent = "Your profile is 100% complete and active for donor matching.";
        } else {
            msgEl.textContent = "Complete your profile to get matched with urgent blood requests.";
        }
    }

    const completeBtn = document.querySelector(".complete-btn");
    if (completeBtn) {
        completeBtn.textContent = percentage === 100 ? "View Profile" : "Complete Profile";
    }
}

async function toggleAvailability() {
    if (!currentProfile) {
        showToast("Please complete your donor profile first.", "info");
        window.location.href = "donor.html";
        return;
    }

    try {
        const newStatus = !currentProfile.available;
        const updated = await apiFetch("/api/donor-profiles/me", {
            method: "PUT",
            body: {
                bloodGroup: currentProfile.bloodGroup,
                phone: currentProfile.phone,
                city: currentProfile.city,
                available: newStatus,
                lastDonationDate: currentProfile.lastDonationDate
            }
        });

        currentProfile = updated;
        const availabilityText = document.getElementById("availabilityText");
        if (availabilityText) {
            availabilityText.textContent = updated.available ? "Available to donate" : "Currently unavailable";
        }
        const dot = document.querySelector("button.availability .status-dot");
        if (dot) {
            dot.style.background = updated.available ? "#10b981" : "#94a3b8";
        }

        showToast(updated.available ? "You are now marked Available to donate!" : "You are now marked Unavailable.", "success");

    } catch (e) {
        console.error("Failed to toggle availability:", e);
        showToast("Unable to change availability. Please try again.", "error");
    }
}

async function loadDashboardStats() {
    try {
        const donations = await apiFetch("/api/donations/me", { redirectOnUnauthorized: false }) || [];
        const donationsCountEl = document.getElementById("dashboardDonationsCount");
        if (donationsCountEl) donationsCountEl.textContent = donations.length;

        const lastDonationDateEl = document.getElementById("dashboardLastDonationDate");
        if (lastDonationDateEl) {
            if (donations.length > 0 && donations[0].donatedAt) {
                lastDonationDateEl.textContent = formatDate(donations[0].donatedAt);
            } else if (currentProfile && currentProfile.lastDonationDate) {
                lastDonationDateEl.textContent = formatDate(currentProfile.lastDonationDate);
            } else {
                lastDonationDateEl.textContent = "None yet";
            }
        }

        const impactCountEl = document.getElementById("dashboardImpactCount");
        if (impactCountEl) {
            impactCountEl.textContent = donations.length;
        }

        const impactMsgEl = document.getElementById("dashboardImpactMessage");
        if (impactMsgEl) {
            if (donations.length > 0) {
                impactMsgEl.innerHTML = `That's <strong>${donations.length} lives</strong> directly helped through your donations. ❤️`;
            } else {
                impactMsgEl.textContent = "Every donation can save up to 3 lives. ❤️";
            }
        }
    } catch (e) {
        console.warn("Error loading dashboard stats:", e);
    }
}

async function loadEmergencyBanner() {
    const container = document.getElementById("emergencyBannerContainer");
    if (!container) return;

    try {
        const emergencies = await apiFetch("/api/blood-requests/emergencies", { redirectOnUnauthorized: false }) || [];
        if (!emergencies || emergencies.length === 0) {
            container.innerHTML = "";
            return;
        }

        container.innerHTML = emergencies.slice(0, 2).map(req => {
            const bgDisplay = formatBloodGroup(req.bloodGroup);
            const unitsNeeded = req.unitsNeeded || 1;
            const unitsFulfilled = req.unitsFulfilled || 0;
            const isCritical = (req.urgency || "").toUpperCase() === "CRITICAL";
            const urgencyBadge = isCritical ? "🚨 CRITICAL EMERGENCY" : "⚠️ URGENT BLOOD NEED";

            return `
                <div class="emergency-alert-card">
                    <div class="emergency-content">
                        <div class="emergency-blood-badge">${bgDisplay}</div>
                        <div class="emergency-details">
                            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px; flex-wrap: wrap;">
                                <span class="emergency-badge-pulse"><span class="dot"></span>${urgencyBadge}</span>
                                <span style="font-size: 12px; color: #991b1b; font-weight: 700;">${unitsFulfilled}/${unitsNeeded} Units Fulfilled</span>
                                ${req.stage ? `<span style="font-size: 11px; background: #fff; padding: 2px 7px; border-radius: 4px; border: 1px solid #fca5a5; color: #b91c1c; font-weight: 600;">Stage: ${escapeHtml(req.stage)}</span>` : ''}
                            </div>
                            <h4>${escapeHtml(req.hospital)} in ${escapeHtml(req.city)}</h4>
                            <p>${req.additionalMessage ? escapeHtml(req.additionalMessage) : 'Urgent blood units needed immediately.'} • Posted ${formatTimeAgo(req.createdAt)}</p>
                        </div>
                    </div>
                    <button class="emergency-action-btn" onclick="registerInterest(${req.id}, '${bgDisplay}')">
                        Volunteer Now ❤️
                    </button>
                </div>
            `;
        }).join("");
    } catch (e) {
        console.warn("Failed to load emergency requests:", e);
        container.innerHTML = "";
    }
}

async function loadOpenRequests() {
    const container = document.getElementById("openRequestsContainer");
    if (!container) return;

    try {
        // Fetch open requests and my expressed interests in parallel
        const [requests, myInterests] = await Promise.all([
            apiFetch("/api/blood-requests", { redirectOnUnauthorized: false }) || [],
            apiFetch("/api/blood-requests/interests/me", { redirectOnUnauthorized: false }).catch(() => [])
        ]);

        myRespondedRequestIds = new Set((myInterests || []).map(i => i.requestId));

        if (!requests || requests.length === 0) {
            container.innerHTML = `
                <div style="padding: 30px; text-align: center; color: #64748b;">
                    <span style="font-size: 24px;">✓</span>
                    <p style="margin-top: 6px;">No urgent blood requests open right now.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = requests.slice(0, 5).map(req => {
            const isMine = currentUser && req.city && currentUser.id === req.requesterId; // or requester check
            const alreadyResponded = myRespondedRequestIds.has(req.id);
            const urgencyClass = (req.urgency || "MEDIUM").toLowerCase();
            const bgDisplay = formatBloodGroup(req.bloodGroup);
            const unitsInfo = req.unitsNeeded ? ` • ${req.unitsFulfilled || 0}/${req.unitsNeeded} units` : '';

            let actionBtnHtml = "";
            if (alreadyResponded) {
                actionBtnHtml = `
                    <div style="display: flex; align-items: center; gap: 6px; flex-wrap: wrap;">
                        <span style="display:inline-block; padding: 5px 9px; background: #ecfdf5; color: #059669; font-size: 11px; font-weight: 600; border-radius: 6px; border: 1px solid #a7f3d0;">Volunteered ✓</span>
                        <button onclick="openChat(${req.id}, ${req.requesterId}, '${escapeHtml(req.requesterName || 'Requester')}', '${escapeHtml(req.hospital)}')" style="cursor:pointer; padding: 5px 9px; background: #2563eb; color: white; border: none; border-radius: 6px; font-weight: 600; font-size: 11px; display: inline-flex; align-items: center; gap: 4px; transition: all 0.2s;">💬 Chat</button>
                        <button onclick="startEmergencyCall(${req.id}, ${req.requesterId}, '${escapeHtml(req.requesterName || 'Requester')}')" style="cursor:pointer; padding: 5px 9px; background: #dc2626; color: white; border: none; border-radius: 6px; font-weight: 600; font-size: 11px; display: inline-flex; align-items: center; gap: 4px; transition: all 0.2s;">📞 Call</button>
                    </div>
                `;
            } else if (currentUser && currentUser.id === req.requesterId) {
                actionBtnHtml = `<span style="display:inline-block; padding: 5px 9px; background: #eff6ff; color: #1d4ed8; font-size: 11px; font-weight: 600; border-radius: 6px;">Your Request</span>`;
            } else {
                actionBtnHtml = `<button onclick="registerInterest(${req.id}, '${bgDisplay}')" style="cursor:pointer; padding: 7px 16px; background: #dc2626; color: white; border: none; border-radius: 6px; font-weight: 600; font-size: 13px; transition: all 0.2s;">Help</button>`;
            }

            const riskBadge = req.riskLevel && req.riskLevel !== 'NORMAL'
                ? `<span class="badge-risk ${req.riskLevel === 'HIGH_RISK' ? 'badge-risk-high' : 'badge-risk-review'}">${req.riskLevel === 'HIGH_RISK' ? '🚨 High Risk' : '⚠️ Review'}</span>`
                : '';

            return `
                <div class="request" style="display: flex; align-items: center; justify-content: space-between; padding: 14px 0; border-bottom: 1px solid #f1f5f9; gap: 12px; flex-wrap: wrap;">
                    <div style="display: flex; align-items: center; gap: 14px; min-width: 250px;">
                        <div class="blood-circle" style="width: 44px; height: 44px; border-radius: 50%; background: #fee2e2; color: #dc2626; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 15px;">
                            ${bgDisplay}
                        </div>
                        <div class="request-info">
                            <div style="display: flex; align-items: center; gap: 6px;">
                                <strong style="color: #1e293b; font-size: 14px;">
                                    ${escapeHtml(req.hospital)}
                                </strong>
                                ${riskBadge}
                            </div>
                            <span style="color: #64748b; font-size: 12px;">
                                ${escapeHtml(req.city)}${unitsInfo} • ${formatTimeAgo(req.createdAt)}
                            </span>
                            ${req.additionalMessage ? `<p style="margin: 4px 0 0 0; font-size: 11px; color: #64748b; max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(req.additionalMessage)}</p>` : ''}
                        </div>
                    </div>
                    <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                        <button type="button" class="btn-action-sm" onclick="openTimelineModal(${req.id}, '${escapeHtml(req.hospital)}', '${bgDisplay}')" title="View request progress timeline">
                            Lifecycle ⏱
                        </button>
                        <div class="urgency ${urgencyClass}">
                            ${req.urgency}
                        </div>
                        ${actionBtnHtml}
                    </div>
                </div>
            `;
        }).join("");

    } catch (e) {
        console.error("Failed to load open requests:", e);
        container.innerHTML = `
            <div style="padding: 20px; text-align: center; color: #94a3b8;">
                Unable to load live blood requests right now.
            </div>
        `;
    }
}

async function registerInterest(requestId, neededBloodGroup) {
    if (!currentProfile) {
        showToast("Please complete your donor profile first.", "warning");
        setTimeout(() => window.location.href = "donor.html", 1000);
        return;
    }

    if (!currentProfile.available) {
        const switchOk = confirm("You are currently marked unavailable. Would you like to switch to Available and respond to this request?");
        if (switchOk) {
            await toggleAvailability();
        } else {
            return;
        }
    }

    try {
        await apiFetch(`/api/blood-requests/${requestId}/interests`, {
            method: "POST"
        });

        showToast("Thank you! Your interest has been sent to the requester.", "success");
        myRespondedRequestIds.add(requestId);

        await Promise.all([
            loadOpenRequests(),
            loadRecentActivity()
        ]);

    } catch (e) {
        console.error("Register interest error:", e);
        showToast(e.message || "Unable to register interest.", "error");
    }
}

async function loadMyRequests() {
    const container = document.getElementById("myRequestsContainer");
    if (!container) return;

    try {
        const myRequests = await apiFetch("/api/blood-requests/me", { redirectOnUnauthorized: false }) || [];

        if (myRequests.length === 0) {
            container.innerHTML = `
                <div style="padding: 24px; text-align: center; color: #64748b;">
                    <p style="margin-bottom: 12px; font-size: 14px;">You haven't published any blood requests yet.</p>
                    <a href="request.html" style="color: #dc2626; font-weight: 600; text-decoration: none;">Create a blood request if you or someone you know needs blood →</a>
                </div>
            `;
            return;
        }

        // For each request, fetch its responding donors
        const requestCardsHtml = await Promise.all(myRequests.map(async req => {
            let interests = [];
            try {
                interests = await apiFetch(`/api/blood-requests/${req.id}/interests`, { redirectOnUnauthorized: false }) || [];
            } catch (err) {
                interests = [];
            }

            const bgDisplay = formatBloodGroup(req.bloodGroup);
            const statusColor = req.status === "OPEN" ? "#0284c7" : req.status === "FULFILLED" ? "#16a34a" : "#64748b";

            let interestsHtml = "";
            if (interests.length === 0) {
                interestsHtml = `<div style="padding: 10px 14px; background: #f8fafc; border-radius: 8px; font-size: 12px; color: #64748b;">Waiting for compatible donors to respond...</div>`;
            } else {
                interestsHtml = interests.map(i => {
                    const donorInitial = (i.donorName || "D").charAt(0).toUpperCase();
                    const iStatus = i.status || "PENDING";
                    const isAccepted = iStatus === "ACCEPTED";
                    const isDeclined = iStatus === "DECLINED";

                    return `
                        <div style="display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; background: #f8fafc; border-radius: 8px; margin-top: 8px; gap: 10px; border: 1px solid #e2e8f0;">
                            <div style="display: flex; align-items: center; gap: 10px;">
                                <div style="width: 32px; height: 32px; border-radius: 50%; background: #fee2e2; color: #dc2626; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px;">
                                    ${donorInitial}
                                </div>
                                <div>
                                    <strong style="font-size: 13px; color: #1e293b;">${escapeHtml(i.donorName)}</strong>
                                    <span style="font-size: 11px; color: #64748b; margin-left: 6px;">(${formatBloodGroup(i.bloodGroup)} • ${escapeHtml(i.city)})</span>
                                    <div style="font-size: 12px; margin-top: 2px;">
                                        📞 <a href="tel:${escapeHtml(i.donorPhone)}" style="color: #dc2626; text-decoration: none; font-weight: 500;">${escapeHtml(i.donorPhone)}</a>
                                        <span style="color: #cbd5e1; margin: 0 4px;">•</span>
                                        ✉ <a href="mailto:${escapeHtml(i.donorEmail)}" style="color: #0284c7; text-decoration: none;">${escapeHtml(i.donorEmail)}</a>
                                    </div>
                                </div>
                            </div>
                            <div style="display: flex; align-items: center; gap: 6px; flex-wrap: wrap;">
                                <button onclick="openChat(${req.id}, ${i.donorId}, '${escapeHtml(i.donorName)}', '${escapeHtml(req.hospital)}')" style="cursor:pointer; padding: 5px 9px; background: #2563eb; color: white; border: none; border-radius: 6px; font-size: 11px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px;">💬 Chat</button>
                                <button onclick="startEmergencyCall(${req.id}, ${i.donorId}, '${escapeHtml(i.donorName)}')" style="cursor:pointer; padding: 5px 9px; background: #dc2626; color: white; border: none; border-radius: 6px; font-size: 11px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px;">📞 Call</button>
                                ${!isAccepted && !isDeclined ? `
                                    <button onclick="updateInterest(${req.id}, ${i.id}, 'ACCEPTED')" style="cursor:pointer; padding: 5px 9px; background: #16a34a; color: white; border: none; border-radius: 6px; font-size: 11px; font-weight: 600;">Accept</button>
                                    <button onclick="updateInterest(${req.id}, ${i.id}, 'DECLINED')" style="cursor:pointer; padding: 5px 9px; background: #94a3b8; color: white; border: none; border-radius: 6px; font-size: 11px; font-weight: 600;">Decline</button>
                                ` : `
                                    <span style="padding: 4px 8px; border-radius: 6px; font-size: 11px; font-weight: 600; background: ${isAccepted ? '#dcfce7' : '#f1f5f9'}; color: ${isAccepted ? '#15803d' : '#64748b'};">
                                        ${iStatus}
                                    </span>
                                `}
                            </div>
                        </div>
                    `;
                }).join("");
            }

            return `
                <div style="border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px; margin-bottom: 16px; background: #ffffff;">
                    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; flex-wrap: wrap; gap: 8px;">
                        <div style="display: flex; align-items: center; gap: 10px;">
                            <span style="display: inline-block; padding: 4px 10px; background: #fee2e2; color: #dc2626; border-radius: 6px; font-weight: 700; font-size: 14px;">
                                ${bgDisplay}
                            </span>
                            <div>
                                <h3 style="margin: 0; font-size: 15px; color: #1e293b;">${escapeHtml(req.hospital)}</h3>
                                <span style="font-size: 12px; color: #64748b;">${escapeHtml(req.city)} • Published ${formatDate(req.createdAt)}</span>
                            </div>
                        </div>
                        <div style="display: flex; align-items: center; gap: 10px;">
                            <span style="padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 700; background: #f1f5f9; color: ${statusColor}; border: 1px solid #e2e8f0;">
                                ${req.status}
                            </span>
                            ${req.status === "OPEN" ? `
                                <button onclick="updateRequestStatus(${req.id}, 'FULFILLED')" style="cursor:pointer; padding: 6px 12px; background: #16a34a; color: white; border: none; border-radius: 6px; font-size: 12px; font-weight: 600;">
                                    Mark Fulfilled ✓
                                </button>
                                <button onclick="updateRequestStatus(${req.id}, 'CLOSED')" style="cursor:pointer; padding: 6px 12px; background: #64748b; color: white; border: none; border-radius: 6px; font-size: 12px;">
                                    Close
                                </button>
                            ` : ''}
                        </div>
                    </div>

                    ${req.additionalMessage ? `<p style="margin: 0 0 12px 0; font-size: 13px; color: #475569; background: #f8fafc; padding: 8px 12px; border-radius: 6px;">${escapeHtml(req.additionalMessage)}</p>` : ''}

                    <div style="margin-top: 12px;">
                        <div style="font-size: 12px; font-weight: 600; color: #475569; margin-bottom: 6px; display: flex; align-items: center; justify-content: space-between;">
                            <span>Donor Responses (${interests.length})</span>
                        </div>
                        ${interestsHtml}
                    </div>
                </div>
            `;
        }));

        container.innerHTML = requestCardsHtml.join("");

    } catch (e) {
        console.error("Failed to load my requests:", e);
        container.innerHTML = `<div style="padding: 20px; text-align: center; color: #94a3b8;">Unable to load your requests.</div>`;
    }
}

async function updateRequestStatus(requestId, newStatus) {
    try {
        await apiFetch(`/api/blood-requests/${requestId}/status`, {
            method: "PATCH",
            body: { status: newStatus }
        });
        showToast(`Request marked as ${newStatus}.`, "success");
        await loadMyRequests();
        await loadOpenRequests();
    } catch (e) {
        console.error("Failed to update status:", e);
        showToast(e.message || "Failed to update request status.", "error");
    }
}

async function updateInterest(requestId, interestId, newStatus) {
    try {
        await apiFetch(`/api/blood-requests/${requestId}/interests/${interestId}`, {
            method: "PATCH",
            body: { status: newStatus }
        });
        showToast(`Donor interest ${newStatus.toLowerCase()}.`, "success");
        await loadMyRequests();
    } catch (e) {
        console.error("Failed to update interest status:", e);
        showToast(e.message || "Failed to update response.", "error");
    }
}

async function loadRecentActivity() {
    const container = document.getElementById("activityListContainer");
    if (!container) return;

    try {
        const [donations, myRequests, myInterests] = await Promise.all([
            apiFetch("/api/donations/me", { redirectOnUnauthorized: false }).catch(() => []),
            apiFetch("/api/blood-requests/me", { redirectOnUnauthorized: false }).catch(() => []),
            apiFetch("/api/blood-requests/interests/me", { redirectOnUnauthorized: false }).catch(() => [])
        ]);

        const items = [];

        (donations || []).forEach(d => {
            items.push({
                type: "donation",
                title: "Blood donation recorded",
                desc: `You donated ${d.units || 1.0} unit of blood at ${d.hospital || 'Hospital'}`,
                date: d.donatedAt || d.createdAt,
                icon: "♥"
            });
        });

        (myRequests || []).forEach(r => {
            items.push({
                type: "request",
                title: `${formatBloodGroup(r.bloodGroup)} request published`,
                desc: `Needed at ${r.hospital}, ${r.city} (${r.status})`,
                date: r.createdAt,
                icon: "+"
            });
        });

        (myInterests || []).forEach(i => {
            items.push({
                type: "volunteer",
                title: `Volunteered for ${formatBloodGroup(i.bloodGroup)} request`,
                desc: `Offered help at ${i.hospital}, ${i.city} • Status: ${i.interestStatus}`,
                date: i.createdAt,
                icon: "✓"
            });
        });

        if (items.length === 0) {
            container.innerHTML = `
                <div style="padding: 24px; text-align: center; color: #94a3b8; font-size: 13px;">
                    No recent activity yet. When you donate or request blood, it will appear here.
                </div>
            `;
            return;
        }

        // Sort latest first
        items.sort((a, b) => new Date(b.date) - new Date(a.date));

        container.innerHTML = items.slice(0, 5).map(item => `
            <div class="activity" style="display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f1f5f9;">
                <div style="display: flex; align-items: center; gap: 12px;">
                    <div class="activity-icon ${item.type === 'donation' ? 'donation' : item.type === 'request' ? 'request' : 'profile-icon'}" style="width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700;">
                        ${item.icon}
                    </div>
                    <div>
                        <strong style="color: #1e293b; font-size: 13px; display: block;">${escapeHtml(item.title)}</strong>
                        <span style="color: #64748b; font-size: 12px;">${escapeHtml(item.desc)}</span>
                    </div>
                </div>
                <time style="color: #94a3b8; font-size: 12px; white-space: nowrap;">
                    ${formatDate(item.date)}
                </time>
            </div>
        `).join("");

    } catch (e) {
        console.warn("Failed to load recent activity:", e);
    }
}

// ============================================
// IN-APP NOTIFICATION CENTER
// ============================================

async function loadNotificationsBadge() {
    try {
        const res = await apiFetch("/api/notifications/unread-count", { redirectOnUnauthorized: false });
        const unreadCount = res ? (res.unreadCount || 0) : 0;
        const dot = document.querySelector(".notification-btn .notification-dot");
        if (dot) {
            dot.style.display = unreadCount > 0 ? "block" : "none";
        }
    } catch (e) {
        console.warn("Failed to load notifications unread count:", e);
    }
}

async function showNotifications() {
    const modal = document.getElementById("notificationModal");
    if (!modal) return;
    modal.style.display = "flex";

    const container = document.getElementById("notificationsListContainer");
    if (container) {
        container.innerHTML = `<div style="text-align: center; color: #94a3b8; padding: 30px;">Loading notifications...</div>`;
    }

    try {
        const res = await apiFetch("/api/notifications", { redirectOnUnauthorized: false });
        const unread = res ? res.unreadCount : 0;
        const list = res ? (res.notifications || []) : [];

        const unreadBadge = document.getElementById("notifModalUnreadBadge");
        if (unreadBadge) {
            unreadBadge.textContent = unread;
            unreadBadge.style.display = unread > 0 ? "inline-block" : "none";
        }

        const dot = document.querySelector(".notification-btn .notification-dot");
        if (dot) dot.style.display = unread > 0 ? "block" : "none";

        if (list.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; color: #64748b; padding: 36px;">
                    <span style="font-size: 28px;">✓</span>
                    <p style="margin-top: 8px; font-size: 14px;">All caught up! No notifications yet.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = list.map(n => {
            let icon = "🔔";
            if (n.type === "EMERGENCY_REQUEST") icon = "🚨";
            else if (n.type === "DONOR_MATCH") icon = "🩸";
            else if (n.type === "DONOR_RESPONSE") icon = "❤️";
            else if (n.type === "LOW_INVENTORY" || n.type === "EXPIRY_WARNING") icon = "📦";
            else if (n.type === "TRANSFER_REQUEST") icon = "⇄";

            const unreadStyle = !n.isRead ? "background: #f8fafc; border-left: 3px solid #dc2626;" : "background: #ffffff;";

            return `
                <div onclick="markNotificationRead(${n.id})" style="cursor: pointer; padding: 12px 14px; border-radius: 10px; border: 1px solid #e2e8f0; ${unreadStyle} transition: all 0.2s;">
                    <div style="display: flex; align-items: flex-start; gap: 10px;">
                        <span style="font-size: 20px;">${icon}</span>
                        <div style="flex: 1;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px;">
                                <strong style="font-size: 13px; color: #1e293b;">${escapeHtml(n.title)}</strong>
                                <span style="font-size: 11px; color: #94a3b8;">${escapeHtml(n.timeAgo)}</span>
                            </div>
                            <p style="margin: 0; font-size: 12px; color: #475569; line-height: 1.4;">${escapeHtml(n.message)}</p>
                            ${!n.isRead ? `<span style="display: inline-block; margin-top: 4px; font-size: 10px; font-weight: 700; color: #dc2626;">● Unread</span>` : ''}
                        </div>
                    </div>
                </div>
            `;
        }).join("");

    } catch (e) {
        console.error("Failed to load notifications list:", e);
        if (container) {
            container.innerHTML = `<div style="text-align: center; color: #dc2626; padding: 20px;">Failed to load notifications: ${escapeHtml(e.message)}</div>`;
        }
    }
}

function closeNotificationsModal() {
    const modal = document.getElementById("notificationModal");
    if (modal) modal.style.display = "none";
}

async function markNotificationRead(id) {
    try {
        await apiFetch(`/api/notifications/${id}/read`, { method: "PATCH" });
        loadNotificationsBadge();
        showNotifications();
    } catch (e) {
        console.warn("Failed to mark notification read:", e);
    }
}

async function markAllNotificationsRead() {
    try {
        await apiFetch("/api/notifications/read-all", { method: "PATCH" });
        loadNotificationsBadge();
        showNotifications();
    } catch (e) {
        console.warn("Failed to mark all notifications read:", e);
    }
}

// Logout handler
const logoutLink = document.getElementById("logoutLink");
if (logoutLink) {
    logoutLink.addEventListener("click", function (e) {
        e.preventDefault();
        logout();
    });
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/[&<>"']/g, function (m) {
        return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m];
    });
}

// Profile Dropdown Menu Handler
function setupProfileDropdown() {
    const btn = document.getElementById("userProfileBtn");
    const menu = document.getElementById("profileMenu");
    const logoutBtn = document.getElementById("menuLogoutBtn");

    if (!btn || !menu) return;

    function toggleMenu(e) {
        e.stopPropagation();
        const isOpen = menu.classList.contains("open");
        if (isOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    function openMenu() {
        menu.classList.add("open");
        btn.classList.add("active");
        btn.setAttribute("aria-expanded", "true");
    }

    function closeMenu() {
        menu.classList.remove("open");
        btn.classList.remove("active");
        btn.setAttribute("aria-expanded", "false");
    }

    btn.addEventListener("click", toggleMenu);

    document.addEventListener("click", function (e) {
        if (!menu.contains(e.target) && !btn.contains(e.target)) {
            closeMenu();
        }
    });

    document.addEventListener("keydown", function (e) {
        if (e.key === "Escape") {
            closeMenu();
        }
    });

    if (logoutBtn) {
        logoutBtn.addEventListener("click", function (e) {
            e.preventDefault();
            logout();
        });
    }
}

// ============================================
// IN-APP CHAT (MESSAGING) SYSTEM
// ============================================
let activeChat = null; // { requestId, otherUserId, otherUserName, requestTitle }
let chatPollTimer = null;

function setupChatListeners() {
    const chatForm = document.getElementById("chatInputForm");
    if (chatForm) {
        chatForm.addEventListener("submit", async function(e) {
            e.preventDefault();
            await sendChatMessage();
        });
    }

    const headerCallBtn = document.getElementById("chatHeaderCallBtn");
    if (headerCallBtn) {
        headerCallBtn.addEventListener("click", function() {
            if (activeChat) {
                startEmergencyCall(activeChat.requestId, activeChat.otherUserId, activeChat.otherUserName);
            }
        });
    }
}

async function openChat(requestId, otherUserId, otherUserName, requestTitle) {
    if (!currentUser) return;
    if (!otherUserId) {
        showToast("Recipient details not found for this request.", "warning");
        return;
    }

    activeChat = { requestId, otherUserId, otherUserName, requestTitle };

    // Update header
    const avatarEl = document.getElementById("chatRecipientAvatar");
    const nameEl = document.getElementById("chatRecipientName");
    const subEl = document.getElementById("chatRequestSub");

    if (avatarEl) {
        avatarEl.textContent = (otherUserName || "U").charAt(0).toUpperCase();
    }
    if (nameEl) {
        nameEl.textContent = otherUserName || "User";
    }
    if (subEl) {
        subEl.textContent = requestTitle ? `Coordination for ${requestTitle}` : "Blood Request Coordination";
    }

    const modal = document.getElementById("chatModal");
    if (modal) {
        modal.style.display = "flex";
    }

    const input = document.getElementById("chatInputText");
    if (input) {
        input.value = "";
        input.focus();
    }

    await loadChatMessages();

    // Start polling every 3 seconds while open
    clearInterval(chatPollTimer);
    chatPollTimer = setInterval(loadChatMessages, 3000);
}

function closeChat() {
    const modal = document.getElementById("chatModal");
    if (modal) {
        modal.style.display = "none";
    }
    clearInterval(chatPollTimer);
    chatPollTimer = null;
    activeChat = null;
}

async function loadChatMessages() {
    if (!activeChat || !currentUser) return;
    const container = document.getElementById("chatMessagesContainer");
    if (!container) return;

    try {
        const messages = await apiFetch(`/api/chat/requests/${activeChat.requestId}/conversations/${activeChat.otherUserId}`, {
            redirectOnUnauthorized: false
        }) || [];

        if (messages.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; color: #94a3b8; padding: 40px 20px;">
                    <div style="font-size: 32px; margin-bottom: 8px;">💬</div>
                    <strong style="color: #475569;">Start a conversation</strong>
                    <p style="margin: 4px 0 0; font-size: 13px;">Coordinate donation timing, blood units, and hospital arrival safely.</p>
                </div>
            `;
            return;
        }

        const isScrolledToBottom = container.scrollHeight - container.clientHeight <= container.scrollTop + 60;

        container.innerHTML = messages.map(msg => {
            const isMine = msg.senderId === currentUser.id;
            const timeStr = new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            return `
                <div class="chat-bubble ${isMine ? 'mine' : 'theirs'}">
                    <div class="chat-bubble-content">${escapeHtml(msg.content)}</div>
                    <span class="chat-time">${timeStr}</span>
                </div>
            `;
        }).join("");

        if (isScrolledToBottom || !container.getAttribute("data-loaded")) {
            container.scrollTop = container.scrollHeight;
            container.setAttribute("data-loaded", "true");
        }
    } catch (e) {
        console.warn("Failed to load chat messages:", e);
    }
}

async function sendChatMessage() {
    if (!activeChat) return;
    const input = document.getElementById("chatInputText");
    if (!input) return;
    const content = input.value.trim();
    if (!content) return;

    const btn = document.getElementById("chatSendBtn");
    if (btn) btn.disabled = true;

    try {
        await apiFetch(`/api/chat/requests/${activeChat.requestId}/conversations/${activeChat.otherUserId}`, {
            method: "POST",
            body: { content }
        });
        input.value = "";
        await loadChatMessages();
    } catch (e) {
        console.error("Failed to send message:", e);
        showToast(e.message || "Failed to send message.", "error");
    } finally {
        if (btn) btn.disabled = false;
        input.focus();
    }
}

// ============================================
// IN-APP EMERGENCY VOICE CALLING (WebRTC + Web Audio)
// ============================================
let peerConnection = null;
let localMediaStream = null;
let remoteAudioElement = null;
let audioContext = null;
let ringToneOscillator = null;
let ringToneGain = null;
let callTimerInterval = null;
let callSeconds = 0;
let callPollTimer = null;
let activeCall = null; // { requestId, targetUserId, targetUserName, isCaller, callState }
let pendingIncomingCall = null;

const rtcConfig = {
    iceServers: [
        { urls: "stun:stun.l.google.com:19302" },
        { urls: "stun:stun1.l.google.com:19302" }
    ]
};

// Web Audio synthesizer for phone ringtone (zero external audio files required)
function playRingTone(isIncoming) {
    stopRingTone();
    try {
        const AudioCtx = window.AudioContext || window.webkitAudioContext;
        if (!AudioCtx) return;
        audioContext = new AudioCtx();
        if (audioContext.state === "suspended") {
            audioContext.resume();
        }

        const osc = audioContext.createOscillator();
        const gain = audioContext.createGain();

        // Dual frequency tone simulation: 440Hz / 480Hz
        osc.type = "sine";
        osc.frequency.setValueAtTime(isIncoming ? 480 : 440, audioContext.currentTime);

        // Ring cadence: 1.2s on, 2.3s off repeating
        const now = audioContext.currentTime;
        gain.gain.setValueAtTime(0, now);
        for (let i = 0; i < 20; i++) {
            const start = now + (i * 3.5);
            gain.gain.setValueAtTime(0.2, start);
            gain.gain.setValueAtTime(0, start + 1.2);
        }

        osc.connect(gain);
        gain.connect(audioContext.destination);
        osc.start();

        ringToneOscillator = osc;
        ringToneGain = gain;
    } catch (e) {
        console.warn("Ring tone audio error:", e);
    }
}

function stopRingTone() {
    if (ringToneOscillator) {
        try {
            ringToneOscillator.stop();
            ringToneOscillator.disconnect();
        } catch (_) {}
        ringToneOscillator = null;
    }
    if (audioContext) {
        try { audioContext.close(); } catch (_) {}
        audioContext = null;
    }
}

function startCallTimer() {
    stopCallTimer();
    callSeconds = 0;
    const timerEl = document.getElementById("callTimer");
    if (timerEl) {
        timerEl.style.display = "block";
        timerEl.textContent = "00:00";
    }
    callTimerInterval = setInterval(() => {
        callSeconds++;
        const mins = String(Math.floor(callSeconds / 60)).padStart(2, "0");
        const secs = String(callSeconds % 60).padStart(2, "0");
        if (timerEl) timerEl.textContent = `${mins}:${secs}`;
    }, 1000);
}

function stopCallTimer() {
    clearInterval(callTimerInterval);
    callTimerInterval = null;
    const timerEl = document.getElementById("callTimer");
    if (timerEl) {
        timerEl.style.display = "none";
        timerEl.textContent = "00:00";
    }
}

async function startEmergencyCall(requestId, targetUserId, targetUserName) {
    if (!currentUser) return;
    if (!targetUserId) {
        showToast("Cannot place call: recipient not specified.", "warning");
        return;
    }

    activeCall = {
        requestId,
        targetUserId,
        targetUserName,
        isCaller: true,
        callState: "CALLING"
    };

    const modal = document.getElementById("callModal");
    const nameEl = document.getElementById("callUserName");
    const statusEl = document.getElementById("callStatusText");
    const incomingActions = document.getElementById("incomingCallActions");
    const activeActions = document.getElementById("activeCallActions");

    if (modal) modal.style.display = "flex";
    if (nameEl) nameEl.textContent = targetUserName || "Blood Bridge Contact";
    if (statusEl) statusEl.textContent = "Ringing emergency voice line...";
    if (incomingActions) incomingActions.style.display = "none";
    if (activeActions) activeActions.style.display = "flex";

    playRingTone(false);

    try {
        await initPeerConnection();

        const offer = await peerConnection.createOffer();
        await peerConnection.setLocalDescription(offer);

        await sendCallSignal({
            requestId,
            recipientId: targetUserId,
            signalType: "CALL_OFFER",
            signalData: JSON.stringify(offer)
        });
    } catch (e) {
        console.error("Failed to start emergency call:", e);
        showToast("Could not access microphone or initiate call: " + e.message, "error");
        hangupCall();
    }
}

async function acceptIncomingCall() {
    if (!pendingIncomingCall) return;
    stopRingTone();

    const incoming = pendingIncomingCall;
    pendingIncomingCall = null;

    activeCall = {
        requestId: incoming.requestId,
        targetUserId: incoming.callerId,
        targetUserName: incoming.callerName,
        isCaller: false,
        callState: "CONNECTING"
    };

    const statusEl = document.getElementById("callStatusText");
    const incomingActions = document.getElementById("incomingCallActions");
    const activeActions = document.getElementById("activeCallActions");

    if (statusEl) statusEl.textContent = "Connecting audio...";
    if (incomingActions) incomingActions.style.display = "none";
    if (activeActions) activeActions.style.display = "flex";

    try {
        await initPeerConnection();

        const offerDesc = new RTCSessionDescription(JSON.parse(incoming.sdpOffer));
        await peerConnection.setRemoteDescription(offerDesc);

        const answer = await peerConnection.createAnswer();
        await peerConnection.setLocalDescription(answer);

        await sendCallSignal({
            requestId: activeCall.requestId,
            recipientId: activeCall.targetUserId,
            signalType: "CALL_ANSWER",
            signalData: JSON.stringify(answer)
        });

        if (statusEl) statusEl.textContent = "Emergency Call Connected (Live Audio)";
        startCallTimer();
    } catch (e) {
        console.error("Failed to accept call:", e);
        showToast("Failed to establish voice call: " + e.message, "error");
        hangupCall();
    }
}

async function rejectIncomingCall() {
    stopRingTone();
    if (pendingIncomingCall) {
        const incoming = pendingIncomingCall;
        pendingIncomingCall = null;
        try {
            await sendCallSignal({
                requestId: incoming.requestId,
                recipientId: incoming.callerId,
                signalType: "CALL_REJECTED",
                signalData: ""
            });
        } catch (_) {}
    }
    const modal = document.getElementById("callModal");
    if (modal) modal.style.display = "none";
}

async function hangupCall() {
    stopRingTone();
    stopCallTimer();

    if (activeCall) {
        try {
            await sendCallSignal({
                requestId: activeCall.requestId,
                recipientId: activeCall.targetUserId,
                signalType: "CALL_ENDED",
                signalData: ""
            });
        } catch (_) {}
    }

    if (localMediaStream) {
        localMediaStream.getTracks().forEach(t => t.stop());
        localMediaStream = null;
    }

    if (peerConnection) {
        peerConnection.close();
        peerConnection = null;
    }

    activeCall = null;
    pendingIncomingCall = null;

    const modal = document.getElementById("callModal");
    if (modal) modal.style.display = "none";
}

let isAudioMuted = false;
function toggleMuteAudio() {
    if (!localMediaStream) return;
    const audioTrack = localMediaStream.getAudioTracks()[0];
    if (!audioTrack) return;

    isAudioMuted = !isAudioMuted;
    audioTrack.enabled = !isAudioMuted;

    const icon = document.getElementById("muteIcon");
    const text = document.getElementById("muteText");

    if (icon) icon.textContent = isAudioMuted ? "🔇" : "🎙️";
    if (text) text.textContent = isAudioMuted ? "Unmute" : "Mute";
    showToast(isAudioMuted ? "Microphone muted" : "Microphone active", "info");
}

async function initPeerConnection() {
    if (peerConnection) {
        peerConnection.close();
    }

    peerConnection = new RTCPeerConnection(rtcConfig);

    // Audio output element
    if (!remoteAudioElement) {
        remoteAudioElement = document.createElement("audio");
        remoteAudioElement.autoplay = true;
        document.body.appendChild(remoteAudioElement);
    }

    peerConnection.ontrack = (event) => {
        if (event.streams && event.streams[0]) {
            remoteAudioElement.srcObject = event.streams[0];
        }
    };

    peerConnection.onicecandidate = async (event) => {
        if (event.candidate && activeCall) {
            try {
                await sendCallSignal({
                    requestId: activeCall.requestId,
                    recipientId: activeCall.targetUserId,
                    signalType: "ICE_CANDIDATE",
                    signalData: JSON.stringify(event.candidate)
                });
            } catch (_) {}
        }
    };

    // Attempt to acquire real microphone stream, with silent audio fallback if mic unavailable
    try {
        localMediaStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
        localMediaStream.getTracks().forEach(track => {
            peerConnection.addTrack(track, localMediaStream);
        });
    } catch (micErr) {
        console.warn("Microphone not available, proceeding in simulated signaling mode:", micErr);
    }
}

async function sendCallSignal(payload) {
    if (payload && payload.recipientId && !payload.targetUserId) {
        payload.targetUserId = payload.recipientId;
    }
    return await apiFetch("/api/calls/signal", {
        method: "POST",
        body: payload
    });
}

function startCallSignalListener() {
    clearInterval(callPollTimer);
    callPollTimer = setInterval(pollCallSignals, 2500);
}

async function pollCallSignals() {
    if (!currentUser) return;
    try {
        const signals = await apiFetch("/api/calls/poll", { redirectOnUnauthorized: false });
        if (!signals || !Array.isArray(signals) || signals.length === 0) return;

        for (const sig of signals) {
            await handleIncomingSignal(sig);
        }
    } catch (e) {
        // Silently ignore polling hiccups
    }
}

async function handleIncomingSignal(sig) {
    switch (sig.signalType) {
        case "CALL_OFFER":
            if (activeCall) {
                // Already in a call, busy/reject
                await sendCallSignal({
                    requestId: sig.requestId,
                    recipientId: sig.callerId,
                    signalType: "CALL_REJECTED",
                    signalData: "BUSY"
                });
                return;
            }

            pendingIncomingCall = {
                requestId: sig.requestId,
                callerId: sig.callerId,
                callerName: sig.callerName,
                sdpOffer: sig.signalData
            };

            const modal = document.getElementById("callModal");
            const nameEl = document.getElementById("callUserName");
            const statusEl = document.getElementById("callStatusText");
            const incomingActions = document.getElementById("incomingCallActions");
            const activeActions = document.getElementById("activeCallActions");

            if (modal) modal.style.display = "flex";
            if (nameEl) nameEl.textContent = sig.callerName || "Blood Request Caller";
            if (statusEl) statusEl.textContent = "Urgent Incoming Blood Request Call 🚨";
            if (incomingActions) incomingActions.style.display = "flex";
            if (activeActions) activeActions.style.display = "none";

            playRingTone(true);
            break;

        case "CALL_ANSWER":
            if (activeCall && activeCall.isCaller && peerConnection) {
                stopRingTone();
                const answerDesc = new RTCSessionDescription(JSON.parse(sig.signalData));
                await peerConnection.setRemoteDescription(answerDesc);
                const statusEl2 = document.getElementById("callStatusText");
                if (statusEl2) statusEl2.textContent = "Emergency Call Connected (Live Audio)";
                startCallTimer();
            }
            break;

        case "ICE_CANDIDATE":
            if (peerConnection && peerConnection.remoteDescription) {
                try {
                    const candidate = new RTCIceCandidate(JSON.parse(sig.signalData));
                    await peerConnection.addIceCandidate(candidate);
                } catch (e) {
                    console.warn("ICE candidate error:", e);
                }
            }
            break;

        case "CALL_REJECTED":
            if (activeCall) {
                stopRingTone();
                const statusEl3 = document.getElementById("callStatusText");
                if (statusEl3) statusEl3.textContent = "Call was declined or user is busy.";
                showToast("The user declined or is currently unavailable.", "info");
                setTimeout(hangupCall, 2000);
            }
            break;

        case "CALL_ENDED":
            if (activeCall || pendingIncomingCall) {
                showToast("Emergency call ended.", "info");
                hangupCall();
            }
            break;
    }
}

// =========================
// LIFECYCLE TIMELINE MODAL
// =========================
const LIFECYCLE_STAGES = [
    { key: "REQUEST_CREATED", label: "Submitted" },
    { key: "MATCHING_DONORS", label: "Matching" },
    { key: "DONORS_CONTACTED", label: "Contacted" },
    { key: "DONOR_CONFIRMED", label: "Confirmed" },
    { key: "BLOOD_COLLECTED", label: "Collected" },
    { key: "HOSPITAL_RECEIVED", label: "In Hospital" },
    { key: "FULFILLED", label: "Fulfilled" }
];

async function openTimelineModal(requestId, hospital, bloodGroup) {
    const modal = document.getElementById("timelineModal");
    const title = document.getElementById("timelineModalTitle");
    const sub = document.getElementById("timelineModalSub");
    const stepper = document.getElementById("timelineModalStepper");
    const container = document.getElementById("timelineEventsContainer");

    if (!modal) return;
    modal.style.display = "flex";
    if (title) title.textContent = `Lifecycle: ${escapeHtml(hospital || 'Hospital')} (${bloodGroup || 'Blood'})`;
    if (sub) sub.textContent = `Tracking milestones for Blood Request #${requestId}`;
    if (container) container.innerHTML = '<div style="text-align: center; color: #94a3b8; padding: 20px;">Loading timeline events...</div>';

    try {
        const events = await apiFetch(`/api/blood-requests/${requestId}/timeline`, { redirectOnUnauthorized: false }) || [];
        
        let latestStageIndex = 0;
        if (events.length > 0) {
            const lastStageKey = events[events.length - 1].stage;
            const idx = LIFECYCLE_STAGES.findIndex(s => s.key === lastStageKey);
            if (idx >= 0) latestStageIndex = idx;
        }

        if (stepper) {
            stepper.innerHTML = LIFECYCLE_STAGES.map((s, idx) => {
                let statusClass = "";
                let dotContent = idx + 1;
                if (idx < latestStageIndex) {
                    statusClass = "completed";
                    dotContent = "✓";
                } else if (idx === latestStageIndex) {
                    statusClass = "active";
                }

                return `
                    <div class="timeline-step ${statusClass}">
                        <div class="timeline-dot">${dotContent}</div>
                        <span class="timeline-label">${s.label}</span>
                    </div>
                `;
            }).join("");
        }

        if (!container) return;
        if (events.length === 0) {
            container.innerHTML = '<div style="text-align: center; color: #64748b; padding: 20px;">No timeline updates recorded yet.</div>';
        } else {
            container.innerHTML = events.map(evt => `
                <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 12px 16px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                        <strong style="color: #1e293b; font-size: 13px;">${escapeHtml(evt.stage)}</strong>
                        <span style="font-size: 11px; color: #64748b;">${formatTimeAgo(evt.createdAt)}</span>
                    </div>
                    <p style="margin: 0 0 4px; font-size: 13px; color: #334155;">${escapeHtml(evt.description)}</p>
                    <small style="color: #94a3b8; font-size: 11px;">By: ${escapeHtml(evt.performedBy || 'System')}</small>
                </div>
            `).join("");
        }

    } catch (e) {
        console.error("Failed to load timeline:", e);
        if (container) container.innerHTML = `<div style="text-align: center; color: #dc2626; padding: 20px;">Failed to load timeline events.</div>`;
    }
}

function closeTimelineModal() {
    const modal = document.getElementById("timelineModal");
    if (modal) modal.style.display = "none";
}

// =========================
// DEMAND FORECAST & ANALYTICS
// =========================
async function openAnalyticsModal() {
    const modal = document.getElementById("analyticsModal");
    if (!modal) return;
    modal.style.display = "flex";

    const tbody = document.getElementById("forecastTableBody");
    if (tbody) tbody.innerHTML = '<tr><td colspan="6" style="padding: 24px; text-align: center; color: #94a3b8;">Calculating historical trends & forecasting...</td></tr>';

    try {
        const data = await apiFetch("/api/analytics/demand-forecast", { redirectOnUnauthorized: false });
        if (!data) return;

        const elInv = document.getElementById("forecastTotalInventory");
        const elPast = document.getElementById("forecastHistorical30d");
        const elProj = document.getElementById("forecastProjectedMonthly");
        if (elInv) elInv.textContent = `${data.totalCurrentInventory || 0} Units`;
        if (elPast) elPast.textContent = `${data.totalHistoricalDemand30Days || 0} Units`;
        if (elProj) elProj.textContent = `${data.totalProjectedMonthlyDemand || 0} Units`;

        if (tbody) {
            tbody.innerHTML = (data.groupForecasts || []).map(f => {
                const riskBadge = f.shortageRisk
                    ? `<span style="background: #fee2e2; color: #dc2626; padding: 3px 8px; border-radius: 6px; font-weight: 700; font-size: 11px;">DEFICIT (-${f.projectedDeficitUnits})</span>`
                    : `<span style="background: #ecfdf5; color: #059669; padding: 3px 8px; border-radius: 6px; font-weight: 700; font-size: 11px;">ADEQUATE</span>`;

                return `
                    <tr style="border-bottom: 1px solid #f1f5f9;">
                        <td style="padding: 10px 14px; font-weight: 700; color: #1e293b;">${f.bloodGroupDisplay}</td>
                        <td style="padding: 10px 14px; font-weight: 600;">${f.currentInventoryUnits}</td>
                        <td style="padding: 10px 14px;">${f.projectedWeeklyDemandUnits}</td>
                        <td style="padding: 10px 14px;">${f.projectedMonthlyDemandUnits}</td>
                        <td style="padding: 10px 14px;">${riskBadge}</td>
                        <td style="padding: 10px 14px; font-size: 12px; color: #475569;">${escapeHtml(f.recommendation)}</td>
                    </tr>
                `;
            }).join("");
        }

        const recList = document.getElementById("forecastRecommendationsList");
        if (recList) {
            recList.innerHTML = (data.systemRecommendations || []).map(r => `<li>${escapeHtml(r)}</li>`).join("");
        }

    } catch (e) {
        console.error("Failed to load forecast analytics:", e);
        if (tbody) tbody.innerHTML = `<tr><td colspan="6" style="padding: 24px; text-align: center; color: #dc2626;">Error calculating demand forecast: ${escapeHtml(e.message)}</td></tr>`;
    }
}

function closeAnalyticsModal() {
    const modal = document.getElementById("analyticsModal");
    if (modal) modal.style.display = "none";
}

// =========================
// SAFETY FLAGS & FRAUD REVIEWS
// =========================
async function loadFlaggedBadge() {
    try {
        const flagged = await apiFetch("/api/blood-requests/flagged", { redirectOnUnauthorized: false });
        if (flagged && flagged.length > 0) {
            const flaggedLink = document.getElementById("sidebarFlaggedLink");
            if (flaggedLink) {
                flaggedLink.innerHTML = `<span>⚠️</span> Safety Flags <span style="background: #dc2626; color: white; border-radius: 9999px; padding: 1px 7px; font-size: 11px; margin-left: auto;">${flagged.length}</span>`;
            }
        }
    } catch (e) {
        console.warn("Could not load flagged badge:", e);
    }
}

async function openFlaggedModal() {
    const modal = document.getElementById("flaggedRequestsModal");
    if (!modal) return;
    modal.style.display = "flex";

    const container = document.getElementById("flaggedRequestsContainer");
    if (container) container.innerHTML = '<div style="text-align: center; color: #94a3b8; padding: 30px;">Loading flagged requests...</div>';

    try {
        const flagged = await apiFetch("/api/blood-requests/flagged", { redirectOnUnauthorized: false }) || [];
        if (!container) return;

        if (flagged.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; color: #059669; padding: 40px;">
                    <span style="font-size: 32px;">✓</span>
                    <h4 style="margin: 8px 0 4px; font-size: 16px;">All Clear</h4>
                    <p style="color: #64748b; font-size: 13px;">No suspicious or duplicate requests detected in the system.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = flagged.map(r => `
            <div style="background: #fff; border: 1.5px solid #fecdd3; border-radius: 12px; padding: 16px; box-shadow: 0 2px 6px rgba(0,0,0,0.02);">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; flex-wrap: wrap; gap: 8px;">
                    <div>
                        <span style="background: #fee2e2; color: #991b1b; padding: 2px 8px; border-radius: 6px; font-size: 11px; font-weight: 700; text-transform: uppercase;">${escapeHtml(r.riskLevel)}</span>
                        <strong style="margin-left: 8px; color: #1e293b; font-size: 14px;">${escapeHtml(r.hospital)} (${formatBloodGroup(r.bloodGroup)})</strong>
                    </div>
                    <span style="font-size: 12px; color: #64748b;">${formatTimeAgo(r.createdAt)}</span>
                </div>
                <div style="font-size: 13px; color: #475569; margin-bottom: 12px; line-height: 1.5;">
                    <div>Requester: <strong>${escapeHtml(r.requesterName)}</strong> • Needed: <strong>${r.unitsNeeded || 1} units</strong></div>
                    ${r.patientReference ? `<div>Patient Ref: <code>${escapeHtml(r.patientReference)}</code></div>` : ''}
                    ${r.additionalMessage ? `<div style="margin-top: 4px; color: #64748b;"><em>"${escapeHtml(r.additionalMessage)}"</em></div>` : ''}
                </div>
                <div style="display: flex; gap: 8px; justify-content: flex-end;">
                    <button class="btn-action-sm" onclick="reviewFlaggedRequest(${r.id}, false)" style="background: #fff1f2; color: #be123c; border-color: #fecdd3;">
                        Reject & Cancel ✕
                    </button>
                    <button class="btn-action-sm" onclick="reviewFlaggedRequest(${r.id}, true)" style="background: #ecfdf5; color: #047857; border-color: #a7f3d0;">
                        Approve / Clear Flag ✓
                    </button>
                </div>
            </div>
        `).join("");

    } catch (e) {
        console.error("Failed to load flagged requests:", e);
        if (container) container.innerHTML = `<div style="text-align: center; color: #dc2626; padding: 20px;">Failed to load flagged requests: ${escapeHtml(e.message)}</div>`;
    }
}

function closeFlaggedModal() {
    const modal = document.getElementById("flaggedRequestsModal");
    if (modal) modal.style.display = "none";
}

async function reviewFlaggedRequest(requestId, approved) {
    const notes = prompt(approved ? "Enter approval notes (optional):" : "Enter rejection reason (optional):") || "";
    try {
        await apiFetch(`/api/blood-requests/${requestId}/risk-review`, {
            method: "PATCH",
            body: { approved, notes }
        });
        showToast(approved ? "Risk flag cleared." : "Request cancelled.", "success");
        openFlaggedModal();
        loadOpenRequests();
        loadFlaggedBadge();
    } catch (e) {
        console.error("Failed to review request:", e);
        showToast(e.message || "Failed to submit risk review.", "error");
    }
}
