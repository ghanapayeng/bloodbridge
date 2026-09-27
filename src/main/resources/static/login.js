// =========================
// BLOODBRIDGE LOGIN
// =========================

const loginForm = document.getElementById("loginForm");
const fillDemoBtn = document.getElementById("fillDemoAccount");

document.addEventListener("DOMContentLoaded", async function () {
    // If already logged in, redirect straight to dashboard
    const user = await getCurrentUser(false);
    if (user) {
        window.location.replace("dashboard.html");
    }
});

if (fillDemoBtn) {
    fillDemoBtn.addEventListener("click", function (e) {
        e.preventDefault();
        const emailInput = document.getElementById("email");
        const passwordInput = document.getElementById("password");
        if (emailInput) emailInput.value = "demo@bloodbridge.org";
        if (passwordInput) passwordInput.value = "BloodBridge123!";
        showToast("Demo credentials filled. Click Login to continue!", "info");
    });
}

if (loginForm) {
    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("password").value;

        if (!email || !password) {
            showToast("Please enter your email and password.", "warning");
            return;
        }

        const submitButton = loginForm.querySelector('button[type="submit"]');
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "Signing in...";
        }

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "same-origin",
                body: JSON.stringify({ email: email, password: password })
            });

            const body = await response.json().catch(() => ({}));

            if (!response.ok) {
                if (response.status === 404 || response.status === 405) {
                    showToast("Backend not found on port " + (window.location.port || "default") + ". Please open http://localhost:8080/login.html", "error");
                } else {
                    showToast(body.message || "Invalid email or password.", "error");
                }
                return;
            }

            showToast("Signed in successfully!", "success");
            setTimeout(() => {
                window.location.href = "dashboard.html";
            }, 600);

        } catch (error) {
            console.error("Login failed:", error);
            if (window.location.port && window.location.port !== "8080") {
                showToast("Backend unreachable on port " + window.location.port + ". Please open http://localhost:8080/login.html", "error");
            } else {
                showToast("Unable to sign in right now. Please try again.", "error");
            }
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = "Login";
            }
        }
    });
}

// Forgot password
const forgotPassword = document.getElementById("forgotPassword");
if (forgotPassword) {
    forgotPassword.addEventListener("click", function (event) {
        event.preventDefault();
        showToast("Password reset feature will be enabled in a future release.", "info");
    });
}
