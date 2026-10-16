const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080")
  .replace(/\/+$/, "");

export async function sendChat(message, city, history) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}/api/chat`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({ message, city, history }),
    });
  } catch {
    throw new Error("Could not reach the SkyCast backend. Check that it is running.");
  }

  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(payload.error || "The SkyCast assistant could not respond.");
  }
  if (typeof payload.message !== "string" || !payload.message.trim()) {
    throw new Error("Ollama returned an unexpected or empty response.");
  }
  return payload.message.trim();
}
