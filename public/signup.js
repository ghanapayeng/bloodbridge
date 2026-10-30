// ===============================================
// BLOODBRIDGE SIGNUP WITH EMAIL & PHONE OTP VERIFICATION
// ===============================================

const signupForm = document.getElementById("signupForm");

let emailVerified = false;
let phoneVerified = false;

document.addEventListener("DOMContentLoaded", async function () {
    const user = await getCurrentUser(false);
    if (user) {
        window.location.replace("dashboard.html");
    }

    setupOtpHandlers();
});

function setupOtpHandlers() {
    // 1. Email OTP
    const sendEmailOtpBtn = document.getElementById("sendEmailOtpBtn");
    const verifyEmailOtpBtn = document.getElementById("verifyEmailOtpBtn");
    const emailOtpGroup = document.getElementById("emailOtpGroup");
    const emailVerifiedBadge = document.getElementById("emailVerifiedBadge");

    if (sendEmailOtpBtn) {
        sendEmailOtpBtn.addEventListener("click", async function () {
            const emailInput = document.getElementById("email");
            const email = emailInput ? emailInput.value.trim() : "";
            if (!email || !email.includes("@")) {
                showToast("Please enter a valid email address first.", "warning");
                return;
            }

            sendEmailOtpBtn.disabled = true;
            sendEmailOtpBtn.textContent = "Sending...";

            try {
                const sendUrl = typeof buildApiUrl === "function" ? buildApiUrl("/api/auth/otp/send") : "/api/auth/otp/send";
                const res = await fetch(sendUrl, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ identifier: email, type: "EMAIL", purpose: "REGISTRATION" })
                });

                const data = await res.json();
                if (!res.ok) {
                    throw new Error(data.message || "Failed to send email OTP.");
                }

                if (emailOtpGroup) emailOtpGroup.style.display = "block";
                showToast(`Verification code sent to ${email}!`, "info");

                // Start cooldown
                startCooldown(sendEmailOtpBtn, "Send OTP", 60);

            } catch (err) {
                console.error("Email OTP error:", err);
                showToast(err.message || "Unable to send email OTP.", "error");
                sendEmailOtpBtn.disabled = false;
                sendEmailOtpBtn.textContent = "Send OTP";
            }
        });
    }

    if (verifyEmailOtpBtn) {
        verifyEmailOtpBtn.addEventListener("click", async function () {
            const emailInput = document.getElementById("email");
            const otpInput = document.getElementById("emailOtp");
            const email = emailInput ? emailInput.value.trim() : "";
            const code = otpInput ? otpInput.value.trim() : "";

            if (!code || code.length !== 6) {
                showToast("Please enter the 6-digit code sent to your email.", "warning");
                return;
            }

            verifyEmailOtpBtn.disabled = true;
            verifyEmailOtpBtn.textContent = "...";

            try {
                const verifyUrl = typeof buildApiUrl === "function" ? buildApiUrl("/api/auth/otp/verify") : "/api/auth/otp/verify";
                const res = await fetch(verifyUrl, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ identifier: email, type: "EMAIL", code: code, purpose: "REGISTRATION" })
                });
                const data = await res.json();
                if (!res.ok) {
                    throw new Error(data.message || "Invalid email OTP.");
                }

                emailVerified = true;
                if (emailVerifiedBadge) emailVerifiedBadge.style.display = "inline";
                verifyEmailOtpBtn.style.background = "#16a34a";
                verifyEmailOtpBtn.textContent = "Verified ✓";
                verifyEmailOtpBtn.disabled = true;
                otpInput.disabled = true;
                emailInput.readOnly = true;

                showToast("Email address verified successfully!", "success");

            } catch (err) {
                console.error("Email OTP verify error:", err);
                showToast(err.message || "Invalid verification code.", "error");
                verifyEmailOtpBtn.disabled = false;
                verifyEmailOtpBtn.textContent = "Verify";
            }
        });
    }

    // 2. Phone OTP
    const sendPhoneOtpBtn = document.getElementById("sendPhoneOtpBtn");
    const verifyPhoneOtpBtn = document.getElementById("verifyPhoneOtpBtn");
    const phoneOtpGroup = document.getElementById("phoneOtpGroup");
    const phoneVerifiedBadge = document.getElementById("phoneVerifiedBadge");

    if (sendPhoneOtpBtn) {
        sendPhoneOtpBtn.addEventListener("click", async function () {
            const phoneInput = document.getElementById("phone");
            const phone = phoneInput ? phoneInput.value.trim() : "";
            if (!phone || phone.length < 8) {
                showToast("Please enter a valid phone number first.", "warning");
                return;
            }

            sendPhoneOtpBtn.disabled = true;
            sendPhoneOtpBtn.textContent = "Sending...";

            try {
                const sendUrl = typeof buildApiUrl === "function" ? buildApiUrl("/api/auth/otp/send") : "/api/auth/otp/send";
                const res = await fetch(sendUrl, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ identifier: phone, type: "PHONE", purpose: "REGISTRATION" })
                });

                const data = await res.json();
                if (!res.ok) {
                    throw new Error(data.message || "Failed to send phone OTP.");
                }

                if (phoneOtpGroup) phoneOtpGroup.style.display = "block";
                showToast(`Verification code sent to ${phone}!`, "info");

                // Start cooldown
                startCooldown(sendPhoneOtpBtn, "Send OTP", 60);

            } catch (err) {
                console.error("Phone OTP error:", err);
                showToast(err.message || "Unable to send phone OTP.", "error");
                sendPhoneOtpBtn.disabled = false;
                sendPhoneOtpBtn.textContent = "Send OTP";
            }
        });
    }

    if (verifyPhoneOtpBtn) {
        verifyPhoneOtpBtn.addEventListener("click", async function () {
            const phoneInput = document.getElementById("phone");
            const otpInput = document.getElementById("phoneOtp");
            const phone = phoneInput ? phoneInput.value.trim() : "";
            const code = otpInput ? otpInput.value.trim() : "";

            if (!code || code.length !== 6) {
                showToast("Please enter the 6-digit code sent to your phone.", "warning");
                return;
            }

            verifyPhoneOtpBtn.disabled = true;
            verifyPhoneOtpBtn.textContent = "...";

            try {
                const verifyUrl = typeof buildApiUrl === "function" ? buildApiUrl("/api/auth/otp/verify") : "/api/auth/otp/verify";
                const res = await fetch(verifyUrl, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ identifier: phone, type: "PHONE", code: code, purpose: "REGISTRATION" })
                });
                const data = await res.json();
                if (!res.ok) {
                    throw new Error(data.message || "Invalid phone OTP.");
                }

                phoneVerified = true;
                if (phoneVerifiedBadge) phoneVerifiedBadge.style.display = "inline";
                verifyPhoneOtpBtn.style.background = "#16a34a";
                verifyPhoneOtpBtn.textContent = "Verified ✓";
                verifyPhoneOtpBtn.disabled = true;
                otpInput.disabled = true;
                phoneInput.readOnly = true;

                showToast("Phone number verified successfully!", "success");

            } catch (err) {
                console.error("Phone OTP verify error:", err);
                showToast(err.message || "Invalid verification code.", "error");
                verifyPhoneOtpBtn.disabled = false;
                verifyPhoneOtpBtn.textContent = "Verify";
            }
        });
    }
}

