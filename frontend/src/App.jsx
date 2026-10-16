import { useCallback, useEffect, useRef, useState } from "react";
import AIChat from "./components/AIChat.jsx";
import CurrentWeather from "./components/CurrentWeather.jsx";
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

export default function App() {
  const [city, setCity] = useState("Chennai");
  const [weather, setWeather] = useState(null);
  const [forecast, setForecast] = useState(null);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState({ message: "", error: false });
  const [theme, setTheme] = useState(() => document.documentElement.dataset.theme || "");
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

  function toggleTheme() {
    const currentTheme = theme
      || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
    const nextTheme = currentTheme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = nextTheme;
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
      <header className="site-header">
        <a className="brand" href="/" aria-label="SkyCast home">
          <span className="brand-mark" aria-hidden="true"><span /></span><span>skycast</span>
        </a>
        <nav className="site-nav" aria-label="Main navigation">
          <a className="nav-link active" href="#today">Today</a>
          <a className="nav-link" id="forecast-nav" href={weather ? "#forecast" : "#forecast-preview"}>5-day forecast</a>
        </nav>
        <button
          className="theme-toggle"
          id="theme-toggle"
          type="button"
          aria-label={`Switch to ${theme === "dark" ? "light" : "dark"} appearance`}
          title="Switch appearance"
          onClick={toggleTheme}
        >
          <span className="theme-glyph" aria-hidden="true" />
        </button>
      </header>

      <main>
        <section className="intro" aria-labelledby="page-title">
          <div className="intro-copy">
            <p className="eyebrow">A little more in tune with the day</p>
            <h1 id="page-title">Weather, at a glance.</h1>
            <p className="intro-text">See current conditions and the forecast for the days ahead.</p>
          </div>
          <SearchBar
            city={city}
            setCity={setCity}
            onSearch={(requestedCity) => loadCity(requestedCity)}
            onVoiceSearch={handleVoiceSearch}
            disabled={loading}
          />
        </section>

        {notice.message && (
          <div className={`notice${notice.error ? " error" : ""}`} id="notice" role="status" aria-live="polite">
            {notice.message}
          </div>
        )}
        <CurrentWeather
          weather={weather}
          loading={loading}
          onSelectCity={(suggestedCity) => loadCity(suggestedCity)}
        />
        <Forecast weather={weather} forecast={forecast} />
        <AIChat city={weather?.city || city} />
      </main>

      <footer className="site-footer">
        <span>SkyCast</span><span>Weather information provided by OpenWeatherMap</span>
      </footer>
    </div>
  );
}
