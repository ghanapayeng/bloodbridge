// =========================
// BLOODBRIDGE REQUEST PAGE
// =========================

const requestForm = document.getElementById("requestForm");

document.addEventListener("DOMContentLoaded", async function () {
    const user = await getCurrentUser(true);
    if (!user) return;

    // Prefill from query parameters if available
    const urlParams = new URLSearchParams(window.location.search);
    const bgParam = urlParams.get("bloodGroup");
    const cityParam = urlParams.get("city");
    const urgencyParam = urlParams.get("urgency");

    if (bgParam && document.getElementById("bloodGroup")) {
        document.getElementById("bloodGroup").value = formatBloodGroup(bgParam) || bgParam;
    }
    if (cityParam && document.getElementById("city")) {
        document.getElementById("city").value = cityParam;
    }
    if (urgencyParam) {
        const radio = document.querySelector(`input[name="urgency"][value="${urgencyParam.toUpperCase()}"]`);
        if (radio) radio.checked = true;
    }
});

let deviceCoordinates = null;

const locationConsentCheckbox = document.getElementById("useLocationConsent");
if (locationConsentCheckbox) {
    locationConsentCheckbox.addEventListener("change", function () {
        const statusEl = document.getElementById("locationStatusText");
        if (this.checked) {
            if ("geolocation" in navigator) {
                if (statusEl) statusEl.textContent = "Acquiring GPS location...";
                navigator.geolocation.getCurrentPosition(
                    function (pos) {
                        deviceCoordinates = {
                            latitude: pos.coords.latitude,
                            longitude: pos.coords.longitude
                        };
                        if (statusEl) statusEl.innerHTML = `<span style="color:#16a34a;font-weight:600;">✓ Location acquired (${pos.coords.latitude.toFixed(3)}, ${pos.coords.longitude.toFixed(3)})</span>`;
                        showToast("Location captured for emergency matching.", "success");
                    },
                    function (err) {
                        console.warn("Geolocation denied/unavailable:", err);
                        locationConsentCheckbox.checked = false;
                        if (statusEl) statusEl.textContent = "Unable to access location. Matching will use City name.";
                        showToast("Location permission was not granted.", "warning");
                    },
                    { timeout: 10000, enableHighAccuracy: false }
                );
            } else {
                this.checked = false;
                if (statusEl) statusEl.textContent = "Geolocation is not supported by your browser.";
            }
        } else {
            deviceCoordinates = null;
            if (statusEl) statusEl.textContent = "Enables fast radius search for closest donors.";
        }
    });
}

if (requestForm) {
    requestForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const bloodGroup = document.getElementById("bloodGroup").value;
        const city = document.getElementById("city").value.trim();
        const hospital = document.getElementById("hospital").value.trim();
        const urgencyEl = document.querySelector('input[name="urgency"]:checked');
        const messageEl = document.getElementById("message");
        const additionalMessage = messageEl ? messageEl.value.trim() : "";

        const unitsNeededEl = document.getElementById("unitsNeeded");
        const unitsNeeded = unitsNeededEl ? parseInt(unitsNeededEl.value, 10) || 1 : 1;

        const patientRefEl = document.getElementById("patientReference");
        const patientReference = patientRefEl ? patientRefEl.value.trim() : null;

        const requiredByEl = document.getElementById("requiredBy");
        let requiredBy = null;
        if (requiredByEl && requiredByEl.value) {
            requiredBy = new Date(requiredByEl.value).toISOString();
        }

        if (!bloodGroup || !city || !hospital || !urgencyEl) {
            showToast("Please fill in all required fields.", "warning");
            return;
        }

        const submitBtn = requestForm.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = "Publishing Emergency Request...";
        }

        try {
            const payload = {
                bloodGroup: bloodGroup,
                city: city,
                hospital: hospital,
                urgency: urgencyEl.value,
                additionalMessage: additionalMessage || null,
                unitsNeeded: unitsNeeded,
                patientReference: patientReference || null,
                requiredBy: requiredBy,
                latitude: deviceCoordinates ? deviceCoordinates.latitude : null,
                longitude: deviceCoordinates ? deviceCoordinates.longitude : null
            };

            const created = await apiFetch("/api/blood-requests", {
                method: "POST",
                body: payload
            });

            if (urgencyEl.value === "CRITICAL" || urgencyEl.value === "URGENT") {
                showToast("🚨 Emergency request published! Nearby compatible donors are being notified.", "success");
            } else {
                showToast("Blood request published successfully!", "success");
            }

            setTimeout(() => {
                window.location.href = "dashboard.html";
            }, 1200);

        } catch (error) {
            console.error("Create blood request error:", error);
            showToast(error.message || "Unable to create blood request. Please try again.", "error");
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = "Find Compatible Donors";
            }
        }
    });
}