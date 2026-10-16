import { useCallback, useEffect, useRef, useState } from "react";
import AIChat from "./components/AIChat.jsx";
import CurrentWeather, { WeatherImage } from "./components/CurrentWeather.jsx";
import Forecast from "./components/Forecast.jsx";
import SearchBar from "./components/SearchBar.jsx";
import { getForecast, getWeather } from "./services/weatherApi.js";

function speakWeather(weather, forecast) {
  if (!window.speechSynthesis || typeof SpeechSynthesisUtterance === "undefined") {
    return false;
  }
  const summary = [
    `In ${weather.city}, it is ${Math.round(weather.temperature)} degrees Celsius with ${weather.description}.`,
    `Humidity is ${Math.round(weather.humidity)} percent.`,
    `Wind speed is ${Number(weather.windSpeed).toFixed(1)} meters per second.`,
  ];
  const nextEntry = forecast.items.find((item) => {
    const date = new Date((item.timestamp + weather.timezone) * 1000).toISOString().slice(0, 10);
    const today = new Date((weather.timestamp + weather.timezone) * 1000).toISOString().slice(0, 10);
    return date !== today;
  });
  if (nextEntry) {
    summary.push(`An upcoming forecast is ${nextEntry.description}, with a temperature of ${Math.round(nextEntry.temperature)} degrees Celsius.`);
  }
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(summary.join(" "));
  utterance.lang = document.documentElement.lang || navigator.language || "en-US";
  window.speechSynthesis.speak(utterance);
  return true;
}

function weatherMood(icon) {
  const code = Number.parseInt(icon || "", 10);
  if (code === 11) return "thunder";
  if (code === 9 || code === 10) return "rain";
  if (code === 13) return "snow";
  if (code === 50) return "fog";
  if (code === 2 || code === 3 || code === 4) return "cloudy";
  return "clear";
}

function cityDate(timestamp, timezone, options) {
  return new Intl.DateTimeFormat(undefined, {
    ...options,
    timeZone: "UTC",
  }).format(new Date((timestamp + timezone) * 1000));
}

