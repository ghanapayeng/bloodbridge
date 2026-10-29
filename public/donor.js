// =========================
// BLOODBRIDGE DONOR PROFILE
// =========================

const availability = document.getElementById("availability");
const availabilityText = document.getElementById("availabilityText");
const donorForm = document.getElementById("donorForm");

function updateAvailabilityText() {
    if (availability.checked) {
        availabilityText.textContent = "Available to donate";
    } else {
        availabilityText.textContent = "Currently unavailable";
    }
}

if (availability) {
    availability.addEventListener("change", updateAvailabilityText);
}

document.addEventListener("DOMContentLoaded", async function () {
    const user = await getCurrentUser(true);
    if (!user) return;

    const nameInput = document.getElementById("name");
    const emailInput = document.getElementById("email");
    if (nameInput) nameInput.value = user.fullName || "";
    if (emailInput) emailInput.value = user.email || "";

    // Load server profile
    try {
        const profile = await apiFetch("/api/donor-profiles/me", { redirectOnUnauthorized: false });
        if (profile) {
            if (document.getElementById("phone")) document.getElementById("phone").value = profile.phone || "";
            if (document.getElementById("city")) document.getElementById("city").value = profile.city || "";
            if (document.getElementById("bloodGroup")) {
                const bgDisplay = formatBloodGroup(profile.bloodGroup);
                document.getElementById("bloodGroup").value = bgDisplay || profile.bloodGroup || "";
            }
            if (availability) {
                availability.checked = profile.available === true;
                updateAvailabilityText();
            }
            if (document.getElementById("lastDonationDate") && profile.lastDonationDate) {
                document.getElementById("lastDonationDate").value = profile.lastDonationDate;
            }
            if (profile.latitude && profile.longitude) {
                const latEl = document.getElementById("latitude");
                const lonEl = document.getElementById("longitude");
                const consentEl = document.getElementById("shareLocationConsent");
                if (latEl) latEl.value = profile.latitude;
                if (lonEl) lonEl.value = profile.longitude;
                if (consentEl) consentEl.checked = true;
            }

            const statusEl = document.querySelector(".profile-status");
            if (statusEl) {
                statusEl.innerHTML = `<span style="color:#10b981;">●</span> Profile active (${formatBloodGroup(profile.bloodGroup)})`;
            }

            // Load detailed eligibility
            loadEligibility();

            // Render digital card & QR code
            renderDigitalCard(profile);

            // Render gamification milestone badges
            renderMilestones(profile.totalDonationsCount);
        }
    } catch (e) {
        // No existing profile yet
        console.log("No existing donor profile found; ready for new setup.");
        const statusEl = document.querySelector(".profile-status");
        if (statusEl) {
            statusEl.innerHTML = `<span style="color:#f59e0b;">●</span> Incomplete profile`;
        }
    }

    // Setup location checkbox listener
    const consentEl = document.getElementById("shareLocationConsent");
    if (consentEl) {
        consentEl.addEventListener("change", function () {
            if (consentEl.checked) {
                if (navigator.geolocation) {
                    navigator.geolocation.getCurrentPosition(
                        (position) => {
                            document.getElementById("latitude").value = position.coords.latitude;
                            document.getElementById("longitude").value = position.coords.longitude;
                            showToast("Approximate location acquired for donor matching.", "info");
                        },
                        (err) => {
                            console.warn("Geolocation denied or unavailable:", err.message);
                            consentEl.checked = false;
                            showToast("Could not access device location. City matching will be used instead.", "warning");
                        }
                    );
                }
            } else {
                document.getElementById("latitude").value = "";
                document.getElementById("longitude").value = "";
            }
        });
    }
});

async function loadEligibility() {
    const card = document.getElementById("eligibilityCard");
    if (!card) return;

    try {
        const elig = await apiFetch("/api/donor-profiles/me/eligibility", { redirectOnUnauthorized: false });
        if (!elig) return;

        card.style.display = "block";
        const badge = document.getElementById("eligibilityBadge");
        const reason = document.getElementById("eligibilityReason");
        const cooldownDetails = document.getElementById("cooldownDetails");
        const nextDateEl = document.getElementById("nextEligibleDateText");
        const daysEl = document.getElementById("cooldownDaysText");
        const disclaimerEl = document.getElementById("eligibilityDisclaimer");

        if (elig.eligible) {
            badge.textContent = "Eligible to Donate ✓";
            badge.style.background = "#ecfdf5";
            badge.style.color = "#059669";
        } else if (elig.status === "REQUIRES_VERIFICATION") {
            badge.textContent = "Verification Required ⚠️";
            badge.style.background = "#fffbeb";
            badge.style.color = "#b45309";
        } else {
            badge.textContent = "Cooldown Period ⏳";
            badge.style.background = "#fef2f2";
            badge.style.color = "#dc2626";
        }

        if (reason) reason.textContent = elig.reason;

        if (cooldownDetails && elig.nextEligibleDate) {
            cooldownDetails.style.display = "block";
            if (nextDateEl) nextDateEl.textContent = elig.nextEligibleDate;
            if (daysEl) daysEl.textContent = elig.daysRemaining > 0 ? `${elig.daysRemaining} days` : "Eligible now";
        }

        if (disclaimerEl) disclaimerEl.textContent = elig.logisticalDisclaimer;

    } catch (e) {
        console.warn("Could not load donor eligibility:", e);
    }
}

