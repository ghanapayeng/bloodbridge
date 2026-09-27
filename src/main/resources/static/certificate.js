let certificates = [];
let currentQr = null;

document.addEventListener("DOMContentLoaded", () => {
    loadCertificates();
});

async function loadCertificates() {
    try {
        const res = await fetch("/api/donations/certificates");
        if (res.status === 401) {
            window.location.href = "login.html";
            return;
        }
        if (!res.ok) {
            throw new Error("Unable to retrieve donation certificates");
        }
        certificates = await res.json();
        const selector = document.getElementById("certSelector");

        if (!certificates || certificates.length === 0) {
            selector.innerHTML = '<option value="">No donations recorded yet</option>';
            document.getElementById("donorName").textContent = "Donor Certificate";
            document.querySelector(".cert-text").textContent = "You have not recorded any blood donations yet. Complete a donation to receive your official Certificate of Appreciation!";
            return;
        }

        selector.innerHTML = certificates.map((c, idx) => `
            <option value="${idx}">Certificate #${c.certificateNumber} (${c.donationDate})</option>
        `).join("");

        renderCertificate(certificates[0]);

    } catch (e) {
        console.error("Certificate error:", e);
        document.getElementById("donorName").textContent = "Error Loading Certificate";
    }
}

function onCertificateSelected(idx) {
    if (idx !== "" && certificates[idx]) {
        renderCertificate(certificates[idx]);
    }
}

function renderCertificate(cert) {
    document.getElementById("donorName").textContent = cert.donorName || "Valued Donor";
    document.getElementById("unitsDonated").textContent = cert.unitsDonated || "1.0";
    document.getElementById("bloodGroup").textContent = formatBloodGroup(cert.bloodGroup);
    document.getElementById("donationDate").textContent = cert.donationDate || "--";
    document.getElementById("hospitalName").textContent = cert.hospital || "BloodBridge Center";
    document.getElementById("locationCity").textContent = cert.city || "National";
    document.getElementById("certNumber").textContent = cert.certificateNumber || "--";

    // QR Code
    const qrDiv = document.getElementById("certQrCode");
    qrDiv.innerHTML = "";
    const verifyUrl = window.location.origin + (cert.verificationQrUrl || "/verify-donor.html");
    new QRCode(qrDiv, {
        text: verifyUrl,
        width: 72,
        height: 72,
        colorDark: "#1e293b",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.M
    });
}

function formatBloodGroup(bg) {
    if (!bg) return "Blood";
    return bg.replace("_POSITIVE", "+").replace("_NEGATIVE", "-");
}
