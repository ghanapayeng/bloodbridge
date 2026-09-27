// =========================
// BLOODBRIDGE FIND DONORS
// =========================

const searchForm = document.getElementById("searchForm");
const donorsContainer = document.getElementById("donorsContainer");
const matchStatusCount = document.getElementById("matchStatusCount");

document.addEventListener("DOMContentLoaded", async function () {
    // Check if user is logged in
    await getCurrentUser(false);

    // Read query parameters
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
    if (urgencyParam && document.getElementById("urgency")) {
        document.getElementById("urgency").value = urgencyParam.toUpperCase();
    }

    // Perform initial search
    fetchAndRenderDonors();
});

if (searchForm) {
    searchForm.addEventListener("submit", function (event) {
        event.preventDefault();
        fetchAndRenderDonors();
    });
}

async function fetchAndRenderDonors() {
    const bloodGroupSelect = document.getElementById("bloodGroup");
    const cityInput = document.getElementById("city");
    const urgencySelect = document.getElementById("urgency");

    const bloodGroupVal = bloodGroupSelect ? bloodGroupSelect.value : "";
    const cityVal = cityInput ? cityInput.value.trim() : "";
    const urgencyVal = urgencySelect ? urgencySelect.value : "MEDIUM";

    if (donorsContainer) {
        donorsContainer.innerHTML = `
            <div style="padding: 40px; text-align: center; color: #64748b;">
                <div style="font-size: 24px; margin-bottom: 8px;">⏳</div>
                Searching compatible donors...
            </div>
        `;
    }

    try {
        const params = new URLSearchParams();
        if (bloodGroupVal) params.append("bloodGroup", bloodGroupVal);
        if (cityVal) params.append("city", cityVal);
        if (urgencyVal) params.append("urgency", urgencyVal);

        const url = "/api/donors/smart-match?" + params.toString();
        const donors = await apiFetch(url, { redirectOnUnauthorized: false });

        renderDonorsList(donors || [], bloodGroupVal, cityVal, urgencyVal);

    } catch (error) {
        console.error("Error fetching smart donor matches:", error);
        if (donorsContainer) {
            donorsContainer.innerHTML = `
                <div style="padding: 30px; text-align: center; color: #dc2626; background: #fef2f2; border-radius: 12px; margin-top: 15px;">
                    Failed to load smart donor matches: ${error.message}
                </div>
            `;
        }
        if (matchStatusCount) matchStatusCount.textContent = "0 matches found";
    }
}

function renderDonorsList(donors, targetBloodGroup, targetCity, urgency) {
    currentDonorsList = donors || [];
    if (!donorsContainer) return;

    if (!donors || donors.length === 0) {
        if (matchStatusCount) matchStatusCount.textContent = "0 matches found";

        donorsContainer.innerHTML = `
            <div style="padding: 48px 24px; text-align: center; background: #ffffff; border-radius: 16px; border: 1px dashed #cbd5e1; margin-top: 16px;">
                <div style="font-size: 36px; margin-bottom: 12px;">🩸</div>
                <h3 style="font-size: 18px; margin-bottom: 8px; color: #1e293b;">No available donors found matching this criteria</h3>
                <p style="color: #64748b; max-width: 420px; margin: 0 auto 20px auto; font-size: 14px;">
                    Post an urgent blood request to notify hospitals and registered donors across nearby areas immediately.
                </p>
                <a href="request.html${targetBloodGroup ? '?bloodGroup=' + encodeURIComponent(targetBloodGroup) : ''}${targetCity ? '&city=' + encodeURIComponent(targetCity) : ''}"
                   style="display: inline-block; padding: 12px 24px; background: #dc2626; color: white; border-radius: 8px; text-decoration: none; font-weight: 600;">
                    Create Blood Request Now →
                </a>
            </div>
        `;
        return;
    }

    if (matchStatusCount) {
        matchStatusCount.textContent = `${donors.length} ranked ${donors.length === 1 ? 'donor' : 'donors'}`;
    }

    donorsContainer.innerHTML = `
        <div style="margin-bottom: 14px; padding: 10px 14px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; font-size: 12px; color: #64748b; display: flex; align-items: center; justify-content: space-between;">
            <span>ℹ️ <strong>Logistical Ranking System:</strong> Scored on Compatibility (40%), Distance (25%), Availability (15%), Eligibility (10%), Response History (10%). Not a clinical medical decision.</span>
        </div>
    ` + donors.map((donor, idx) => {
        const initial = (donor.fullName || "D").charAt(0).toUpperCase();
        const donorBg = formatBloodGroup(donor.bloodGroup);
        const distanceStr = donor.distanceFormatted || (donor.distanceKm ? `${donor.distanceKm} km away` : escapeHtml(donor.city));

        const availBadge = donor.available
            ? `<span style="display:inline-block; font-size: 11px; padding: 2px 8px; border-radius: 9999px; background: #dcfce7; color: #15803d; font-weight: 600;">● Available</span>`
            : `<span style="display:inline-block; font-size: 11px; padding: 2px 8px; border-radius: 9999px; background: #f1f5f9; color: #64748b; font-weight: 500;">Unavailable</span>`;

        const eligBadge = donor.eligible
            ? `<span style="display:inline-block; font-size: 11px; padding: 2px 8px; border-radius: 9999px; background: #eff6ff; color: #1d4ed8; font-weight: 600;">Eligible ✓</span>`
            : `<span style="display:inline-block; font-size: 11px; padding: 2px 8px; border-radius: 9999px; background: #fff7ed; color: #c2410c; font-weight: 500;">Cooldown/Pending</span>`;

        const compatBadge = donor.compatibilityLabel
            ? `<span style="font-size: 11px; color: #475569; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-weight: 600;">${escapeHtml(donor.compatibilityLabel)}</span>`
            : '';

        return `
            <div class="donor-card" style="margin-bottom: 14px; display: flex; align-items: center; justify-content: space-between; padding: 16px; background: white; border-radius: 12px; border: 1px solid #e2e8f0; gap: 14px;">
                <div style="display: flex; align-items: center; gap: 12px;">
                    <div class="rank" style="font-size: 14px; font-weight: 700; color: #94a3b8; width: 24px;">#${idx + 1}</div>

                    <div class="donor-avatar" style="width: 44px; height: 44px; border-radius: 50%; background: #fee2e2; color: #dc2626; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 16px;">${initial}</div>

                    <div class="donor-info">
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <h3 style="margin: 0; font-size: 15px; color: #1e293b;">${escapeHtml(donor.fullName)}</h3>
                            ${compatBadge}
                        </div>
                        <p style="margin: 3px 0 6px 0; color: #64748b; font-size: 13px;">
                            <strong>${donorBg}</strong> • 📍 ${escapeHtml(distanceStr)}
                        </p>
                        <div style="display: flex; gap: 6px; align-items: center;">
                            ${availBadge}
                            ${eligBadge}
                        </div>
                    </div>
                </div>

                <div style="display: flex; align-items: center; gap: 16px;">
                    <div class="match-score" style="text-align: right;">
                        <strong style="font-size: 20px; color: #059669; display: block;">${donor.matchScore}%</strong>
                        <span style="font-size: 11px; color: #64748b;">Match score</span>
                    </div>

                    <button class="contact-btn" onclick="requestBloodFromDonor('${donorBg}', '${escapeJs(donor.city)}', '${escapeJs(donor.fullName)}')">
                        Request Help
                    </button>
                </div>
            </div>
        `;
    }).join("");
}

