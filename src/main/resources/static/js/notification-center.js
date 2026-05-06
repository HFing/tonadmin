(function () {
    const messageCounter = document.getElementById("messageCounter");
    const alertCounter = document.getElementById("alertCounter");
    const messageList = document.getElementById("messageList");
    const alertList = document.getElementById("alertList");

    if (!messageCounter || !alertCounter || !messageList || !alertList) {
        return;
    }

    function setCounter(element, count) {
        const value = Number(count || 0);

        if (value <= 0) {
            element.classList.add("d-none");
            element.textContent = "0";
            return;
        }

        element.classList.remove("d-none");
        element.textContent = value > 9 ? "9+" : String(value);
    }

    function iconFor(type, channel) {
        if (channel === "ALERT") {
            return { icon: "fas fa-donate", color: "bg-success" };
        }

        if (type === "SALES_ORDER_CREATED") {
            return { icon: "fas fa-shopping-cart", color: "bg-primary" };
        }

        if (type === "STOCK_OUT") {
            return { icon: "fas fa-exclamation-triangle", color: "bg-danger" };
        }

        return { icon: "fas fa-box-open", color: "bg-warning" };
    }

    function escapeHtml(value) {
        const div = document.createElement("div");
        div.textContent = value || "";
        return div.innerHTML;
    }

    function renderItem(item) {
        const icon = iconFor(item.type, item.channel);
        const href = item.id ? `/notifications/${encodeURIComponent(item.id)}/open` : (item.targetUrl || "#");

        return `
            <a class="dropdown-item d-flex align-items-center" href="${escapeHtml(href)}">
                <div class="mr-3">
                    <div class="icon-circle ${icon.color}">
                        <i class="${icon.icon} text-white"></i>
                    </div>
                </div>
                <div>
                    <div class="small text-gray-500">${escapeHtml(item.createdAt)}</div>
                    <div class="font-weight-bold">${escapeHtml(item.title)}</div>
                    <div class="small text-gray-700">${escapeHtml(item.message)}</div>
                </div>
            </a>
        `;
    }

    function renderList(element, items, emptyText) {
        if (!items || items.length === 0) {
            element.innerHTML = `<div class="dropdown-item text-center small text-gray-500">${emptyText}</div>`;
            return;
        }

        element.innerHTML = items.map(renderItem).join("");
    }

    async function refreshNotifications() {
        const response = await fetch("/notifications/summary", {
            headers: { "Accept": "application/json" }
        });

        if (!response.ok) {
            return;
        }

        const data = await response.json();
        setCounter(messageCounter, data.messageCount);
        setCounter(alertCounter, data.alertCount);
        renderList(messageList, data.messages, "Chưa có thông báo mới.");
        renderList(alertList, data.alerts, "Chưa có cảnh báo mới.");
    }

    refreshNotifications().catch(function () {});

    if ("EventSource" in window) {
        const source = new EventSource("/notifications/stream");
        source.addEventListener("notifications", function () {
            refreshNotifications().catch(function () {});
        });
    } else {
        window.setInterval(function () {
            refreshNotifications().catch(function () {});
        }, 30000);
    }
})();