function renderDigitalCard(profile) {
    const cardSection = document.getElementById("digitalCardSection");
    if (!cardSection || !profile || !profile.donorUuid) return;

    cardSection.style.display = "block";
    const nameEl = document.getElementById("cardDonorName");
    const bgEl = document.getElementById("cardDonorBg");
    const uuidEl = document.getElementById("cardDonorUuid");
    if (nameEl) nameEl.textContent = profile.fullName || "Verified Donor";
    if (bgEl) bgEl.textContent = formatBloodGroup(profile.bloodGroup) || profile.bloodGroup;
    if (uuidEl) uuidEl.textContent = profile.donorUuid;

    const verifyUrl = `${window.location.origin}/verify-donor.html?token=${encodeURIComponent(profile.donorUuid)}`;
    const linkEl = document.getElementById("cardVerifyLink");
    if (linkEl) linkEl.href = verifyUrl;

    const qrImg = document.getElementById("donorQrImage");
    if (qrImg) {
        qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=140x140&data=${encodeURIComponent(verifyUrl)}`;
        qrImg.style.display = "block";
    }
}

// Save profile
if (donorForm) {
    donorForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const phone = document.getElementById("phone").value.trim();
        const city = document.getElementById("city").value.trim();
        const bloodGroupVal = document.getElementById("bloodGroup").value;
        const isAvailable = availability ? availability.checked : true;
        const lastDonationDateVal = document.getElementById("lastDonationDate") ? document.getElementById("lastDonationDate").value : null;
        const latVal = document.getElementById("latitude") && document.getElementById("latitude").value ? parseFloat(document.getElementById("latitude").value) : null;
        const lonVal = document.getElementById("longitude") && document.getElementById("longitude").value ? parseFloat(document.getElementById("longitude").value) : null;

        if (!phone || !city || !bloodGroupVal) {
            showToast("Please fill in all required fields (phone, city, blood group).", "warning");
            return;
        }

        const submitBtn = donorForm.querySelector('button[type="submit"]');
        if (submitBtn) submitBtn.disabled = true;

        try {
            const payload = {
                bloodGroup: bloodGroupVal,
                phone: phone,
                city: city,
                available: isAvailable,
                lastDonationDate: lastDonationDateVal || null,
                latitude: latVal,
                longitude: lonVal
            };

            const updated = await apiFetch("/api/donor-profiles/me", {
                method: "PUT",
                body: payload
            });

            showToast("Donor profile saved successfully!", "success");

            setTimeout(() => {
                window.location.href = "dashboard.html";
            }, 1000);

        } catch (error) {
            console.error("Save profile error:", error);
            showToast(error.message || "Failed to save donor profile. Please try again.", "error");
        } finally {
            if (submitBtn) submitBtn.disabled = false;
        }
    });
}

function renderMilestones(count) {
    count = count || 0;
    const badgeBronze = document.getElementById("badgeBronze");
    const badgeSilver = document.getElementById("badgeSilver");
    const badgeGold = document.getElementById("badgeGold");
    const badgePlatinum = document.getElementById("badgePlatinum");

    function unlock(el, active) {
        if (!el) return;
        if (active) {
            el.style.opacity = "1";
            el.style.borderColor = "#f59e0b";
            el.style.background = "#fffbeb";
            el.style.boxShadow = "0 4px 12px rgba(245, 158, 11, 0.15)";
        } else {
            el.style.opacity = "0.5";
            el.style.borderColor = "#e2e8f0";
            el.style.background = "#f8fafc";
            el.style.boxShadow = "none";
        }
    }

    unlock(badgeBronze, count >= 1);
    unlock(badgeSilver, count >= 3);
    unlock(badgeGold, count >= 5);
    unlock(badgePlatinum, count >= 10);

    const progressLabel = document.getElementById("milestoneProgressLabel");
    const progressText = document.getElementById("milestoneProgressText");
    const progressBar = document.getElementById("milestoneProgressBar");

    if (!progressLabel || !progressText || !progressBar) return;

    if (count < 1) {
        progressLabel.textContent = "Progress to First Drop (🥉)";
        progressText.textContent = `${count} / 1 donation`;
        progressBar.style.width = `${(count / 1) * 100}%`;
    } else if (count < 3) {
        progressLabel.textContent = "Progress to Life Saver (🥈)";
        progressText.textContent = `${count} / 3 donations`;
        progressBar.style.width = `${(count / 3) * 100}%`;
    } else if (count < 5) {
        progressLabel.textContent = "Progress to Community Hero (🥇)";
        progressText.textContent = `${count} / 5 donations`;
        progressBar.style.width = `${(count / 5) * 100}%`;
    } else if (count < 10) {
        progressLabel.textContent = "Progress to Legendary Donor (💎)";
        progressText.textContent = `${count} / 10 donations`;
        progressBar.style.width = `${(count / 10) * 100}%`;
    } else {
        progressLabel.textContent = "Highest Honor Achieved! (💎)";
        progressText.textContent = `${count} donations recorded`;
        progressBar.style.width = "100%";
    }
}
