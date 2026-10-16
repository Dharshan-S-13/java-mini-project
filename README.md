# VaanilAI

VaanilAI is a weather dashboard built with React and Vite, backed by a Java 17 Spring Boot API built with Gradle. The backend retrieves weather from OpenWeatherMap and sends AI chat requests to a local Ollama Qwen 3.5 model. The browser communicates only with Spring Boot; it never receives the OpenWeatherMap key or calls Ollama directly.

## Architecture

```text
React + Vite (http://localhost:5173)
    |
    | HTTP/JSON
    v
Spring Boot + Java 17 + Gradle (http://localhost:8080)
    |                                  |
    v                                  v
OpenWeatherMap                   Ollama + Qwen 3.5
                                  (http://127.0.0.1:11434)
```

## Requirements

- Java Development Kit (JDK) 17
- Node.js 18 or later and npm
- Ollama, with `qwen3.5:latest` installed for AI chat
- An OpenWeatherMap API key for live weather

The Gradle Wrapper is included, so a global Gradle installation is not required. Verify the installed tools:

```powershell
java -version
node --version
npm --version
ollama list
```

## Project structure

```text
java-mini-project/
├── .env                         # Local backend secrets; ignored by Git
├── skycast/
│   ├── backend/
│   │   ├── gradlew
│   │   ├── gradlew.bat
│   │   ├── gradle/wrapper/
│   │   ├── build.gradle
│   │   ├── settings.gradle
│   │   └── src/main/
│   └── frontend/
│       ├── .env.example
│       ├── package.json
│       ├── index.html
│       └── src/
│           ├── components/
│           ├── services/
│           ├── styles/
│           ├── App.jsx
│           └── main.jsx
├── backend/                     # Earlier Gradle build retained
├── frontend/                    # Previous frontend retained
└── src/                         # Original Java/web application retained
```

`skycast/backend` and `skycast/frontend` are the active VaanilAI application. The earlier project files are retained separately and are not needed to run the active app.

## Environment variables

The backend reads `OPENWEATHER_API_KEY` from its process environment or from the repository-root `.env` file. Keep the key on the backend only; do not put it in a frontend `.env` file or commit it.

Create or update `.env` in the repository root:

```text
OPENWEATHER_API_KEY=your_openweathermap_api_key
```

Alternatively, set the variable in the PowerShell session used to start the backend:

```powershell
$env:OPENWEATHER_API_KEY = "your_openweathermap_api_key"
```

Ollama defaults to `http://127.0.0.1:11434` and `qwen3.5:latest`. Optional backend-process overrides are `OLLAMA_URL` and `OLLAMA_MODEL`.

The frontend uses `VITE_API_BASE_URL=http://localhost:8080`. This value is not a secret. The provided `skycast/frontend/.env.example` contains only this setting; copy it to `skycast/frontend/.env.local` to override the API URL for local development.

## Backend setup (Terminal 1)

From the repository root:

```powershell
cd skycast/backend
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

The backend listens on `http://localhost:8080`. Its Gradle Wrapper downloads the configured Gradle distribution on first use. The root `.env` is discovered when the backend runs from `skycast/backend`.

## Frontend setup (Terminal 2)

Open another PowerShell terminal from the repository root:

```powershell
cd skycast/frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Create the optional `.env.local` only when changing `VITE_API_BASE_URL`; do not put backend credentials there.

## Ollama setup (Terminal 3)

In a third terminal, check the Ollama service and installed models:

```powershell
ollama list
```

If `qwen3.5:latest` is missing, install it:

```powershell
ollama pull qwen3.5:latest
```

Ollama Desktop normally starts its local service automatically. Do not run `ollama serve` if Ollama is already running. Chat requires Ollama; weather search does not.

## API endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/weather?city=Chennai` | Current conditions |
| `GET` | `/api/forecast?city=Chennai` | Three-hour forecast entries used by the hourly and five-day views |
| `POST` | `/api/chat` | Ollama chat, optionally with verified weather context |

Example chat request:

```json
{
  "message": "Should I carry an umbrella?",
  "city": "Chennai",
  "history": []
}
```

The APIs return JSON errors with an appropriate HTTP status. The OpenWeatherMap key is never included in API responses.

## Troubleshooting

- **Java version or Gradle build error:** install JDK 17 and run `.\gradlew.bat clean build` from `skycast/backend`. A global Gradle install is not required.
- **Weather is not configured:** set `OPENWEATHER_API_KEY` in the repository-root `.env` or in the backend process environment, then restart Spring Boot.
- **City not found:** check the spelling or try a city supported by OpenWeatherMap.
- **Weather provider/network error:** check internet connectivity, the OpenWeatherMap key, and the provider's availability. The frontend displays the backend's returned error message.
- **Frontend cannot reach the backend:** confirm Spring Boot is listening on port 8080 and the Vite origin is `http://localhost:5173`, which is allowed by the backend CORS configuration.
- **Ollama unavailable:** check `ollama list` and confirm the existing local Ollama service is running. Do not start a duplicate server.
- **Qwen model missing:** run `ollama pull qwen3.5:latest`, or set `OLLAMA_MODEL` to an installed model.
- **Voice input unavailable:** allow microphone access and use a browser supporting `SpeechRecognition` or `webkitSpeechRecognition`; typed search remains available.
- **Text-to-speech unavailable:** use a browser/OS that provides `window.speechSynthesis`; weather data and chat still work.
- **Port already in use:** inspect the listener before stopping any process:

  ```powershell
  Get-NetTCPConnection -LocalPort 8080 -State Listen
  Get-NetTCPConnection -LocalPort 5173 -State Listen
  ```
