import { useId } from "react";

function WeatherImage({ icon, description, className = "" }) {
  const gradientPrefix = useId().replace(/:/g, "");
  const match = /^(\d{2})([dn])$/.exec(icon || "");
  const code = match ? Number(match[1]) : 1;
  const night = match?.[2] === "n";
  const storm = code === 11;
  const rainy = code === 9 || code === 10;
  const snowy = code === 13;
  const foggy = code === 50;
  const partlyCloudy = code === 2;
  const cloudy = code === 3 || code === 4 || rainy || snowy || storm || foggy;

  return (
    <svg
      className={`weather-icon ${className}`.trim()}
      viewBox="0 0 64 64"
      role="img"
      aria-label={description ? `${description} icon` : "Weather icon"}
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
    >
      <defs>
        <radialGradient id={`${gradientPrefix}-sun`} cx="34%" cy="28%" r="74%">
          <stop offset="0%" stopColor="#fff6cf" />
          <stop offset="55%" stopColor="#ffd46f" />
          <stop offset="100%" stopColor="#ed963e" />
        </radialGradient>
        <linearGradient id={`${gradientPrefix}-moon`} x1="18%" y1="15%" x2="78%" y2="88%">
          <stop stopColor="#fff9dd" />
          <stop offset="100%" stopColor="#e4bd75" />
        </linearGradient>
        <linearGradient id={`${gradientPrefix}-cloud`} x1="24%" y1="8%" x2="63%" y2="100%">
          <stop stopColor="#ffffff" />
          <stop offset="46%" stopColor="#dce8ed" />
          <stop offset="100%" stopColor="#8ea8b7" />
        </linearGradient>
        <linearGradient id={`${gradientPrefix}-cloud-shadow`} x1="50%" y1="0%" x2="50%" y2="100%">
          <stop stopColor="#91a9b6" />
          <stop offset="100%" stopColor="#627c8a" />
        </linearGradient>
        <linearGradient id={`${gradientPrefix}-rain`} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop stopColor="#d8f7ff" />
          <stop offset="100%" stopColor="#54a9d6" />
        </linearGradient>
        <linearGradient id={`${gradientPrefix}-snow`} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop stopColor="#ffffff" />
          <stop offset="100%" stopColor="#a8e5fa" />
        </linearGradient>
        <linearGradient id={`${gradientPrefix}-bolt`} x1="30%" y1="0%" x2="80%" y2="100%">
          <stop stopColor="#fff5a2" />
          <stop offset="100%" stopColor="#ff9d35" />
        </linearGradient>
      </defs>
      {night ? (
        <path
          className="weather-icon__moon"
          d="M38.2 9.5a21.5 21.5 0 1 0 16.3 35.5A23.2 23.2 0 0 1 38.2 9.5Z"
          fill={`url(#${gradientPrefix}-moon)`}
        />
      ) : !cloudy || partlyCloudy ? (
        <g className="weather-icon__sun" stroke="currentColor" strokeLinecap="round">
          <circle cx="34" cy="25" r="10" fill={`url(#${gradientPrefix}-sun)`} stroke="none" />
          {!cloudy && !partlyCloudy && (
            <path strokeWidth="1.8" d="M34 8v4m0 26v4m17-17h-4m-26 0h-4m29-12-3 3m-18 18-3 3m24 0-3-3M23 14l-3-3" />
          )}
        </g>
      ) : null}
      {partlyCloudy && (
        <g className="weather-icon__cloud">
          <path d="M15 46h34a9 9 0 0 0 0-18h-1a15 15 0 0 0-29-2 10 10 0 0 0-4 20Z" fill={`url(#${gradientPrefix}-cloud-shadow)`} transform="translate(0 2)" />
          <path d="M15 44h34a9 9 0 0 0 0-18h-1a15 15 0 0 0-29-2 10 10 0 0 0-4 20Z" fill={`url(#${gradientPrefix}-cloud)`} />
          <path d="M20 36c1-7 7-11 14-11 5 0 9 2 11 6" fill="none" stroke="#fff" strokeLinecap="round" strokeOpacity=".72" strokeWidth="2" />
        </g>
      )}
      {cloudy && (
        <g className="weather-icon__cloud">
          <path d="M13 44h37a10 10 0 0 0 0-20h-1a16 16 0 0 0-31-2 11 11 0 0 0-5 22Z" fill={`url(#${gradientPrefix}-cloud-shadow)`} transform="translate(0 2)" />
          <path d="M13 42h37a10 10 0 0 0 0-20h-1a16 16 0 0 0-31-2 11 11 0 0 0-5 22Z" fill={`url(#${gradientPrefix}-cloud)`} />
          <path d="M18 33c1-7 7-12 15-12 5 0 9 2 12 6" fill="none" stroke="#fff" strokeLinecap="round" strokeOpacity=".74" strokeWidth="2" />
        </g>
      )}
      {rainy && (
        <g className="weather-icon__rain">
          <path d="M21 47c0 2-2 5-3 5s-2-1-2-2c0-2 2-5 3-6 1 1 2 2 2 3Z" fill={`url(#${gradientPrefix}-rain)`} />
          <path d="M33 48c0 2-2 5-3 5s-2-1-2-2c0-2 2-5 3-6 1 1 2 2 2 3Z" fill={`url(#${gradientPrefix}-rain)`} />
          <path d="M45 47c0 2-2 5-3 5s-2-1-2-2c0-2 2-5 3-6 1 1 2 2 2 3Z" fill={`url(#${gradientPrefix}-rain)`} />
        </g>
      )}
      {snowy && (
        <g className="weather-icon__snow" stroke={`url(#${gradientPrefix}-snow)`} strokeLinecap="round" strokeWidth="1.4">
          <path d="M21 49v7m-3.1-5.3 6.2 3.6m0-3.6-6.2 3.6m18-5.3v7m-3.1-5.3 6.2 3.6m0-3.6-6.2 3.6m18-5.3v7m-3.1-5.3 6.2 3.6m0-3.6-6.2 3.6" />
        </g>
      )}
      {storm && (
        <path className="weather-icon__lightning" d="m35 42-8 11h7l-3 9 11-14h-7l3-6Z" fill={`url(#${gradientPrefix}-bolt)`} />
      )}
      {foggy && (
        <g className="weather-icon__fog" stroke="currentColor" strokeLinecap="round" strokeWidth="2">
          <path d="M15 49h34M20 54h24" />
        </g>
      )}
    </svg>
  );
}

