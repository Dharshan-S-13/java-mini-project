# SkyCast

SkyCast is a weather dashboard with a React + Vite frontend and a Java 17 Spring Boot backend. The backend retrieves weather information from OpenWeatherMap and routes AI chat through a local Ollama installation. The browser never receives the OpenWeatherMap API key.

## Architecture

```text
React + Vite (http://localhost:5173)
    | HTTP/JSON
Spring Boot API (http://localhost:8080)
    |                  |
OpenWeatherMap      Ollama (http://127.0.0.1:11434)
                       Qwen 3.5
```

The UI keeps the original SkyCast visual design, weather details, hourly and five-day forecast, appearance toggle, voice search, text-to-speech, and chat. Browser speech uses the built-in Web Speech API; no paid speech service is used.

## Project structure

```text
java-mini-project/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/skycast/
│       ├── SkyCastApplication.java
│       ├── config/
│       ├── controller/
│       ├── model/
│       └── service/
├── frontend/
│   ├── .env.example
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── components/
│       ├── services/
│       ├── styles/skycast.css
│       ├── App.jsx
│       └── main.jsx
├── src/                  # Original Java and web app retained
├── .env                  # Local secrets; ignored by Git
└── README.md
```

## Requirements

- Java Development Kit (JDK) 17
- Apache Maven 3.6.3 or later
- Node.js 18 or later and npm
- An OpenWeatherMap API key
- Ollama installed locally and the `qwen3.5:latest` model (for chat)

Install Java, Node.js, Maven, and Ollama using their official installers for Windows. Verify the tools in PowerShell:

```powershell
java -version
mvn -version
node --version
npm --version
ollama list
```

## Environment variables

The backend reads `OPENWEATHER_API_KEY` from the process environment or the project-root `.env` file. The frontend does not receive or read that secret. The existing `.env` file is excluded from Git; keep it private and do not paste its contents into issue reports.

Example `.env` in the project root:

```text
OPENWEATHER_API_KEY=your_openweathermap_api_key
```

Alternatively, set it in the PowerShell session used to launch the backend:

```powershell
$env:OPENWEATHER_API_KEY = "your_openweathermap_api_key"
```

Chat defaults to `qwen3.5:latest` at `http://127.0.0.1:11434`. Optional overrides for the backend process:

```powershell
$env:OLLAMA_MODEL = "qwen3.5:latest"
$env:OLLAMA_URL = "http://127.0.0.1:11434"
```

The frontend API base URL defaults to `http://localhost:8080`. To change it for Vite development, copy `frontend/.env.example` to `frontend/.env.local` and update `VITE_API_BASE_URL`.

## Ollama and Qwen setup

Check whether Ollama is running and the model is installed:

```powershell
ollama list
```

If the model is missing, install it:

```powershell
ollama pull qwen3.5:latest
```

Ollama Desktop normally runs the local service automatically. **Do not run `ollama serve` if it is already running.** Start `ollama serve` in a separate terminal only if Ollama is not running.

## Run the project

Open three PowerShell terminals in the project root.

**Terminal 1 — Spring Boot backend**

```powershell
cd backend
mvn spring-boot:run
```

The backend starts at `http://localhost:8080`. It finds the root `.env` when launched from the `backend` directory.

**Terminal 2 — React frontend**

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The browser voice input requires microphone permission and a browser that supports `SpeechRecognition` or `webkitSpeechRecognition`. Voice responses use `window.speechSynthesis`.

**Terminal 3 — Ollama status**

```powershell
ollama list
```

Confirm that `qwen3.5:latest` is listed. Do not launch a second Ollama service when it is already running. Chat needs Ollama; weather search does not.

## API endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/weather?city=Chennai` | Current weather |
| `GET` | `/api/forecast?city=Chennai` | Three-hour forecast entries used for hourly and five-day views |
| `POST` | `/api/chat` | Local Qwen chat with current weather context when a city is supplied |

Example chat request:

```json
{
  "message": "Should I carry an umbrella?",
  "city": "Chennai",
  "history": []
}
```

Weather and chat errors return JSON with an `error` message and HTTP status. The backend does not return the OpenWeatherMap API key.

## Troubleshooting

- **Backend command not found:** install Maven and JDK 17, reopen PowerShell, and verify `mvn -version` and `java -version`.
- **Weather says it is not configured:** set `OPENWEATHER_API_KEY` or check the root `.env` entry, then restart Spring Boot.
- **City not found:** check spelling and try a city name supported by OpenWeatherMap.
- **Frontend cannot reach the backend:** confirm Spring Boot is listening on port 8080 and Vite is on port 5173. The backend CORS policy permits the local Vite origin.
- **Ollama unavailable:** check `ollama list` and ensure the local Ollama application/service is running. Do not start a duplicate server.
- **Qwen model missing:** run `ollama pull qwen3.5:latest`, or set `OLLAMA_MODEL` to a model listed by `ollama list`.
- **Voice search unavailable:** use a browser with Web Speech API support, allow microphone access, or type the city instead. Speech synthesis availability varies by browser and operating system.
- **Port in use:** inspect the listener before stopping anything:

  ```powershell
  Get-NetTCPConnection -LocalPort 8080 -State Listen
  Get-NetTCPConnection -LocalPort 5173 -State Listen
  ```

## Original application

The original Java command-line app and static web app under `src/` are intentionally retained. The separated `backend/` and `frontend/` implementation can be built and run independently; the old files are not required by the new app.
