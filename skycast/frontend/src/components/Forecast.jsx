import { WeatherImage } from "./CurrentWeather.jsx";

function cityDateKey(timestamp, offset) {
  return new Date((timestamp + offset) * 1000).toISOString().slice(0, 10);
}

function cityDate(timestamp, offset, options) {
  return new Intl.DateTimeFormat(undefined, {
    ...options,
    timeZone: "UTC",
  }).format(new Date((timestamp + offset) * 1000));
}

function dailyForecast(forecast, weather) {
  const offset = weather.timezone || 0;
  const today = cityDateKey(weather.timestamp, offset);
  const grouped = new Map();
  for (const item of forecast.items) {
    const key = cityDateKey(item.timestamp, offset);
    if (!grouped.has(key)) grouped.set(key, []);
    grouped.get(key).push(item);
  }

  return [...grouped.entries()].slice(0, 5).map(([date, items]) => {
    const midday = items.find((item) => {
      const hour = new Date((item.timestamp + offset) * 1000).getUTCHours();
      return hour >= 11 && hour <= 14;
    }) || items[Math.floor(items.length / 2)];
    return {
      date,
      label: date === today
        ? "Today"
        : cityDate(Date.parse(`${date}T00:00:00Z`) / 1000, 0, { weekday: "long" }),
      high: Math.round(Math.max(...items.map((item) => item.maxTemperature))),
      low: Math.round(Math.min(...items.map((item) => item.minTemperature))),
      precipitation: Math.round(Math.max(...items.map((item) => item.precipitation)) * 100),
      condition: midday,
    };
  });
}

function chartPoints(values) {
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = Math.max(max - min, 1);

  return values.map((value, index) => ({
    x: 100 + (800 * index) / Math.max(values.length - 1, 1),
    y: 88 - ((value - min) / span) * 48,
  }));
}

function chartPath(points) {
  return points.reduce((path, point, index) => {
    if (index === 0) return `M ${point.x} ${point.y}`;
    const previous = points[index - 1];
    const middleX = (previous.x + point.x) / 2;
    return `${path} Q ${middleX} ${previous.y} ${middleX} ${(previous.y + point.y) / 2} T ${point.x} ${point.y}`;
  }, "");
}

export default function Forecast({ weather, forecast }) {
  if (!weather || !forecast) {
    return (
      <section className="forecast-section empty-forecast" id="forecast" aria-label="Forecast preview">
        <h2>Forecast</h2>
        <p>Your hourly and five-day forecast will appear here after you search.</p>
      </section>
    );
  }

  const offset = weather.timezone || 0;
  const today = cityDateKey(weather.timestamp, offset);
  const hourly = forecast.items
    .filter((item) => cityDateKey(item.timestamp, offset) === today && item.timestamp > weather.timestamp)
    .slice(0, 6);
  const days = dailyForecast(forecast, weather);
  const points = days.length > 1 ? chartPoints(days.map((day) => day.high)) : [];

  return (
    <div className="forecast-stack">
      {hourly.length > 0 && (
        <section className="forecast-section hourly-section" id="hourly" aria-labelledby="hourly-title">
          <div className="section-heading">
            <h2 id="hourly-title">The next hours</h2>
            <span>Today in {weather.city}</span>
          </div>
          <div className="hourly-list" id="hourly-list" aria-live="polite">
            {hourly.map((item) => (
              <div className="hour-item" key={item.timestamp}>
                <span className="hour-time">{cityDate(item.timestamp, offset, { hour: "numeric" })}</span>
                <WeatherImage icon={item.icon} description={item.description} />
                <span className="hour-temp">{Math.round(item.temperature)}°</span>
                <span className="hour-rain">{Math.round(item.precipitation * 100)}% rain</span>
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="forecast-section daily-section" id="forecast" aria-labelledby="forecast-title">
        <div className="section-heading">
          <h2 id="forecast-title">Five-day forecast</h2>
          <span id="forecast-location">High / low · °C</span>
        </div>
        {days.length > 0 ? (
          <>
            <div className="daily-list" id="daily-list" aria-live="polite">
              {days.map((day, index) => (
                <article
                  className="day-row"
                  key={day.date}
                  style={{ "--stagger": `${index * 80}ms` }}
                >
                  <span className="day-name">{day.label}</span>
                  <WeatherImage icon={day.condition.icon} description={day.condition.description} />
                  <span className="day-condition">{day.condition.description || "Forecast"}</span>
                  <span className="day-rain">{day.precipitation > 0 ? `${day.precipitation}% rain` : "Dry"}</span>
                  <span className="day-temperatures">
                    <strong>{day.high}°</strong><span>{day.low}°</span>
                  </span>
                </article>
              ))}
            </div>
            {points.length > 1 && (
              <svg
                className="forecast-chart"
                viewBox="0 0 1000 120"
                role="img"
                aria-label={`Daily high temperatures: ${days.map((day) => `${day.label} ${day.high} degrees`).join(", ")}`}
                preserveAspectRatio="none"
              >
                <path d={chartPath(points)} />
                {points.map((point, index) => (
                  <circle key={days[index].date} cx={point.x} cy={point.y} r="4" />
                ))}
              </svg>
            )}
          </>
        ) : (
          <div className="forecast-empty-state">
            <p>No forecast is available right now.</p>
            <p>Try another city or check again later.</p>
          </div>
        )}
      </section>
    </div>
  );
}