export default function CurrentWeather({ weather, loading, onSelectCity }) {
  return (
    <section
      className={`sidebar-current${weather ? "" : " is-empty"}`}
      id="current-conditions"
      aria-label="Current weather"
      aria-busy={loading}
    >
      {weather ? (
        <>
          <div className="sidebar-condition">
            <div className="sidebar-temperature" id="temperature">
              <span aria-label={`${Math.round(weather.temperature)} degrees Celsius`}>
                {Math.round(weather.temperature)}
              </span>
              <span className="temperature-unit" aria-hidden="true">°</span>
            </div>
            <p className="sidebar-condition-name" id="condition">{weather.description}</p>
            <div className="sidebar-feels">
              Feels like <strong>{Math.round(weather.feelsLike)}°</strong>
            </div>
            <WeatherImage
              className="sidebar-weather-icon"
              icon={weather.icon}
              description={weather.description}
            />
          </div>
          <dl className="sidebar-metrics" aria-label="Weather details">
            <div className="sidebar-metric">
              <dt>Humidity</dt>
              <dd>{Math.round(weather.humidity)}%</dd>
            </div>
            <div className="sidebar-metric">
              <dt>Wind</dt>
              <dd>{(weather.windSpeed * 3.6).toFixed(1)} km/h</dd>
            </div>
            <div className="sidebar-metric">
              <dt>Pressure</dt>
              <dd>{weather.pressure} hPa</dd>
            </div>
          </dl>
        </>
      ) : (
        <div className="sidebar-empty">
          <p>Search a city to see local conditions.</p>
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
    </section>
  );
}

export { WeatherImage };
