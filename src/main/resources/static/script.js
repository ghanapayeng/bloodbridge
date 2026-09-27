// =========================
// BLOODBRIDGE HOME PAGE
// =========================

document.addEventListener("DOMContentLoaded", async function () {
    // Check if user is logged in
    const user = await getCurrentUser(false);

    if (user) {
        // Update nav links
        const loginLink = document.querySelector(".login-link");
        const navBtn = document.querySelector(".nav-button");

        if (loginLink) {
            loginLink.textContent = "Dashboard";
            loginLink.href = "dashboard.html";
            loginLink.style.fontWeight = "600";
            loginLink.style.color = "#dc2626";
        }

        if (navBtn) {
            navBtn.textContent = "My Profile";
            navBtn.href = "donor.html";
        }

        // Update hero buttons
        const heroDonorBtn = document.querySelector('.hero-buttons a[href="signup.html"]');
        if (heroDonorBtn) {
            heroDonorBtn.href = "donor.html";
            heroDonorBtn.textContent = "My Donor Profile";
        }
    }
});