function startCooldown(button, originalText, seconds) {
    let remaining = seconds;
    const interval = setInterval(() => {
        remaining--;
        if (remaining > 0) {
            button.textContent = `Wait ${remaining}s`;
        } else {
            clearInterval(interval);
            button.disabled = false;
            button.textContent = originalText;
        }
    }, 1000);
}

if (signupForm) {
    signupForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const name = document.getElementById("name").value.trim();
        const email = document.getElementById("email").value.trim();
        const phone = document.getElementById("phone").value.trim();
        const password = document.getElementById("password").value;
        const confirmPassword = document.getElementById("confirmPassword").value;
        const terms = document.getElementById("terms").checked;

        const emailOtpInput = document.getElementById("emailOtp");
        const phoneOtpInput = document.getElementById("phoneOtp");
        const emailOtp = emailOtpInput ? emailOtpInput.value.trim() : "";
        const phoneOtp = phoneOtpInput ? phoneOtpInput.value.trim() : "";

        if (!name || !email || !password || !confirmPassword) {
            showToast("Please fill in all required fields.", "warning");
            return;
        }

        if (password.length < 12) {
            showToast("Password must contain at least 12 characters.", "warning");
            return;
        }

        if (password !== confirmPassword) {
            showToast("Passwords do not match.", "warning");
            return;
        }

        if (!terms) {
            showToast("Please agree to the BloodBridge terms to continue.", "warning");
            return;
        }

        const submitButton = signupForm.querySelector('button[type="submit"]');
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "Creating Account...";
        }

        try {
            // 1. Register with OTP payload
            const registerPayload = {
                fullName: name,
                email: email,
                password: password,
                phone: phone,
                emailOtp: emailOtp || null,
                phoneOtp: phoneOtp || null
            };

            const regUrl = typeof buildApiUrl === "function" ? buildApiUrl("/api/auth/register") : "/api/auth/register";
            const registerResponse = await fetch(regUrl, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "include",
                body: JSON.stringify(registerPayload)
            });

            const registerBody = await registerResponse.json().catch(() => ({}));

            if (!registerResponse.ok) {
                const msg = registerBody.message || (registerBody.fieldErrors ? Object.values(registerBody.fieldErrors)[0] : "Unable to create account.");
                showToast(msg, "error");
                return;
            }

            // 2. Automatically Log in
            const loginUrl = typeof buildApiUrl === "function" ? buildApiUrl("/api/auth/login") : "/api/auth/login";
            const loginResponse = await fetch(loginUrl, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "include",
                body: JSON.stringify({ email: email, password: password })
            });

            if (!loginResponse.ok) {
                showToast("Account created! Please sign in with your password.", "info");
                setTimeout(() => window.location.href = "login.html", 1000);
                return;
            }

            showToast("Account created successfully! Welcome to BloodBridge.", "success");

            // Direct new user to setup donor profile
            setTimeout(() => {
                window.location.href = "donor.html";
            }, 800);

        } catch (error) {
            console.error("Account creation failed:", error);
            showToast("Unable to create account right now. Please try again.", "error");
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = "Create Account";
            }
        }
    });
}
