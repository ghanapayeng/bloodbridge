// ===============================================
// BLOODBRIDGE LOGIN (PASSWORD & TWILIO EMAIL OTP)
// ===============================================

const loginForm = document.getElementById("loginForm");
const otpLoginForm = document.getElementById("otpLoginForm");
const tabPasswordBtn = document.getElementById("tabPasswordBtn");
const tabOtpBtn = document.getElementById("tabOtpBtn");
const fillDemoBtn = document.getElementById("fillDemoAccount");
const sendLoginOtpBtn = document.getElementById("sendLoginOtpBtn");
const resendLoginOtpLink = document.getElementById("resendLoginOtpLink");
const loginOtpGroup = document.getElementById("loginOtpGroup");
const loginOtpCountdown = document.getElementById("loginOtpCountdown");

let otpCooldownTimer = null;

document.addEventListener("DOMContentLoaded", async function () {
    // If already logged in, redirect straight to dashboard
    const user = await getCurrentUser(false);
    if (user) {
        window.location.replace("dashboard.html");
    }

    setupTabs();
    setupOtpLogin();
});

// -----------------------------------------------
// TAB SWITCHING
// -----------------------------------------------
function setupTabs() {
    if (tabPasswordBtn && tabOtpBtn) {
        tabPasswordBtn.addEventListener("click", function () {
            tabPasswordBtn.classList.add("active");
            tabOtpBtn.classList.remove("active");
            if (loginForm) loginForm.style.display = "block";
            if (otpLoginForm) otpLoginForm.style.display = "none";
        });

        tabOtpBtn.addEventListener("click", function () {
            tabOtpBtn.classList.add("active");
            tabPasswordBtn.classList.remove("active");
            if (loginForm) loginForm.style.display = "none";
            if (otpLoginForm) otpLoginForm.style.display = "block";

            // If email was entered in password tab, copy it over
            const pwdEmail = document.getElementById("email");
            const otpEmail = document.getElementById("otpEmail");
            if (pwdEmail && otpEmail && pwdEmail.value && !otpEmail.value) {
                otpEmail.value = pwdEmail.value.trim();
            }
        });
    }
}

// -----------------------------------------------
// DEMO QUICK LOGIN
// -----------------------------------------------
if (fillDemoBtn) {
    fillDemoBtn.addEventListener("click", function (e) {
        e.preventDefault();
        // If on OTP tab, switch to password tab
        if (tabPasswordBtn && !tabPasswordBtn.classList.contains("active")) {
            tabPasswordBtn.click();
        }
        const emailInput = document.getElementById("email");
        const passwordInput = document.getElementById("password");
        if (emailInput) emailInput.value = "demo@bloodbridge.org";
        if (passwordInput) passwordInput.value = "BloodBridge123!";
        showToast("Demo credentials filled. Click Login to continue!", "info");
    });
}

// -----------------------------------------------
// STANDARD PASSWORD LOGIN
// -----------------------------------------------
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

// -----------------------------------------------
// TWILIO EMAIL OTP LOGIN
// -----------------------------------------------
function setupOtpLogin() {
    async function handleSendOtp() {
        const emailInput = document.getElementById("otpEmail");
        const email = emailInput ? emailInput.value.trim() : "";

        if (!email || !email.includes("@")) {
            showToast("Please enter a valid email address.", "warning");
            return;
        }

        if (sendLoginOtpBtn) {
            sendLoginOtpBtn.disabled = true;
            sendLoginOtpBtn.textContent = "Sending...";
        }

        try {
            const res = await fetch("/api/auth/otp/send", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ identifier: email, type: "EMAIL" })
            });

            const data = await res.json();
            if (!res.ok) {
                throw new Error(data.message || "Failed to send email verification code.");
            }

            if (loginOtpGroup) {
                loginOtpGroup.style.display = "block";
            }
            const codeInput = document.getElementById("loginOtpCode");
            if (codeInput) {
                codeInput.focus();
            }

            const previewMsg = data.debugCode ? ` (Code: ${data.debugCode})` : "";
            showToast(`Verification code sent to ${email} via Twilio!${previewMsg}`, "info");

            startOtpCooldown(45);

        } catch (err) {
            console.error("OTP send error:", err);
            showToast(err.message || "Unable to send verification code. Please try again.", "error");
            if (sendLoginOtpBtn) {
                sendLoginOtpBtn.disabled = false;
                sendLoginOtpBtn.textContent = "Send OTP";
            }
        }
    }

    if (sendLoginOtpBtn) {
        sendLoginOtpBtn.addEventListener("click", handleSendOtp);
    }

    if (resendLoginOtpLink) {
        resendLoginOtpLink.addEventListener("click", function (e) {
            e.preventDefault();
            handleSendOtp();
        });
    }

    if (otpLoginForm) {
        otpLoginForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            const emailInput = document.getElementById("otpEmail");
            const codeInput = document.getElementById("loginOtpCode");
            const email = emailInput ? emailInput.value.trim() : "";
            const code = codeInput ? codeInput.value.trim() : "";

            if (!email) {
                showToast("Please enter your email address.", "warning");
                return;
            }

            if (!code || code.length !== 6) {
                showToast("Please enter the 6-digit verification code.", "warning");
                return;
            }

            const submitBtn = document.getElementById("loginOtpSubmitBtn");
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = "Signing in...";
            }

            try {
                const response = await fetch("/api/auth/login/otp", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "same-origin",
                    body: JSON.stringify({ email: email, code: code })
                });

                const body = await response.json().catch(() => ({}));

                if (!response.ok) {
                    showToast(body.message || "Invalid or expired verification code.", "error");
                    return;
                }

                showToast("Signed in successfully!", "success");
                setTimeout(() => {
                    window.location.href = "dashboard.html";
                }, 600);

            } catch (err) {
                console.error("OTP login failed:", err);
                showToast("Unable to sign in. Please check your code and try again.", "error");
            } finally {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.textContent = "Login with OTP";
                }
            }
        });
    }
}

function startOtpCooldown(seconds) {
    if (otpCooldownTimer) {
        clearInterval(otpCooldownTimer);
    }

    let remaining = seconds;
    if (sendLoginOtpBtn) {
        sendLoginOtpBtn.disabled = true;
    }
    if (resendLoginOtpLink) {
        resendLoginOtpLink.style.display = "none";
    }

    const updateDisplay = () => {
        if (loginOtpCountdown) {
            loginOtpCountdown.textContent = `Resend available in ${remaining}s`;
        }
        if (sendLoginOtpBtn) {
            sendLoginOtpBtn.textContent = `${remaining}s`;
        }
    };

    updateDisplay();

    otpCooldownTimer = setInterval(() => {
        remaining--;
        if (remaining <= 0) {
            clearInterval(otpCooldownTimer);
            otpCooldownTimer = null;
            if (loginOtpCountdown) loginOtpCountdown.textContent = "";
            if (sendLoginOtpBtn) {
                sendLoginOtpBtn.disabled = false;
                sendLoginOtpBtn.textContent = "Send OTP";
            }
            if (resendLoginOtpLink) {
                resendLoginOtpLink.style.display = "inline";
            }
        } else {
            updateDisplay();
        }
    }, 1000);
}

// -----------------------------------------------
// FORGOT PASSWORD
// -----------------------------------------------
const forgotPassword = document.getElementById("forgotPassword");
if (forgotPassword) {
    forgotPassword.addEventListener("click", function (event) {
        event.preventDefault();
        showToast("Use Email OTP tab above for instant passwordless login!", "info");
    });
}
