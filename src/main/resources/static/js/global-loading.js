(function () {
    function showLoading() {
        const loading = document.getElementById("globalLoading");
        if (loading) {
            loading.classList.remove("d-none");
        }
    }

    function hideLoading() {
        const loading = document.getElementById("globalLoading");
        if (loading) {
            loading.classList.add("d-none");
        }
    }

    window.showGlobalLoading = showLoading;
    window.hideGlobalLoading = hideLoading;

    document.addEventListener("DOMContentLoaded", function () {
        hideLoading();

        document.querySelectorAll("form").forEach(function (form) {
            form.addEventListener("submit", function () {
                showLoading();
            });
        });

        document.querySelectorAll("a[href]").forEach(function (link) {
            link.addEventListener("click", function () {
                const href = link.getAttribute("href");

                if (!href) return;
                if (href === "#") return;
                if (href.startsWith("#")) return;
                if (href.startsWith("javascript:")) return;
                if (link.getAttribute("target") === "_blank") return;
                if (link.hasAttribute("data-toggle")) return;
                if (link.hasAttribute("data-dismiss")) return;

                showLoading();
            });
        });

        window.addEventListener("pageshow", function () {
            hideLoading();
        });
    });
})();