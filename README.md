# 🌦️ SkyCast – Real-Time Weather Application Using Java Stream API

SkyCast is a web-based weather application built using Java that displays current conditions, an hourly outlook, and a five-day forecast for a searched city. It uses the OpenWeatherMap API to retrieve weather data and serves a responsive browser interface from a lightweight Java web server.

## ✨ Features

* 🌍 **City-Based Weather Search** – Search for weather information by city name.
* 🌡️ **Current Weather** – View temperature, feels-like temperature, humidity, wind speed, and atmospheric pressure.
* 🕒 **Today's Forecast** – View available three-hour temperature forecasts.
* 📅 **Five-Day Forecast** – View daily high and low temperatures and precipitation chances.
* ☁️ **Weather Icons** – Display weather conditions with corresponding icons.
* 📱 **Responsive Interface** – Browse a clear dashboard on desktop or mobile, with light and dark appearances.
* 🎙️ **Voice Weather Assistant** – Search by voice in a supported browser or use Windows speech recognition in the terminal, with spoken weather summaries.
* 💬 **Local AI Chat** – Chat with Qwen 3.5 through Ollama running on the same device as the website.
* 🔐 **Environment-Based API Keys** – Keep API keys outside the source code.

## 🛠️ Technologies Used

* Java 17
* Java Stream API
* Java HTTP Client
* Java `HttpServer`
* HTML5
* CSS3
* JavaScript
* OpenWeatherMap API
* Visual Studio Code
* Git and GitHub

## 📋 Prerequisites

Before running the application, make sure you have:

* Java Development Kit (JDK) 17
* Visual Studio Code or another Java-compatible editor
* An OpenWeatherMap API key
* [Ollama](https://ollama.com/) with the `qwen3.5:latest` model for the local chat assistant
* Internet access

## 📁 Project Structure

```text
java-mini-project/
├── src/
│   ├── Main.java
│   ├── Weather.java
│   ├── WeatherService.java
│   ├── Forecast.java
│   ├── WeatherWebServer.java
│   ├── OllamaService.java
│   ├── index.html
│   ├── styles.css
│   └── app.js
└── README.md
```

## 🔑 API Configuration

### 1. OpenWeatherMap API

Obtain an API key from [OpenWeatherMap](https://openweathermap.org/api).

Create a `.env` file in the project root (next to this README) with:

```text
OPENWEATHER_API_KEY=YOUR_OPENWEATHER_API_KEY
```

Replace the placeholder with your actual key. `.env` is excluded by `.gitignore`; never commit API keys to GitHub. The application checks the `OPENWEATHER_API_KEY` environment variable first, then reads the ignored project-root `.env` file.

Alternatively, configure the key in PowerShell for the current session:

```powershell
$env:OPENWEATHER_API_KEY = "YOUR_OPENWEATHER_API_KEY"
```

## ▶️ How to Run the Project

### Step 1: Clone the Repository

```powershell
git clone https://github.com/balajiads/java-mini-project.git
```

### Step 2: Open the Project

```powershell
cd java-mini-project
```

Open this folder in Visual Studio Code.

### Step 3: Configure API Keys

Set the OpenWeatherMap API key in the same PowerShell terminal that will run the application.

### Step 4: Compile and start the browser app with Java 17

From PowerShell, run the following commands (the project folder is named `java-mini-project`; Windows paths are case-insensitive):

```powershell
cd "D:\Git Hub Projects\Java Mini Project\Java-mini-project\src"
javac *.java
java --add-modules jdk.httpserver -cp . WeatherWebServer
```

Then open **http://localhost:8080**. Keep this terminal open while using the dashboard.

### Step 5: Start Ollama for local chat

Make sure the Qwen model is available:

```powershell
ollama list
```

If needed, download it with `ollama pull qwen3.5:latest`. Ollama must be running and listening on its default address, `http://localhost:11434`. If it is not already running, start it in another terminal with `ollama serve`.

Ollama must be running and listening on its default address, `http://127.0.0.1:11434`. If it is not already running, start it in another terminal with `ollama serve`. The server uses `qwen3.5:latest` by default. To use a different installed Ollama model, set `OLLAMA_MODEL` in the same terminal that starts the Java web server.

### Step 6: Run the Java Terminal Weather App

```powershell
java Main
```

Enter a city name when prompted. The app prints current conditions, temperature, humidity, and wind speed.

Press Enter at the city prompt to speak a city or ask for its weather. The terminal voice assistant uses Windows PowerShell and the built-in `System.Speech` service. A microphone and an installed Windows speech recognizer are required; if voice input is unavailable, type a city when prompted.

Run both commands from the `src` directory. No build tool or external JAR is needed.

Use the **Ask SkyCast** panel to chat with your local Qwen model. After weather loads successfully, SkyCast includes the current conditions and forecast in the chat context, so Qwen can answer questions about the displayed city. Chat messages go through the Java server to Ollama and are not sent to a hosted AI service. For this setup, open the website on the same device where Ollama is running; a public deployment cannot directly access Ollama on your personal computer.

In a browser that supports the Web Speech API, select **Speak**, allow microphone access, and say a city or a request such as “weather in Chennai.” The dashboard reads the current conditions and the next forecast aloud. Browser voice search works on localhost and may require a secure connection and microphone permission.

## 🌦️ Weather API

The terminal app requests current conditions directly from OpenWeatherMap. The browser server also exposes `/api/weather?city=Chennai` and `/api/forecast?city=Chennai`. Both use `OPENWEATHER_API_KEY`; the key stays on the server and is not sent to the browser.

The browser server exposes `POST /api/chat` for the local assistant. It connects to Ollama at `http://127.0.0.1:11434/api/chat`; set `OLLAMA_MODEL` if your installed model uses a different tag.

## ⚠️ Troubleshooting

### Port 8080 Is Already in Use

Check the process using port 8080:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

Check the process ID shown under `OwningProcess`:

```powershell
Get-Process -Id YOUR_PID
```

If you confirm it is your previous Java weather server, stop it:

```powershell
Stop-Process -Id YOUR_PID -Force
```

Then start the application again.

### Weather Data Is Not Loading

* Verify that `OPENWEATHER_API_KEY` is configured.
* Check your internet connection.
* Check the server terminal for error messages.
* Confirm that the OpenWeatherMap API key has access to the requested endpoints.
* Test the weather endpoint directly:

```text
http://localhost:8080/api/weather?city=Chennai
```

### Local Chat Is Not Responding

* Make sure Ollama is running (`ollama serve` if it is not already running).
* Run `ollama list` and confirm that `qwen3.5:latest` is installed, or set `OLLAMA_MODEL` to an installed model tag before starting the Java server.
* The browser, Java server, and Ollama must be on the same device for this local setup.

## 🔒 Security

* Do not hardcode API keys in Java or HTML files.
* Do not upload API keys to GitHub.
* Use environment variables to configure credentials.
* If a key is accidentally exposed, revoke it and generate a replacement.

## 🚀 Future Enhancements

* Automatic location-based weather
* Weather alerts and notifications
* Weather data visualization and charts
* Unit conversion between Celsius and Fahrenheit
* More detailed forecast analytics
* Automated tests

## 👨‍💻 Author

**Balaji**

GitHub: [balajiads](https://github.com/balajiads)

## 📄 License

This project is intended for learning and educational purposes. Add a license file if you want to specify the terms under which others may use or distribute the project.