// ============================================
// INTERACTIVE MAP VIEW (LEAFLET + OPENSTREETMAP)
// ============================================

let currentDonorsList = [];
let donorsMap = null;
let mapRadiusCircle = null;
let donorMarkersLayer = null;
let currentSearchRadiusKm = 10;
let mapCenterCoords = [28.6139, 77.2090]; // Default New Delhi / center

function switchDonorView(mode) {
    const listBtn = document.getElementById("listViewBtn");
    const mapBtn = document.getElementById("mapViewBtn");
    const listContainer = document.getElementById("donorsContainer");
    const mapSection = document.getElementById("donorsMapSection");

    if (mode === "map") {
        if (listContainer) listContainer.style.display = "none";
        if (mapSection) mapSection.style.display = "block";

        if (mapBtn) {
            mapBtn.style.background = "#ffffff";
            mapBtn.style.color = "#0f172a";
            mapBtn.style.boxShadow = "0 1px 3px rgba(0,0,0,0.1)";
        }
        if (listBtn) {
            listBtn.style.background = "transparent";
            listBtn.style.color = "#64748b";
            listBtn.style.boxShadow = "none";
        }

        setTimeout(() => {
            initOrUpdateMap();
        }, 100);
    } else {
        if (listContainer) listContainer.style.display = "block";
        if (mapSection) mapSection.style.display = "none";

        if (listBtn) {
            listBtn.style.background = "#ffffff";
            listBtn.style.color = "#0f172a";
            listBtn.style.boxShadow = "0 1px 3px rgba(0,0,0,0.1)";
        }
        if (mapBtn) {
            mapBtn.style.background = "transparent";
            mapBtn.style.color = "#64748b";
            mapBtn.style.boxShadow = "none";
        }
    }
}

function setMapSearchRadius(km) {
    currentSearchRadiusKm = km;
    document.querySelectorAll(".radius-btn").forEach(btn => {
        if (btn.textContent.trim().startsWith(km + " km")) {
            btn.style.background = "#fee2e2";
            btn.style.color = "#dc2626";
            btn.style.fontWeight = "700";
        } else {
            btn.style.background = "#ffffff";
            btn.style.color = "#334155";
            btn.style.fontWeight = "600";
        }
    });

    if (mapRadiusCircle && donorsMap) {
        mapRadiusCircle.setRadius(km * 1000);
        donorsMap.fitBounds(mapRadiusCircle.getBounds(), { padding: [25, 25] });
    }
}

