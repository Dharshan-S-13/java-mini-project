const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080").replace(/\/+$/, "");

async function request(path, city) {
  const res = await fetch(`${API_BASE_URL}${path}?city=${encodeURIComponent(city)}`);
  if (!res.ok) throw new Error("Backend error");
  return res.json();
}

export function getWeather(city) { return request('/api/weather', city); }
export function getForecast(city) { return request('/api/forecast', city); }
