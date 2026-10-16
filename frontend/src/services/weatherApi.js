const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080")
  .replace(/\/+$/, "");

async function requestWeather(path, city, signal) {
  const url = `${API_BASE_URL}${path}?city=${encodeURIComponent(city)}`;
  let response;
  try {
    response = await fetch(url, {
      signal,
      headers: { Accept: "application/json" },
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new Error("Could not reach the SkyCast backend. Check that it is running.");
  }
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(payload.error || "Weather data could not be loaded.");
  }
  return payload;
}

export function getWeather(city, signal) {
  return requestWeather("/api/weather", city, signal);
}

export function getForecast(city, signal) {
  return requestWeather("/api/forecast", city, signal);
}
