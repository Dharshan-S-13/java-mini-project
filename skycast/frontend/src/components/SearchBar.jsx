import { useEffect, useRef, useState } from "react";

export function normalizeCityRequest(request) {
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

export default function SearchBar({ city, setCity, onSearch, onVoiceSearch, disabled }) {
  const [supported, setSupported] = useState(false);
  const [listening, setListening] = useState(false);
  const recognitionRef = useRef(null);

  useEffect(() => {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Recognition) return undefined;

    const recognition = new Recognition();
    recognition.lang = document.documentElement.lang || navigator.language || "en-US";
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    recognition.onstart = () => {
      setListening(true);
      onVoiceSearch(null, null);
    };
    recognition.onend = () => setListening(false);
    recognition.onerror = (event) => {
      setListening(false);
      const messages = {
        "audio-capture": "No microphone was found. Check your microphone and try again.",
        "no-speech": "No speech was detected. Try again or type a city.",
        "not-allowed": "Microphone access was blocked. Allow access in your browser settings.",
        "service-not-allowed": "Voice search is unavailable. Try again or type a city.",
      };
      onVoiceSearch(null, messages[event.error] || "Voice search stopped unexpectedly.");
    };
    recognition.onresult = (event) => {
      const request = event.results[0]?.[0]?.transcript || "";
      const normalizedCity = normalizeCityRequest(request);
      if (!normalizedCity) {
        onVoiceSearch(null, "No city was heard. Try again or type a city.");
        return;
      }
      setCity(normalizedCity);
      onVoiceSearch(normalizedCity);
    };
    recognitionRef.current = recognition;
    setSupported(true);

    return () => {
      recognition.abort();
      recognitionRef.current = null;
    };
  }, [onVoiceSearch, setCity]);

  function submit(event) {
    event.preventDefault();
    const normalizedCity = normalizeCityRequest(city);
    if (!normalizedCity) {
      onVoiceSearch(null, "Enter a city name to search.");
      return;
    }
    setCity(normalizedCity);
    onSearch(normalizedCity);
  }

  function toggleListening() {
    const recognition = recognitionRef.current;
    if (!recognition) return;
    if (listening) {
      recognition.stop();
      return;
    }
    try {
      recognition.start();
    } catch {
      onVoiceSearch(null, "Voice search could not start. Try again or type a city.");
    }
  }

  return (
    <form className="search-form top-search" onSubmit={submit} role="search">
      <label className="visually-hidden" htmlFor="city-input">Search by city</label>
      <span className="search-glyph" aria-hidden="true" />
      <input
        id="city-input"
        name="city"
        type="search"
        maxLength="100"
        placeholder="Search a city"
        autoComplete="off"
        value={city}
        onChange={(event) => setCity(event.target.value)}
        aria-describedby="search-help"
      />
      {supported && (
        <button
          className={`voice-button${listening ? " listening" : ""}`}
          type="button"
          aria-label={listening ? "Stop voice search" : "Search by voice"}
          aria-pressed={listening}
          disabled={disabled}
          onClick={toggleListening}
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="9" y="3" width="6" height="12" rx="3" />
            <path d="M5.5 11a6.5 6.5 0 0 0 13 0M12 18v3m-4 0h8" />
          </svg>
          <span>{listening ? "Stop" : "Speak"}</span>
        </button>
      )}
      <button className="search-button" id="search-button" type="submit" disabled={disabled}>
        {disabled ? "Loading" : "Search"}
      </button>
      <span className="visually-hidden" id="search-help">
        Enter a city name or use voice search to hear its weather.
      </span>
    </form>
  );
}
