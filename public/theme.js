// ============================================================
// BLOODBRIDGE THEME MANAGEMENT & COMMON UI UTILITIES
// ============================================================

(function () {
    const STORAGE_KEY = "bloodbridge_theme";

    function getSavedTheme() {
        try {
            return localStorage.getItem(STORAGE_KEY);
        } catch (_) {
            return null;
        }
    }

    function getSystemTheme() {
        if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
            return "dark";
        }
        return "light";
    }

    function getEffectiveTheme() {
        return getSavedTheme() || getSystemTheme();
    }

    function updateToggleButtons(theme) {
        const isDark = theme === "dark";
        const icon = isDark ? "☀️" : "🌙";
        const label = isDark ? "Light" : "Dark";
        const title = isDark ? "Switch to day mode" : "Switch to night mode";

        const buttons = document.querySelectorAll(".theme-toggle-btn");
        buttons.forEach((btn) => {
            btn.setAttribute("aria-label", title);
            btn.setAttribute("title", title);
            const iconSpan = btn.querySelector(".theme-icon");
            const labelSpan = btn.querySelector(".theme-label");
            if (iconSpan) iconSpan.textContent = icon;
            if (labelSpan) labelSpan.textContent = label;
            if (!iconSpan && !labelSpan) {
                btn.innerHTML = `<span class="theme-icon">${icon}</span> <span class="theme-label" style="font-size:12px;">${label}</span>`;
            }
        });
    }

    function applyTheme(theme, persist = true) {
        document.documentElement.setAttribute("data-theme", theme);
        if (persist) {
            try {
                localStorage.setItem(STORAGE_KEY, theme);
            } catch (_) {}
        }
        updateToggleButtons(theme);

        // Notify Chart.js charts if present
        if (window.Chart && typeof window.updateChartsForTheme === "function") {
            window.updateChartsForTheme(theme);
        }
    }

    function toggleTheme() {
        const current = document.documentElement.getAttribute("data-theme") || getEffectiveTheme();
        const next = current === "dark" ? "light" : "dark";
        applyTheme(next, true);
    }

    // Expose functions globally
    window.toggleTheme = toggleTheme;
    window.applyTheme = applyTheme;
    window.getEffectiveTheme = getEffectiveTheme;

    // Listen for OS theme changes if user has not set an explicit override
    if (window.matchMedia) {
        window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (e) => {
            if (!getSavedTheme()) {
                applyTheme(e.matches ? "dark" : "light", false);
            }
        });
    }

    // Initialize as soon as DOM is ready
    document.addEventListener("DOMContentLoaded", () => {
        const initial = getEffectiveTheme();
        applyTheme(initial, false);

        // Setup dynamic copyright year
        const copyYearElements = document.querySelectorAll(".copyright-year, #copyrightYear");
        const currentYear = new Date().getFullYear();
        copyYearElements.forEach((el) => {
            el.textContent = currentYear;
        });

        // Setup mobile menu
        setupMobileMenu();

        // Setup logo destination
        setupLogoLink();
    });

    function setupMobileMenu() {
        const toggleBtn = document.getElementById("mobileMenuToggle");
        const drawer = document.getElementById("mobileNavDrawer");

        if (!toggleBtn || !drawer) return;

        toggleBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            drawer.classList.toggle("open");
            const isOpen = drawer.classList.contains("open");
            toggleBtn.setAttribute("aria-expanded", isOpen ? "true" : "false");
            toggleBtn.textContent = isOpen ? "✕" : "☰";
        });

        // Close on link click
        drawer.querySelectorAll("a").forEach((link) => {
            link.addEventListener("click", () => {
                drawer.classList.remove("open");
                toggleBtn.textContent = "☰";
            });
        });

        // Close on click outside
        document.addEventListener("click", (e) => {
            if (drawer.classList.contains("open") && !drawer.contains(e.target) && e.target !== toggleBtn) {
                drawer.classList.remove("open");
                toggleBtn.textContent = "☰";
            }
        });
    }

    function setupLogoLink() {
        const logoLinks = document.querySelectorAll(".logo, #logoLink");
        let hasUser = false;
        try {
            hasUser = !!sessionStorage.getItem("bloodbridge_user");
        } catch (_) {}

        logoLinks.forEach((link) => {
            // If user is logged in and not on index, clicking logo navigates to dashboard
            if (hasUser) {
                const currentPage = window.location.pathname.split("/").pop();
                if (currentPage && currentPage !== "index.html" && currentPage !== "") {
                    link.setAttribute("href", "dashboard.html");
                }
            }
        });
    }
})();