function initOrUpdateMap() {
    if (typeof L === "undefined") {
        console.warn("Leaflet map library is not loaded yet.");
        return;
    }

    const mapElement = document.getElementById("donorsMap");
    if (!mapElement) return;

    if (!donorsMap) {
        donorsMap = L.map("donorsMap").setView(mapCenterCoords, 12);
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19,
            attribution: '© <a href="https://openstreetmap.org">OpenStreetMap</a>'
        }).addTo(donorsMap);

        donorMarkersLayer = L.layerGroup().addTo(donorsMap);
    } else {
        donorsMap.invalidateSize();
    }

    renderMapMarkers();
}

function renderMapMarkers() {
    if (!donorsMap || !donorMarkersLayer) return;

    donorMarkersLayer.clearLayers();

    // 1. Center / Search Location Marker
    const centerIcon = L.divIcon({
        className: 'custom-map-center-pin',
        html: `<div style="background: #dc2626; color: white; width: 34px; height: 34px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 16px; border: 2.5px solid white; box-shadow: 0 3px 8px rgba(0,0,0,0.35);">🏥</div>`,
        iconSize: [34, 34],
        iconAnchor: [17, 17]
    });

    const searchMarker = L.marker(mapCenterCoords, { icon: centerIcon }).addTo(donorMarkersLayer);
    const cityInput = document.getElementById("city");
    const cityName = cityInput && cityInput.value ? cityInput.value : "Search Hub";
    searchMarker.bindPopup(`<strong>🏥 Search Hub: ${escapeHtml(cityName)}</strong><br><span style="font-size: 12px; color: #64748b;">Active Search Origin</span>`);

    // 2. Search Radius Circle
    if (mapRadiusCircle) {
        donorsMap.removeLayer(mapRadiusCircle);
    }
    mapRadiusCircle = L.circle(mapCenterCoords, {
        color: '#ef4444',
        fillColor: '#fee2e2',
        fillOpacity: 0.2,
        radius: currentSearchRadiusKm * 1000
    }).addTo(donorsMap);

    // 3. Plot candidate donors
    if (currentDonorsList && currentDonorsList.length > 0) {
        currentDonorsList.forEach((donor, index) => {
            // Apply slight privacy jitter (0.01 - 0.04 degrees) around center if exact GPS not available
            const angle = (index * (360 / Math.max(1, currentDonorsList.length))) * (Math.PI / 180);
            const distanceOffset = Math.min(0.04, 0.008 * (index + 1));
            const lat = mapCenterCoords[0] + (distanceOffset * Math.cos(angle));
            const lng = mapCenterCoords[1] + (distanceOffset * Math.sin(angle));

            const donorBg = formatBloodGroup(donor.bloodGroup);
            const isAvail = donor.available;

            const markerHtml = `
                <div style="background: ${isAvail ? '#059669' : '#64748b'}; color: white; padding: 4px 7px; border-radius: 12px; font-weight: 700; font-size: 12px; border: 2px solid white; box-shadow: 0 2px 6px rgba(0,0,0,0.25); display: inline-flex; align-items: center; gap: 3px;">
                    <span>🩸</span>${donorBg}
                </div>
            `;

            const donorIcon = L.divIcon({
                className: 'custom-donor-pin',
                html: markerHtml,
                iconSize: [44, 26],
                iconAnchor: [22, 13]
            });

            const marker = L.marker([lat, lng], { icon: donorIcon }).addTo(donorMarkersLayer);
            marker.bindPopup(`
                <div style="font-family: inherit; font-size: 13px;">
                    <strong style="color: #0f172a; font-size: 14px;">Compatible Donor (${donorBg})</strong>
                    <div style="color: #059669; font-weight: 600; margin: 2px 0;">Match Score: ${donor.matchScore}%</div>
                    <div style="color: #64748b; font-size: 12px;">📍 ${escapeHtml(donor.distanceFormatted || donor.city || 'Nearby')}</div>
                    <div style="margin-top: 4px; font-size: 11px; color: ${isAvail ? '#15803d' : '#64748b'};">
                        ${isAvail ? '● Available to Donate' : '○ Currently Unavailable'}
                    </div>
                    <button onclick="requestBloodFromDonor('${donorBg}', '${escapeJs(donor.city)}', '${escapeJs(donor.fullName)}')"
                            style="margin-top: 8px; cursor: pointer; background: #dc2626; color: white; border: none; border-radius: 6px; padding: 5px 12px; font-size: 12px; font-weight: 600;">
                        Request Help
                    </button>
                </div>
            `);
        });
    }

    donorsMap.fitBounds(mapRadiusCircle.getBounds(), { padding: [25, 25] });
}

function requestBloodFromDonor(bloodGroup, city, donorName) {
    const confirmed = confirm(
        `Would you like to publish a blood request for ${bloodGroup} in ${city} to connect with ${donorName} and nearby donors?`
    );
    if (confirmed) {
        window.location.href = `request.html?bloodGroup=${encodeURIComponent(bloodGroup)}&city=${encodeURIComponent(city)}`;
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/[&<>"']/g, function (m) {
        return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m];
    });
}

function escapeJs(str) {
    if (!str) return "";
    return str.replace(/'/g, "\\'").replace(/"/g, '\\"');
}