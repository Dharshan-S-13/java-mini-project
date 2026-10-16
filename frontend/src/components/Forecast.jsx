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

export default function Forecast({ weather, forecast }) {
  if (!weather || !forecast) {
    return (
      <section className="empty-forecast" id="forecast-preview" aria-label="Forecast preview">
        <p className="empty-forecast-title">A little more clarity for whatever’s next.</p>
        <p>Your hourly and five-day forecasts will appear here after you search.</p>
      </section>
    );
  }

  const offset = weather.timezone || 0;
  const today = cityDateKey(weather.timestamp, offset);
  const hourly = forecast.items
    .filter((item) => cityDateKey(item.timestamp, offset) === today && item.timestamp > weather.timestamp)
    .slice(0, 6);
  const days = dailyForecast(forecast, weather);

  return (
    <>
      {hourly.length > 0 && (
        <section className="forecast-section hourly-section" id="hourly" aria-labelledby="hourly-title">
          <div className="section-heading">
            <div><p className="eyebrow">The hours ahead</p><h2 id="hourly-title">Today, hour by hour</h2></div>
          </div>
          <div className="hourly-list" id="hourly-list" aria-live="polite">
            {hourly.map((item) => (
              <div className="hour-item" key={item.timestamp}>
                <span className="hour-time">{cityDate(item.timestamp, offset, { hour: "numeric" })}</span>
                <WeatherImage icon={item.icon} size="2x" />
                <span className="hour-temp">{Math.round(item.temperature)}°C</span>
              </div>
            ))}
          </div>
        </section>
      )}
      <section className="forecast-section daily-section" id="forecast" aria-labelledby="forecast-title">
        <div className="section-heading">
          <div><p className="eyebrow">Plan a little further ahead</p><h2 id="forecast-title">The next five days</h2></div>
          <p className="section-note" id="forecast-location">
            Forecast for {weather.city}. Temperatures in °C.
          </p>
        </div>
        {days.length > 0 ? (
          <div className="daily-list" id="daily-list" aria-live="polite">
            {days.map((day) => (
              <div className="day-row" key={day.date}>
                <div>
                  <span className="day-name">{day.label}</span>
                  <span className="day-date">{cityDate(Date.parse(`${day.date}T00:00:00Z`) / 1000, 0, { month: "short", day: "numeric" })}</span>
                </div>
                <div className="day-condition">
                  <WeatherImage icon={day.condition.icon} size="2x" />
                  <span>{day.condition.description || "Forecast"}</span>
                </div>
                <span className="day-rain">{day.precipitation > 0 ? `Rain ${day.precipitation}%` : "No rain"}</span>
                <div className="day-temperatures">
                  <span>{day.high}°</span><span>{day.low}°</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-forecast">
            <p className="empty-forecast-title">No forecast is available right now.</p>
            <p>Try another city or check again later.</p>
          </div>
        )}
      </section>
    </>
  );
}
