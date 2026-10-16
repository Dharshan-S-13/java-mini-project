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

### Step 4: Compile with Java 17

```powershell
Set-Location .\src
javac *.java
```

### Step 5: Run the Java Terminal Weather App

```powershell
java Main
```

Enter a city name when prompted. The app prints current conditions, temperature, humidity, and wind speed.

Press Enter at the city prompt to speak a city or ask for its weather. The terminal voice assistant uses Windows PowerShell and the built-in `System.Speech` service. A microphone and an installed Windows speech recognizer are required; if voice input is unavailable, type a city when prompted.

Run both commands from the `src` directory. No build tool or external JAR is needed.

### Run the Browser Weather Dashboard

To start the existing browser UI after compiling, run:

```powershell
java --add-modules jdk.httpserver -cp . WeatherWebServer
```

Keep the terminal open and visit:

**http://localhost:8080**

In a browser that supports the Web Speech API, select **Speak**, allow microphone access, and say a city or a request such as “weather in Chennai.” The dashboard reads the current conditions and the next forecast aloud. Browser voice search works on localhost and may require a secure connection and microphone permission.

## 🌦️ Weather API

The terminal app requests current conditions directly from OpenWeatherMap. The browser server also exposes `/api/weather?city=Chennai` and `/api/forecast?city=Chennai`. Both use `OPENWEATHER_API_KEY`; the key stays on the server and is not sent to the browser.

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