export default function App() {
  const [city, setCity] = useState("Chennai");
  const [weather, setWeather] = useState(null);
  const [forecast, setForecast] = useState(null);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState({ message: "", error: false });
  const [theme, setTheme] = useState(() => document.documentElement.dataset.theme || "dark");
  const requestRef = useRef(null);

  const loadCity = useCallback(async (requestedCity, shouldSpeak = false) => {
    requestRef.current?.abort();
    const controller = new AbortController();
    requestRef.current = controller;
    setCity(requestedCity);
    setLoading(true);
    setNotice({ message: `Finding the latest weather for ${requestedCity}…`, error: false });
    try {
      const [current, upcoming] = await Promise.all([
        getWeather(requestedCity, controller.signal),
        getForecast(requestedCity, controller.signal),
      ]);
      setWeather(current);
      setForecast(upcoming);
      setCity(current.city || requestedCity);
      setNotice({ message: "", error: false });
      if (shouldSpeak && !speakWeather(current, upcoming)) {
        setNotice({ message: "Weather loaded, but spoken responses are not available in this browser.", error: false });
      }
    } catch (error) {
      if (error.name !== "AbortError") {
        setNotice({ message: error.message, error: true });
      }
    } finally {
      if (!controller.signal.aborted) setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCity("Chennai");
    return () => requestRef.current?.abort();
  }, [loadCity]);

  useEffect(() => {
    if (!weather) return undefined;
    document.documentElement.dataset.weather = weatherMood(weather.icon);
    document.documentElement.dataset.period = weather.icon?.endsWith("n") ? "night" : "day";
    return () => {
      delete document.documentElement.dataset.weather;
      delete document.documentElement.dataset.period;
    };
  }, [weather]);

  function toggleTheme() {
    const currentTheme = theme
      || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
    const nextTheme = currentTheme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = nextTheme;
    document.querySelector('meta[name="theme-color"]')?.setAttribute(
      "content",
      nextTheme === "dark" ? "#171c20" : "#eaf3f7",
    );
    setTheme(nextTheme);
  }

  const handleVoiceSearch = useCallback((voiceCity, errorMessage) => {
    if (errorMessage) {
      setNotice({ message: errorMessage, error: true });
    } else if (voiceCity) {
      loadCity(voiceCity, true);
    } else {
      setNotice({ message: "Listening. Say a city or ask for its weather.", error: false });
    }
  }, [loadCity]);

  return (
    <div className="page-shell">
      <div className="weather-app">
        <div className="weather-scene-stack" aria-hidden="true">
          <img
            className="weather-scene weather-scene--clear"
            src="/images/sky-clouds-and-atmosphere.jpg"
            alt=""
            width="1920"
            height="1280"
          />
          <img
            className="weather-scene weather-scene--cloudy"
            src="/images/sky-forest-clouds.jpg"
            alt=""
            width="1920"
            height="1347"
          />
          <img
            className="weather-scene weather-scene--rain"
            src="/images/sky-clouds-sea.jpg"
            alt=""
            width="1920"
            height="1280"
          />
          <img
            className="weather-scene weather-scene--storm"
            src="/images/port-and-lighthouse-overnight-storm-with-lightning.jpg"
            alt=""
            width="1920"
            height="1080"
          />
        </div>
        <aside className="weather-sidebar" aria-label="Current conditions">
          <a className="brand" href="/" aria-label="VaanilAI home">
            <img
              className="brand-logo"
              src="/images/vaanilai-weather-tech-logo.png"
              alt=""
              width="365"
              height="119"
            />
          </a>
          <CurrentWeather
            weather={weather}
            loading={loading}
            onSelectCity={(suggestedCity) => loadCity(suggestedCity)}
          />
          <AIChat city={weather?.city || city} />
          <p className="sidebar-caption">
            {weather
              ? `Updated ${cityDate(weather.timestamp, weather.timezone, { hour: "numeric", minute: "2-digit" })} · Live conditions by OpenWeatherMap`
              : "Live conditions by OpenWeatherMap"}
          </p>
        </aside>

        <main className="dashboard-main" id="main-content">
          <header className="main-toolbar">
            <SearchBar
              city={city}
              setCity={setCity}
              onSearch={(requestedCity) => loadCity(requestedCity)}
              onVoiceSearch={handleVoiceSearch}
              disabled={loading}
            />
            <nav className="dashboard-nav" aria-label="Weather sections">
              <a href="#forecast" id="forecast-nav">Forecast</a>
              <a href="#assistant">AI assistant</a>
            </nav>
            <button
              className="theme-toggle"
              id="theme-toggle"
              type="button"
              aria-label={`Switch to ${theme === "dark" ? "light" : "dark"} appearance`}
              title={`Switch to ${theme === "dark" ? "light" : "dark"} mode`}
              onClick={toggleTheme}
            >
              <svg className="theme-glyph" viewBox="0 0 24 24" aria-hidden="true">
                {theme === "dark" ? (
                  <g>
                    <circle cx="12" cy="12" r="4" />
                    <path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42" />
                  </g>
                ) : (
                  <path d="M20.2 15.2A8.6 8.6 0 0 1 8.8 3.8 8.8 8.8 0 1 0 20.2 15.2Z" />
                )}
              </svg>
            </button>
          </header>

          {notice.message && (
            <div className={`notice${notice.error ? " error" : ""}`} id="notice" role="status" aria-live="polite">
              {notice.message}
            </div>
          )}

          <section className="weather-hero" id="today" aria-busy={loading} aria-label="Weather forecast">
            <div className="hero-copy">
              <h1 id="page-title">
                {weather?.description || "Weather forecast"}
                {weather?.city && (
                  <span className="hero-location">
                    {" · "}
                    {weather.city}
                    {weather.country ? `, ${weather.country}` : ""}
                  </span>
                )}
              </h1>
              <p className="hero-summary">
                {weather
                  ? `Feels like ${Math.round(weather.feelsLike)}°. Wind ${Number(weather.windSpeed * 3.6).toFixed(1)} km/h. Pressure ${weather.pressure} hPa.`
                  : "Search a city to see its current conditions and forecast."}
              </p>
              {weather && (
                <p className="hero-precipitation">
                  Next forecast: {forecast?.items?.[0]
                    ? `${Math.round(forecast.items[0].precipitation * 100)}% chance of precipitation`
                    : "precipitation data unavailable"}
                </p>
              )}
            </div>
            {weather && (
              <div className="hero-icon" aria-hidden="true">
                <WeatherImage icon={weather.icon} description={weather.description} />
              </div>
            )}
          </section>
          <Forecast weather={weather} forecast={forecast} />
          <Forecast weather={weather} forecast={forecast} />
          <footer className="dashboard-footer">
            <span>Weather data by OpenWeatherMap</span>
            <span className="image-credits">
              Images:
              {" "}
              <a
                href="https://commons.wikimedia.org/wiki/File:Port_and_lighthouse_overnight_storm_with_lightning_in_Port-la-Nouvelle.jpg"
                target="_blank"
                rel="noreferrer"
              >
                Storm · Maxime Raynal · CC BY 2.0
              </a>
              {" · "}
              <a
                href="https://commons.wikimedia.org/wiki/File:Sky_Clouds_Sea.jpg"
                target="_blank"
                rel="noreferrer"
              >
                Sea clouds · CC0
              </a>
              {" · "}
              <a
                href="https://commons.wikimedia.org/wiki/File:Sky_Clouds_and_Atmosphere.jpg"
                target="_blank"
                rel="noreferrer"
              >
                Cloud atmosphere · CC0
              </a>
              {" · "}
              <a
                href="https://commons.wikimedia.org/wiki/File:Sky_Forest_Clouds.jpg"
                target="_blank"
                rel="noreferrer"
              >
                Forest clouds · CC0
              </a>
            </span>
          </footer>
        </main>
      </div>
    </div>
  );
}
