const form = document.querySelector("#search-form");
const cityInput = document.querySelector("#city-input");
const searchButton = document.querySelector("#search-button");
const voiceButton = document.querySelector("#voice-button");
const notice = document.querySelector("#notice");
const currentPanel = document.querySelector("#current-panel");
const emptyPrompt = document.querySelector("#empty-prompt");
const emptyForecast = document.querySelector("#forecast-preview, #empty-forecast, .empty-forecast");
const emptyForecastTitle = emptyForecast?.querySelector(".empty-forecast-title");
const emptyForecastMessage = emptyForecast?.querySelector("p:not(.empty-forecast-title)");
const loadingCurrent = document.querySelector("#loading-current");
const hourlySection = document.querySelector("#hourly");
const dailySection = document.querySelector("#forecast");
const forecastLink = document.querySelector("#forecast-nav");
const themeToggle = document.querySelector("#theme-toggle");
const currentIcon = document.querySelector("#current-icon");
const chatForm = document.querySelector("#chat-form");
const chatInput = document.querySelector("#chat-input");
const chatSend = document.querySelector("#chat-send");
const chatMessages = document.querySelector("#chat-messages");
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

let activeRequest;
let recognition;
let isListening = false;
let speechError = false;
const chatHistory = [];
let liveWeatherContext = "";

function setText(selector, text) {
  document.querySelector(selector).textContent = text;
}

function showNotice(message, isError = false) {
  notice.textContent = message;
  notice.classList.toggle("error", isError);
  notice.hidden = !message;
}

function setLoading(isLoading) {
  searchButton.disabled = isLoading;
  voiceButton.disabled = isLoading;
  searchButton.textContent = isLoading ? "Loading" : "Search";
  loadingCurrent.hidden = !isLoading;
  currentPanel.setAttribute("aria-busy", String(isLoading));
}

