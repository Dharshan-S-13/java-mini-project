
const form = document.getElementById("search-form");
const cityInput = document.getElementById("city-input");
const searchButton = document.getElementById("search-button");
const statusMessage = document.getElementById("status");
const weatherContent = document.getElementById("weather-content");
const chatForm = document.getElementById("chat-form");
const chatInput = document.getElementById("chat-input");
const chatSend = document.getElementById("chat-send");
const chatMessages = document.getElementById("chat-messages");
const chatStatus = document.getElementById("chat-status");

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

function formatDate(dateString) {
    const date = new Date(dateString + "T12:00:00");
    return date.toLocaleDateString("en-IN", {
        weekday: "short",
        day: "numeric",
        month: "short"
    });
}

function weatherSymbol(description) {
    const text = (description || "").toLowerCase();

    if (text.includes("thunder")) return "⛈️";
    if (text.includes("rain") || text.includes("drizzle")) return "🌧️";
    if (text.includes("snow")) return "❄️";
    if (text.includes("cloud")) return "☁️";
    if (text.includes("mist") || text.includes("fog")) return "🌫️";

    return "☀️";
}

function renderToday(readings, description) {
    const list = document.getElementById("today-list");
    const empty = document.getElementById("today-empty");

    if (!readings || readings.length === 0) {
        list.innerHTML = "";
        empty.classList.remove("hidden");
        return;
    }

    empty.classList.add("hidden");

    list.innerHTML = readings.map(reading => `
        <article class="hour-card">
            <div class="hour">${escapeHtml(reading.time)}</div>
            <span class="weather-symbol">${weatherSymbol(description)}</span>
            <div class="hour-temp">${Number(reading.temperature).toFixed(1)}°</div>
        </article>
    `).join("");
}

function renderDaily(days) {
    const list = document.getElementById("daily-list");

    list.innerHTML = days.map((day, index) => {
        const label = index === 0 ? "Tomorrow" : formatDate(day.date);

        const temperature = day.average == null
            ? "—"
            : `${Number(day.average).toFixed(1)}°`;

        return `
            <article class="day-card">
                <div class="day-name">${escapeHtml(label)}</div>
                <div class="day-symbol">🌤️</div>
                <div class="day-temp">${temperature}</div>
                <div class="average-label">Average °C</div>
            </article>
        `;
    }).join("");
}

async function loadWeather(city) {
    const trimmedCity = city.trim();

    if (!trimmedCity) {
        statusMessage.textContent = "Please enter a city name.";
        statusMessage.className = "status error";
        return;
    }

    searchButton.disabled = true;
    statusMessage.className = "status";
    statusMessage.textContent = "Fetching weather for " + trimmedCity + "...";
    weatherContent.classList.add("hidden");

    try {
        const response = await fetch(
            `/api/weather?city=${encodeURIComponent(trimmedCity)}`
        );

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || "Unable to load weather.");
        }

        document.getElementById("city-name").textContent = data.city;
        document.getElementById("current-date").textContent =
            new Date().toLocaleDateString("en-IN", {
                weekday: "long",
                day: "numeric",
                month: "long",
                year: "numeric"
            });

        document.getElementById("description").textContent =
            data.current.description;

        document.getElementById("current-temp").textContent =
            Number(data.current.temperature).toFixed(1);

        document.getElementById("feels-like").textContent =
            Number(data.current.feelsLike).toFixed(1) + "°C";

        document.getElementById("stat-feels").textContent =
            Number(data.current.feelsLike).toFixed(1) + "°C";

        document.getElementById("humidity").textContent =
            Number(data.current.humidity).toFixed(0) + "%";

        document.getElementById("wind-speed").textContent =
            Number(data.current.windSpeed).toFixed(1) + " m/s";

        renderToday(data.today, data.current.description);
        renderDaily(data.daily);

        weatherContent.classList.remove("hidden");
        statusMessage.textContent = "Weather updated successfully.";
    } catch (error) {
        statusMessage.textContent = error.message
            || "Something went wrong. Please try again.";
        statusMessage.className = "status error";
    } finally {
        searchButton.disabled = false;
    }
}

form.addEventListener("submit", event => {
    event.preventDefault();
    loadWeather(cityInput.value);
});

function addChatMessage(message, sender) {
    const item = document.createElement("p");
    item.className = `chat-message ${sender}-message`;

    const label = document.createElement("span");
    label.className = "message-label";
    label.textContent = sender === "user" ? "You" : "SkyCast AI";

    item.append(label, document.createTextNode(message));
    chatMessages.append(item);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

chatForm.addEventListener("submit", async event => {
    event.preventDefault();
    const message = chatInput.value.trim();
    const city = cityInput.value.trim();

    if (!message) return;
    if (!city) {
        chatStatus.textContent = "Enter a city before asking about its weather.";
        chatStatus.className = "chat-status error";
        return;
    }

    addChatMessage(message, "user");
    chatInput.value = "";
    chatSend.disabled = true;
    chatStatus.className = "chat-status";
    chatStatus.textContent = "SkyCast AI is thinking...";

    try {
        const response = await fetch("/api/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message, city })
        });
        let data;
        try {
            data = await response.json();
        } catch {
            throw new Error("The weather assistant returned an unreadable response. Please try again.");
        }

        if (!response.ok || typeof data.reply !== "string" || !data.reply.trim()) {
            throw new Error(data.error || "I couldn't answer just now.");
        }

        addChatMessage(data.reply, "assistant");
        chatStatus.textContent = "";
    } catch (error) {
        const friendlyMessage = error instanceof Error
                && error.name === "Error" && error.message
            ? error.message
            : "I couldn't reach the weather assistant. Please try again.";
        addChatMessage(friendlyMessage, "assistant");
        chatStatus.textContent = friendlyMessage;
        chatStatus.className = "chat-status error";
    } finally {
        chatSend.disabled = false;
        chatInput.focus();
    }
});

loadWeather("Chennai");