if (typeof window !== "undefined") {
    window.addEventListener("DOMContentLoaded", () => {
        if (window.location.port === "5500" || window.location.port === "5501" || window.location.protocol === "file:") {
            if (document.getElementById("backend-warning-banner")) return;
            const banner = document.createElement("div");
            banner.id = "backend-warning-banner";
            banner.style.cssText = "position:sticky;top:0;left:0;right:0;background:#991b1b;color:white;text-align:center;padding:10px 16px;z-index:99999;font-size:14px;box-shadow:0 2px 8px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;gap:12px;font-family:inherit;";
            const pageName = window.location.pathname.split("/").pop() || "index.html";
            banner.innerHTML = `<span>⚠️ You opened this page via <strong>VS Code Live Server (port ${window.location.port || 'file'})</strong>. The Spring Boot backend runs on <strong>http://localhost:8080</strong>.</span>
            <a href="http://localhost:8080/${pageName}" style="background:white;color:#991b1b;padding:4px 12px;border-radius:6px;text-decoration:none;font-weight:700;font-size:13px;">Open on http://localhost:8080</a>`;
            document.body.prepend(banner);
        }
    });
}

let _cachedCsrf = null;

async function getCsrfToken() {
    try {
        const response = await fetch("/api/auth/csrf", {
            credentials: "same-origin"
        });
        if (response.ok) {
            _cachedCsrf = await response.json();
            return _cachedCsrf;
        }
    } catch (e) {
        console.warn("Unable to fetch CSRF token:", e);
    }
    return _cachedCsrf;
}

async function apiFetch(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const headers = Object.assign({}, options.headers || {});

    if (["POST", "PUT", "PATCH", "DELETE"].includes(method)) {
        if (!_cachedCsrf) {
            await getCsrfToken();
        }
        if (_cachedCsrf && _cachedCsrf.headerName && _cachedCsrf.token) {
            headers[_cachedCsrf.headerName] = _cachedCsrf.token;
        }
    }

    if (options.body && typeof options.body === "object" && !(options.body instanceof FormData)) {
        headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(options.body);
    }

    const config = {
        ...options,
        method: method,
        headers: headers,
        credentials: "same-origin"
    };

    const response = await fetch(url, config);

    if (response.status === 401 && options.redirectOnUnauthorized !== false) {
        window.location.replace("login.html");
        return null;
    }

    let data = null;
    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
        try {
            data = await response.json();
        } catch (e) {
            data = null;
        }
    }

    if (!response.ok) {
        const errorMsg = (data && (data.message || data.error)) || "Request failed with status " + response.status;
        const error = new Error(errorMsg);
        error.status = response.status;
        error.data = data;
        throw error;
    }

    return data;
}

async function getCurrentUser(redirectIfUnauth = false) {
    try {
        const user = await apiFetch("/api/auth/me", { redirectOnUnauthorized: redirectIfUnauth });
        return user;
    } catch (e) {
        if (redirectIfUnauth) {
            window.location.replace("login.html");
        }
        return null;
    }
}

async function logout() {
    try {
        await apiFetch("/api/auth/logout", { method: "POST", redirectOnUnauthorized: false });
    } catch (e) {
        console.warn("Logout error:", e);
    } finally {
        localStorage.removeItem("bloodBridgeDonor");
        window.location.href = "index.html";
    }
}

function formatBloodGroup(bg) {
    if (!bg) return "—";
    const map = {
        "A_POSITIVE": "A+",
        "A_NEGATIVE": "A-",
        "B_POSITIVE": "B+",
        "B_NEGATIVE": "B-",
        "AB_POSITIVE": "AB+",
        "AB_NEGATIVE": "AB-",
        "O_POSITIVE": "O+",
        "O_NEGATIVE": "O-"
    };
    return map[bg] || bg;
}

function toBloodGroupEnum(display) {
    if (!display) return null;
    const map = {
        "A+": "A_POSITIVE",
        "A-": "A_NEGATIVE",
        "B+": "B_POSITIVE",
        "B-": "B_NEGATIVE",
        "AB+": "AB_POSITIVE",
        "AB-": "AB_NEGATIVE",
        "O+": "O_POSITIVE",
        "O-": "O_NEGATIVE"
    };
    return map[display] || display;
}

function formatDate(dateInput) {
    if (!dateInput) return "—";
    try {
        const d = new Date(dateInput);
        if (isNaN(d.getTime())) return dateInput;
        return d.toLocaleDateString("en-GB", {
            day: "2-digit",
            month: "short",
            year: "numeric"
        });
    } catch (e) {
        return dateInput;
    }
}

function formatTimeAgo(dateInput) {
    if (!dateInput) return "";
    try {
        const d = new Date(dateInput);
        const now = new Date();
        const diffMs = now - d;
        const diffSecs = Math.floor(diffMs / 1000);
        const diffMins = Math.floor(diffSecs / 60);
        const diffHours = Math.floor(diffMins / 60);
        const diffDays = Math.floor(diffHours / 24);

        if (diffDays > 0) return diffDays === 1 ? "1 day ago" : diffDays + " days ago";
        if (diffHours > 0) return diffHours === 1 ? "1 hr ago" : diffHours + " hrs ago";
        if (diffMins > 0) return diffMins + " mins ago";
        return "Just now";
    } catch (e) {
        return "";
    }
}

function showToast(message, type = "success") {
    let container = document.getElementById("toast-container");
    if (!container) {
        container = document.createElement("div");
        container.id = "toast-container";
        container.style.cssText = `
            position: fixed;
            bottom: 24px;
            right: 24px;
            z-index: 99999;
            display: flex;
            flex-direction: column;
            gap: 10px;
            pointer-events: none;
        `;
        document.body.appendChild(container);
    }

    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    const bgColors = {
        success: "#0f5132",
        error: "#842029",
        info: "#055160",
        warning: "#664d03"
    };
    const borders = {
        success: "#badbcc",
        error: "#f5c2c7",
        info: "#b6effb",
        warning: "#ffecb5"
    };

    toast.style.cssText = `
        pointer-events: auto;
        padding: 14px 20px;
        background: ${bgColors[type] || "#1e293b"};
        color: #ffffff;
        border-radius: 10px;
        box-shadow: 0 10px 25px rgba(0,0,0,0.25);
        font-family: inherit;
        font-size: 14px;
        font-weight: 500;
        display: flex;
        align-items: center;
        gap: 12px;
        opacity: 0;
        transform: translateY(20px);
        transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        max-width: 360px;
    `;

    const icon = type === "success" ? "✓" : type === "error" ? "✕" : "ℹ";
    toast.innerHTML = `<span style="font-weight:700;font-size:16px;">${icon}</span> <span>${message}</span>`;
    container.appendChild(toast);

    requestAnimationFrame(() => {
        toast.style.opacity = "1";
        toast.style.transform = "translateY(0)";
    });

    setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transform = "translateY(15px)";
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