function normalizeCityRequest(request) {
  return request
    .trim()
    .replace(/^[\s,.;:!?]+|[\s,.;:!?]+$/g, "")
    .replace(
      /^(?:(?:what is|what's|tell me|show me|get|check)\s+)?(?:the\s+)?(?:weather(?:\s+(?:like|forecast))?|forecast|temperature)\s+(?:in|for|at)\s+/i,
      "",
    )
    .replace(
      /\s+(?:(?:weather|forecast)(?:\s+(?:for\s+)?today)?|today|tomorrow|please)$/i,
      "",
    )
    .replace(/^[\s,.;:!?]+|[\s,.;:!?]+$/g, "")
    .trim();
}

function cityDateKey(timestamp, offset) {
  return new Date((timestamp + offset) * 1000).toISOString().slice(0, 10);
}

function cityDate(timestamp, offset, options) {
  const adjustedDate = new Date((timestamp + offset) * 1000);
  return new Intl.DateTimeFormat(undefined, {
    ...options,
    timeZone: "UTC",
  }).format(adjustedDate);
}

function weatherIconUrl(code, size = "2x") {
  const safeCode = /^\d{2}[dn]$/.test(code || "") ? code : "01d";
  return `https://openweathermap.org/img/wn/${safeCode}@${size}.png`;
}

function updateCurrentWeather(weather) {
  const offset = weather.timezone || 0;
  const condition = weather.weather?.[0];
  const description = condition?.description || "Conditions unavailable";
  const country = weather.sys?.country ? `, ${weather.sys.country}` : "";

  setText("#city-name", weather.name || "Weather");
  setText("#current-date", cityDate(weather.dt, offset, {
    weekday: "long",
    month: "long",
    day: "numeric",
  }));
  setText("#temperature", Math.round(weather.main.temp));
  document.querySelector("#temperature").setAttribute(
    "aria-label",
    `${Math.round(weather.main.temp)} degrees Celsius`,
  );
  setText("#condition", description);
  setText("#country-name", `${weather.name || "Your city"}${country}`);
  setText("#feels-like", `${Math.round(weather.main.feels_like)}°C`);
  setText("#humidity", `${Math.round(weather.main.humidity)}%`);
  setText("#wind-speed", `${Number(weather.wind.speed).toFixed(1)} m/s`);
  setText("#pressure", `${weather.main.pressure} hPa`);
  setText("#updated-time", `Updated ${cityDate(weather.dt, offset, {
    hour: "numeric",
    minute: "2-digit",
  })}`);

  currentIcon.src = weatherIconUrl(condition?.icon);
  currentIcon.alt = condition ? `${description} icon` : "";
  currentIcon.hidden = false;
  currentIcon.onerror = () => {
    currentIcon.hidden = true;
  };
}

function createWeatherImage(condition, size = "2x") {
  const image = document.createElement("img");
  image.src = weatherIconUrl(condition?.icon, size);
  image.alt = "";
  image.setAttribute("aria-hidden", "true");
  image.onerror = () => {
    image.hidden = true;
  };
  return image;
}

function renderHourlyForecast(forecast, weather) {
  const offset = weather.timezone || 0;
  const today = cityDateKey(weather.dt, offset);
  const upcoming = forecast.list
    .filter((item) => cityDateKey(item.dt, offset) === today && item.dt > weather.dt)
    .slice(0, 6);
  const list = document.querySelector("#hourly-list");
  list.replaceChildren();

  if (upcoming.length === 0) {
    hourlySection.hidden = true;
    return;
  }

  for (const item of upcoming) {
    const row = document.createElement("div");
    row.className = "hour-item";

    const time = document.createElement("span");
    time.className = "hour-time";
    time.textContent = cityDate(item.dt, offset, {
      hour: "numeric",
    });

    const condition = item.weather?.[0];
    const temperature = document.createElement("span");
    temperature.className = "hour-temp";
    temperature.textContent = `${Math.round(item.main.temp)}°C`;

    row.append(time, createWeatherImage(condition, "2x"), temperature);
    list.append(row);
  }
  hourlySection.hidden = false;
}

function makeDailyForecast(forecast, weather) {
  const offset = weather.timezone || 0;
  const today = cityDateKey(weather.dt, offset);
  const grouped = new Map();

  for (const item of forecast.list) {
    const key = cityDateKey(item.dt, offset);
    if (!grouped.has(key)) {
      grouped.set(key, []);
    }
    grouped.get(key).push(item);
  }

  return [...grouped.entries()].slice(0, 5).map(([date, items]) => {
    const midday = items.find((item) => {
      const hour = new Date((item.dt + offset) * 1000).getUTCHours();
      return hour >= 11 && hour <= 14;
    }) || items[Math.floor(items.length / 2)];
    const conditions = items.map((item) => item.main);
    const precipitation = Math.max(...items.map((item) => item.pop || 0));

    return {
      date,
      label: date === today
        ? "Today"
        : cityDate(Date.parse(`${date}T00:00:00Z`) / 1000, 0, { weekday: "long" }),
      high: Math.round(Math.max(...conditions.map((item) => item.temp_max))),
      low: Math.round(Math.min(...conditions.map((item) => item.temp_min))),
      precipitation: Math.round(precipitation * 100),
      condition: midday.weather?.[0],
    };
  });
}

function renderDailyForecast(forecast, weather) {
  const days = makeDailyForecast(forecast, weather);
  const list = document.querySelector("#daily-list");
  list.replaceChildren();
  setText("#forecast-location", `Forecast for ${weather.name}. Temperatures in °C.`);

  for (const day of days) {
    const row = document.createElement("div");
    row.className = "day-row";

    const date = document.createElement("div");
    const dayName = document.createElement("span");
    dayName.className = "day-name";
    dayName.textContent = day.label;
    const dayDate = document.createElement("span");
    dayDate.className = "day-date";
    dayDate.textContent = cityDate(Date.parse(`${day.date}T00:00:00Z`) / 1000, 0, {
      month: "short",
      day: "numeric",
    });
    date.append(dayName, dayDate);

    const condition = document.createElement("div");
    condition.className = "day-condition";
    condition.append(createWeatherImage(day.condition, "2x"));
    const conditionText = document.createElement("span");
    conditionText.textContent = day.condition?.description || "Forecast";
    condition.append(conditionText);

    const rain = document.createElement("span");
    rain.className = "day-rain";
    rain.textContent = day.precipitation > 0 ? `Rain ${day.precipitation}%` : "No rain";

    const temperatures = document.createElement("div");
    temperatures.className = "day-temperatures";
    const high = document.createElement("span");
    high.textContent = `${day.high}°`;
    const low = document.createElement("span");
    low.textContent = `${day.low}°`;
    temperatures.append(high, low);

    row.append(date, condition, rain, temperatures);
    list.append(row);
  }
  dailySection.hidden = days.length === 0;
}

function makeWeatherChatContext(weather, forecast) {
  const condition = weather.weather?.[0]?.description || "unavailable";
  const country = weather.sys?.country ? `, ${weather.sys.country}` : "";
  const days = makeDailyForecast(forecast, weather);

  return [
    "CURRENT SKYCAST WEATHER DATA from OpenWeatherMap:",
    `Location: ${weather.name || "Unknown"}${country}.`,
    `Updated: ${cityDate(weather.dt, weather.timezone || 0, {
      weekday: "long",
      month: "long",
      day: "numeric",
      hour: "numeric",
      minute: "2-digit",
    })}.`,
    `Current conditions: ${Math.round(weather.main.temp)}°C, ${condition}; feels like ${Math.round(weather.main.feels_like)}°C.`,
    `Humidity: ${Math.round(weather.main.humidity)}%. Wind: ${Number(weather.wind.speed).toFixed(1)} m/s. Pressure: ${weather.main.pressure} hPa.`,
    "Forecast:",
    ...days.map((day) =>
      `${day.label} (${day.date}): ${day.condition?.description || "conditions unavailable"}, high ${day.high}°C, low ${day.low}°C, precipitation ${day.precipitation}%.`,
    ),
    "Use this latest data to answer questions about the weather. Do not claim you lack live weather data when this context is present. Do not invent values that are not listed.",
  ].join("\n");
}

function speakWeather(weather, forecast) {
  if (!window.speechSynthesis || typeof SpeechSynthesisUtterance === "undefined") {
    return false;
  }

  const condition = weather.weather?.[0]?.description || "conditions unavailable";
  const summary = [
    `In ${weather.name}, it is ${Math.round(weather.main.temp)} degrees Celsius with ${condition}.`,
    `Humidity is ${Math.round(weather.main.humidity)} percent.`,
    `Wind speed is ${Number(weather.wind.speed).toFixed(1)} meters per second.`,
  ];
  const nextDay = makeDailyForecast(forecast, weather)
    .find((day) => day.label !== "Today");
  if (nextDay) {
    summary.push(
      `The next forecast is ${nextDay.label}, with a high of ${nextDay.high} and a low of ${nextDay.low} degrees Celsius.`,
    );
  }

  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(summary.join(" "));
  utterance.lang = document.documentElement.lang || navigator.language || "en-US";
  window.speechSynthesis.speak(utterance);
  return true;
}

async function loadCity(city, { speak = false } = {}) {
  activeRequest?.abort();
  activeRequest = new AbortController();
  const { signal } = activeRequest;
  setLoading(true);
  showNotice(`Finding the latest weather for ${city}…`);

  try {
    const query = `?city=${encodeURIComponent(city)}`;
    const [weatherResponse, forecastResponse] = await Promise.all([
      fetch(`/api/weather${query}`, { signal, headers: { Accept: "application/json" } }),
      fetch(`/api/forecast${query}`, { signal, headers: { Accept: "application/json" } }),
    ]);
    const [weather, forecast] = await Promise.all([
      readResponse(weatherResponse),
      readResponse(forecastResponse),
    ]);

    updateCurrentWeather(weather);
    renderHourlyForecast(forecast, weather);
    renderDailyForecast(forecast, weather);
    liveWeatherContext = makeWeatherChatContext(weather, forecast);
    if (dailySection.hidden) {
      if (emptyForecastTitle) emptyForecastTitle.textContent = "No forecast is available right now.";
      if (emptyForecastMessage) emptyForecastMessage.textContent = "Try another city or check again later.";
      if (emptyForecast) emptyForecast.hidden = false;
      forecastLink.href = "#forecast-preview";
    } else {
      if (emptyForecast) emptyForecast.hidden = true;
      forecastLink.href = "#forecast";
    }
    cityInput.value = weather.name || city;
    emptyPrompt.hidden = true;
    showNotice("");
    if (speak && !speakWeather(weather, forecast)) {
      showNotice("Weather loaded, but spoken responses are not available in this browser.");
    }
  } catch (error) {
    if (error.name === "AbortError") {
      return;
    }
    const message = error instanceof TypeError
      ? "Could not reach the weather service. Check your connection and try again."
      : error.message;
    showNotice(message, true);
  } finally {
    if (!signal.aborted) {
      setLoading(false);
    }
  }
}

async function readResponse(response) {
  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload.error || "Weather data could not be loaded.");
  }
  return payload;
}

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const city = normalizeCityRequest(cityInput.value);
  if (!city) {
    showNotice("Enter a city name to search.", true);
    cityInput.focus();
    return;
  }
  cityInput.value = city;
  loadCity(city);
});

