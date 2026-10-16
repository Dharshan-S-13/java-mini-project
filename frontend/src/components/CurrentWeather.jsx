function weatherIconUrl(code, size = "2x") {
  const safeCode = /^\d{2}[dn]$/.test(code || "") ? code : "01d";
  return `https://openweathermap.org/img/wn/${safeCode}@${size}.png`;
}

function cityDate(timestamp, offset, options) {
  return new Intl.DateTimeFormat(undefined, {
    ...options,
    timeZone: "UTC",
  }).format(new Date((timestamp + offset) * 1000));
}

function WeatherImage({ icon, description, size = "2x" }) {
  return (
    <img
      src={weatherIconUrl(icon, size)}
      alt={description ? `${description} icon` : ""}
      aria-hidden={description ? undefined : "true"}
      onError={(event) => { event.currentTarget.hidden = true; }}
    />
  );
}

export default function CurrentWeather({ weather, loading, onSelectCity }) {
  const offset = weather?.timezone || 0;

  return (
    <section className="weather-layout" id="today" aria-label="Current weather">
      <article className="current-panel" id="current-panel" aria-busy={loading}>
        <div className="current-copy">
          <div className="current-heading">
            <p className="eyebrow">Right now</p>
            <h2 id="city-name">{weather?.city || "Your day starts here"}</h2>
            <p className="current-date" id="current-date">
              {weather
                ? cityDate(weather.timestamp, offset, {
                  weekday: "long", month: "long", day: "numeric",
                })
                : "Search for a city to see local conditions."}
            </p>
          </div>
          <div className="current-reading">
            <span className="temperature-group">
              <span className="temperature" id="temperature" aria-label={
                weather ? `${Math.round(weather.temperature)} degrees Celsius` : undefined
              }>
                {weather ? Math.round(weather.temperature) : "--"}
              </span>
              <span className="temperature-unit" aria-hidden="true">°C</span>
            </span>
            <div className="condition-copy">
              <p id="condition">{weather?.description || "Search to see local weather."}</p>
              <span id="country-name">
                {weather
                  ? `${weather.city}${weather.country ? `, ${weather.country}` : ""}`
                  : "Your next forecast is one search away."}
              </span>
            </div>
          </div>
        </div>
        <div className="weather-art" id="weather-art">
          {weather && <WeatherImage icon={weather.icon} description={weather.description} />}
        </div>
        {!weather && (
          <div className="empty-prompt" id="empty-prompt">
            <p>Try a city you know</p>
            <div className="city-suggestions" aria-label="Example city searches">
              {["Chennai", "London", "Tokyo"].map((suggestion) => (
                <button key={suggestion} type="button" onClick={() => onSelectCity(suggestion)}>
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        )}
        {loading && (
          <div className="loading-current" id="loading-current" aria-hidden="true">
            <span className="skeleton skeleton-line" />
            <span className="skeleton skeleton-temp" />
            <span className="skeleton skeleton-line short" />
          </div>
        )}
      </article>

      <aside className="details-panel" aria-label="Weather details">
        <div className="details-heading">
          <h2>Today’s details</h2>
          <span>{weather
            ? `Updated ${cityDate(weather.timestamp, offset, { hour: "numeric", minute: "2-digit" })}`
            : "Local conditions"}</span>
        </div>
        <dl className="detail-list">
          <div className="detail-item"><dt>Feels like</dt><dd>{weather ? `${Math.round(weather.feelsLike)}°C` : "--"}</dd></div>
          <div className="detail-item"><dt>Humidity</dt><dd>{weather ? `${Math.round(weather.humidity)}%` : "--"}</dd></div>
          <div className="detail-item"><dt>Wind</dt><dd>{weather ? `${weather.windSpeed.toFixed(1)} m/s` : "--"}</dd></div>
          <div className="detail-item"><dt>Pressure</dt><dd>{weather ? `${weather.pressure} hPa` : "--"}</dd></div>
        </dl>
      </aside>
    </section>
  );
}

export { WeatherImage };
