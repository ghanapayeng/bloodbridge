// ============================================
// BLOODBRIDGE DONOR VERIFICATION
// ============================================

document.addEventListener("DOMContentLoaded", async function () {
    const params = new URLSearchParams(window.location.search);
    const token = params.get("token");

    const loadingState = document.getElementById("loadingState");
    const verifiedState = document.getElementById("verifiedState");
    const errorState = document.getElementById("errorState");
    const statusIcon = document.getElementById("statusIcon");

    if (!token) {
        if (loadingState) loadingState.style.display = "none";
        if (errorState) {
            errorState.style.display = "block";
            document.getElementById("errorMsgText").textContent = "No donor verification token provided.";
        }
        if (statusIcon) {
            statusIcon.textContent = "✕";
            statusIcon.style.color = "#dc2626";
        }
        return;
    }

    try {
        const res = await apiFetch(`/api/donors/verify/${encodeURIComponent(token)}`, { redirectOnUnauthorized: false });

        if (loadingState) loadingState.style.display = "none";

        if (res && res.verified) {
            if (verifiedState) verifiedState.style.display = "block";
            if (statusIcon) {
                statusIcon.textContent = "✓";
                statusIcon.style.color = "#059669";
            }

            document.getElementById("donorNameDisplay").textContent = res.donorName || "Verified Donor";
            document.getElementById("bloodGroupDisplay").textContent = formatBloodGroup(res.bloodGroup) || res.bloodGroup;
            document.getElementById("eligibilityDisplay").textContent = res.eligibilityStatus || "Eligible";
            document.getElementById("donationsDisplay").textContent = `${res.totalDonations || 0} times`;
            document.getElementById("memberSinceDisplay").textContent = res.memberSinceYear || "2026";
            if (res.verificationMessage) {
                document.getElementById("verificationMsg").textContent = res.verificationMessage;
            }
        } else {
            if (errorState) {
                errorState.style.display = "block";
                document.getElementById("errorMsgText").textContent = (res && res.verificationMessage) || "Invalid donor verification token.";
            }
            if (statusIcon) {
                statusIcon.textContent = "✕";
                statusIcon.style.color = "#dc2626";
            }
        }

    } catch (e) {
        console.error("Verification error:", e);
        if (loadingState) loadingState.style.display = "none";
        if (errorState) {
            errorState.style.display = "block";
            document.getElementById("errorMsgText").textContent = e.message || "Failed to verify donor credentials.";
        }
        if (statusIcon) {
            statusIcon.textContent = "✕";
            statusIcon.style.color = "#dc2626";
        }
    }
});