if (SpeechRecognition) {
  recognition = new SpeechRecognition();
  recognition.lang = document.documentElement.lang || navigator.language || "en-US";
  recognition.interimResults = false;
  recognition.maxAlternatives = 1;

  recognition.onstart = () => {
    isListening = true;
    speechError = false;
    voiceButton.classList.add("listening");
    voiceButton.setAttribute("aria-pressed", "true");
    voiceButton.setAttribute("aria-label", "Stop voice search");
    voiceButton.textContent = "Stop";
    showNotice("Listening. Say a city or ask for its weather.");
  };

  recognition.onresult = (event) => {
    const request = event.results[0]?.[0]?.transcript || "";
    const city = normalizeCityRequest(request);
    if (!city) {
      showNotice("No city was heard. Try again or type a city.", true);
      return;
    }
    cityInput.value = city;
    loadCity(city, { speak: true });
  };

  recognition.onerror = (event) => {
    speechError = true;
    const messages = {
      "audio-capture": "No microphone was found. Check your microphone and try again.",
      "no-speech": "No speech was detected. Try again or type a city.",
      "not-allowed": "Microphone access was blocked. Allow access in your browser settings.",
      "service-not-allowed": "Voice search is unavailable. Try again or type a city.",
    };
    showNotice(
      messages[event.error] || "Voice search stopped unexpectedly. Try again or type a city.",
      true,
    );
  };

  recognition.onend = () => {
    isListening = false;
    voiceButton.classList.remove("listening");
    voiceButton.setAttribute("aria-pressed", "false");
    voiceButton.setAttribute("aria-label", "Search by voice");
    voiceButton.textContent = "Speak";
    voiceButton.disabled = searchButton.disabled;
    if (!speechError && notice.textContent === "Listening. Say a city or ask for its weather.") {
      showNotice("No speech was detected. Try again or type a city.", true);
    }
  };

  voiceButton.hidden = false;
  voiceButton.addEventListener("click", () => {
    if (isListening) {
      recognition.stop();
      return;
    }
    try {
      recognition.start();
    } catch {
      showNotice("Voice search could not start. Try again or type a city.", true);
    }
  });
} else {
  voiceButton.hidden = true;
}

document.querySelectorAll("[data-city]").forEach((button) => {
  button.addEventListener("click", () => {
    const city = button.dataset.city;
    cityInput.value = city;
    loadCity(city);
  });
});

themeToggle.addEventListener("click", () => {
  const currentTheme = document.documentElement.dataset.theme
    || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
  const nextTheme = currentTheme === "dark" ? "light" : "dark";
  document.documentElement.dataset.theme = nextTheme;
  themeToggle.setAttribute(
    "aria-label",
    `Switch to ${nextTheme === "dark" ? "light" : "dark"} appearance`,
  );
});

cityInput.value = "Chennai";
loadCity(cityInput.value);

function addChatMessage(role, text, extraClass = "") {
  const message = document.createElement("p");
  message.className = `chat-message ${role}${extraClass ? ` ${extraClass}` : ""}`;
  message.textContent = text;
  chatMessages.append(message);
  chatMessages.scrollTop = chatMessages.scrollHeight;
  return message;
}

chatForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const message = chatInput.value.trim();
  if (!message || chatSend.disabled) {
    return;
  }

  addChatMessage("user", message);
  chatInput.value = "";
  chatInput.disabled = true;
  chatSend.disabled = true;
  chatSend.textContent = "Thinking…";
  const thinkingMessage = addChatMessage("assistant", "Thinking…", "thinking");
  const transcript = [
    liveWeatherContext,
    ...chatHistory.map((turn) => `${turn.role === "user" ? "User" : "Assistant"}: ${turn.content}`),
    `User: ${message}`,
  ].filter(Boolean)
    .join("\n\n");

  try {
    const response = await fetch("/api/chat", {
      method: "POST",
      headers: { "Content-Type": "text/plain; charset=utf-8", Accept: "application/json" },
      body: transcript,
    });
    const payload = await readResponse(response);
    if (typeof payload.message?.content !== "string") {
      throw new Error("Ollama returned an unexpected response. Check the server logs and try again.");
    }

    const reply = payload.message.content.trim();
    if (!reply) {
      throw new Error("Qwen returned an empty response. Please try again.");
    }
    thinkingMessage.textContent = reply;
    thinkingMessage.classList.remove("thinking");
    chatHistory.push({ role: "user", content: message }, { role: "assistant", content: reply });
    if (chatHistory.length > 12) {
      chatHistory.splice(0, chatHistory.length - 12);
    }
  } catch (error) {
    thinkingMessage.textContent = error instanceof TypeError
      ? "Could not reach the local chat service. Make sure the SkyCast server is running."
      : error.message;
    thinkingMessage.classList.remove("thinking");
    thinkingMessage.classList.add("error");
  } finally {
    chatInput.disabled = false;
    chatSend.disabled = false;
    chatSend.textContent = "Send";
    chatInput.focus();
  }
});
